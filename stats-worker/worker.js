// Dr VPN anonymous usage stats (Cloudflare Worker + D1).
//   POST /ping    {"id","v","region","hop","gaming","connects","brand","model","android"}  from the app, once a day
//   GET  /badge   shields.io endpoint JSON: active installs in the last 30 days (public)
//   GET  /        the admin panel (login with ADMIN_USER / ADMIN_PASSWORD, secrets of the Worker)
//   GET  /stats   the panel's data as JSON (logged in only)
// Nothing about the person is kept: the request's IP address is never read or stored.
import { loginPage, dashboardPage, settingsPage } from './pages.js';

const SESSION_DAYS = 30;

const html = (body, status = 200, headers = {}) =>
  new Response(body, { status, headers: { 'content-type': 'text/html; charset=utf-8', 'cache-control': 'no-store', ...headers } });

const b64url = (buf) => btoa(String.fromCharCode(...new Uint8Array(buf))).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
const fromB64url = (s) => Uint8Array.from(atob(s.replace(/-/g, '+').replace(/_/g, '/')), (c) => c.charCodeAt(0));

// A small key/value table for settings (the admin password the owner sets in the panel).
async function dbGet(env, key) {
  try { return (await env.DB.prepare('SELECT value FROM settings WHERE key = ?').bind(key).first())?.value ?? null; } catch { return null; }
}
async function dbSet(env, key, value) {
  await env.DB.prepare('INSERT INTO settings (key, value) VALUES (?, ?) ON CONFLICT (key) DO UPDATE SET value = excluded.value').bind(key, value).run();
}

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
  return signature === (await sign(env, expiry));
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

