// Dr VPN anonymous usage stats (Cloudflare Worker + D1).
//   POST /ping    {"id","v","region","hop","gaming","connects","brand","model","android","server"}  from the app, once a day
//   GET  /badge   shields.io endpoint JSON: active installs in the last 30 days (public)
//   GET  /        the admin panel (login with ADMIN_USER / ADMIN_PASSWORD, secrets of the Worker)
//   GET  /stats   the panel's data as JSON (logged in only)
//   GET  /health  database health for the settings page (logged in only)
// Nothing about the person is kept: the request's IP address is never read or stored.
//
// D1's free plan reads at most 5 million rows a day; past that every query fails until midnight UTC,
// pings included. So the panel's numbers are computed in a few cheap queries and kept for
// STATS_TTL_MS, past days are rolled up once (table daily), each install's first day is kept in
// table installs, and the badge is recomputed every few hours.
import { loginPage, dashboardPage, settingsPage } from './pages.js';

const SESSION_DAYS = 30;
const STATS_TTL_MS = 15 * 60 * 1000;
/** A "refresh now" click recomputes only when the kept numbers are older than this. */
const FRESH_MIN_MS = 60 * 1000;
const BADGE_TTL_MS = 6 * 60 * 60 * 1000;
/**
 * Rows the panel may read in one UTC day (of D1's 5 million). Past it the panel shows its kept
 * numbers until midnight, so the app's pings always have room.
 */
const STATS_DAILY_READS = 2_000_000;

const html = (body, status = 200, headers = {}) =>
  new Response(body, { status, headers: { 'content-type': 'text/html; charset=utf-8', 'cache-control': 'no-store', ...headers } });

const b64url = (buf) => btoa(String.fromCharCode(...new Uint8Array(buf))).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
const fromB64url = (s) => Uint8Array.from(atob(s.replace(/-/g, '+').replace(/_/g, '/')), (c) => c.charCodeAt(0));

// ---------- Schema ----------

/** Columns pings has gained over time; a database made with an older schema.sql gets them added. */
const PING_COLUMNS = {
  brand: "TEXT NOT NULL DEFAULT '?'",
  model: "TEXT NOT NULL DEFAULT '?'",
  android: "TEXT NOT NULL DEFAULT '?'",
  country: "TEXT NOT NULL DEFAULT '??'",
  lat: 'REAL',
  lon: 'REAL',
  city: "TEXT NOT NULL DEFAULT '?'",
  server: "TEXT NOT NULL DEFAULT '?'",
};

let schemaReady = null;
/** Brings the database up to date, once per Worker instance. */
function ensureSchema(env) {
  if (!schemaReady) schemaReady = migrate(env).catch((e) => { schemaReady = null; throw e; });
  return schemaReady;
}

async function migrate(env) {
  await env.DB.batch([
    env.DB.prepare(`CREATE TABLE IF NOT EXISTS pings (day TEXT NOT NULL, id TEXT NOT NULL, version TEXT NOT NULL,
      region TEXT NOT NULL, hop TEXT NOT NULL, gaming INTEGER NOT NULL, connects INTEGER NOT NULL, PRIMARY KEY (day, id))`),
    env.DB.prepare('CREATE INDEX IF NOT EXISTS pings_day ON pings(day)'),
    env.DB.prepare('CREATE TABLE IF NOT EXISTS settings (key TEXT PRIMARY KEY, value TEXT NOT NULL)'),
    env.DB.prepare('CREATE TABLE IF NOT EXISTS installs (id TEXT PRIMARY KEY, first TEXT NOT NULL)'),
    env.DB.prepare('CREATE INDEX IF NOT EXISTS installs_first ON installs(first)'),
    env.DB.prepare('CREATE TABLE IF NOT EXISTS daily (day TEXT PRIMARY KEY, active INTEGER NOT NULL, connects INTEGER NOT NULL)'),
  ]);
  const have = new Set((await env.DB.prepare('PRAGMA table_info(pings)').all()).results.map((c) => c.name));
  for (const [name, type] of Object.entries(PING_COLUMNS)) {
    if (!have.has(name)) await env.DB.prepare(`ALTER TABLE pings ADD COLUMN ${name} ${type}`).run();
  }
  // The installs table came later: fill it once from the pings already there.
  if ((await dbGet(env, 'installs_filled')) !== '1') {
    await env.DB.prepare('INSERT OR IGNORE INTO installs (id, first) SELECT id, MIN(day) FROM pings GROUP BY id').run();
    await dbSet(env, 'installs_filled', '1');
  }
}

