// The admin panel's two pages: login and dashboard. Persian, right to left, light and dark.

const head = (title) => `<!doctype html>
<html lang="fa" dir="rtl">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="robots" content="noindex">
<title>${title}</title>
<link rel="preconnect" href="https://fonts.googleapis.com">
<link href="https://fonts.googleapis.com/css2?family=Vazirmatn:wght@400;500;600;700;800&display=swap" rel="stylesheet">
<style>
:root{--bg:#eef2f9;--card:#fff;--card2:#f8fafc;--text:#0f172a;--muted:#64748b;--faint:#94a3b8;--line:#e5e9f2;--blue:#2563eb;--green:#16a34a;--red:#dc2626;--violet:#7c3aed;--amber:#f59e0b;--pink:#db2777;--shadow:0 1px 2px rgba(15,23,42,.04),0 10px 30px rgba(15,23,42,.07);--radius:18px;--blur:none;--bgimg:none}
/* dark values, shared by auto-dark and the explicit dark theme */
@media (prefers-color-scheme:dark){:root:not([data-theme]){--bg:#070c18;--card:#0f1726;--card2:#131c2e;--text:#e5edf7;--muted:#9aa7bd;--faint:#64748b;--line:#1c2740;--shadow:0 1px 2px rgba(0,0,0,.4),0 12px 34px rgba(0,0,0,.45)}}
:root[data-theme="dark"]{--bg:#070c18;--card:#0f1726;--card2:#131c2e;--text:#e5edf7;--muted:#9aa7bd;--faint:#64748b;--line:#1c2740;--shadow:0 1px 2px rgba(0,0,0,.4),0 12px 34px rgba(0,0,0,.45)}
/* glass: a soft aurora background with frosted, translucent cards */
:root[data-theme="glass"]{--bg:#e6eefc;--card:rgba(255,255,255,.58);--card2:rgba(255,255,255,.42);--text:#1f2a44;--muted:#566586;--faint:#8794b2;--line:rgba(120,140,190,.22);--shadow:0 10px 34px rgba(70,90,170,.16);--radius:22px;--blur:saturate(180%) blur(18px);--bgimg:radial-gradient(55vw 48vh at 90% -6%,rgba(130,190,255,.7),transparent 62%),radial-gradient(60vw 52vh at 8% 8%,rgba(150,205,255,.45),transparent 60%),radial-gradient(75vw 60vh at 50% 116%,rgba(170,140,255,.6),transparent 60%),linear-gradient(155deg,#dfeaff,#e8ecff 42%,#f0e8ff)}
:root[data-theme="glass"] .card,:root[data-theme="glass"] .kpi,:root[data-theme="glass"] .chip,:root[data-theme="glass"] .seg,:root[data-theme="glass"] .ico,:root[data-theme="glass"] select#theme{border-color:rgba(255,255,255,.75)}
:root[data-theme="glass"] .kpi,:root[data-theme="glass"] .card{background-image:linear-gradient(180deg,rgba(255,255,255,.35),transparent 40%)}
*{box-sizing:border-box}
body{margin:0;background:var(--bg);background-image:var(--bgimg);background-attachment:fixed;color:var(--text);font-family:Vazirmatn,system-ui,sans-serif;-webkit-font-smoothing:antialiased;font-feature-settings:"ss01"}
.logo{width:44px;height:44px;border-radius:13px;background:linear-gradient(160deg,#60a5fa,#1d4ed8);display:grid;place-items:center;box-shadow:0 6px 18px rgba(37,99,235,.4);flex:none}
.logo svg{width:24px;height:24px}
button,input,select{font:inherit}
</style>
<script>try{var t=localStorage.getItem('drvpn_theme');if(t&&t!=='auto')document.documentElement.setAttribute('data-theme',t)}catch(e){}</script>`;

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

