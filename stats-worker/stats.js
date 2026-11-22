// The panel's numbers. Every function here reads the database in a few passes and counts the rows it
// read (D1's free plan has a daily limit), and the callers keep the results for a while.
import { reader, day, now, dbGet, dbSet } from './db.js';

/**
 * What the dashboard can be narrowed to, and the column each is in. The value list comes from the
 * unfiltered breakdowns, so only values that exist are offered.
 */
export const FILTERS = {
  country: 'country', region: 'region', version: 'version', server: 'server', hop: 'hop',
  isp: 'asn', brand: 'brand', android: 'android',
};

/** The filter in the request (?f=country&v=IR), or null. */
export function parseFilter(params) {
  const key = params.get('f');
  const col = FILTERS[key];
  const raw = params.get('v');
  if (!col || raw == null || raw === '') return null;
  const val = col === 'asn' ? parseInt(raw) || 0 : String(raw).slice(0, 60);
  return { key, col, val, tag: key + '=' + encodeURIComponent(String(val)).slice(0, 60) };
}

/** Network names that are servers, not homes or phones: a ping from there went through a VPN. */
const HOSTING = /hosting|host|cloud|data ?cent|datacenter|server|hetzner|ovh|digitalocean|amazon|aws|google|microsoft|azure|oracle|m247|contabo|linode|akamai|vultr|choopa|leaseweb|scaleway|alibaba|tencent|cdn77|datacamp|g-core|gcore|stark|clouvider|psychz|colocrossing|hostinger|ionos|kamatera|frantech|buyvm|aeza|timeweb|selectel|serverius|worldstream|packet|cogent/i;
export const isHosting = (isp) => HOSTING.test(isp || '');

/** Bytes moved by a connection that sends nothing back: below this a user "connected but got nothing". */
const DEAD_BYTES = 200 * 1024;

/**
 * Everything the dashboard shows for the last [rangeDays] days, optionally narrowed to one value of
 * one column. The breakdowns use each install's latest day in the window (its current version,
 * hop, phone, place), read in one pass; that pass also sums its connections, failures and bytes.
 */