// A small key/value table for settings and kept results.
async function dbGet(env, key) {
  try { return (await env.DB.prepare('SELECT value FROM settings WHERE key = ?').bind(key).first())?.value ?? null; } catch { return null; }
}
async function dbSet(env, key, value) {
  await env.DB.prepare('INSERT INTO settings (key, value) VALUES (?, ?) ON CONFLICT (key) DO UPDATE SET value = excluded.value').bind(key, value).run();
}
const parse = (text) => { try { return text ? JSON.parse(text) : null; } catch { return null; } };

// ---------- Login ----------

// PBKDF2(SHA-256) so the stored password is never kept in the clear.
async function pbkdf2(password, saltB64) {
  const base = await crypto.subtle.importKey('raw', new TextEncoder().encode(password), 'PBKDF2', false, ['deriveBits']);
  const bits = await crypto.subtle.deriveBits({ name: 'PBKDF2', salt: fromB64url(saltB64), iterations: 120000, hash: 'SHA-256' }, base, 256);
  return b64url(bits);
}
/** The password set in the panel (salt.hash), or null — then the ADMIN_PASSWORD secret is used. */
const storedPassword = (env) => dbGet(env, 'password');
async function setPassword(env, password) {
  const salt = b64url(crypto.getRandomValues(new Uint8Array(16)));
  await dbSet(env, 'password', salt + '.' + (await pbkdf2(password, salt)));
}
async function verifyPassword(env, password) {
  const stored = await storedPassword(env);
  if (stored) { const [salt, hash] = stored.split('.'); return same(await pbkdf2(password, salt), hash); }
  return !!env.ADMIN_PASSWORD && same(password, env.ADMIN_PASSWORD);
}
/** The signing secret for sessions; changing the password ends every open session. */
const sessionSecret = async (env) => (await storedPassword(env)) || env.ADMIN_PASSWORD || '';
const loginEnabled = async (env) => !!(env.ADMIN_USER && (await sessionSecret(env)));

async function sign(env, text) {
  const key = await crypto.subtle.importKey('raw', new TextEncoder().encode((await sessionSecret(env)) + '|' + env.ADMIN_USER),
    { name: 'HMAC', hash: 'SHA-256' }, false, ['sign']);
  return b64url(await crypto.subtle.sign('HMAC', key, new TextEncoder().encode(text)));
}

/** "expiry.signature": valid until expiry; a new password ends every session. */
async function newSession(env) {
  const expiry = String(Date.now() + SESSION_DAYS * 86400000);
  return `${expiry}.${await sign(env, expiry)}`;
}
const sessionCookie = async (env) => `drvpn_admin=${await newSession(env)}; Path=/; Max-Age=${SESSION_DAYS * 86400}; HttpOnly; Secure; SameSite=Strict`;

async function loggedIn(request, env) {
  if (!(await loginEnabled(env))) return false;
  const cookie = (request.headers.get('cookie') || '').match(/(?:^|;\s*)drvpn_admin=([^;]+)/);
  if (!cookie) return false;
  const [expiry, signature] = cookie[1].split('.');
  if (!expiry || !signature || Number(expiry) < Date.now()) return false;
  return same(signature, await sign(env, expiry));
}

