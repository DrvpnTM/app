// Dr VPN anonymous usage stats and admin panel (Cloudflare Worker + D1).
//   POST /ping       {"id","v","region","hop","gaming","connects","fails","up","down","via","brand","model","android","server"}
//                    from the app, once a day; connects, fails and bytes are counted since its last ping (0.2.8 on)
//   GET  /badge      shields.io endpoint JSON: active installs in the last 30 days (public)
//   GET  /           the admin panel (login with ADMIN_USER / ADMIN_PASSWORD, secrets of the Worker)
//   GET  /settings   the panel's settings page
//   GET  /stats      the dashboard's numbers (?days=7|30|90, &f=<filter>&v=<value>)
//   GET  /users      weekly cohorts and versions per day        GET /live       newest pings
//   GET  /downloads  APK downloads from GitHub                 GET|POST /notice the in-app announcement
//   GET  /health     database health and settings for the settings page
// Nothing about the person is kept: the request's IP address is never stored. The network it came
// from is kept by name and number (e.g. "AS44244 Irancell"), never the address. Of the traffic, only
// two byte totals are sent: nothing about what was visited or where.
//
// D1's free plan reads at most 5 million rows a day; past that every query fails until midnight UTC,
// pings included. So the panel's numbers are kept for a while, past days are rolled up once, and the
// panel stops reading after STATS_DAILY_READS rows a day (see db.js).
import { loginPage, dashboardPage, settingsPage } from './pages.js';
import clientJs from './client/panel.js';
import { ensureSchema, dbGet, dbSet, dbDel, parse, charge, overBudget, readsToday, day, now, describe, json, STATS_DAILY_READS } from './db.js';
import { computeStats, computeUsers, computeLive, parseFilter } from './stats.js';
import { downloads, readNotice, writeNotice, githubToken, telegramConfig, telegramSend, dailyReport, checkAlerts } from './extras.js';

const SESSION_DAYS = 30;
const STATS_TTL_MS = 15 * 60 * 1000;
const USERS_TTL_MS = 6 * 60 * 60 * 1000;
const LIVE_TTL_MS = 20 * 1000;
/** A "refresh now" click recomputes only when the kept numbers are older than this. */
const FRESH_MIN_MS = 60 * 1000;
const BADGE_TTL_MS = 6 * 60 * 60 * 1000;
/** Wrong passwords from one address before it waits LOCK_MS. */
const LOCK_TRIES = 5;
const LOCK_MS = 15 * 60 * 1000;
/** A ping's byte totals are capped at this (10 TB), so one broken counter cannot swamp the charts. */
const MAX_BYTES = 10 * 1024 ** 4;

const html = (body, status = 200, headers = {}) =>
  new Response(body, {
    status,
    headers: {
      'content-type': 'text/html; charset=utf-8', 'cache-control': 'no-store',
      'x-frame-options': 'DENY', 'referrer-policy': 'same-origin', ...headers,
    },
  });

const b64url = (buf) => btoa(String.fromCharCode(...new Uint8Array(buf))).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
const fromB64url = (s) => Uint8Array.from(atob(s.replace(/-/g, '+').replace(/_/g, '/')), (c) => c.charCodeAt(0));

/** A short version tag of the dashboard script, so browsers fetch it again after a deploy. */
const CLIENT_TAG = (() => { let h = 5381; for (let i = 0; i < clientJs.length; i++) h = ((h * 33) ^ clientJs.charCodeAt(i)) >>> 0; return h.toString(36); })();

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

async function hmac(env, text) {
  const key = await crypto.subtle.importKey('raw', new TextEncoder().encode((await sessionSecret(env)) + '|' + env.ADMIN_USER),
    { name: 'HMAC', hash: 'SHA-256' }, false, ['sign']);
  return b64url(await crypto.subtle.sign('HMAC', key, new TextEncoder().encode(text)));
}