export async function computeStats(env, rangeDays, filter) {
  const r = reader(env);
  const today = day();
  const span = Math.max(rangeDays, 30); // active week/month come from the same pass
  const start = day(rangeDays - 1);
  const prevStart = day(2 * rangeDays - 1);
  const prevEnd = day(rangeDays);
  const from = prevStart < start ? prevStart : start;
  const fSql = filter ? ` AND ${filter.col} = ?` : '';
  const fSqlP = filter ? ` AND p.${filter.col} = ?` : '';
  const fArgs = filter ? [filter.val] : [];

  const latest = await r.all(env.DB.prepare(
    `SELECT p.day, p.version, p.region, p.hop, p.gaming, p.brand, p.model, p.android, p.country, p.city,
       p.lat, p.lon, p.server, p.asn, p.isp, p.via, i.first, l.up, l.down, l.cn, l.fl
     FROM (SELECT id, MAX(day) AS d,
             SUM(CASE WHEN day >= ? THEN up END) AS up, SUM(CASE WHEN day >= ? THEN down END) AS down,
             SUM(CASE WHEN day >= ? THEN connects ELSE 0 END) AS cn, SUM(CASE WHEN day >= ? THEN fails END) AS fl
           FROM pings WHERE day >= ?${fSql} GROUP BY id) l
     JOIN pings p ON p.id = l.id AND p.day = l.d
     LEFT JOIN installs i ON i.id = l.id`
  ).bind(start, start, start, start, day(span - 1), ...fArgs));

  // Per-day totals. Unfiltered, past days are rolled up once into daily (they never change).
  const perDay = {};
  let newByDay = {};
  if (!filter) {
    const kept = await r.all(env.DB.prepare('SELECT day, active, connects, up, down, fails FROM daily WHERE day >= ? AND day < ?').bind(from, today));
    const have = new Set(kept.map((x) => x.day));
    const missing = [];
    for (let i = 2 * rangeDays - 1; i >= 1; i--) if (!have.has(day(i))) missing.push(day(i));
    if (missing.length) {
      const rolled = await r.all(env.DB.prepare(
        `SELECT day, COUNT(*) AS active, COALESCE(SUM(connects), 0) AS connects, COALESCE(SUM(up), 0) AS up,
           COALESCE(SUM(down), 0) AS down, COALESCE(SUM(fails), 0) AS fails
         FROM pings WHERE day BETWEEN ? AND ? GROUP BY day`
      ).bind(missing[0], missing[missing.length - 1]));
      const byDay = Object.fromEntries(rolled.map((x) => [x.day, x]));
      const rows = missing.map((d) => byDay[d] || { day: d, active: 0, connects: 0, up: 0, down: 0, fails: 0 });
      // A day with nothing yet is kept too, except yesterday: late pings can still land for it.
      const writes = rows.filter((x) => x.active > 0 || x.day < day(1)).map((x) => env.DB.prepare(
        'INSERT OR REPLACE INTO daily (day, active, connects, up, down, fails) VALUES (?, ?, ?, ?, ?, ?)'
      ).bind(x.day, x.active, x.connects, x.up, x.down, x.fails));
      for (let i = 0; i < writes.length; i += 50) await env.DB.batch(writes.slice(i, i + 50));
      kept.push(...rows);
    }
    for (const x of kept) perDay[x.day] = x;
    perDay[today] = { day: today, ...(await r.first(env.DB.prepare(
      `SELECT COUNT(*) AS active, COALESCE(SUM(connects), 0) AS connects, COALESCE(SUM(up), 0) AS up,
         COALESCE(SUM(down), 0) AS down, COALESCE(SUM(fails), 0) AS fails FROM pings WHERE day = ?`
    ).bind(today))) };
    newByDay = Object.fromEntries((await r.all(env.DB.prepare(
      'SELECT first AS day, COUNT(*) AS n FROM installs WHERE first >= ? GROUP BY first'
    ).bind(from))).map((x) => [x.day, x.n]));
  } else {
    for (const x of await r.all(env.DB.prepare(
      `SELECT day, COUNT(*) AS active, COALESCE(SUM(connects), 0) AS connects, COALESCE(SUM(up), 0) AS up,
         COALESCE(SUM(down), 0) AS down, COALESCE(SUM(fails), 0) AS fails
       FROM pings WHERE day >= ?${fSql} GROUP BY day`
    ).bind(from, ...fArgs))) perDay[x.day] = x;
    // New installs whose first day matched the filter.
    newByDay = Object.fromEntries((await r.all(env.DB.prepare(
      `SELECT p.day, COUNT(*) AS n FROM pings p JOIN installs i ON i.id = p.id AND i.first = p.day
       WHERE p.day >= ?${fSqlP} GROUP BY p.day`
    ).bind(from, ...fArgs))).map((x) => [x.day, x.n]));
  }

  const series = [];
  for (let i = rangeDays - 1; i >= 0; i--) {
    const d = day(i), x = perDay[d] || {};
    series.push({ day: d, active: x.active || 0, connects: x.connects || 0, fails: x.fails || 0, up: x.up || 0, down: x.down || 0, newUsers: newByDay[d] || 0 });
  }
  const sumIn = (a, b, f) => Object.values(perDay).filter((x) => x.day >= a && x.day <= b).reduce((s, x) => s + (x[f] || 0), 0);
  const newIn = (a, b) => Object.entries(newByDay).filter(([d]) => d >= a && d <= b).reduce((s, [, n]) => s + n, 0);

  // Distinct installs in the window before: it is all in the past, so it is counted once and kept.
  const prevKey = `win_${prevStart}_${prevEnd}` + (filter ? '_' + filter.tag : '');
  const keptPrev = await dbGet(env, prevKey);
  let prevUsers = Number(keptPrev);
  if (!keptPrev) {
    prevUsers = (await r.first(env.DB.prepare(`SELECT COUNT(DISTINCT id) AS n FROM pings WHERE day BETWEEN ? AND ?${fSql}`)
      .bind(prevStart, prevEnd, ...fArgs))).n || 0;
    await dbSet(env, prevKey, String(prevUsers));
  }

  const inRange = latest.filter((x) => x.day >= start);
  // Place and network only from pings that did not go through the VPN (then they are the server's).
  const located = inRange.filter((x) => x.via !== 'vpn');
  const count = (rows, key, top) => {
    const m = new Map();
    for (const x of rows) { const k = key(x); m.set(k, (m.get(k) || 0) + 1); }
    const out = [...m].map(([k, n]) => ({ k, n })).sort((a, b) => b.n - a.n);
    return top ? out.slice(0, top) : out;
  };
  const points = count(located.filter((x) => x.lat != null && x.lon != null), (x) => x.lat + ',' + x.lon)
    .map(({ k, n }) => { const [lat, lon] = k.split(','); return { lat: Number(lat), lon: Number(lon), n }; });
  const ispKey = (x) => x.asn + '|' + (x.isp || '?');

  // Bytes per group, put on each install's latest server, hop and place in the window.
  const volume = (rows, key, top) => {
    const m = new Map();
    for (const x of rows) {
      const b = (x.up || 0) + (x.down || 0);
      if (b > 0) { const k = key(x); m.set(k, (m.get(k) || 0) + b); }
    }
    const out = [...m].map(([k, n]) => ({ k, n })).sort((a, b) => b.n - a.n);
    return top ? out.slice(0, top) : out;
  };
  const reporting = inRange.filter((x) => x.up != null || x.down != null);
  const GB = 1024 ** 3;
  const BUCKETS = [['0', 0], ['<100MB', 100 * 1024 ** 2], ['100MB-1GB', GB], ['1-5GB', 5 * GB], ['5-20GB', 20 * GB], ['>20GB', Infinity]];
  const perUser = BUCKETS.map(([k]) => ({ k, n: 0 }));
  for (const x of reporting) {
    const b = (x.up || 0) + (x.down || 0);
    perUser[b === 0 ? 0 : BUCKETS.findIndex(([, max], i) => i > 0 && b < max)].n++;
  }

  // Connections that worked and that failed (versions that report failures, 0.2.8 on), per group.
  const failing = inRange.filter((x) => x.fl != null);
  const rate = (rows, key, top) => {
    const m = new Map();
    for (const x of rows) {
      const k = key(x), v = m.get(k) || { k, ok: 0, fail: 0, users: 0 };
      v.ok += x.cn || 0; v.fail += x.fl || 0; v.users++;
      m.set(k, v);
    }
    const out = [...m.values()].filter((v) => v.ok + v.fail > 0).sort((a, b) => b.ok + b.fail - (a.ok + a.fail));
    return top ? out.slice(0, top) : out;
  };
  // "Connected but got nothing": connections made, under DEAD_BYTES moved. A filtered method shows here.
  const dead = (rows, key) => {
    const m = new Map();
    for (const x of rows) {
      if (!(x.cn > 0) || (x.up == null && x.down == null)) continue;
      const k = key(x), v = m.get(k) || { k, n: 0, dead: 0 };
      v.n++; if ((x.up || 0) + (x.down || 0) < DEAD_BYTES) v.dead++;
      m.set(k, v);
    }
    return [...m.values()].sort((a, b) => b.n - a.n);
  };

  // Week-1 retention: of installs first seen 7 to 13 days ago, how many were back in the last 7 days.
  const cohort = latest.filter((x) => x.first && x.first >= day(13) && x.first <= day(7));
  const retention = { cohort: cohort.length, back: cohort.filter((x) => x.day >= day(6)).length };
  const active = {
    day: latest.filter((x) => x.day === today).length,
    week: latest.filter((x) => x.day >= day(6)).length,
    month: latest.filter((x) => x.day >= day(29)).length,
  };
  const last7 = series.slice(-7);
  const users = {
    // Active in the window, first seen before it.
    returning: inRange.filter((x) => x.first && x.first < start).length,
    // Seen in the last 30 days but not in the last 7.
    lost: latest.filter((x) => x.day >= day(29) && x.day < day(6)).length,
    // Average daily users of the last 7 days over the month's users: how many come back every day.
    dauMau: active.month ? last7.reduce((s, x) => s + x.active, 0) / last7.length / active.month : 0,
  };

  // The last 24 hours, by hour (pings from 0.2.8 on, and every ping once this panel is deployed).
  const since = now() - 86400;
  const hourly = await r.all(env.DB.prepare(
    `SELECT (at / 3600) AS h, COUNT(*) AS n FROM pings WHERE at >= ?${fSql} GROUP BY h`
  ).bind(since, ...fArgs));

  const totalUsers = filter ? null : (await r.first(env.DB.prepare('SELECT COUNT(*) AS n FROM installs'))).n || 0;

  return {
    rangeDays,
    filter: filter ? { key: filter.key, val: filter.val } : null,
    updated: new Date().toISOString(),
    totalUsers,
    active,
    period: {
      users: inRange.length, newUsers: newIn(start, today), connects: sumIn(start, today, 'connects'),
      fails: sumIn(start, today, 'fails'), up: sumIn(start, today, 'up'), down: sumIn(start, today, 'down'),
    },
    prev: {
      users: prevUsers, newUsers: newIn(prevStart, prevEnd), connects: sumIn(prevStart, prevEnd, 'connects'),
      fails: sumIn(prevStart, prevEnd, 'fails'), up: sumIn(prevStart, prevEnd, 'up'), down: sumIn(prevStart, prevEnd, 'down'),
    },
    retention,
    users,
    series,
    hourly: hourly.map((x) => ({ at: x.h * 3600, n: x.n })),
    breakdown: {
      byVersion: count(inRange, (x) => x.version),
      byRegion: count(inRange, (x) => x.region),
      byHop: count(inRange, (x) => x.hop),
      byServer: count(inRange, (x) => x.server || '?'),
      gaming: count(inRange, (x) => x.gaming),
      byBrand: count(inRange, (x) => x.brand),
      byModel: count(inRange, (x) => x.brand + ' ' + x.model, 15),
      byAndroid: count(inRange, (x) => x.android),
      byCountry: count(located.filter((x) => x.country && x.country !== '??'), (x) => x.country, 20),
      byCity: count(located.filter((x) => x.city && x.city !== '?'), (x) => x.country + '|' + x.city, 20),
      byIsp: count(located.filter((x) => x.asn), ispKey, 25),
      byVia: count(inRange, (x) => x.via || '?'),
    },
    traffic: {
      reporting: reporting.length,
      byServer: volume(inRange, (x) => x.server || '?'),
      byHop: volume(inRange, (x) => x.hop),
      byRegion: volume(inRange, (x) => x.region),
      byCountry: volume(located.filter((x) => x.country && x.country !== '??'), (x) => x.country, 20),
      byIsp: volume(located.filter((x) => x.asn), ispKey, 15),
      perUser,
    },
    conn: {
      reporting: failing.length,
      byHop: rate(failing, (x) => x.hop),
      byServer: rate(failing, (x) => x.server || '?'),
      byVersion: rate(failing, (x) => x.version),
      byCountry: rate(failing.filter((x) => x.via !== 'vpn' && x.country && x.country !== '??'), (x) => x.country, 15),
      byIsp: rate(failing.filter((x) => x.via !== 'vpn' && x.asn), ispKey, 15),
      deadByHop: dead(inRange, (x) => x.hop),
      deadByServer: dead(inRange, (x) => x.server || '?'),
    },
    // One point per user area (approximate IP location, rounded to ~11 km), for the map.
    points,
    rowsRead: r.rows,
  };
}

