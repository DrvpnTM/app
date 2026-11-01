// The admin panel's two pages: login and dashboard. Persian, right to left, light and dark.

const head = (title) => `<!doctype html>
<html lang="fa" dir="rtl">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="robots" content="noindex">
<title>${title}</title>
<link rel="preconnect" href="https://fonts.googleapis.com">
<link href="https://fonts.googleapis.com/css2?family=Vazirmatn:wght@400;500;700;800&display=swap" rel="stylesheet">
<style>
:root{--bg:#f4f6fb;--card:#fff;--text:#0f172a;--muted:#64748b;--line:#e2e8f0;--blue:#2563eb;--green:#16a34a;--violet:#7c3aed;--amber:#f59e0b;--pink:#db2777;--shadow:0 1px 2px rgba(15,23,42,.04),0 8px 24px rgba(15,23,42,.06)}
@media (prefers-color-scheme:dark){:root{--bg:#0b1120;--card:#121a2e;--text:#e2e8f0;--muted:#94a3b8;--line:#1e293b;--shadow:0 1px 2px rgba(0,0,0,.3),0 8px 24px rgba(0,0,0,.35)}}
*{box-sizing:border-box}
body{margin:0;background:var(--bg);color:var(--text);font-family:Vazirmatn,system-ui,sans-serif;-webkit-font-smoothing:antialiased}
.logo{width:44px;height:44px;border-radius:13px;background:linear-gradient(160deg,#60a5fa,#1d4ed8);display:grid;place-items:center;box-shadow:0 6px 18px rgba(37,99,235,.35)}
.logo svg{width:24px;height:24px}
button,input{font:inherit}
</style>`;

const logo = `<div class="logo"><svg viewBox="0 0 24 24" fill="none"><path d="M12 2.8l7.5 3v5.7c0 4.6-3.2 8.4-7.5 9.7-4.3-1.3-7.5-5.1-7.5-9.7V5.8z" fill="#fff" fill-opacity=".95"/><path d="M12 8.2v7.6M8.2 12h7.6" stroke="#2563eb" stroke-width="2.4" stroke-linecap="round"/></svg></div>`;

export const loginPage = (failed) => `${head('ورود · آمار دکتر وی پی ان')}
<style>
.wrap{min-height:100vh;display:grid;place-items:center;padding:16px}
.box{width:100%;max-width:380px;background:var(--card);border:1px solid var(--line);border-radius:22px;padding:32px 26px;box-shadow:var(--shadow)}
.brand{display:flex;align-items:center;gap:12px;margin-bottom:26px}
.brand b{font-size:18px}.brand span{display:block;color:var(--muted);font-size:13px}
label{display:block;font-size:13px;color:var(--muted);margin:14px 0 6px}
input{width:100%;padding:12px 14px;border-radius:12px;border:1px solid var(--line);background:var(--bg);color:var(--text);outline:none;direction:ltr;text-align:left}
input:focus{border-color:var(--blue);box-shadow:0 0 0 3px rgba(37,99,235,.18)}
button{width:100%;margin-top:22px;padding:13px;border:0;border-radius:12px;background:linear-gradient(160deg,#3b82f6,#1d4ed8);color:#fff;font-weight:700;cursor:pointer}
button:hover{filter:brightness(1.08)}
.err{margin-top:16px;padding:10px 12px;border-radius:10px;background:rgba(220,38,38,.1);color:#dc2626;font-size:13px}
</style>
</head>
<body><div class="wrap"><form class="box" method="post" action="/login">
<div class="brand">${logo}<div><b>دکتر وی پی ان</b><span>پنل آمار</span></div></div>
<label for="u">نام کاربری</label><input id="u" name="user" autocomplete="username" required autofocus>
<label for="p">رمز عبور</label><input id="p" name="password" type="password" autocomplete="current-password" required>
<button type="submit">ورود</button>
${failed ? '<div class="err">نام کاربری یا رمز عبور اشتباه است.</div>' : ''}
</form></div></body></html>`;

