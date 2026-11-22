// The database: schema upkeep, the small settings table, the panel's daily read budget and dates.

/**
 * Rows the panel may read in one UTC day (of D1's 5 million). Past it the panel shows its kept
 * numbers until midnight, so the app's pings always have room.
 */
export const STATS_DAILY_READS = 2_000_000;

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
  // NULL for versions that do not send them, so "none" and "not reported" stay apart.
  up: 'INTEGER',
  down: 'INTEGER',
  fails: 'INTEGER',
  // The network the ping came from (Cloudflare): its AS number and name, never the address.
  asn: 'INTEGER',
  isp: "TEXT NOT NULL DEFAULT '?'",
  // When the ping came (unix seconds), for the live feed, the last-24-hours chart and alerts.
  at: 'INTEGER',
  // How the ping went out: "direct", or "vpn" (then its place and network are the VPN server's).
  via: "TEXT NOT NULL DEFAULT '?'",
};
/** Columns daily has gained. */
const DAILY_COLUMNS = {
  up: 'INTEGER NOT NULL DEFAULT 0',
  down: 'INTEGER NOT NULL DEFAULT 0',
  fails: 'INTEGER NOT NULL DEFAULT 0',
};

let schemaReady = null;
/** Brings the database up to date, once per Worker instance. */
export function ensureSchema(env) {
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
    env.DB.prepare('CREATE TABLE IF NOT EXISTS daily_dim (day TEXT NOT NULL, dim TEXT NOT NULL, k TEXT NOT NULL, n INTEGER NOT NULL, PRIMARY KEY (day, dim, k))'),
  ]);
  const columns = async (table) => new Set((await env.DB.prepare(`PRAGMA table_info(${table})`).all()).results.map((c) => c.name));
  const have = await columns('pings');
  for (const [name, type] of Object.entries(PING_COLUMNS)) {
    if (!have.has(name)) await env.DB.prepare(`ALTER TABLE pings ADD COLUMN ${name} ${type}`).run();
  }
  const haveDaily = await columns('daily');
  for (const [name, type] of Object.entries(DAILY_COLUMNS)) {
    if (!haveDaily.has(name)) await env.DB.prepare(`ALTER TABLE daily ADD COLUMN ${name} ${type}`).run();
  }
  await env.DB.prepare('CREATE INDEX IF NOT EXISTS pings_at ON pings(at)').run();
  // The installs table came later: fill it once from the pings already there.
  if ((await dbGet(env, 'installs_filled')) !== '1') {
    await env.DB.prepare('INSERT OR IGNORE INTO installs (id, first) SELECT id, MIN(day) FROM pings GROUP BY id').run();
    await dbSet(env, 'installs_filled', '1');
  }
}

// A small key/value table for settings and kept results.
export async function dbGet(env, key) {
  try { return (await env.DB.prepare('SELECT value FROM settings WHERE key = ?').bind(key).first())?.value ?? null; } catch { return null; }
}
export async function dbSet(env, key, value) {
  await env.DB.prepare('INSERT INTO settings (key, value) VALUES (?, ?) ON CONFLICT (key) DO UPDATE SET value = excluded.value').bind(key, value).run();
}
export const dbDel = (env, key) => env.DB.prepare('DELETE FROM settings WHERE key = ?').bind(key).run();
export const parse = (text) => { try { return text ? JSON.parse(text) : null; } catch { return null; } };

/** Rows the panel read today, and adding to them. */
export const readsToday = async (env) => Number(await dbGet(env, 'reads_' + day())) || 0;
export async function charge(env, rows) {
  if (rows > 0) await dbSet(env, 'reads_' + day(), String((await readsToday(env)) + rows));
}
export const overBudget = async (env) => (await readsToday(env)) >= STATS_DAILY_READS;

/** A counter of rows read, wrapped around D1 calls. */
export function reader(env) {
  const r = {
    rows: 0,
    all: async (stmt) => { const res = await stmt.all(); r.rows += res.meta?.rows_read || 0; return res.results; },
    first: async (stmt) => (await r.all(stmt))[0] || {},
  };
  return r;
}

/** YYYY-MM-DD (UTC), [offset] days ago. */
export const day = (offset = 0) => new Date(Date.now() - offset * 86400000).toISOString().slice(0, 10);
export const now = () => Math.floor(Date.now() / 1000);

/** What went wrong, in words the owner can act on. */
export function describe(e) {
  const m = String(e?.message || e || '');
  if (/limit|exceeded|quota|too many/i.test(m)) {
    return 'سقف روزانهٔ رایگان پایگاه‌داده (D1) تمام شده است. تا ساعت ۰۰:۰۰ UTC (۳:۳۰ بامداد تهران) صبر کنید. ' + m.slice(0, 160);
  }
  if (/no such table|no such column/i.test(m)) return 'جدول‌های پایگاه‌داده کامل نیستند: ' + m.slice(0, 160);
  return m.slice(0, 200) || 'خطای ناشناخته';
}

export const json = (body, status = 200, extra = {}) =>
  new Response(JSON.stringify(body), {
    status,
    headers: { 'content-type': 'application/json', 'access-control-allow-origin': '*', 'cache-control': 'no-store', ...extra },
  });