/**
 * Weekly cohorts (of the installs first seen in each of the last 8 weeks, how many were seen again
 * in each week after) and the versions in use per day for the last 45 days.
 */
export async function computeUsers(env) {
  const r = reader(env);
  const today = day();
  const cohorts = await r.all(env.DB.prepare(
    `SELECT CAST((julianday(?) - julianday(i.first)) / 7 AS INTEGER) AS c,
       CAST((julianday(p.day) - julianday(i.first)) / 7 AS INTEGER) AS w, COUNT(DISTINCT p.id) AS n
     FROM installs i JOIN pings p ON p.id = i.id AND p.day >= i.first
     WHERE i.first >= ? GROUP BY c, w`
  ).bind(today, day(7 * 8 - 1)));

  // Versions per day: past days rolled up once into daily_dim (a "*" row marks a rolled day).
  const DAYS = 45;
  const kept = await r.all(env.DB.prepare("SELECT day, k, n FROM daily_dim WHERE dim = 'version' AND day >= ? AND day < ?").bind(day(DAYS - 1), today));
  const rolled = new Set(kept.filter((x) => x.k === '*').map((x) => x.day));
  const missing = [];
  for (let i = DAYS - 1; i >= 1; i--) if (!rolled.has(day(i))) missing.push(day(i));
  const rows = kept.filter((x) => x.k !== '*');
  if (missing.length) {
    const got = await r.all(env.DB.prepare('SELECT day, version AS k, COUNT(*) AS n FROM pings WHERE day BETWEEN ? AND ? GROUP BY day, version')
      .bind(missing[0], missing[missing.length - 1]));
    const wanted = new Set(missing);
    const fresh = got.filter((x) => wanted.has(x.day));
    rows.push(...fresh);
    // Yesterday is not kept yet: late pings can still land for it.
    const done = missing.filter((d) => d < day(1));
    const writes = [
      ...fresh.filter((x) => x.day < day(1)).map((x) => env.DB.prepare('INSERT OR REPLACE INTO daily_dim (day, dim, k, n) VALUES (?, ?, ?, ?)').bind(x.day, 'version', x.k, x.n)),
      ...done.map((d) => env.DB.prepare("INSERT OR REPLACE INTO daily_dim (day, dim, k, n) VALUES (?, 'version', '*', 0)").bind(d)),
    ];
    for (let i = 0; i < writes.length; i += 50) await env.DB.batch(writes.slice(i, i + 50));
  }
  rows.push(...(await r.all(env.DB.prepare('SELECT ? AS day, version AS k, COUNT(*) AS n FROM pings WHERE day = ? GROUP BY version').bind(today, today))));

  return { updated: new Date().toISOString(), cohorts, versions: rows, days: DAYS, rowsRead: r.rows };
}

