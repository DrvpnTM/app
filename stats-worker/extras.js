// What the panel does beyond the app's stats: GitHub downloads, the in-app announcement
// (notice.json in the app's repo) and Telegram reports and alerts.
import { dbGet, dbSet, parse, day, now, reader } from './db.js';

const REPO = 'DrvpnTM/app';
const repo = (env) => env.GITHUB_REPO || REPO;

// ---------- GitHub ----------

/** A token for GitHub: the GITHUB_TOKEN secret, else the one saved in the panel's settings. */
export const githubToken = async (env) => env.GITHUB_TOKEN || (await dbGet(env, 'github_token')) || '';

async function gh(env, path, init = {}) {
  const token = await githubToken(env);
  const res = await fetch('https://api.github.com' + path, {
    ...init,
    headers: {
      'user-agent': 'drvpn-stats-panel',
      accept: 'application/vnd.github+json',
      'x-github-api-version': '2022-11-28',
      ...(token ? { authorization: 'Bearer ' + token } : {}),
      ...(init.headers || {}),
    },
  });
  if (!res.ok) {
    const text = (await res.text()).slice(0, 200);
    const err = new Error(`GitHub ${res.status}: ${text}`);
    err.status = res.status;
    throw err;
  }
  return res.json();
}

/**
 * APK downloads per release (app versions and country editions, not CI's build files), kept for an
 * hour: GitHub allows 60 calls an hour without a token.
 */
export async function downloads(env, fresh) {
  const cached = parse(await dbGet(env, 'gh_downloads'));
  if (cached && Date.now() - cached.at < (fresh ? 5 * 60000 : 3600000)) return cached.data;
  try {
    const releases = [];
    for (let page = 1; page <= 3; page++) {
      const got = await gh(env, `/repos/${repo(env)}/releases?per_page=100&page=${page}`);
      releases.push(...got);
      if (got.length < 100) break;
    }
    const out = releases
      .filter((r) => /^(v|countries-v)\d/.test(r.tag_name))
      .map((r) => {
        const apks = (r.assets || []).filter((a) => a.name.endsWith('.apk'));
        return {
          tag: r.tag_name,
          at: r.published_at,
          edition: r.tag_name.startsWith('countries-') ? 'countries' : 'main',
          n: apks.reduce((s, a) => s + a.download_count, 0),
          assets: apks.map((a) => ({ name: a.name, n: a.download_count })).sort((a, b) => b.n - a.n),
        };
      })
      .sort((a, b) => (a.at < b.at ? 1 : -1));
    const data = { at: Date.now(), repo: repo(env), total: out.reduce((s, r) => s + r.n, 0), releases: out };
    // A daily snapshot of the total, for the "downloads per day" chart.
    const hist = parse(await dbGet(env, 'gh_history')) || {};
    hist[day()] = data.total;
    const keys = Object.keys(hist).sort().slice(-120);
    data.history = Object.fromEntries(keys.map((k) => [k, hist[k]]));
    await dbSet(env, 'gh_history', JSON.stringify(data.history));
    await dbSet(env, 'gh_downloads', JSON.stringify({ at: Date.now(), data }));
    return data;
  } catch (e) {
    if (cached) return { ...cached.data, stale: true, error: String(e.message || e) };
    throw e;
  }
}

/** notice.json as it is on GitHub now, and whether the panel can change it (a token is set). */
export async function readNotice(env) {
  const token = await githubToken(env);
  if (token) {
    const file = await gh(env, `/repos/${repo(env)}/contents/notice.json?ref=main`);
    const text = new TextDecoder().decode(Uint8Array.from(atob(file.content.replace(/\n/g, '')), (c) => c.charCodeAt(0)));
    return { notice: JSON.parse(text), sha: file.sha, canEdit: true };
  }
  const res = await fetch(`https://raw.githubusercontent.com/${repo(env)}/main/notice.json`, { cf: { cacheTtl: 0 } });
  if (!res.ok) throw new Error('notice.json: ' + res.status);
  return { notice: await res.json(), sha: null, canEdit: false };
}

const LANGS = ['fa', 'en', 'ru', 'zh'];
const text = (v, max) => String(v ?? '').replace(/\r/g, '').slice(0, max);

