// Dr VPN anonymous usage stats (Cloudflare Worker + D1).
//   POST /ping    {"id","v","region","hop","gaming","connects"}  from the app, once a day
//   GET  /stats   JSON: active installs (1, 7, 30 days), all-time installs, by version / region / hop
//   GET  /badge   shields.io endpoint JSON: active installs in the last 30 days
// Nothing about the person is kept: the request's IP address is never read or stored.

const json = (body, status = 200) =>
  new Response(JSON.stringify(body), {
    status,
    headers: { 'content-type': 'application/json', 'access-control-allow-origin': '*', 'cache-control': 'max-age=300' },
  });

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
      await env.DB.prepare(
        `INSERT INTO pings (day, id, version, region, hop, gaming, connects) VALUES (?, ?, ?, ?, ?, ?, ?)
         ON CONFLICT (day, id) DO UPDATE SET version = excluded.version, region = excluded.region,
           hop = excluded.hop, gaming = excluded.gaming, connects = MAX(pings.connects, excluded.connects)`
      )
        .bind(day(), id, clean(body.v, 16) || '?', clean(body.region, 8) || 'other', clean(body.hop, 12) || 'off',
          body.gaming ? 1 : 0, Math.min(Math.max(parseInt(body.connects) || 0, 0), 10000))
        .run();
      return json({ ok: true });
    }

    const active = async (days) =>
      (await env.DB.prepare('SELECT COUNT(DISTINCT id) AS n FROM pings WHERE day >= ?').bind(day(days - 1)).first()).n;

    if (url.pathname === '/badge') {
      const n = await active(30);
      return json({ schemaVersion: 1, label: 'نصب فعال / active', message: short(n), color: '2563eb' });
    }

    if (url.pathname === '/stats') {
      const since30 = day(29);
      const group = async (column) =>
        (await env.DB.prepare(
          `SELECT ${column} AS k, COUNT(DISTINCT id) AS n FROM pings WHERE day >= ? GROUP BY ${column} ORDER BY n DESC`
        ).bind(since30).all()).results;
      const daily = (await env.DB.prepare(
        'SELECT day, COUNT(*) AS installs, SUM(connects) AS connects FROM pings WHERE day >= ? GROUP BY day ORDER BY day'
      ).bind(since30).all()).results;
      return json({
        active: { day: await active(1), week: await active(7), month: await active(30) },
        installsAllTime: (await env.DB.prepare('SELECT COUNT(DISTINCT id) AS n FROM pings').first()).n,
        last30Days: {
          byVersion: await group('version'),
          byRegion: await group('region'),
          byHop: await group('hop'),
          gaming: await group('gaming'),
          daily,
        },
      });
    }

    return json({ ok: true, service: 'Dr VPN stats' });
  },
};