/** "expiry.signature": valid until expiry; a new password ends every session. */
async function newSession(env) {
  const expiry = String(Date.now() + SESSION_DAYS * 86400000);
  return `${expiry}.${await hmac(env, expiry)}`;
}
const sessionCookie = async (env) => `drvpn_admin=${await newSession(env)}; Path=/; Max-Age=${SESSION_DAYS * 86400}; HttpOnly; Secure; SameSite=Strict`;

async function loggedIn(request, env) {
  if (!(await loginEnabled(env))) return false;
  const cookie = (request.headers.get('cookie') || '').match(/(?:^|;\s*)drvpn_admin=([^;]+)/);
  if (!cookie) return false;
  const [expiry, signature] = cookie[1].split('.');
  if (!expiry || !signature || Number(expiry) < Date.now()) return false;
  return same(signature, await hmac(env, expiry));
}

/** Compares without leaking where the strings differ through timing. */
function same(a, b) {
  a = String(a); b = String(b);
  let diff = a.length ^ b.length;
  for (let i = 0; i < Math.max(a.length, b.length); i++) diff |= (a.charCodeAt(i) || 0) ^ (b.charCodeAt(i) || 0);
  return diff === 0;
}

/** The login attempts of one address, under a keyed hash of it (the address itself is not kept). */
async function triesKey(env, request) {
  return 'lf_' + (await hmac(env, 'login|' + (request.headers.get('cf-connecting-ip') || '?'))).slice(0, 22);
}

/** Browser and system from the User-Agent, for the login log. */
function device(ua) {
  ua = ua || '';
  const os = /Android/.test(ua) ? 'Android' : /iPhone|iPad/.test(ua) ? 'iOS' : /Windows/.test(ua) ? 'Windows' : /Mac OS/.test(ua) ? 'macOS' : /Linux/.test(ua) ? 'Linux' : '?';
  const br = /Edg\//.test(ua) ? 'Edge' : /OPR\//.test(ua) ? 'Opera' : /Firefox\//.test(ua) ? 'Firefox' : /Chrome\//.test(ua) ? 'Chrome' : /Safari\//.test(ua) ? 'Safari' : '?';
  return br + ' / ' + os;
}

/** The last 30 logins (and failed tries): when, from which country and city, on what. */
async function logLogin(env, request, ok) {
  const list = parse(await dbGet(env, 'login_log')) || [];
  const cf = request.cf || {};
  list.unshift({ at: Date.now(), ok, cc: String(cf.country || '??').slice(0, 2), city: String(cf.city || '').slice(0, 40), dev: device(request.headers.get('user-agent')) });
  await dbSet(env, 'login_log', JSON.stringify(list.slice(0, 30)));
}

async function login(request, env) {
  if (!(await loginEnabled(env))) return html(loginPage('off'), 503);
  const key = await triesKey(env, request);
  const tries = parse(await dbGet(env, key));
  const fresh = tries && Date.now() - tries.first < LOCK_MS;
  if (fresh && tries.n >= LOCK_TRIES) {
    const minutes = Math.ceil((tries.first + LOCK_MS - Date.now()) / 60000);
    return html(loginPage('locked', minutes), 429);
  }
  const form = await request.formData();
  if (same(form.get('user'), env.ADMIN_USER) && (await verifyPassword(env, form.get('password') || ''))) {
    await dbDel(env, key).catch(() => {});
    await logLogin(env, request, true).catch(() => {});
    return new Response(null, { status: 303, headers: { location: '/', 'set-cookie': await sessionCookie(env) } });
  }
  await dbSet(env, key, JSON.stringify(fresh ? { first: tries.first, n: tries.n + 1 } : { first: Date.now(), n: 1 })).catch(() => {});
  await logLogin(env, request, false).catch(() => {});
  // A short pause makes guessing slow.
  await new Promise((r) => setTimeout(r, 1500));
  return html(loginPage('failed'), 401);
}