/** Compares without leaking where the strings differ through timing. */
function same(a, b) {
  a = String(a); b = String(b);
  let diff = a.length ^ b.length;
  for (let i = 0; i < Math.max(a.length, b.length); i++) diff |= (a.charCodeAt(i) || 0) ^ (b.charCodeAt(i) || 0);
  return diff === 0;
}

const json = (body, status = 200, extra = {}) =>
  new Response(JSON.stringify(body), {
    status,
    headers: { 'content-type': 'application/json', 'access-control-allow-origin': '*', 'cache-control': 'no-store', ...extra },
  });

// Device names have spaces and brackets ("Galaxy A54 5G"); keep letters, digits and a few signs.
const cleanName = (value, max) => String(value ?? '').replace(/[^\p{L}\p{N} ._+()\-]/gu, '').trim().slice(0, max);
const clean = (value, max) => String(value ?? '').replace(/[^\w.\-]/g, '').slice(0, max);
const day = (offset = 0) => new Date(Date.now() - offset * 86400000).toISOString().slice(0, 10);
const short = (n) => (n >= 1e6 ? (n / 1e6).toFixed(1) + 'M' : n >= 1e3 ? (n / 1e3).toFixed(1) + 'K' : String(n)).replace('.0', '');

/** What went wrong, in words the owner can act on. */
function describe(e) {
  const m = String(e?.message || e || '');
  if (/limit|exceeded|quota|too many/i.test(m)) {
    return 'سقف روزانهٔ رایگان پایگاه‌داده (D1) تمام شده است. تا ساعت ۰۰:۰۰ UTC (۳:۳۰ بامداد تهران) صبر کنید. ' + m.slice(0, 160);
  }
  if (/no such table|no such column/i.test(m)) return 'جدول‌های پایگاه‌داده کامل نیستند: ' + m.slice(0, 160);
  return m.slice(0, 200) || 'خطای ناشناخته';
}

// ---------- Ping ----------

async function ping(request, env) {
  let body;
  try {
    body = await request.json();
  } catch {
    return json({ ok: false }, 400);
  }
  const id = clean(body.id, 40);
  if (id.length < 16) return json({ ok: false }, 400);
  // Approximate location from the request IP, added by Cloudflare. Country level, and lat/lon
  // rounded to ~11 km, for the world map. No street, no address, no GPS.
  const cf = request.cf || {};
  const country = clean(cf.country, 2).toUpperCase() || '??';
  const city = cleanName(cf.city, 40) || '?';
  const round1 = (v) => (v == null || v === '' || isNaN(parseFloat(v)) ? null : Math.round(parseFloat(v) * 10) / 10);
  const lat = round1(cf.latitude), lon = round1(cf.longitude);
  const today = day();
  try {
    await ensureSchema(env);
    await env.DB.batch([
      env.DB.prepare(
        `INSERT INTO pings (day, id, version, region, hop, gaming, connects, brand, model, android, country, lat, lon, city, server)
           VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
         ON CONFLICT (day, id) DO UPDATE SET version = excluded.version, region = excluded.region,
           hop = excluded.hop, gaming = excluded.gaming, connects = MAX(pings.connects, excluded.connects),
           brand = excluded.brand, model = excluded.model, android = excluded.android,
           country = excluded.country, lat = excluded.lat, lon = excluded.lon, city = excluded.city, server = excluded.server`
      ).bind(today, id, clean(body.v, 16) || '?', clean(body.region, 8) || 'other', clean(body.hop, 12) || 'off',
        body.gaming ? 1 : 0, Math.min(Math.max(parseInt(body.connects) || 0, 0), 10000),
        cleanName(body.brand, 30) || '?', cleanName(body.model, 40) || '?', clean(body.android, 8) || '?',
        country, lat, lon, city, clean(body.server, 12) || '?'),
      env.DB.prepare('INSERT OR IGNORE INTO installs (id, first) VALUES (?, ?)').bind(id, today),
    ]);
  } catch (e) {
    console.error('ping failed', e);
    // The app tries again tomorrow; a 503 tells it this one did not count.
    return json({ ok: false, error: describe(e) }, 503);
  }
  return json({ ok: true });
}