/**
 * Saves the announcement: only enabled, title, message and link change; everything else in the
 * file (stats_url) is kept. [renew] gives it a new id, so people who closed the last one see it.
 */
export async function writeNotice(env, input) {
  const { notice, sha, canEdit } = await readNotice(env);
  if (!canEdit) throw new Error('برای ذخیرهٔ اعلان، اول توکن گیت‌هاب را در تنظیمات بگذارید.');
  if (input.sha && input.sha !== sha) throw new Error('اعلان در این فاصله روی گیت‌هاب عوض شده است. صفحه را تازه کنید.');
  const next = { ...notice };
  next.enabled = !!input.enabled;
  // Also a system notification, once per id (app 0.2.8 on).
  if (input.notify) next.notify = true; else delete next.notify;
  for (const field of ['title', 'message']) {
    const obj = {};
    for (const lang of LANGS) {
      const v = text(input[field]?.[lang], field === 'title' ? 120 : 1200).trim();
      if (v) obj[lang] = v;
    }
    next[field] = obj;
  }
  const link = text(input.link, 300).trim();
  if (link && !/^https?:\/\//.test(link)) throw new Error('لینک باید با https:// شروع شود.');
  if (link) next.link = link; else delete next.link;
  if (input.renew || !next.id) {
    next.id = 'n-' + new Date().toISOString().slice(0, 16).replace(/[-:T]/g, '');
  }
  // Keep "stats_url" last, as in the file.
  if ('stats_url' in next) { const s = next.stats_url; delete next.stats_url; next.stats_url = s; }
  const body = JSON.stringify(next, null, 2) + '\n';
  const content = btoa(String.fromCharCode(...new TextEncoder().encode(body)));
  const res = await gh(env, `/repos/${repo(env)}/contents/notice.json`, {
    method: 'PUT',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({
      message: next.enabled ? `Announcement: ${next.title.en || next.title.fa || next.id}` : 'Announcement: off',
      content,
      sha,
      branch: 'main',
    }),
  });
  return { notice: next, sha: res.content?.sha, commit: res.commit?.html_url };
}

// ---------- Telegram ----------

/** {token, chat, daily, alerts} saved in the panel's settings, or the TELEGRAM_* secrets. */
export async function telegramConfig(env) {
  const saved = parse(await dbGet(env, 'telegram')) || {};
  return {
    token: saved.token || env.TELEGRAM_BOT_TOKEN || '',
    chat: saved.chat || env.TELEGRAM_CHAT_ID || '',
    daily: saved.daily !== false,
    alerts: saved.alerts !== false,
  };
}

export async function telegramSend(env, message, config) {
  const c = config || (await telegramConfig(env));
  if (!c.token || !c.chat) return { ok: false, error: 'ربات تلگرام تنظیم نشده است' };
  const res = await fetch(`https://api.telegram.org/bot${c.token}/sendMessage`, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ chat_id: c.chat, text: message, parse_mode: 'HTML', disable_web_page_preview: true }),
  });
  const body = await res.json().catch(() => ({}));
  return body.ok ? { ok: true } : { ok: false, error: body.description || 'Telegram ' + res.status };
}

const fa = (n) => Number(n || 0).toLocaleString('fa-IR');
const pct = (a, b) => (b ? Math.round((a / b) * 100) : 0);
function vol(b) {
  const units = ['بایت', 'کیلوبایت', 'مگابایت', 'گیگابایت', 'ترابایت'];
  let i = 0; b = Number(b || 0);
  while (b >= 1024 && i < units.length - 1) { b /= 1024; i++; }
  return Number(b.toFixed(b >= 100 || i === 0 ? 0 : 1)).toLocaleString('fa-IR') + ' ' + units[i];
}
const arrow = (cur, prev) => {
  if (!prev) return '';
  const p = Math.round(((cur - prev) / prev) * 100);
  return p === 0 ? ' (بدون تغییر)' : ` (${p > 0 ? '▲' : '▼'} ${fa(Math.abs(p))}٪)`;
};