export const dashboardPage = () => `${head('پنل مدیریت دکتر وی پی ان')}
<style>
.hdr{position:sticky;top:0;z-index:20;background:color-mix(in srgb,var(--bg) 82%,transparent);backdrop-filter:blur(12px);border-bottom:1px solid var(--line)}
.hdr .in{max-width:1240px;margin:auto;padding:12px 18px;display:flex;align-items:center;gap:14px}
.hdr b{font-size:16px;line-height:1.2}.hdr small{display:block;color:var(--muted);font-size:12px}
.hdr .brand{display:flex;align-items:center;gap:11px}
.sp{flex:1}
.seg{display:flex;background:var(--card2);border:1px solid var(--line);border-radius:12px;padding:3px;gap:2px}
.seg button{border:0;background:transparent;color:var(--muted);padding:7px 14px;border-radius:9px;cursor:pointer;font-size:13px;font-weight:600}
.seg button.on{background:var(--card);color:var(--text);box-shadow:0 1px 3px rgba(15,23,42,.12)}
.ico{display:grid;place-items:center;min-width:38px;height:38px;padding:0 12px;border:1px solid var(--line);border-radius:11px;background:var(--card);color:var(--muted);cursor:pointer;text-decoration:none;font-size:14px;font-weight:600}
.ico:hover{color:var(--text)}
.ico.spin svg{animation:sp 1s linear infinite}@keyframes sp{to{transform:rotate(360deg)}}
.card,.kpi,.chip,.seg,.ico,select#theme{backdrop-filter:var(--blur);-webkit-backdrop-filter:var(--blur)}
select#theme{height:38px;border:1px solid var(--line);border-radius:11px;background:var(--card);color:var(--muted);padding:0 10px;cursor:pointer;outline:none}
main{max-width:1240px;margin:auto;padding:18px}
.live{display:flex;flex-wrap:wrap;align-items:center;gap:10px;margin-bottom:16px;color:var(--muted);font-size:13px}
.load-error{color:var(--red);font-size:12px}.load-error[hidden]{display:none}
.chip{display:inline-flex;align-items:center;gap:7px;background:var(--card);border:1px solid var(--line);border-radius:999px;padding:6px 13px;box-shadow:var(--shadow)}
.chip b{color:var(--text);font-size:14px}
.dot{width:8px;height:8px;border-radius:50%;background:var(--green);box-shadow:0 0 0 4px rgba(22,163,74,.18)}
.kpis{display:grid;grid-template-columns:repeat(4,1fr);gap:14px}
.kpi{background:var(--card);border:1px solid var(--line);border-radius:var(--radius);padding:16px 16px 6px;box-shadow:var(--shadow);position:relative;overflow:hidden}
.kpi .top{display:flex;align-items:flex-start;justify-content:space-between;gap:8px}
.kpi .lbl{color:var(--muted);font-size:13px;font-weight:600}
.kpi .val{font-size:30px;font-weight:800;letter-spacing:-.5px;margin-top:8px}
.kpi .spark{height:46px;margin:4px -8px -2px}
.trend{font-size:12px;font-weight:700;padding:3px 8px;border-radius:999px;white-space:nowrap}
.trend.up{color:#15803d;background:rgba(22,163,74,.12)}
.trend.down{color:#dc2626;background:rgba(220,38,38,.12)}
.trend.flat{color:var(--faint);background:var(--card2)}
.sect{display:flex;align-items:center;gap:8px;margin:26px 2px 12px}
.sect h2{font-size:15px;margin:0;font-weight:700}
.sect span{width:7px;height:16px;border-radius:4px;background:var(--blue)}
.sect small{color:var(--faint);font-size:12px;font-weight:500}
.grid{display:grid;grid-template-columns:2fr 1fr;gap:14px}
.grid3{display:grid;grid-template-columns:repeat(3,1fr);gap:14px}
.grid2{display:grid;grid-template-columns:repeat(2,1fr);gap:14px}
.card{background:var(--card);border:1px solid var(--line);border-radius:var(--radius);padding:16px;box-shadow:var(--shadow);min-width:0}
.card h3{margin:0 0 14px;font-size:14px;font-weight:700;display:flex;justify-content:space-between;align-items:center}
.card h3 em{color:var(--faint);font-weight:500;font-size:12px;font-style:normal}
.chart{position:relative;height:280px}
.small .chart{height:230px}
.empty{color:var(--faint);text-align:center;padding:72px 0;font-size:14px}
table{width:100%;border-collapse:collapse;font-size:14px}
td{padding:9px 4px;border-bottom:1px solid var(--line)}
tr:last-child td{border-bottom:0}
td.n{text-align:left;font-weight:700;width:64px;font-variant-numeric:tabular-nums}
td.rank{color:var(--faint);width:26px;font-size:13px}
.bar{height:6px;border-radius:6px;background:linear-gradient(90deg,#93c5fd,#2563eb);margin-top:6px}
.note{color:var(--faint);font-size:12px;margin:26px 0 8px;text-align:center}
.skel{color:transparent;background:linear-gradient(90deg,var(--line),var(--card2),var(--line));background-size:200% 100%;animation:sh 1.3s infinite;border-radius:8px}
@keyframes sh{to{background-position:-200% 0}}
@media (max-width:1000px){.kpis{grid-template-columns:repeat(2,1fr)}.grid,.grid3,.grid2{grid-template-columns:1fr}}
@media (max-width:620px){.hdr small{display:none}.seg button{padding:7px 10px}}
</style>
<script src="https://cdnjs.cloudflare.com/ajax/libs/Chart.js/4.4.1/chart.umd.min.js"></script>
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.min.css">
<script src="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.min.js"></script>
</head>
<body>
<header class="hdr"><div class="in">
<div class="brand">${logo}<div><b>پنل مدیریت</b><small>آمار دکتر وی پی ان</small></div></div>
<div class="sp"></div>
<div class="seg" id="seg"><button data-d="7">۷ روز</button><button data-d="30" class="on">۳۰ روز</button><button data-d="90">۹۰ روز</button></div>
<select id="theme" title="تم"><option value="auto">خودکار</option><option value="light">روشن</option><option value="dark">تیره</option><option value="glass">شیشه‌ای</option></select>
<button class="ico" id="refresh" title="به‌روزرسانی"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><path d="M20 11A8 8 0 1 0 18.4 16"/><path d="M20 4v5h-5"/></svg></button>
<a class="ico" id="csv" title="دانلود CSV">CSV</a>
<a class="ico" href="/settings" title="تنظیمات"><svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.6 1.6 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.6 1.6 0 0 0-2.7 1.1V21a2 2 0 1 1-4 0v-.1A1.6 1.6 0 0 0 7 19.4a1.6 1.6 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.6 1.6 0 0 0-1.1-2.7H1a2 2 0 1 1 0-4h.1A1.6 1.6 0 0 0 2.6 7a1.6 1.6 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.6 1.6 0 0 0 1.8.3H7a1.6 1.6 0 0 0 1-1.5V1a2 2 0 1 1 4 0v.1a1.6 1.6 0 0 0 2.7 1.1 1.6 1.6 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.6 1.6 0 0 0-.3 1.8V7a1.6 1.6 0 0 0 1.5 1H23a2 2 0 1 1 0 4h-.1a1.6 1.6 0 0 0-1.5 1z"/></svg></a>
<a class="ico" href="/logout">خروج</a>
</div></header>
<main>
<div class="live" id="live"><span class="dot"></span><span>فعال امروز: <b id="aDay">–</b></span><span>·</span><span>هفته: <b id="aWeek">–</b></span><span>·</span><span>ماه: <b id="aMonth">–</b></span><span class="sp"></span><span id="upd"></span><span id="loadError" class="load-error" role="alert" hidden></span></div>

<section class="kpis">
<div class="kpi"><div class="top"><span class="lbl">کاربران فعال</span><span class="trend flat" id="tUsers">–</span></div><div class="val" id="vUsers">–</div><div class="spark"><canvas id="sUsers"></canvas></div></div>
<div class="kpi"><div class="top"><span class="lbl">کاربران جدید</span><span class="trend flat" id="tNew">–</span></div><div class="val" id="vNew">–</div><div class="spark"><canvas id="sNew"></canvas></div></div>
<div class="kpi"><div class="top"><span class="lbl">اتصال‌ها</span><span class="trend flat" id="tCon">–</span></div><div class="val" id="vCon">–</div><div class="spark"><canvas id="sCon"></canvas></div></div>
<div class="kpi"><div class="top"><span class="lbl">کل کاربران</span><span class="trend up" id="tAll">–</span></div><div class="val" id="vAll">–</div><div class="spark"><canvas id="sAll"></canvas></div></div>
</section>

<div class="sect"><span></span><h2>روند کاربران</h2><small id="rngLabel"></small></div>
<section class="grid">
<div class="card"><h3>کاربران فعال و جدید <em>در روز</em></h3><div class="chart"><canvas id="cDaily"></canvas></div></div>
<div class="card"><h3>منطقه</h3><div class="chart"><canvas id="cRegion"></canvas></div></div>
</section>

<div class="sect"><span style="background:var(--green)"></span><h2>نقشهٔ جهان</h2><small>موقعیت تقریبی از روی IP · یک نقطه برای هر ناحیه</small></div>
<section class="grid">
<div class="card"><h3>کاربران روی نقشه <em>با غلتک ماوس زوم، با کشیدن جابه‌جا</em></h3><div id="cMap" style="height:420px;border-radius:14px;overflow:hidden;z-index:0"></div></div>
<div class="card"><h3>کشورها <em>بر پایهٔ کاربر</em></h3><div id="tCountries" style="max-height:420px;overflow:auto"></div></div>
</section>
<section class="grid2" style="margin-top:14px">
<div class="card"><h3>شهرها <em>برای تبلیغ محلی · فقط تعداد</em></h3><div id="tCities"></div></div>
<div class="card"><h3>راهنما</h3><p style="color:var(--faint);font-size:13px;line-height:1.9;margin:0">هر نقطه روی نقشه، ناحیهٔ تقریبیِ یک یا چند کاربر است که از روی آدرس IP و با دقت حدود ۱۱ کیلومتر مشخص می‌شود. آدرس دقیق، خیابان یا موقعیت GPS جمع نمی‌شود. برای برنامه‌ریزی تبلیغات، تعداد کاربر در هر کشور و شهر کافی است.</p></div>
</section>

<div class="sect"><span style="background:var(--pink)"></span><h2>استفاده</h2></div>
<section class="grid3 small">
<div class="card"><h3>اتصال‌ها در روز</h3><div class="chart"><canvas id="cConnects"></canvas></div></div>
<div class="card"><h3>حلقهٔ اضافه</h3><div class="chart"><canvas id="cHop"></canvas></div></div>
<div class="card"><h3>حالت گیمینگ</h3><div class="chart"><canvas id="cGaming"></canvas></div></div>
</section>

<div class="sect"><span style="background:var(--violet)"></span><h2>دستگاه‌ها</h2></div>
<section class="grid">
<div class="card"><h3>پرکاربردترین گوشی‌ها <em>بر پایهٔ کاربر</em></h3><div id="tModels"></div></div>
<div class="card"><h3>برند گوشی</h3><div class="chart"><canvas id="cBrand"></canvas></div></div>
</section>
<section class="grid2 small" style="margin-top:14px">
<div class="card"><h3>نسخهٔ اندروید</h3><div class="chart"><canvas id="cAndroid"></canvas></div></div>
<div class="card"><h3>نسخهٔ برنامه</h3><div class="chart"><canvas id="cVersion"></canvas></div></div>
</section>

<p class="note">فقط آمار ناشناس: یک شناسهٔ تصادفی برای هر نصب، بدون IP و بدون ترافیک. هر دقیقه به‌روز می‌شود.</p>
</main>
<script>
const fa = (n) => Number(n || 0).toLocaleString('fa-IR');
const REGION = { ir: 'ایران', cn: 'چین', ru: 'روسیه', other: 'سایر کشورها' };
const HOP = { off: 'بدون حلقه', lantern: 'جادهٔ ابریشم', shield: 'دیوار چین', psiphon: 'PS Pro', tunnel: 'تونل دکتر', tor: 'CDN سیاه نا امن', emergency: 'حالت اضطراری' };
const HOP_COLOR = { off: '#94a3b8', lantern: '#0d9488', shield: '#c62828', psiphon: '#f59e0b', tunnel: '#2563eb', tor: '#334155', emergency: '#db2777' };
const PALETTE = ['#2563eb', '#7c3aed', '#16a34a', '#f59e0b', '#db2777', '#0d9488', '#64748b', '#ea580c', '#0891b2'];
const CNAME = { IR: 'ایران', CN: 'چین', RU: 'روسیه', US: 'آمریکا', DE: 'آلمان', TR: 'ترکیه', AE: 'امارات', GB: 'انگلیس', FR: 'فرانسه', NL: 'هلند', CA: 'کانادا', IN: 'هند', IQ: 'عراق', AF: 'افغانستان', PK: 'پاکستان', SE: 'سوئد', FI: 'فنلاند', IT: 'ایتالیا', ES: 'اسپانیا', PL: 'لهستان', AZ: 'آذربایجان', AM: 'ارمنستان', GE: 'گرجستان', QA: 'قطر', OM: 'عمان', KW: 'کویت', SA: 'عربستان', MY: 'مالزی', ID: 'اندونزی', JP: 'ژاپن', KR: 'کره جنوبی', AU: 'استرالیا', BR: 'برزیل', UA: 'اوکراین', BY: 'بلاروس', CH: 'سوئیس', AT: 'اتریش', BE: 'بلژیک', NO: 'نروژ', DK: 'دانمارک', CZ: 'چک', RO: 'رومانی' };
const flag = (cc) => (cc && cc.length === 2 && /^[A-Z]{2}$/.test(cc)) ? String.fromCodePoint(...[...cc].map((c) => 0x1f1e6 + c.charCodeAt(0) - 65)) : '🏳️';
const cname = (cc) => CNAME[cc] || cc;
// Leaflet world map: real pan/zoom. One small circle per user area (approximate IP location).
let leafMap = null, leafTiles = null, leafMarkers = null, leafDark = null, leafFitted = false;
function tileUrl() { return isDark() ? 'https://server.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Dark_Gray_Base/MapServer/tile/{z}/{y}/{x}' : 'https://server.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Light_Gray_Base/MapServer/tile/{z}/{y}/{x}'; }
function rows(el, data, label, color) {
  const max = Math.max(1, ...data.map((r) => r.n));
  el.innerHTML = data.length
    ? '<table>' + data.map((r, i) => '<tr><td class="rank">' + fa(i + 1) + '</td><td>' + label(r) + '<div class="bar" style="width:' + Math.round((r.n / max) * 100) + '%;background:' + (color || 'linear-gradient(90deg,#93c5fd,#2563eb)') + '"></div></td><td class="n">' + fa(r.n) + '</td></tr>').join('') + '</table>'
    : '<div class="empty">هنوز داده‌ای نیست</div>';
}
function renderMap(points) {
  const el = document.getElementById('cMap');
  if (typeof L === 'undefined') { el.innerHTML = '<div class="empty">نقشه بارگذاری نشد</div>'; return; }
  if (!leafMap) {
    leafMap = L.map(el, { worldCopyJump: true, zoomControl: true, attributionControl: true, minZoom: 2 }).setView([25, 20], 2);
    leafMarkers = L.layerGroup().addTo(leafMap);
  }
  // Tiles only swap when the theme actually changed (keeps the admin's current view).
  if (leafTiles === null || leafDark !== isDark()) {
    if (leafTiles) leafMap.removeLayer(leafTiles);
    leafDark = isDark();
    leafTiles = L.tileLayer(tileUrl(), { maxZoom: 16, attribution: 'Tiles &copy; Esri' }).addTo(leafMap);
  }
  leafMarkers.clearLayers();
  const pts = (points || []).filter((p) => p.lat != null && p.lon != null);
  const bounds = [];
  pts.forEach((p) => {
    const r = 4 + Math.min(16, Math.sqrt(p.n) * 2.4);
    L.circleMarker([p.lat, p.lon], { radius: r, color: '#1d4ed8', weight: 1, fillColor: '#2563eb', fillOpacity: 0.45 })
      .bindTooltip('کاربران این ناحیه: ' + fa(p.n), { direction: 'top' })
      .addTo(leafMarkers);
    bounds.push([p.lat, p.lon]);
  });
  // Fit to the data once; later refreshes keep whatever the admin is looking at.
  if (bounds.length && !leafFitted) { leafMap.fitBounds(bounds, { padding: [40, 40], maxZoom: 6 }); leafFitted = true; }
  setTimeout(() => leafMap.invalidateSize(), 60);
}
const css = (n) => getComputedStyle(document.documentElement).getPropertyValue(n).trim();
function isDark() { const t = document.documentElement.getAttribute('data-theme'); if (t === 'dark') return true; if (t === 'light' || t === 'glass') return false; return matchMedia('(prefers-color-scheme:dark)').matches; }
function chartTheme() { Chart.defaults.color = css('--muted') || '#64748b'; Chart.defaults.borderColor = css('--line') || '#e5e9f2'; }
Chart.defaults.font.family = 'Vazirmatn, system-ui, sans-serif';
Chart.defaults.font.size = 12;
chartTheme();
Chart.defaults.plugins.legend.labels.usePointStyle = true;
Chart.defaults.plugins.legend.labels.boxWidth = 8;
Chart.defaults.plugins.legend.labels.padding = 14;
Chart.defaults.plugins.tooltip.rtl = true;
Chart.defaults.plugins.tooltip.boxPadding = 6;
Chart.defaults.plugins.tooltip.callbacks.label = (c) => ' ' + (c.dataset.label ? c.dataset.label + ': ' : c.label + ': ') + fa(c.parsed.y ?? c.parsed);

const charts = {};
function put(id, config) {
  const el = document.getElementById(id);
  const has = config.data.datasets.some((d) => d.data.some((v) => v > 0));
  el.parentElement.querySelector('.empty')?.remove();
  el.style.display = has ? '' : 'none';
  if (!has) { el.insertAdjacentHTML('afterend', '<div class="empty">هنوز داده‌ای نیست</div>'); return; }
  if (charts[id]) { charts[id].data = config.data; charts[id].update('none'); return; }
  charts[id] = new Chart(el, Object.assign({}, config, { options: Object.assign({ maintainAspectRatio: false }, config.options) }));
}
const doughnut = (rows, names, colors) => ({
  type: 'doughnut',
  data: { labels: rows.map((r) => (names && names[r.k]) || r.k), datasets: [{ data: rows.map((r) => r.n), backgroundColor: rows.map((r, i) => (colors && colors[r.k]) || PALETTE[i % PALETTE.length]), borderWidth: 0, hoverOffset: 6 }] },
  options: { cutout: '66%', plugins: { legend: { position: 'bottom' } } },
});
const bars = (labels, data, color) => ({
  type: 'bar',
  data: { labels, datasets: [{ label: 'کاربر', data, backgroundColor: color, borderRadius: 7, maxBarThickness: 46 }] },
  options: { plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true, ticks: { precision: 0, callback: fa } }, x: { grid: { display: false } } } },
});
function spark(id, data, color) {
  const el = document.getElementById(id);
  if (charts[id]) { charts[id].data.datasets[0].data = data; charts[id].update('none'); return; }
  charts[id] = new Chart(el, {
    type: 'line',
    data: { labels: data.map((_, i) => i), datasets: [{ data, borderColor: color, backgroundColor: color + '22', fill: true, tension: .4, pointRadius: 0, borderWidth: 2 }] },
    options: { maintainAspectRatio: false, plugins: { legend: { display: false }, tooltip: { enabled: false } }, scales: { x: { display: false }, y: { display: false } }, animation: false, elements: { line: { capBezierPoints: true } } },
  });
}
function trend(id, cur, prev) {
  const el = document.getElementById(id);
  let pct = prev > 0 ? Math.round(((cur - prev) / prev) * 100) : (cur > 0 ? 100 : 0);
  if (cur === 0 && prev === 0) { el.className = 'trend flat'; el.textContent = '—'; return; }
  el.className = 'trend ' + (pct >= 0 ? 'up' : 'down');
  el.textContent = (pct >= 0 ? '▲ ' : '▼ ') + fa(Math.abs(pct)) + '٪';
}
function fillDays(series, n) {
  const map = Object.fromEntries(series.map((d) => [d.day, d]));
  const out = [];
  for (let i = n - 1; i >= 0; i--) {
    const iso = new Date(Date.now() - i * 864e5).toISOString().slice(0, 10);
    const r = map[iso] || { active: 0, newUsers: 0, connects: 0 };
    out.push({ day: iso, label: new Date(iso).toLocaleDateString('fa-IR', { month: 'short', day: 'numeric' }), active: r.active || 0, newUsers: r.newUsers || 0, connects: r.connects || 0 });
  }
  return out;
}

let days = 30, lastRows = [], loadSequence = 0;
async function load() {
  const sequence = ++loadSequence;
  const btn = document.getElementById('refresh'); btn.classList.add('spin');
  const error = document.getElementById('loadError');
  error.hidden = true;
  try {
  const r = await fetch('/stats?days=' + days, { credentials: 'same-origin' });
  if (sequence !== loadSequence) return;
  if (r.status === 401) { location.reload(); return; }
  if (!r.ok) throw new Error('Stats request failed with status ' + r.status);
  const s = await r.json();
  if (sequence !== loadSequence) return;
  const d = fillDays(s.series, s.rangeDays); lastRows = d;
  document.getElementById('aDay').textContent = fa(s.active.day);
  document.getElementById('aWeek').textContent = fa(s.active.week);
  document.getElementById('aMonth').textContent = fa(s.active.month);
  document.getElementById('upd').textContent = 'به‌روزرسانی: ' + new Date(s.updated).toLocaleTimeString('fa-IR', { hour: '2-digit', minute: '2-digit' });
  document.getElementById('rngLabel').textContent = fa(s.rangeDays) + ' روز اخیر';

  document.getElementById('vUsers').textContent = fa(s.period.users);
  document.getElementById('vNew').textContent = fa(s.period.newUsers);
  document.getElementById('vCon').textContent = fa(s.period.connects);
  document.getElementById('vAll').textContent = fa(s.totalUsers);
  trend('tUsers', s.period.users, s.prev.users);
  trend('tNew', s.period.newUsers, s.prev.newUsers);
  trend('tCon', s.period.connects, s.prev.connects);
  document.getElementById('tAll').className = 'trend up';
  document.getElementById('tAll').textContent = '+' + fa(s.period.newUsers);

  spark('sUsers', d.map((x) => x.active), '#2563eb');
  spark('sNew', d.map((x) => x.newUsers), '#16a34a');
  spark('sCon', d.map((x) => x.connects), '#db2777');
  let base = s.totalUsers - d.reduce((a, x) => a + x.newUsers, 0), cum = [];
  d.forEach((x) => { base += x.newUsers; cum.push(base); });
  spark('sAll', cum, '#f59e0b');

  const labels = d.map((x) => x.label), skip = { ticks: { maxTicksLimit: s.rangeDays > 40 ? 8 : 10 } };
  put('cDaily', { type: 'line', data: { labels, datasets: [
    { label: 'فعال', data: d.map((x) => x.active), borderColor: '#2563eb', backgroundColor: 'rgba(37,99,235,.12)', fill: true, tension: .35, pointRadius: 0, borderWidth: 2.5 },
    { label: 'جدید', data: d.map((x) => x.newUsers), borderColor: '#16a34a', backgroundColor: 'transparent', tension: .35, pointRadius: 0, borderWidth: 2 },
  ] }, options: { interaction: { mode: 'index', intersect: false }, plugins: { legend: { position: 'top', align: 'end' } }, scales: { y: { beginAtZero: true, ticks: { precision: 0, callback: fa } }, x: Object.assign({ grid: { display: false } }, skip) } } });
  put('cConnects', { type: 'bar', data: { labels, datasets: [{ label: 'اتصال', data: d.map((x) => x.connects), backgroundColor: '#db2777', borderRadius: 5, maxBarThickness: 22 }] },
    options: { plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true, ticks: { precision: 0, callback: fa } }, x: Object.assign({ grid: { display: false } }, { ticks: { maxTicksLimit: 6 } }) } } });

  const bd = s.breakdown;
  put('cRegion', doughnut(bd.byRegion, REGION));
  put('cHop', doughnut(bd.byHop, HOP, HOP_COLOR));
  put('cGaming', doughnut(bd.gaming.map((r) => ({ n: r.n, k: r.k ? 'روشن' : 'خاموش' })), null, { 'روشن': '#7c3aed', 'خاموش': isDark() ? '#334155' : '#cbd5e1' }));
  put('cBrand', doughnut(bd.byBrand.map((r) => ({ n: r.n, k: r.k === '?' ? 'نامشخص' : r.k }))));
  put('cAndroid', bars(bd.byAndroid.map((r) => r.k === '?' ? 'نامشخص' : 'اندروید ' + r.k), bd.byAndroid.map((r) => r.n), '#16a34a'));
  put('cVersion', doughnut(bd.byVersion.map((r) => ({ n: r.n, k: 'v' + r.k }))));

  const top = bd.byModel.filter((r) => !r.k.includes('?')), max = Math.max(1, ...top.map((r) => r.n));
  document.getElementById('tModels').innerHTML = top.length
    ? '<table>' + top.map((r, i) => '<tr><td class="rank">' + fa(i + 1) + '</td><td>' + r.k.replace(/[<>&]/g, '') + '<div class="bar" style="width:' + Math.round((r.n / max) * 100) + '%"></div></td><td class="n">' + fa(r.n) + '</td></tr>').join('') + '</table>'
    : '<div class="empty">هنوز داده‌ای نیست (از نسخهٔ ۰٫۲٫۰ ثبت می‌شود)</div>';

  rows(document.getElementById('tCountries'), bd.byCountry || [], (r) => flag(r.k) + '&nbsp; ' + cname(r.k), 'linear-gradient(90deg,#86efac,#16a34a)');
  rows(document.getElementById('tCities'), (bd.byCity || []).map((r) => ({ n: r.n, k: r.k })), (r) => { const p = r.k.split('|'); return flag(p[0]) + '&nbsp; ' + (p[1] || '?').replace(/[<>&]/g, '') + ' <span style="color:var(--faint)">· ' + cname(p[0]) + '</span>'; }, 'linear-gradient(90deg,#c4b5fd,#7c3aed)');
  renderMap(s.points || []);
  } catch (e) {
    if (sequence === loadSequence) {
      error.textContent = 'بارگذاری آمار ناموفق بود. اتصال را بررسی کنید و دوباره تلاش کنید.';
      error.hidden = false;
      console.error('Failed to load dashboard statistics:', e);
    }
  } finally {
    if (sequence === loadSequence) btn.classList.remove('spin');
  }
}

document.getElementById('seg').addEventListener('click', (e) => {
  const b = e.target.closest('button'); if (!b) return;
  document.querySelectorAll('#seg button').forEach((x) => x.classList.remove('on'));
  b.classList.add('on'); days = Number(b.dataset.d); load();
});
document.getElementById('refresh').addEventListener('click', load);
const themeSel = document.getElementById('theme');
try { themeSel.value = localStorage.getItem('drvpn_theme') || 'auto'; } catch (e) {}
themeSel.addEventListener('change', () => {
  const v = themeSel.value;
  try { localStorage.setItem('drvpn_theme', v); } catch (e) {}
  if (v === 'auto') document.documentElement.removeAttribute('data-theme');
  else document.documentElement.setAttribute('data-theme', v);
  Object.values(charts).forEach((c) => c.destroy());
  for (const k in charts) delete charts[k];
  chartTheme();
  load();
});
document.getElementById('csv').addEventListener('click', (e) => {
  e.preventDefault();
  const rows = [['date', 'active_users', 'new_users', 'connects'], ...lastRows.map((x) => [x.day, x.active, x.newUsers, x.connects])];
  const url = URL.createObjectURL(new Blob([rows.map((r) => r.join(',')).join('\\n')], { type: 'text/csv' }));
  const a = document.createElement('a'); a.href = url; a.download = 'drvpn-stats-' + days + 'd.csv'; a.click(); URL.revokeObjectURL(url);
});
load(); setInterval(load, 60000);
</script>
</body></html>`;