// ---------- Panel numbers ----------

/**
 * Everything the dashboard shows for the last [rangeDays] days. The breakdowns use each install's
 * latest day in the window (its current version, hop, phone, place), read in one pass.
 */
async function computeStats(env, rangeDays) {
  let rowsRead = 0;
  const all = async (stmt) => { const r = await stmt.all(); rowsRead += r.meta?.rows_read || 0; return r.results; };
  const first = async (stmt) => (await all(stmt))[0] || {};

  const today = day();
  const span = Math.max(rangeDays, 30); // active week/month come from the same pass
  const start = day(rangeDays - 1);
  const prevStart = day(2 * rangeDays - 1);
  const prevEnd = day(rangeDays);

  const latest = await all(env.DB.prepare(
    `SELECT p.day, p.version, p.region, p.hop, p.gaming, p.brand, p.model, p.android, p.country, p.city,
       p.lat, p.lon, p.server, i.first
     FROM (SELECT id, MAX(day) AS d FROM pings WHERE day >= ? GROUP BY id) l
     JOIN pings p ON p.id = l.id AND p.day = l.d
     LEFT JOIN installs i ON i.id = l.id`
  ).bind(day(span - 1)));

  // Days before today never change: they are rolled up once into daily.
  const from = prevStart < start ? prevStart : start;
  const kept = await all(env.DB.prepare('SELECT day, active, connects FROM daily WHERE day >= ? AND day < ?').bind(from, today));
  const have = new Set(kept.map((r) => r.day));
  const missing = [];
  for (let i = 2 * rangeDays - 1; i >= 1; i--) if (!have.has(day(i))) missing.push(day(i));
  if (missing.length) {
    const rolled = await all(env.DB.prepare(
      'SELECT day, COUNT(*) AS active, COALESCE(SUM(connects), 0) AS connects FROM pings WHERE day BETWEEN ? AND ? GROUP BY day'
    ).bind(missing[0], missing[missing.length - 1]));
    const byDay = Object.fromEntries(rolled.map((r) => [r.day, r]));
    const rows = missing.map((d) => byDay[d] || { day: d, active: 0, connects: 0 });
    // A day with nothing yet is kept too, except yesterday: late pings can still land for it.
    const writes = rows.filter((r) => r.active > 0 || r.day < day(1))
      .map((r) => env.DB.prepare('INSERT OR REPLACE INTO daily (day, active, connects) VALUES (?, ?, ?)').bind(r.day, r.active, r.connects));
    for (let i = 0; i < writes.length; i += 50) await env.DB.batch(writes.slice(i, i + 50));
    kept.push(...rows);
  }
  const now = await first(env.DB.prepare('SELECT COUNT(*) AS active, COALESCE(SUM(connects), 0) AS connects FROM pings WHERE day = ?').bind(today));
  const newByDay = Object.fromEntries((await all(env.DB.prepare(
    'SELECT first AS day, COUNT(*) AS n FROM installs WHERE first >= ? GROUP BY first'
  ).bind(from))).map((r) => [r.day, r.n]));

  const perDay = Object.fromEntries(kept.map((r) => [r.day, r]));
  perDay[today] = { day: today, active: now.active || 0, connects: now.connects || 0 };
  const series = [];
  for (let i = rangeDays - 1; i >= 0; i--) {
    const d = day(i), r = perDay[d] || { active: 0, connects: 0 };
    series.push({ day: d, active: r.active, connects: r.connects, newUsers: newByDay[d] || 0 });
  }
  const sumIn = (a, b, f) => Object.values(perDay).filter((r) => r.day >= a && r.day <= b).reduce((s, r) => s + (r[f] || 0), 0);
  const newIn = (a, b) => Object.entries(newByDay).filter(([d]) => d >= a && d <= b).reduce((s, [, n]) => s + n, 0);

  // Distinct installs in the window before: it is all in the past, so it is counted once and kept.
  const prevKey = `win_${prevStart}_${prevEnd}`;
  const keptPrev = await dbGet(env, prevKey);
  let prevUsers = Number(keptPrev);
  if (!keptPrev) {
    prevUsers = (await first(env.DB.prepare('SELECT COUNT(DISTINCT id) AS n FROM pings WHERE day BETWEEN ? AND ?').bind(prevStart, prevEnd))).n || 0;
    await dbSet(env, prevKey, String(prevUsers));
  }

  const inRange = latest.filter((r) => r.day >= start);
  const count = (rows, key, top) => {
    const m = new Map();
    for (const r of rows) { const k = key(r); m.set(k, (m.get(k) || 0) + 1); }
    const out = [...m].map(([k, n]) => ({ k, n })).sort((a, b) => b.n - a.n);
    return top ? out.slice(0, top) : out;
  };
  const points = count(inRange.filter((r) => r.lat != null && r.lon != null), (r) => r.lat + ',' + r.lon)
    .map(({ k, n }) => { const [lat, lon] = k.split(','); return { lat: Number(lat), lon: Number(lon), n }; });

  // Week-1 retention: of installs first seen 7 to 13 days ago, how many were back in the last 7 days.
  const cohort = latest.filter((r) => r.first && r.first >= day(13) && r.first <= day(7));
  const retention = { cohort: cohort.length, back: cohort.filter((r) => r.day >= day(6)).length };

  const totalUsers = (await first(env.DB.prepare('SELECT COUNT(*) AS n FROM installs'))).n || 0;

  return {
    rangeDays,
    updated: new Date().toISOString(),
    totalUsers,
    active: {
      day: latest.filter((r) => r.day === today).length,
      week: latest.filter((r) => r.day >= day(6)).length,
      month: latest.filter((r) => r.day >= day(29)).length,
    },
    period: { users: inRange.length, newUsers: newIn(start, today), connects: sumIn(start, today, 'connects') },
    prev: { users: prevUsers, newUsers: newIn(prevStart, prevEnd), connects: sumIn(prevStart, prevEnd, 'connects') },
    retention,
    series,
    breakdown: {
      byVersion: count(inRange, (r) => r.version),
      byRegion: count(inRange, (r) => r.region),
      byHop: count(inRange, (r) => r.hop),
      byServer: count(inRange, (r) => r.server || '?'),
      gaming: count(inRange, (r) => r.gaming),
      byBrand: count(inRange, (r) => r.brand),
      byModel: count(inRange, (r) => r.brand + ' ' + r.model, 15),
      byAndroid: count(inRange, (r) => r.android),
      byCountry: count(inRange.filter((r) => r.country && r.country !== '??'), (r) => r.country, 20),
      byCity: count(inRange.filter((r) => r.city && r.city !== '?'), (r) => r.country + '|' + r.city, 20),
    },
    // One point per user area (approximate IP location, rounded to ~11 km), for the map.
    points,
    rowsRead,
  };
}