export const dashboardPage = () => `${head('آمار دکتر وی پی ان')}
<style>
.top{position:sticky;top:0;z-index:5;background:color-mix(in srgb,var(--bg) 85%,transparent);backdrop-filter:blur(10px);border-bottom:1px solid var(--line)}
.top .in{max-width:1180px;margin:auto;padding:12px 16px;display:flex;align-items:center;gap:12px}
.top b{font-size:17px}.top small{display:block;color:var(--muted);font-size:12px}
.top .sp{flex:1}
.top a{color:var(--muted);text-decoration:none;font-size:13px;padding:8px 12px;border:1px solid var(--line);border-radius:10px}
.top a:hover{color:var(--text)}
main{max-width:1180px;margin:auto;padding:18px 16px 40px}
.kpis{display:grid;grid-template-columns:repeat(5,1fr);gap:12px}
.kpi{background:var(--card);border:1px solid var(--line);border-radius:18px;padding:16px;box-shadow:var(--shadow);position:relative;overflow:hidden}
.kpi:before{content:"";position:absolute;inset:0 0 auto 0;height:3px;background:var(--c)}
.kpi span{color:var(--muted);font-size:13px}
.kpi b{display:block;font-size:30px;font-weight:800;margin-top:6px;letter-spacing:-.5px}
.kpi i{font-style:normal;color:var(--muted);font-size:12px}
.grid{display:grid;grid-template-columns:2fr 1fr;gap:12px;margin-top:12px}
.grid4{display:grid;grid-template-columns:repeat(auto-fit,minmax(200px,1fr));gap:12px;margin-top:12px}
.card{background:var(--card);border:1px solid var(--line);border-radius:18px;padding:16px;box-shadow:var(--shadow);min-width:0}
.card h3{margin:0 0 12px;font-size:15px}
.card h3 small{color:var(--muted);font-weight:400;font-size:12px;margin-inline-start:6px}
.chart{position:relative;height:260px}
.small .chart{height:220px}
.empty{color:var(--muted);text-align:center;padding:70px 0;font-size:14px}
table{width:100%;border-collapse:collapse;font-size:14px}
td{padding:9px 6px;border-bottom:1px solid var(--line)}
tr:last-child td{border-bottom:0}
td.n{text-align:left;font-weight:700;width:70px}
td.rank{color:var(--muted);width:28px}
.bar{height:6px;border-radius:6px;background:linear-gradient(90deg,#60a5fa,#2563eb);margin-top:5px}
.note{color:var(--muted);font-size:12px;margin-top:16px;text-align:center}
@media (max-width:900px){.kpis{grid-template-columns:repeat(2,1fr)}.kpis .kpi:last-child{grid-column:1/-1}.grid{grid-template-columns:1fr}}

</style>
<script src="https://cdnjs.cloudflare.com/ajax/libs/Chart.js/4.4.1/chart.umd.min.js"></script>
</head>
<body>
<div class="top"><div class="in">${logo}<div><b>آمار دکتر وی پی ان</b><small id="upd">در حال بارگذاری…</small></div><div class="sp"></div><a href="/logout">خروج</a></div></div>
<main>
<section class="kpis">
<div class="kpi" style="--c:var(--blue)"><span>کاربران فعال امروز</span><b id="k1">–</b><i>امروز برنامه را باز کرده‌اند</i></div>
<div class="kpi" style="--c:var(--violet)"><span>کاربران فعال هفته</span><b id="k7">–</b><i>در ۷ روز اخیر</i></div>
<div class="kpi" style="--c:var(--green)"><span>کاربران فعال ماه</span><b id="k30">–</b><i>در ۳۰ روز اخیر</i></div>
<div class="kpi" style="--c:var(--amber)"><span>کل کاربران</span><b id="kall">–</b><i id="knew">جدید امروز: –</i></div>
<div class="kpi" style="--c:var(--pink)"><span>اتصال امروز</span><b id="kcon">–</b><i>تعداد وصل شدن‌ها</i></div>
</section>
<section class="grid">
<div class="card"><h3>کاربران فعال روزانه <small>۳۰ روز اخیر</small></h3><div class="chart"><canvas id="cDaily"></canvas></div></div>
<div class="card"><h3>منطقه</h3><div class="chart"><canvas id="cRegion"></canvas></div></div>
</section>
<section class="grid">
<div class="card"><h3>پرکاربردترین گوشی‌ها <small>۳۰ روز اخیر</small></h3><div id="tModels"></div></div>
<div class="card"><h3>برند گوشی</h3><div class="chart"><canvas id="cBrand"></canvas></div></div>
</section>
<section class="grid4 small">
<div class="card"><h3>نسخهٔ اندروید</h3><div class="chart"><canvas id="cAndroid"></canvas></div></div>
<div class="card"><h3>اتصال‌ها در روز</h3><div class="chart"><canvas id="cConnects"></canvas></div></div>
<div class="card"><h3>حلقهٔ اضافه</h3><div class="chart"><canvas id="cHop"></canvas></div></div>
<div class="card"><h3>نسخهٔ برنامه</h3><div class="chart"><canvas id="cVersion"></canvas></div></div>
<div class="card"><h3>حالت گیمینگ</h3><div class="chart"><canvas id="cGaming"></canvas></div></div>
</section>
<p class="note">فقط آمار ناشناس: یک شناسهٔ تصادفی برای هر نصب، بدون IP و بدون ترافیک. هر دقیقه به‌روز می‌شود.</p>
</main>
<script>
const fa = (n) => Number(n || 0).toLocaleString('fa-IR');
const REGION = { ir: 'ایران', cn: 'چین', ru: 'روسیه', other: 'سایر کشورها' };
const HOP = { off: 'بدون حلقه', lantern: 'جادهٔ ابریشم', shield: 'دیوار چین', psiphon: 'PS Pro', tunnel: 'تونل دکتر', tor: 'CDN سیاه نا امن', emergency: 'حالت اضطراری' };
const HOP_COLOR = { off: '#94a3b8', lantern: '#0d9488', shield: '#c62828', psiphon: '#f59e0b', tunnel: '#2563eb', tor: '#1f2937', emergency: '#db2777' };
const PALETTE = ['#2563eb', '#7c3aed', '#16a34a', '#f59e0b', '#db2777', '#0d9488', '#64748b', '#ea580c'];
const dark = matchMedia('(prefers-color-scheme: dark)').matches;
Chart.defaults.font.family = 'Vazirmatn, system-ui, sans-serif';
Chart.defaults.color = dark ? '#94a3b8' : '#64748b';
Chart.defaults.borderColor = dark ? '#1e293b' : '#e2e8f0';
Chart.defaults.plugins.legend.labels.usePointStyle = true;
Chart.defaults.plugins.tooltip.rtl = true;
Chart.defaults.plugins.tooltip.callbacks.label = (c) => ' ' + (c.dataset.label ? c.dataset.label + ': ' : c.label + ': ') + fa(c.parsed.y ?? c.parsed);
const charts = {};
function draw(id, config) {
  const el = document.getElementById(id);
  const has = config.data.datasets.some((d) => d.data.some((v) => v > 0));
  el.parentElement.querySelector('.empty')?.remove();
  el.style.display = has ? '' : 'none';
  if (!has) { el.insertAdjacentHTML('afterend', '<div class="empty">هنوز داده‌ای نیست</div>'); return; }
  if (charts[id]) { charts[id].data = config.data; charts[id].update(); return; }
  charts[id] = new Chart(el, { ...config, options: { maintainAspectRatio: false, ...config.options } });
}
const doughnut = (rows, names, colors) => ({
  type: 'doughnut',
  data: { labels: rows.map((r) => names?.[r.k] ?? r.k), datasets: [{ data: rows.map((r) => r.n), backgroundColor: rows.map((r, i) => colors?.[r.k] ?? PALETTE[i % PALETTE.length]), borderWidth: 0 }] },
  options: { cutout: '64%', plugins: { legend: { position: 'bottom' } } },
});
function days30(daily) {
  const map = Object.fromEntries(daily.map((d) => [d.day, d]));
  const out = [];
  for (let i = 29; i >= 0; i--) {
    const d = new Date(Date.now() - i * 864e5).toISOString().slice(0, 10);
    out.push({ day: d, label: new Date(d).toLocaleDateString('fa-IR', { month: 'short', day: 'numeric' }), ...(map[d] || { installs: 0, connects: 0, newInstalls: 0 }) });
  }
  return out;
}
async function load() {
  const r = await fetch('/stats', { credentials: 'same-origin' });
  if (r.status === 401) { location.reload(); return; }
  const s = await r.json();
  document.getElementById('k1').textContent = fa(s.active.day);
  document.getElementById('k7').textContent = fa(s.active.week);
  document.getElementById('k30').textContent = fa(s.active.month);
  document.getElementById('kall').textContent = fa(s.installsAllTime);
  document.getElementById('knew').textContent = 'جدید امروز: ' + fa(s.today.newInstalls);
  document.getElementById('kcon').textContent = fa(s.today.connects);
  document.getElementById('upd').textContent = 'به‌روزرسانی: ' + new Date(s.updated).toLocaleTimeString('fa-IR', { hour: '2-digit', minute: '2-digit' });
  const d = days30(s.last30Days.daily), labels = d.map((x) => x.label);
  draw('cDaily', { type: 'line', data: { labels, datasets: [
    { label: 'فعال', data: d.map((x) => x.installs), borderColor: '#2563eb', backgroundColor: 'rgba(37,99,235,.12)', fill: true, tension: .35, pointRadius: 0, borderWidth: 2.5 },
    { label: 'نصب جدید', data: d.map((x) => x.newInstalls), borderColor: '#16a34a', backgroundColor: 'transparent', tension: .35, pointRadius: 0, borderWidth: 2 },
  ] }, options: { interaction: { mode: 'index', intersect: false }, scales: { y: { beginAtZero: true, ticks: { precision: 0, callback: fa } }, x: { grid: { display: false }, ticks: { maxTicksLimit: 8 } } } } });
  draw('cConnects', { type: 'bar', data: { labels, datasets: [{ label: 'اتصال', data: d.map((x) => x.connects), backgroundColor: '#db2777', borderRadius: 6 }] },
    options: { plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true, ticks: { precision: 0, callback: fa } }, x: { grid: { display: false }, ticks: { maxTicksLimit: 6 } } } } });
  draw('cRegion', doughnut(s.last30Days.byRegion, REGION));
  draw('cHop', doughnut(s.last30Days.byHop, HOP, HOP_COLOR));
  draw('cVersion', doughnut(s.last30Days.byVersion.map((r) => ({ ...r, k: 'v' + r.k }))));
  const brands = s.last30Days.byBrand || [], models = s.last30Days.byModel || [], androids = s.last30Days.byAndroid || [];
  draw('cBrand', doughnut(brands.map((r) => ({ ...r, k: r.k === '?' ? 'نامشخص (نسخهٔ قدیمی)' : r.k }))));
  draw('cAndroid', { type: 'bar', data: { labels: androids.map((r) => r.k === '?' ? 'نامشخص' : 'اندروید ' + r.k), datasets: [{ label: 'کاربر', data: androids.map((r) => r.n), backgroundColor: '#16a34a', borderRadius: 6 }] },
    options: { plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true, ticks: { precision: 0, callback: fa } }, x: { grid: { display: false } } } } });
  const top = models.filter((r) => !r.k.includes('?')), max = Math.max(1, ...top.map((r) => r.n));
  document.getElementById('tModels').innerHTML = top.length
    ? '<table>' + top.map((r, i) => '<tr><td class="rank">' + fa(i + 1) + '</td><td>' + r.k.replace(/[<>&]/g, '') + '<div class="bar" style="width:' + Math.round(r.n / max * 100) + '%"></div></td><td class="n">' + fa(r.n) + '</td></tr>').join('') + '</table>'
    : '<div class="empty">هنوز داده‌ای نیست (از نسخهٔ ۰٫۲٫۰ ثبت می‌شود)</div>';
  draw('cGaming', doughnut(s.last30Days.gaming.map((r) => ({ ...r, k: r.k ? 'روشن' : 'خاموش' })), null, { 'روشن': '#7c3aed', 'خاموش': '#cbd5e1' }));
}
load(); setInterval(load, 60000);
</script>
</body></html>`;