export const settingsPage = () => `${head('تنظیمات · آمار دکتر وی پی ان')}
<style>
.hdr{position:sticky;top:0;z-index:20;background:color-mix(in srgb,var(--bg) 82%,transparent);backdrop-filter:blur(12px);border-bottom:1px solid var(--line)}
.hdr .in{max-width:720px;margin:auto;padding:12px 18px;display:flex;align-items:center;gap:12px}
.hdr b{font-size:16px}.hdr small{display:block;color:var(--muted);font-size:12px}
.hdr .brand{display:flex;align-items:center;gap:11px}.sp{flex:1}
.ico{display:grid;place-items:center;height:38px;padding:0 14px;border:1px solid var(--line);border-radius:11px;background:var(--card);color:var(--muted);cursor:pointer;text-decoration:none;font-size:13px;font-weight:600;backdrop-filter:var(--blur)}
.ico:hover{color:var(--text)}
main{max-width:720px;margin:auto;padding:18px}
.card{background:var(--card);border:1px solid var(--line);border-radius:var(--radius);padding:20px;box-shadow:var(--shadow);margin-bottom:16px;backdrop-filter:var(--blur)}
.card h3{margin:0 0 4px;font-size:15px}
.card p{margin:0 0 16px;color:var(--muted);font-size:13px;line-height:1.7}
label{display:block;font-size:13px;color:var(--muted);margin:12px 0 6px}
input{width:100%;padding:11px 13px;border-radius:11px;border:1px solid var(--line);background:var(--bg);color:var(--text);outline:none;direction:ltr;text-align:left}
input:focus{border-color:var(--blue);box-shadow:0 0 0 3px rgba(37,99,235,.18)}
.btn{margin-top:16px;padding:11px 18px;border:0;border-radius:11px;background:linear-gradient(160deg,#3b82f6,#1d4ed8);color:#fff;font-weight:700;cursor:pointer}
.btn:hover{filter:brightness(1.08)}.btn:disabled{opacity:.6;cursor:default}
.btn.ghost{background:var(--card2);color:var(--text);border:1px solid var(--line)}
.btn.danger{background:linear-gradient(160deg,#f87171,#dc2626)}
.row{display:flex;gap:10px;flex-wrap:wrap;align-items:flex-end}
.msg{margin-top:14px;padding:10px 12px;border-radius:10px;font-size:13px;display:none}
.msg.ok{display:block;background:rgba(22,163,74,.12);color:#15803d}
.msg.err{display:block;background:rgba(220,38,38,.12);color:#dc2626}
.danger-zone{border-color:rgba(220,38,38,.3)}
code{background:var(--card2);padding:2px 7px;border-radius:7px;direction:ltr;display:inline-block;font-size:12px}
select#theme{height:40px;border:1px solid var(--line);border-radius:11px;background:var(--bg);color:var(--text);padding:0 10px;cursor:pointer;outline:none}
</style></head>
<body>
<header class="hdr"><div class="in">
<div class="brand">${logo}<div><b>تنظیمات</b><small>آمار دکتر وی پی ان</small></div></div>
<div class="sp"></div>
<a class="ico" href="/">بازگشت به داشبورد</a>
</div></header>
<main>

<div class="card">
<h3>تم پیش‌فرض</h3>
<p>ظاهر پنل. روی همین مرورگر ذخیره می‌شود.</p>
<select id="theme"><option value="auto">خودکار (مثل سیستم)</option><option value="light">روشن</option><option value="dark">تیره</option><option value="glass">شیشه‌ای</option></select>
</div>

<div class="card">
<h3>تغییر رمز عبور</h3>
<p>نام کاربری ثابت است: <code>admin</code>. رمز جدید باید حداقل ۸ نویسه باشد. با تغییر رمز، همهٔ دستگاه‌های واردشده از پنل خارج می‌شوند.</p>
<label for="cur">رمز فعلی</label><input id="cur" type="password" autocomplete="current-password">
<label for="n1">رمز جدید</label><input id="n1" type="password" autocomplete="new-password">
<label for="n2">تکرار رمز جدید</label><input id="n2" type="password" autocomplete="new-password">
<button class="btn" id="pwBtn">ذخیرهٔ رمز</button>
<div class="msg" id="pwMsg"></div>
</div>

<div class="card">
<h3>اطلاعات</h3>
<p>نشان «نصب فعال» (عمومی):<br><code id="badgeUrl"></code></p>
<p>آمار فقط ناشناس است: شناسهٔ تصادفی هر نصب، بدون IP ذخیره‌شده، بدون ترافیک. موقعیت روی نقشه تقریبی و از روی IP است (گرد‌شده به ~۱۱ کیلومتر)، بدون آدرس دقیق.</p>
</div>

<div class="card danger-zone">
<h3>مدیریت داده</h3>
<p>حذف دادهٔ قدیمی برای سبک ماندن، یا پاک کردن کامل آمار (برگشت‌ناپذیر).</p>
<div class="row">
<div><label for="pdays">حذف دادهٔ قدیمی‌تر از (روز)</label><input id="pdays" type="number" value="90" min="7" max="365" style="width:120px"></div>
<button class="btn ghost" id="pruneBtn">حذف قدیمی‌ها</button>
</div>
<div style="margin-top:18px"><button class="btn danger" id="resetBtn">پاک کردن کامل آمار</button></div>
<div class="msg" id="dataMsg"></div>
</div>

</main>
<script>
const sel = document.getElementById('theme');
try { sel.value = localStorage.getItem('drvpn_theme') || 'auto'; } catch (e) {}
sel.addEventListener('change', () => {
  const v = sel.value; try { localStorage.setItem('drvpn_theme', v); } catch (e) {}
  if (v === 'auto') document.documentElement.removeAttribute('data-theme'); else document.documentElement.setAttribute('data-theme', v);
});
document.getElementById('badgeUrl').textContent = location.origin + '/badge';
function show(el, ok, text) { el.className = 'msg ' + (ok ? 'ok' : 'err'); el.textContent = text; }
async function post(path, form) { const r = await fetch(path, { method: 'POST', body: form, credentials: 'same-origin' }); let j = {}; try { j = await r.json(); } catch (e) {} return { r, j }; }

document.getElementById('pwBtn').addEventListener('click', async (e) => {
  const btn = e.target, msg = document.getElementById('pwMsg');
  const cur = document.getElementById('cur').value, n1 = document.getElementById('n1').value, n2 = document.getElementById('n2').value;
  if (n1 !== n2) return show(msg, false, 'رمز جدید و تکرارش یکی نیستند');
  if (n1.length < 8) return show(msg, false, 'رمز جدید باید حداقل ۸ نویسه باشد');
  btn.disabled = true;
  try {
    const f = new FormData(); f.set('current', cur); f.set('next', n1);
    const { j } = await post('/settings/password', f);
    if (j.ok) { show(msg, true, 'رمز با موفقیت عوض شد.'); document.getElementById('cur').value = document.getElementById('n1').value = document.getElementById('n2').value = ''; }
    else show(msg, false, j.error || 'خطا در تغییر رمز');
  } catch (err) {
    show(msg, false, 'ارتباط با سرور برقرار نشد. دوباره تلاش کنید.');
    console.error('Failed to change admin password:', err);
  } finally {
    btn.disabled = false;
  }
});
document.getElementById('pruneBtn').addEventListener('click', async (e) => {
  const msg = document.getElementById('dataMsg'), days = document.getElementById('pdays').value;
  if (!confirm('دادهٔ قدیمی‌تر از ' + days + ' روز حذف شود؟')) return;
  e.target.disabled = true; const f = new FormData(); f.set('days', days);
  try {
    const { j } = await post('/settings/prune', f);
    show(msg, !!j.ok, j.ok ? (j.deleted + ' ردیف قدیمی حذف شد.') : 'خطا');
  } catch (err) {
    show(msg, false, 'ارتباط با سرور برقرار نشد. دوباره تلاش کنید.');
    console.error('Failed to prune old statistics:', err);
  } finally {
    e.target.disabled = false;
  }
});
document.getElementById('resetBtn').addEventListener('click', async (e) => {
  const msg = document.getElementById('dataMsg');
  if (!confirm('همهٔ آمار برای همیشه پاک شود؟ این کار برگشت‌ناپذیر است.')) return;
  if (!confirm('مطمئنید؟ تمام داده‌ها حذف می‌شود.')) return;
  e.target.disabled = true;
  try {
    const { j } = await post('/settings/reset', new FormData());
    show(msg, !!j.ok, j.ok ? (j.deleted + ' ردیف حذف شد. آمار از صفر شروع می‌شود.') : 'خطا');
  } catch (err) {
    show(msg, false, 'ارتباط با سرور برقرار نشد. دوباره تلاش کنید.');
    console.error('Failed to reset statistics:', err);
  } finally {
    e.target.disabled = false;
  }
});
</script>
</body></html>`;