/** The panel's numbers, kept for STATS_TTL_MS; the last good ones when the database fails. */
async function stats(env, rangeDays, fresh) {
  const key = 'stats_' + rangeDays;
  const cached = parse(await dbGet(env, key));
  const age = cached ? Date.now() - cached.at : Infinity;
  if (cached && (age < (fresh ? FRESH_MIN_MS : STATS_TTL_MS))) return json({ ...cached.data, cachedAt: cached.at });
  const budgetKey = 'reads_' + day();
  const readToday = Number(await dbGet(env, budgetKey)) || 0;
  if (cached && readToday >= STATS_DAILY_READS) return json({ ...cached.data, cachedAt: cached.at, limited: true });
  try {
    await ensureSchema(env);
    const data = await computeStats(env, rangeDays);
    await dbSet(env, key, JSON.stringify({ at: Date.now(), data }));
    await dbSet(env, budgetKey, String(readToday + (data.rowsRead || 0)));
    await dbSet(env, 'last_compute', JSON.stringify({ at: Date.now(), rangeDays, rowsRead: data.rowsRead }));
    return json(data);
  } catch (e) {
    console.error('stats failed', e);
    if (cached) return json({ ...cached.data, cachedAt: cached.at, stale: true, error: describe(e) });
    return json({ ok: false, error: describe(e) }, 503);
  }
}