export default {
  async fetch(request, env) {
    const url = new URL(request.url);

    if (request.method === 'POST' && url.pathname === '/ping') {
      let body;
      try {
        body = await request.json();
      } catch {
        return json({ ok: false }, 400);
      }
      const id = clean(body.id, 40);
      if (id.length < 16) return json({ ok: false }, 400);
      // Approximate location from the request IP, added by Cloudflare. Country level, and lat/lon
      // rounded to ~11 km, for the world map. No city name, no street, no address, no GPS.
      const cf = request.cf || {};
      const country = clean(cf.country, 2).toUpperCase() || '??';
      const city = cleanName(cf.city, 40) || '?';
      const round1 = (v) => (v == null || v === '' || isNaN(parseFloat(v)) ? null : Math.round(parseFloat(v) * 10) / 10);
      const lat = round1(cf.latitude), lon = round1(cf.longitude);
      await env.DB.prepare(
        `INSERT INTO pings (day, id, version, region, hop, gaming, connects, brand, model, android, country, lat, lon, city)
           VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
         ON CONFLICT (day, id) DO UPDATE SET version = excluded.version, region = excluded.region,
           hop = excluded.hop, gaming = excluded.gaming, connects = MAX(pings.connects, excluded.connects),
           brand = excluded.brand, model = excluded.model, android = excluded.android,
           country = excluded.country, lat = excluded.lat, lon = excluded.lon, city = excluded.city`
      )
        .bind(day(), id, clean(body.v, 16) || '?', clean(body.region, 8) || 'other', clean(body.hop, 12) || 'off',
          body.gaming ? 1 : 0, Math.min(Math.max(parseInt(body.connects) || 0, 0), 10000),
          cleanName(body.brand, 30) || '?', cleanName(body.model, 40) || '?', clean(body.android, 8) || '?',
          country, lat, lon, city)
        .run();
      return json({ ok: true });
    }

    const active = async (days) =>
      (await env.DB.prepare('SELECT COUNT(DISTINCT id) AS n FROM pings WHERE day >= ?').bind(day(days - 1)).first()).n;

    if (url.pathname === '/badge') {
      const n = await active(30);
      return json({ schemaVersion: 1, label: 'نصب فعال / active', message: short(n), color: '2563eb' });
    }

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
    if (url.pathname.startsWith('/settings/') && request.method === 'POST') {
      if (!(await loggedIn(request, env))) return json({ ok: false, error: 'login' }, 401);
      const form = await request.formData();
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
        return json({ ok: true, deleted: r.meta?.changes ?? 0 });
      }
      if (url.pathname === '/settings/prune') {
        const keep = Math.min(Math.max(parseInt(form.get('days')) || 90, 7), 365);
        const r = await env.DB.prepare('DELETE FROM pings WHERE day < ?').bind(day(keep)).run();
        return json({ ok: true, deleted: r.meta?.changes ?? 0 });
      }
      return json({ ok: false }, 404);
    }

    if (url.pathname === '/' && request.method === 'GET') {
      return html((await loggedIn(request, env)) ? dashboardPage() : loginPage(false));
    }

    if (url.pathname === '/stats') {
      if (!(await loggedIn(request, env))) return json({ ok: false, error: 'login' }, 401);
      // The window: 7, 30 or 90 days up to today; prev is the window before it, for trends.
      const rangeDays = [7, 30, 90].includes(Number(url.searchParams.get('days'))) ? Number(url.searchParams.get('days')) : 30;
      const start = day(rangeDays - 1);
      const prevStart = day(2 * rangeDays - 1);
      const prevEnd = day(rangeDays);

      // Distinct installs, new installs and connections within [from, to].
      const window = async (from, to) =>
        (await env.DB.prepare(
          `SELECT COUNT(DISTINCT p.id) AS users,
             COUNT(DISTINCT CASE WHEN f.first BETWEEN ? AND ? THEN p.id END) AS newUsers,
             COALESCE(SUM(p.connects), 0) AS connects
           FROM pings p JOIN (SELECT id, MIN(day) AS first FROM pings GROUP BY id) f ON f.id = p.id
           WHERE p.day BETWEEN ? AND ?`
        ).bind(from, to, from, to).first());

      const group = async (column) =>
        (await env.DB.prepare(
          `SELECT ${column} AS k, COUNT(DISTINCT id) AS n FROM pings WHERE day >= ? GROUP BY ${column} ORDER BY n DESC`
        ).bind(start).all()).results;

      const series = (await env.DB.prepare(
        `SELECT p.day AS day, COUNT(DISTINCT p.id) AS active, COALESCE(SUM(p.connects),0) AS connects,
           COUNT(DISTINCT CASE WHEN f.first = p.day THEN p.id END) AS newUsers
         FROM pings p JOIN (SELECT id, MIN(day) AS first FROM pings GROUP BY id) f ON f.id = p.id
         WHERE p.day >= ? GROUP BY p.day ORDER BY p.day`
      ).bind(start).all()).results;

      return json({
        rangeDays,
        updated: new Date().toISOString(),
        totalUsers: (await env.DB.prepare('SELECT COUNT(DISTINCT id) AS n FROM pings').first()).n,
        active: { day: await active(1), week: await active(7), month: await active(30) },
        period: await window(start, day()),
        prev: await window(prevStart, prevEnd),
        series,
        breakdown: {
          byVersion: await group('version'),
          byRegion: await group('region'),
          byHop: await group('hop'),
          gaming: await group('gaming'),
          byBrand: await group('brand'),
          byModel: (await group("brand || ' ' || model")).slice(0, 15),
          byAndroid: await group('android'),
          byCountry: (await group('country')).filter((r) => r.k && r.k !== '??').slice(0, 20),
          byCity: (await group("country || '|' || city")).filter((r) => r.k && !r.k.endsWith('|?')).slice(0, 20),
        },
        // One point per user area (approximate IP location, rounded to ~11 km), for the map.
        points: (await env.DB.prepare(
          `SELECT lat, lon, COUNT(DISTINCT id) AS n FROM pings WHERE day >= ? AND lat IS NOT NULL AND lon IS NOT NULL GROUP BY lat, lon`
        ).bind(start).all()).results,
      });
    }

    return json({ ok: false }, 404);
  },
};