// ---------- Ping ----------

// Device names have spaces and brackets ("Galaxy A54 5G"); keep letters, digits and a few signs.
const cleanName = (value, max) => String(value ?? '').replace(/[^\p{L}\p{N} ._+()&,\-]/gu, '').trim().slice(0, max);
const clean = (value, max) => String(value ?? '').replace(/[^\w.\-]/g, '').slice(0, max);
const short = (n) => (n >= 1e6 ? (n / 1e6).toFixed(1) + 'M' : n >= 1e3 ? (n / 1e3).toFixed(1) + 'K' : String(n)).replace('.0', '');
/** A count or byte total since the app's last ping; null when the version does not send it. */
const amount = (v, max) => (v == null || v === '' || isNaN(Number(v)) ? null : Math.min(Math.max(Math.floor(Number(v)), 0), max));

async function ping(request, env) {
  let body;
  try {
    body = await request.json();
  } catch {
    return json({ ok: false }, 400);
  }
  const id = clean(body.id, 40);
  if (id.length < 16) return json({ ok: false }, 400);
  // Approximate location and network from Cloudflare: country, city area (lat/lon rounded to
  // ~11 km, for the map) and the network's name and number. Not the address, no street, no GPS.
  const cf = request.cf || {};
  const country = clean(cf.country, 2).toUpperCase() || '??';
  const city = cleanName(cf.city, 40) || '?';
  const round1 = (v) => (v == null || v === '' || isNaN(parseFloat(v)) ? null : Math.round(parseFloat(v) * 10) / 10);
  const lat = round1(cf.latitude), lon = round1(cf.longitude);
  const asn = parseInt(cf.asn) || null;
  const isp = cleanName(cf.asOrganization, 60) || '?';
  const via = ['vpn', 'direct'].includes(body.via) ? body.via : '?';
  const today = day();
  try {
    await ensureSchema(env);
    // A second ping the same day only comes when the first one's answer was lost: it repeats the
    // first one's totals plus what came since, so the larger is kept (adding would count it twice).
    await env.DB.batch([
      env.DB.prepare(
        `INSERT INTO pings (day, id, version, region, hop, gaming, connects, brand, model, android, country, lat, lon, city, server,
           up, down, fails, asn, isp, at, via)
           VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
         ON CONFLICT (day, id) DO UPDATE SET version = excluded.version, region = excluded.region,
           hop = excluded.hop, gaming = excluded.gaming, connects = MAX(pings.connects, excluded.connects),
           brand = excluded.brand, model = excluded.model, android = excluded.android,
           country = excluded.country, lat = excluded.lat, lon = excluded.lon, city = excluded.city, server = excluded.server,
           up = CASE WHEN excluded.up IS NULL THEN pings.up ELSE MAX(COALESCE(pings.up, 0), excluded.up) END,
           down = CASE WHEN excluded.down IS NULL THEN pings.down ELSE MAX(COALESCE(pings.down, 0), excluded.down) END,
           fails = CASE WHEN excluded.fails IS NULL THEN pings.fails ELSE MAX(COALESCE(pings.fails, 0), excluded.fails) END,
           asn = excluded.asn, isp = excluded.isp, at = excluded.at, via = excluded.via`
      ).bind(today, id, clean(body.v, 16) || '?', clean(body.region, 8) || 'other', clean(body.hop, 12) || 'off',
        body.gaming ? 1 : 0, amount(body.connects, 10000) ?? 0,
        cleanName(body.brand, 30) || '?', cleanName(body.model, 40) || '?', clean(body.android, 8) || '?',
        country, lat, lon, city, clean(body.server, 12) || '?',
        amount(body.up, MAX_BYTES), amount(body.down, MAX_BYTES), amount(body.fails, 10000), asn, isp, now(), via),
      env.DB.prepare('INSERT OR IGNORE INTO installs (id, first) VALUES (?, ?)').bind(id, today),
    ]);
  } catch (e) {
    console.error('ping failed', e);
    // The app tries again tomorrow; a 503 tells it this one did not count.
    return json({ ok: false, error: describe(e) }, 503);
  }
  return json({ ok: true });
}