/** Active installs of the last 30 days for the README badge; recomputed every few hours. */
async function badge(env) {
  const reply = (n, color = '2563eb') =>
    json({ schemaVersion: 1, label: 'نصب فعال / active', message: n == null ? '?' : short(n), color, cacheSeconds: 3600 }, 200,
      { 'cache-control': 'public, max-age=3600' });
  const cached = parse(await dbGet(env, 'badge'));
  if (cached && Date.now() - cached.at < BADGE_TTL_MS) return reply(cached.n);
  // The panel's own count, when it is recent enough.
  const panel = parse(await dbGet(env, 'stats_30'));
  if (panel && Date.now() - panel.at < BADGE_TTL_MS) {
    await dbSet(env, 'badge', JSON.stringify({ at: panel.at, n: panel.data.active.month })).catch(() => {});
    return reply(panel.data.active.month);
  }
  try {
    const n = (await env.DB.prepare('SELECT COUNT(DISTINCT id) AS n FROM pings WHERE day >= ?').bind(day(29)).first()).n;
    await dbSet(env, 'badge', JSON.stringify({ at: Date.now(), n }));
    return reply(n);
  } catch (e) {
    console.error('badge failed', e);
    return reply(cached?.n ?? null, cached ? '2563eb' : 'lightgrey');
  }
}

/** Database health for the settings page: all cheap queries. */
async function health(env) {
  const out = { ok: true };
  try {
    await ensureSchema(env);
    const r = await env.DB.prepare('SELECT COUNT(*) AS n FROM installs').all();
    out.installs = r.results[0].n;
    out.dbBytes = r.meta?.size_after ?? null;
    out.todayPings = (await env.DB.prepare('SELECT COUNT(*) AS n FROM pings WHERE day = ?').bind(day()).first()).n;
    out.oldestDay = (await env.DB.prepare('SELECT MIN(day) AS d FROM pings').first()).d;
  } catch (e) {
    out.ok = false;
    out.error = describe(e);
  }
  out.lastCompute = parse(await dbGet(env, 'last_compute'));
  out.readsToday = Number(await dbGet(env, 'reads_' + day())) || 0;
  out.readsBudget = STATS_DAILY_READS;
  out.keepDays = Number(await dbGet(env, 'keep_days')) || 0;
  out.statsTtlMinutes = STATS_TTL_MS / 60000;
  return json(out);
}