/** Yesterday in a few lines, sent every morning (04:17 UTC, 07:47 Tehran). */
export async function dailyReport(env, panelUrl) {
  const r = reader(env);
  const y = day(1), y2 = day(2);
  const sum = (d) => r.first(env.DB.prepare(
    `SELECT COUNT(*) AS active, COALESCE(SUM(connects), 0) AS connects, COALESCE(SUM(fails), 0) AS fails,
       COUNT(fails) AS failing, COALESCE(SUM(up), 0) + COALESCE(SUM(down), 0) AS bytes FROM pings WHERE day = ?`
  ).bind(d));
  const a = await sum(y), b = await sum(y2);
  const fresh = (await r.first(env.DB.prepare('SELECT COUNT(*) AS n FROM installs WHERE first = ?').bind(y))).n || 0;
  const total = (await r.first(env.DB.prepare('SELECT COUNT(*) AS n FROM installs'))).n || 0;
  const countries = await r.all(env.DB.prepare(
    "SELECT country AS k, COUNT(*) AS n FROM pings WHERE day = ? AND country != '??' AND via != 'vpn' GROUP BY country ORDER BY n DESC LIMIT 3"
  ).bind(y));
  const versions = await r.all(env.DB.prepare('SELECT version AS k, COUNT(*) AS n FROM pings WHERE day = ? GROUP BY version ORDER BY n DESC LIMIT 3').bind(y));
  const lines = [
    `📊 <b>گزارش دیروز دکتر وی پی ان</b> (${y})`,
    '',
    `👥 کاربران فعال: <b>${fa(a.active)}</b>${arrow(a.active, b.active)}`,
    `🆕 نصب جدید: <b>${fa(fresh)}</b> · کل نصب‌ها: ${fa(total)}`,
    `🔌 اتصال‌ها: <b>${fa(a.connects)}</b>${a.failing ? ` · موفقیت: <b>${fa(pct(a.connects, a.connects + a.fails))}٪</b>` : ''}`,
    a.bytes ? `📦 حجم مصرفی: <b>${vol(a.bytes)}</b>${arrow(a.bytes, b.bytes)}` : null,
    countries.length ? `🌍 کشورها: ${countries.map((c) => `${c.k} ${fa(c.n)}`).join(' · ')}` : null,
    versions.length ? `📱 نسخه‌ها: ${versions.map((v) => `v${v.k} ${fa(v.n)}`).join(' · ')}` : null,
    panelUrl ? `\n<a href="${panelUrl}">باز کردن پنل</a>` : null,
  ].filter((x) => x != null);
  return { text: lines.join('\n'), rowsRead: r.rows };
}

/**
 * Checks of the last 3 hours against the same hours yesterday: far fewer pings than usual, or most
 * connections failing. Each alert is sent at most once every 6 hours.
 */
export async function checkAlerts(env, config) {
  const r = reader(env);
  const t = now(), w = 3 * 3600;
  const cur = await r.first(env.DB.prepare(
    'SELECT COUNT(*) AS n, COALESCE(SUM(connects), 0) AS ok, COALESCE(SUM(fails), 0) AS fail FROM pings WHERE at >= ?'
  ).bind(t - w));
  const prev = await r.first(env.DB.prepare('SELECT COUNT(*) AS n FROM pings WHERE at >= ? AND at < ?').bind(t - 86400 - w, t - 86400));
  const alerts = [];
  if (prev.n >= 30 && cur.n < prev.n * 0.5) {
    alerts.push(['drop', `⚠️ <b>افت شدید کاربران</b>\nسه ساعت اخیر ${fa(cur.n)} گزارش رسید؛ همین ساعت‌ها دیروز ${fa(prev.n)}. ممکن است اپ یا سرور آمار فیلتر شده باشد.`]);
  }
  const tries = cur.ok + cur.fail;
  if (tries >= 20 && cur.fail / tries > 0.5) {
    alerts.push(['fail', `🚨 <b>بیشتر اتصال‌ها ناموفق است</b>\nسه ساعت اخیر ${fa(pct(cur.fail, tries))}٪ از ${fa(tries)} تلاش برای اتصال ناموفق بود.`]);
  }
  const sent = [];
  for (const [kind, message] of alerts) {
    const key = 'alert_' + kind;
    const last = Number(await dbGet(env, key)) || 0;
    if (Date.now() - last < 6 * 3600000) continue;
    const res = await telegramSend(env, message, config);
    if (res.ok) { await dbSet(env, key, String(Date.now())); sent.push(kind); }
  }
  return { sent, rowsRead: r.rows };
}