// ---------- Panel data ----------

/**
 * A result kept in settings under [key] for [ttl]; [compute] makes a fresh one (it returns its
 * rowsRead). The last good one is shown when the database fails or the day's reads are spent.
 */
async function kept(env, key, ttl, fresh, compute) {
  const cached = parse(await dbGet(env, key));
  const age = cached ? Date.now() - cached.at : Infinity;
  if (cached && age < (fresh ? FRESH_MIN_MS : ttl)) return json({ ...cached.data, cachedAt: cached.at });
  if (cached && (await overBudget(env))) return json({ ...cached.data, cachedAt: cached.at, limited: true });
  try {
    await ensureSchema(env);
    const data = await compute();
    await dbSet(env, key, JSON.stringify({ at: Date.now(), data }));
    await charge(env, data.rowsRead || 0);
    await dbSet(env, 'last_compute', JSON.stringify({ at: Date.now(), what: key, rowsRead: data.rowsRead }));
    return json(data);
  } catch (e) {
    console.error(key + ' failed', e);
    if (cached) return json({ ...cached.data, cachedAt: cached.at, stale: true, error: describe(e) });
    return json({ ok: false, error: describe(e) }, 503);
  }
}

// The live feed is asked for every 30 s by each open panel: one read per LIVE_TTL_MS per Worker.
let liveCache = null;
async function live(env) {
  if (liveCache && Date.now() - liveCache.at < LIVE_TTL_MS) return json(liveCache.data);
  if (await overBudget(env)) return json(liveCache ? { ...liveCache.data, limited: true } : { ok: false, error: 'سهمیهٔ خواندن امروز تمام شده' }, liveCache ? 200 : 503);
  try {
    await ensureSchema(env);
    const data = await computeLive(env);
    await charge(env, data.rowsRead);
    liveCache = { at: Date.now(), data };
    return json(data);
  } catch (e) {
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

/** Database health and the settings page's state: all cheap queries. */
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
  out.readsToday = await readsToday(env);
  out.readsBudget = STATS_DAILY_READS;
  out.keepDays = Number(await dbGet(env, 'keep_days')) || 0;
  out.statsTtlMinutes = STATS_TTL_MS / 60000;
  out.logins = parse(await dbGet(env, 'login_log')) || [];
  const tg = await telegramConfig(env);
  out.telegram = { set: !!(tg.token && tg.chat), chat: tg.chat, daily: tg.daily, alerts: tg.alerts, fromSecrets: !!env.TELEGRAM_BOT_TOKEN };
  out.github = { set: !!(await githubToken(env)), fromSecret: !!env.GITHUB_TOKEN };
  out.lock = { tries: LOCK_TRIES, minutes: LOCK_MS / 60000 };
  return json(out);
}

async function prune(env, keep) {
  const r = await env.DB.prepare('DELETE FROM pings WHERE day < ?').bind(day(keep)).run();
  return r.meta?.changes ?? 0;
}

// ---------- Settings ----------

async function settings(path, request, env) {
  const form = await request.formData();
  if (path === '/settings/password') {
    const next = String(form.get('next') || '');
    if (!(await verifyPassword(env, form.get('current') || ''))) return json({ ok: false, error: 'رمز فعلی اشتباه است' }, 400);
    if (next.length < 8) return json({ ok: false, error: 'رمز جدید باید حداقل ۸ نویسه باشد' }, 400);
    await setPassword(env, next);
    // The signing key changed, so re-issue this session's cookie.
    return json({ ok: true }, 200, { 'set-cookie': await sessionCookie(env) });
  }
  if (path === '/settings/reset') {
    const r = await env.DB.prepare('DELETE FROM pings').run();
    await env.DB.batch([
      env.DB.prepare('DELETE FROM installs'),
      env.DB.prepare('DELETE FROM daily'),
      env.DB.prepare('DELETE FROM daily_dim'),
      env.DB.prepare("DELETE FROM settings WHERE key LIKE 'stats\\_%' ESCAPE '\\' OR key LIKE 'win\\_%' ESCAPE '\\' OR key IN ('badge', 'last_compute', 'users')"),
    ]);
    return json({ ok: true, deleted: r.meta?.changes ?? 0 });
  }
  if (path === '/settings/prune') {
    const keep = Math.min(Math.max(parseInt(form.get('days')) || 90, 7), 365);
    return json({ ok: true, deleted: await prune(env, keep) });
  }
  if (path === '/settings/autoprune') {
    // 0 = off; otherwise data older than this many days is deleted every night.
    const raw = parseInt(form.get('days')) || 0;
    const keep = raw <= 0 ? 0 : Math.min(Math.max(raw, 30), 365);
    await dbSet(env, 'keep_days', String(keep));
    return json({ ok: true, keepDays: keep });
  }
  if (path === '/settings/telegram') {
    const saved = parse(await dbGet(env, 'telegram')) || {};
    const token = String(form.get('token') || '').trim();
    const chat = String(form.get('chat') || '').trim();
    if (token && !/^\d+:[\w-]{20,}$/.test(token)) return json({ ok: false, error: 'توکن ربات درست نیست (شکلش این است: 123456:ABC...)' }, 400);
    if (chat && !/^(-?\d+|@\w{4,})$/.test(chat)) return json({ ok: false, error: 'شناسهٔ چت باید عدد (مثل -100123...) یا @نام_کانال باشد' }, 400);
    const next = { token: token || saved.token || '', chat: chat || saved.chat || '', daily: form.get('daily') === '1', alerts: form.get('alerts') === '1' };
    if (form.get('clear') === '1') { await dbDel(env, 'telegram'); return json({ ok: true }); }
    await dbSet(env, 'telegram', JSON.stringify(next));
    return json({ ok: true });
  }
  if (path === '/settings/telegram/test') {
    const report = await dailyReport(env, new URL(request.url).origin);
    await charge(env, report.rowsRead);
    const res = await telegramSend(env, '✅ پنل دکتر وی پی ان به این چت وصل است.\n\nنمونهٔ گزارش روزانه:\n\n' + report.text);
    return json(res, res.ok ? 200 : 400);
  }
  if (path === '/settings/github') {
    const token = String(form.get('token') || '').trim();
    if (form.get('clear') === '1') { await dbDel(env, 'github_token'); return json({ ok: true }); }
    if (!/^(github_pat_|ghp_)[\w]{20,}$/.test(token)) return json({ ok: false, error: 'توکن گیت‌هاب باید با github_pat_ یا ghp_ شروع شود' }, 400);
    await dbSet(env, 'github_token', token);
    try {
      const { canEdit } = await readNotice(env);
      return json({ ok: true, canEdit });
    } catch (e) {
      await dbDel(env, 'github_token');
      return json({ ok: false, error: 'گیت‌هاب این توکن را قبول نکرد: ' + String(e.message).slice(0, 120) }, 400);
    }
  }
  if (path === '/settings/logins/clear') {
    await dbDel(env, 'login_log');
    return json({ ok: true });
  }
  return json({ ok: false }, 404);
}

// ---------- Routes ----------

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    const path = url.pathname;

    if (request.method === 'POST' && path === '/ping') return ping(request, env);
    if (path === '/badge') return badge(env);
    if (path === '/login' && request.method === 'POST') return login(request, env);
    if (path === '/logout') {
      return new Response(null, { status: 303, headers: { location: '/', 'set-cookie': 'drvpn_admin=; Path=/; Max-Age=0; HttpOnly; Secure; SameSite=Strict' } });
    }
    if (path === '/app.js') {
      return new Response(clientJs, { headers: { 'content-type': 'text/javascript; charset=utf-8', 'cache-control': 'public, max-age=31536000, immutable' } });
    }

    const authed = await loggedIn(request, env);
    if (path === '/' && request.method === 'GET') return html(authed ? dashboardPage(CLIENT_TAG) : loginPage(null));
    if (path === '/settings' && request.method === 'GET') return authed ? html(settingsPage(CLIENT_TAG)) : Response.redirect(new URL('/', url), 303);
    if (!authed) return json({ ok: false, error: 'login' }, 401);

    try {
      if (path === '/stats') {
        const rangeDays = [7, 30, 90, 180].includes(Number(url.searchParams.get('days'))) ? Number(url.searchParams.get('days')) : 30;
        const filter = parseFilter(url.searchParams);
        const key = 'stats_' + rangeDays + (filter ? '_' + filter.tag : '');
        return kept(env, key, STATS_TTL_MS, url.searchParams.get('fresh') === '1', () => computeStats(env, rangeDays, filter));
      }
      if (path === '/users') return kept(env, 'users', USERS_TTL_MS, url.searchParams.get('fresh') === '1', () => computeUsers(env));
      if (path === '/live') return live(env);
      if (path === '/health') return health(env);
      if (path === '/downloads') return json(await downloads(env, url.searchParams.get('fresh') === '1'));
      if (path === '/notice' && request.method === 'GET') return json({ ok: true, ...(await readNotice(env)) });
      if (path === '/notice' && request.method === 'POST') return json({ ok: true, ...(await writeNotice(env, await request.json())) });
      if (path.startsWith('/settings/') && request.method === 'POST') return settings(path, request, env);
    } catch (e) {
      console.error(path + ' failed', e);
      return json({ ok: false, error: e.status ? String(e.message).slice(0, 240) : describe(e) }, 503);
    }
    return json({ ok: false }, 404);
  },

  // Every hour (wrangler.toml [triggers]): alerts; at 03 UTC old data; at 04 UTC (07:30 Tehran) the daily report.
  async scheduled(event, env, ctx) {
    const hour = new Date(event.scheduledTime).getUTCHours();
    try {
      await ensureSchema(env);
      if (hour === 3) {
        const keep = Number(await dbGet(env, 'keep_days')) || 0;
        if (keep > 0) console.log('pruned', await prune(env, keep));
        // Kept results that are not needed any more.
        await env.DB.prepare("DELETE FROM settings WHERE key LIKE 'win\\_%' ESCAPE '\\' AND key < ?").bind('win_' + day(200)).run();
        await env.DB.prepare("DELETE FROM settings WHERE key LIKE 'reads\\_%' ESCAPE '\\' AND key < ?").bind('reads_' + day(7)).run();
        await env.DB.prepare("DELETE FROM settings WHERE key LIKE 'lf\\_%' ESCAPE '\\'").run();
        // Filtered results are kept only for a day.
        await env.DB.prepare("DELETE FROM settings WHERE key LIKE 'stats\\_%\\_%' ESCAPE '\\'").run();
        await env.DB.prepare('DELETE FROM daily_dim WHERE day < ?').bind(day(120)).run();
      }
      const tg = await telegramConfig(env);
      if (!tg.token || !tg.chat) return;
      if (tg.alerts) await charge(env, (await checkAlerts(env, tg)).rowsRead);
      if (tg.daily && hour === 4) {
        const report = await dailyReport(env, env.PANEL_URL || '');
        await charge(env, report.rowsRead);
        const res = await telegramSend(env, report.text, tg);
        if (!res.ok) console.error('daily report failed', res.error);
      }
    } catch (e) {
      console.error('scheduled failed', e);
    }
  },
};