async function prune(env, keep) {
  const r = await env.DB.prepare('DELETE FROM pings WHERE day < ?').bind(day(keep)).run();
  return r.meta?.changes ?? 0;
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);

    if (request.method === 'POST' && url.pathname === '/ping') return ping(request, env);
    if (url.pathname === '/badge') return badge(env);

    if (url.pathname === '/login' && request.method === 'POST') {
      const form = await request.formData();
      if ((await loginEnabled(env)) && same(form.get('user'), env.ADMIN_USER) && (await verifyPassword(env, form.get('password') || ''))) {
        return new Response(null, { status: 303, headers: { location: '/', 'set-cookie': await sessionCookie(env) } });
      }
      // A short pause makes guessing slow.
      await new Promise((r) => setTimeout(r, 1500));
      return html(loginPage(true), 401);
    }

    if (url.pathname === '/logout') {
      return new Response(null, { status: 303, headers: { location: '/', 'set-cookie': 'drvpn_admin=; Path=/; Max-Age=0; HttpOnly; Secure; SameSite=Strict' } });
    }

    // ---- Settings (logged in only) ----
    if (url.pathname === '/settings' && request.method === 'GET') {
      return (await loggedIn(request, env)) ? html(settingsPage()) : Response.redirect(new URL('/', url), 303);
    }
    if (url.pathname === '/health') {
      return (await loggedIn(request, env)) ? health(env) : json({ ok: false, error: 'login' }, 401);
    }
    if (url.pathname.startsWith('/settings/') && request.method === 'POST') {
      if (!(await loggedIn(request, env))) return json({ ok: false, error: 'login' }, 401);
      const form = await request.formData();
      try {
        if (url.pathname === '/settings/password') {
          const next = String(form.get('next') || '');
          if (!(await verifyPassword(env, form.get('current') || ''))) return json({ ok: false, error: 'رمز فعلی اشتباه است' }, 400);
          if (next.length < 8) return json({ ok: false, error: 'رمز جدید باید حداقل ۸ نویسه باشد' }, 400);
          await setPassword(env, next);
          // The signing key changed, so re-issue this session's cookie.
          return json({ ok: true }, 200, { 'set-cookie': await sessionCookie(env) });
        }
        if (url.pathname === '/settings/reset') {
          const r = await env.DB.prepare('DELETE FROM pings').run();
          await env.DB.batch([
            env.DB.prepare('DELETE FROM installs'),
            env.DB.prepare('DELETE FROM daily'),
            env.DB.prepare("DELETE FROM settings WHERE key LIKE 'stats\\_%' ESCAPE '\\' OR key LIKE 'win\\_%' ESCAPE '\\' OR key IN ('badge', 'last_compute')"),
          ]);
          return json({ ok: true, deleted: r.meta?.changes ?? 0 });
        }
        if (url.pathname === '/settings/prune') {
          const keep = Math.min(Math.max(parseInt(form.get('days')) || 90, 7), 365);
          return json({ ok: true, deleted: await prune(env, keep) });
        }
        if (url.pathname === '/settings/autoprune') {
          // 0 = off; otherwise data older than this many days is deleted every night.
          const raw = parseInt(form.get('days')) || 0;
          const keep = raw <= 0 ? 0 : Math.min(Math.max(raw, 30), 365);
          await dbSet(env, 'keep_days', String(keep));
          return json({ ok: true, keepDays: keep });
        }
      } catch (e) {
        console.error('settings failed', e);
        return json({ ok: false, error: describe(e) }, 503);
      }
      return json({ ok: false }, 404);
    }

    if (url.pathname === '/' && request.method === 'GET') {
      return html((await loggedIn(request, env)) ? dashboardPage() : loginPage(false));
    }

    if (url.pathname === '/stats') {
      if (!(await loggedIn(request, env))) return json({ ok: false, error: 'login' }, 401);
      const rangeDays = [7, 30, 90].includes(Number(url.searchParams.get('days'))) ? Number(url.searchParams.get('days')) : 30;
      return stats(env, rangeDays, url.searchParams.get('fresh') === '1');
    }

    return json({ ok: false }, 404);
  },

  // Every night (wrangler.toml [triggers]): delete old data when the owner turned that on.
  async scheduled(event, env) {
    try {
      await ensureSchema(env);
      const keep = Number(await dbGet(env, 'keep_days')) || 0;
      if (keep > 0) console.log('pruned', await prune(env, keep));
      // Kept counts of windows long gone are not needed any more.
      await env.DB.prepare("DELETE FROM settings WHERE key LIKE 'win\\_%' ESCAPE '\\' AND key < ?").bind('win_' + day(200)).run();
      await env.DB.prepare("DELETE FROM settings WHERE key LIKE 'reads\\_%' ESCAPE '\\' AND key < ?").bind('reads_' + day(7)).run();
    } catch (e) {
      console.error('scheduled failed', e);
    }
  },
};