/**
 * The newest pings (no install ID, no city, no phone model: country, version, method and maker
 * only), how many came in the last hour and day, and areas seen in the last hour for the map.
 */
export async function computeLive(env) {
  const r = reader(env);
  const t = now();
  const feed = await r.all(env.DB.prepare(
    `SELECT at, country, version, hop, server, brand, region, via, asn, isp, connects, fails, up, down
     FROM pings WHERE at >= ? ORDER BY at DESC LIMIT 40`
  ).bind(t - 86400));
  const counts = await r.first(env.DB.prepare(
    'SELECT COUNT(*) AS day, SUM(at >= ?) AS hour, SUM(at >= ?) AS quarter FROM pings WHERE at >= ?'
  ).bind(t - 3600, t - 900, t - 86400));
  const spots = await r.all(env.DB.prepare(
    "SELECT lat, lon, COUNT(*) AS n FROM pings WHERE at >= ? AND lat IS NOT NULL AND via != 'vpn' GROUP BY lat, lon"
  ).bind(t - 3600));
  return {
    at: t,
    feed: feed.map((x) => ({ ...x, isp: x.via === 'vpn' ? null : x.isp })),
    counts: { day: counts.day || 0, hour: counts.hour || 0, quarter: counts.quarter || 0 },
    spots,
    rowsRead: r.rows,
  };
}
