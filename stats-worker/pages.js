// The admin panel's pages: login, dashboard and settings. Persian, right to left, with five themes
// (light mint is the default, cyber the dark one). The pages are only HTML and CSS: their script is client/panel.js, served
// at /app.js.

const head = (title) => `<!doctype html>
<html lang="fa" dir="rtl">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="robots" content="noindex">
<meta name="theme-color" content="#03060d">
<title>${title}</title>
<link rel="icon" href="data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'%3E%3Cpath d='M12 2.8l7.5 3v5.7c0 4.6-3.2 8.4-7.5 9.7-4.3-1.3-7.5-5.1-7.5-9.7V5.8z' fill='%2300e5ff'/%3E%3C/svg%3E">
<link rel="preconnect" href="https://fonts.googleapis.com">
<link href="https://fonts.googleapis.com/css2?family=Vazirmatn:wght@400;500;600;700;800&family=JetBrains+Mono:wght@500;700&display=swap" rel="stylesheet">
<style>
:root{--bg:#eef2f9;--card:#fff;--card2:#f8fafc;--text:#0f172a;--muted:#64748b;--faint:#94a3b8;--line:#e5e9f2;--blue:#2563eb;--green:#16a34a;--red:#dc2626;--violet:#7c3aed;--amber:#f59e0b;--pink:#db2777;--shadow:0 1px 2px rgba(15,23,42,.04),0 10px 30px rgba(15,23,42,.07);--radius:18px;--blur:none;--bgimg:none}
/* dark values, shared by auto-dark and the explicit dark theme */
@media (prefers-color-scheme:dark){:root:not([data-theme]){--bg:#070c18;--card:#0f1726;--card2:#131c2e;--text:#e5edf7;--muted:#9aa7bd;--faint:#64748b;--line:#1c2740;--shadow:0 1px 2px rgba(0,0,0,.4),0 12px 34px rgba(0,0,0,.45)}}
:root[data-theme="dark"]{--bg:#070c18;--card:#0f1726;--card2:#131c2e;--text:#e5edf7;--muted:#9aa7bd;--faint:#64748b;--line:#1c2740;--shadow:0 1px 2px rgba(0,0,0,.4),0 12px 34px rgba(0,0,0,.45)}
/* glass: a soft aurora background with frosted, translucent cards */
:root[data-theme="glass"]{--bg:#e6eefc;--card:rgba(255,255,255,.58);--card2:rgba(255,255,255,.42);--text:#1f2a44;--muted:#566586;--faint:#8794b2;--line:rgba(120,140,190,.22);--shadow:0 10px 34px rgba(70,90,170,.16);--radius:22px;--blur:saturate(180%) blur(18px);--bgimg:radial-gradient(55vw 48vh at 90% -6%,rgba(130,190,255,.7),transparent 62%),radial-gradient(60vw 52vh at 8% 8%,rgba(150,205,255,.45),transparent 60%),radial-gradient(75vw 60vh at 50% 116%,rgba(170,140,255,.6),transparent 60%),linear-gradient(155deg,#dfeaff,#e8ecff 42%,#f0e8ff)}
:root[data-theme="glass"] .card,:root[data-theme="glass"] .kpi,:root[data-theme="glass"] .chip,:root[data-theme="glass"] .seg,:root[data-theme="glass"] .ico,:root[data-theme="glass"] select#theme{border-color:rgba(255,255,255,.75)}
:root[data-theme="glass"] .kpi,:root[data-theme="glass"] .card{background-image:linear-gradient(180deg,rgba(255,255,255,.35),transparent 40%)}
/* the top bar per theme; light is a soft mint like a control panel */
:root{--topbar:linear-gradient(90deg,#3d8b67,#6cbf92 55%,#a8e6c1)}
:root[data-theme="light"]{--bg:#f2f7f4;--bgimg:radial-gradient(70vw 50vh at 100% 0,rgba(134,239,172,.25),transparent 60%),radial-gradient(60vw 50vh at 0 100%,rgba(167,243,208,.25),transparent 60%);--line:#e3ece7;--card2:#f6faf8}
:root[data-theme="dark"]{--topbar:linear-gradient(90deg,#0f1726,#1e293b)}
@media (prefers-color-scheme:dark){:root:not([data-theme]){--topbar:linear-gradient(90deg,#0f1726,#1e293b)}}
:root[data-theme="glass"]{--topbar:linear-gradient(90deg,#4f6bd8,#8b7cf0)}
:root[data-theme="cyber"] .top{border-bottom:1px solid rgba(0,229,255,.25);box-shadow:0 0 24px rgba(0,229,255,.12)}
:root[data-theme="cyber"] .side button.on .ni{background:linear-gradient(160deg,#00e5ff,#7a2cff);box-shadow:0 0 12px rgba(0,229,255,.5)}
:root[data-theme="cyber"] .hk .val{font-family:"JetBrains Mono",Vazirmatn,monospace;color:#eaffff;text-shadow:0 0 14px rgba(0,229,255,.5)}
/* cyber: a dark control room, neon cyan and magenta on a grid, glowing numbers */
:root[data-theme="cyber"]{--topbar:linear-gradient(90deg,#050b18,#0a1a30);--bg:#03060d;--card:rgba(7,15,28,.82);--card2:rgba(12,24,44,.72);--text:#dcf7ff;--muted:#7fa3bd;--faint:#4d6a82;--line:rgba(0,229,255,.16);--blue:#00e5ff;--green:#39ff9f;--red:#ff3b6b;--violet:#b26bff;--amber:#ffc53d;--pink:#ff2bd6;--shadow:0 0 0 1px rgba(0,229,255,.05),0 0 28px rgba(0,229,255,.05),0 18px 40px rgba(0,0,0,.55);--radius:14px;--blur:blur(8px);--bgimg:linear-gradient(rgba(0,229,255,.04) 1px,transparent 1px),linear-gradient(90deg,rgba(0,229,255,.04) 1px,transparent 1px),radial-gradient(60vw 50vh at 85% -10%,rgba(0,229,255,.16),transparent 60%),radial-gradient(60vw 50vh at 0% 110%,rgba(255,43,214,.13),transparent 60%)}
:root[data-theme="cyber"] body{background-size:34px 34px,34px 34px,auto,auto}
:root[data-theme="cyber"] body::after{content:"";position:fixed;inset:0;pointer-events:none;z-index:100;background:repeating-linear-gradient(0deg,rgba(255,255,255,.018) 0 1px,transparent 1px 3px)}
:root[data-theme="cyber"] .card,:root[data-theme="cyber"] .kpi{border-color:rgba(0,229,255,.2);position:relative}
:root[data-theme="cyber"] .card::before,:root[data-theme="cyber"] .kpi::before{content:"";position:absolute;inset:-1px;border-radius:inherit;pointer-events:none;background:linear-gradient(var(--blue),var(--blue)) top right/16px 2px no-repeat,linear-gradient(var(--blue),var(--blue)) top right/2px 16px no-repeat,linear-gradient(var(--pink),var(--pink)) bottom left/16px 2px no-repeat,linear-gradient(var(--pink),var(--pink)) bottom left/2px 16px no-repeat;opacity:.85}
:root[data-theme="cyber"] .kpi .val,:root[data-theme="cyber"] .big{color:#eaffff;text-shadow:0 0 14px rgba(0,229,255,.55),0 0 2px rgba(0,229,255,.9)}
:root[data-theme="cyber"] .sect h2,:root[data-theme="cyber"] .hdr b{text-shadow:0 0 10px rgba(0,229,255,.45)}
:root[data-theme="cyber"] .sect span{box-shadow:0 0 12px currentColor}
:root[data-theme="cyber"] .logo{background:linear-gradient(160deg,#00e5ff,#7a2cff);box-shadow:0 0 22px rgba(0,229,255,.55)}
:root[data-theme="cyber"] .tabs button.on{color:#eaffff;background:rgba(0,229,255,.12);box-shadow:inset 0 -2px 0 var(--blue),0 0 18px rgba(0,229,255,.18)}
:root[data-theme="cyber"] .seg button.on{background:rgba(0,229,255,.16);color:#eaffff;box-shadow:0 0 12px rgba(0,229,255,.25)}
:root[data-theme="cyber"] .btn{background:linear-gradient(160deg,#00c8e0,#7a2cff);box-shadow:0 0 18px rgba(0,229,255,.25)}
:root[data-theme="cyber"] .btn.ghost{background:var(--card2);box-shadow:none}
:root[data-theme="cyber"] .btn.danger{background:linear-gradient(160deg,#ff5c8a,#c2185b)}
:root[data-theme="cyber"] .kpi .val{font-family:"JetBrains Mono",Vazirmatn,monospace}
*{box-sizing:border-box}
body{margin:0;background:var(--bg);background-image:var(--bgimg);background-attachment:fixed;color:var(--text);font-family:Vazirmatn,system-ui,sans-serif;-webkit-font-smoothing:antialiased;font-feature-settings:"ss01"}
.logo{width:42px;height:42px;border-radius:13px;background:linear-gradient(160deg,#60a5fa,#1d4ed8);display:grid;place-items:center;box-shadow:0 6px 18px rgba(37,99,235,.4);flex:none}
.logo svg{width:23px;height:23px}
button,input,select,textarea{font:inherit}
a{color:var(--blue)}
.hdr{position:sticky;top:0;z-index:20;background:color-mix(in srgb,var(--bg) 84%,transparent);backdrop-filter:blur(12px);-webkit-backdrop-filter:blur(12px);border-bottom:1px solid var(--line)}
.hdr .in{max-width:1320px;margin:auto;padding:10px 18px;display:flex;align-items:center;gap:10px;flex-wrap:wrap}
.hdr b{font-size:16px;line-height:1.2}.hdr small{display:block;color:var(--muted);font-size:12px}
.hdr .brand{display:flex;align-items:center;gap:11px;text-decoration:none;color:inherit}
.sp{flex:1}
.ico{display:grid;place-items:center;min-width:38px;height:38px;padding:0 12px;border:1px solid var(--line);border-radius:11px;background:var(--card);color:var(--muted);cursor:pointer;text-decoration:none;font-size:13px;font-weight:600;backdrop-filter:var(--blur);-webkit-backdrop-filter:var(--blur)}
.ico:hover{color:var(--text)}
.ico.spin svg{animation:sp 1s linear infinite}@keyframes sp{to{transform:rotate(360deg)}}
select.pick{height:38px;border:1px solid var(--line);border-radius:11px;background:var(--card);color:var(--text);padding:0 10px;cursor:pointer;outline:none;max-width:230px}
select.pick option{background:var(--bg);color:var(--text)}
.card{background:var(--card);border:1px solid var(--line);border-radius:var(--radius);padding:16px;box-shadow:var(--shadow);min-width:0;backdrop-filter:var(--blur);-webkit-backdrop-filter:var(--blur)}
.card h3{margin:0 0 14px;font-size:14px;font-weight:700;display:flex;justify-content:space-between;align-items:center;gap:8px}
.card h3 em{color:var(--faint);font-weight:500;font-size:12px;font-style:normal;text-align:left}
.btn{padding:10px 18px;border:0;border-radius:11px;background:linear-gradient(160deg,#3b82f6,#1d4ed8);color:#fff;font-weight:700;cursor:pointer}
.btn:hover{filter:brightness(1.08)}.btn:disabled{opacity:.55;cursor:default}
.btn.ghost{background:var(--card2);color:var(--text);border:1px solid var(--line);box-shadow:none}
.btn.danger{background:linear-gradient(160deg,#f87171,#dc2626)}
label.f{display:block;font-size:13px;color:var(--muted);margin:12px 0 6px}
input.in,textarea.in{width:100%;padding:10px 12px;border-radius:11px;border:1px solid var(--line);background:var(--card2);color:var(--text);outline:none}
input.in:focus,textarea.in:focus{border-color:var(--blue);box-shadow:0 0 0 3px color-mix(in srgb,var(--blue) 22%,transparent)}
.ltr{direction:ltr;text-align:left}
.msg{margin-top:12px;padding:10px 12px;border-radius:10px;font-size:13px;display:none;line-height:1.7}
.msg.ok{display:block;background:color-mix(in srgb,var(--green) 13%,transparent);color:var(--green)}
.msg.err{display:block;background:color-mix(in srgb,var(--red) 13%,transparent);color:var(--red)}
table{width:100%;border-collapse:collapse;font-size:14px}
td,th{padding:9px 4px;border-bottom:1px solid var(--line);text-align:right}
th{color:var(--faint);font-weight:600;font-size:12px}
tr:last-child td{border-bottom:0}
td.n{text-align:left;font-weight:700;width:72px;font-variant-numeric:tabular-nums;white-space:nowrap}
td.rank{color:var(--faint);width:26px;font-size:13px}
.empty{color:var(--faint);text-align:center;padding:60px 0;font-size:14px}
code{background:var(--card2);padding:2px 7px;border-radius:7px;direction:ltr;display:inline-block;font-size:12px;word-break:break-all}
.switch{position:relative;display:inline-flex;align-items:center;gap:10px;cursor:pointer;font-size:14px}
.switch input{appearance:none;-webkit-appearance:none;width:42px;height:24px;border-radius:99px;background:var(--line);position:relative;cursor:pointer;transition:.2s;margin:0;flex:none}
.switch input::after{content:"";position:absolute;top:3px;right:3px;width:18px;height:18px;border-radius:50%;background:#fff;transition:.2s}
.switch input:checked{background:var(--green)}
.switch input:checked::after{right:21px}
</style>
<script>try{var t=localStorage.getItem('drvpn_theme')||'light';if(t!=='auto')document.documentElement.setAttribute('data-theme',t)}catch(e){document.documentElement.setAttribute('data-theme','light')}</script>`;

const logo = `<div class="logo"><svg viewBox="0 0 24 24" fill="none"><path d="M12 2.8l7.5 3v5.7c0 4.6-3.2 8.4-7.5 9.7-4.3-1.3-7.5-5.1-7.5-9.7V5.8z" fill="#fff" fill-opacity=".95"/><path d="M12 8.2v7.6M8.2 12h7.6" stroke="#2563eb" stroke-width="2.4" stroke-linecap="round"/></svg></div>`;

const themeOptions = '<option value="light">روشن (نعنایی)</option><option value="cyber">سایبری</option><option value="auto">خودکار</option><option value="dark">تیره</option><option value="glass">شیشه‌ای</option>';

const LOGIN_MESSAGES = {
  failed: 'نام کاربری یا رمز عبور اشتباه است.',
  off: 'ورود تنظیم نشده است: ADMIN_USER و ADMIN_PASSWORD را به‌عنوان secret در Worker بگذارید.',
};

/** [state]: null, "failed", "locked" (then [minutes] to wait) or "off". */
export const loginPage = (state, minutes) => `${head('ورود · پنل دکتر وی پی ان')}
<style>
.wrap{min-height:100vh;display:grid;place-items:center;padding:16px}
.box{width:100%;max-width:380px;padding:32px 26px}
.brand{display:flex;align-items:center;gap:12px;margin-bottom:22px}
.brand b{font-size:18px}.brand span{display:block;color:var(--muted);font-size:13px}
.box .btn{width:100%;margin-top:22px;padding:13px}
.err{margin-top:16px;padding:10px 12px;border-radius:10px;background:rgba(220,38,38,.12);color:var(--red);font-size:13px;line-height:1.7}
.term{font-family:"JetBrains Mono",monospace;font-size:11px;color:var(--faint);direction:ltr;text-align:left;margin-top:18px;opacity:.8}
</style>
</head>
<body><div class="wrap"><form class="box card" method="post" action="/login">
<div class="brand">${logo}<div><b>دکتر وی پی ان</b><span>پنل مدیریت</span></div></div>
<label class="f" for="u">نام کاربری</label><input class="in ltr" id="u" name="user" autocomplete="username" required autofocus>
<label class="f" for="p">رمز عبور</label><input class="in ltr" id="p" name="password" type="password" autocomplete="current-password" required>
<button class="btn" type="submit"${state === 'locked' ? ' disabled' : ''}>ورود</button>
${state === 'locked'
    ? `<div class="err">تلاش‌های ناموفق زیاد بود. ${Number(minutes).toLocaleString('fa-IR')} دقیقهٔ دیگر دوباره امتحان کنید.</div>`
    : state ? `<div class="err">${LOGIN_MESSAGES[state]}</div>` : ''}
<div class="term">&gt; secure session · ${new Date().toISOString().slice(0, 16).replace('T', ' ')} UTC_</div>
</form></div></body></html>`;

const ICON = {
  refresh: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><path d="M20 11A8 8 0 1 0 18.4 16"/><path d="M20 4v5h-5"/></svg>',
  full: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><path d="M4 9V4h5M20 9V4h-5M4 15v5h5M20 15v5h-5"/></svg>',
  gear: '<svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1.1 1.7 1.7 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1z"/></svg>',
};

const TABS = [
  ['overview', 'داشبورد'], ['live', 'زنده'], ['users', 'کاربران'], ['conn', 'اتصال'], ['traffic', 'حجم مصرفی'],
  ['geo', 'جغرافیا و اپراتور'], ['devices', 'دستگاه‌ها'], ['downloads', 'دانلودها'], ['notice', 'اعلان'],
];

/** Small line icons for the side menu and card titles. */
const svg = (d) => `<svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">${d}</svg>`;
const NAV_ICON = {
  overview: svg('<path d="M3 11l9-8 9 8M5 10v10h14V10"/>'),
  live: svg('<path d="M22 12h-4l-3 9L9 3l-3 9H2"/>'),
  users: svg('<circle cx="9" cy="8" r="4"/><path d="M2 21a7 7 0 0 1 14 0M16 3.5a4 4 0 0 1 0 9M22 21a7 7 0 0 0-4-6.3"/>'),
  conn: svg('<path d="M5 12.5a10 10 0 0 1 14 0M8.5 16a5 5 0 0 1 7 0M2 9a15 15 0 0 1 20 0"/><circle cx="12" cy="19.5" r="1"/>'),
  traffic: svg('<path d="M3 3v18h18"/><path d="M7 16V11M12 16V7M17 16v-3"/>'),
  geo: svg('<circle cx="12" cy="12" r="10"/><path d="M2 12h20M12 2a15 15 0 0 1 0 20M12 2a15 15 0 0 0 0 20"/>'),
  devices: svg('<rect x="6" y="2" width="12" height="20" rx="2"/><path d="M11 18h2"/>'),
  downloads: svg('<path d="M12 3v12M7 10l5 5 5-5M4 21h16"/>'),
  notice: svg('<path d="M3 11l15-6v14L3 13zM7 12.5V18a2 2 0 0 0 4 0v-4"/>'),
  settings: svg('<circle cx="12" cy="12" r="3"/><path d="M12 2v3M12 19v3M4.2 4.2l2.1 2.1M17.7 17.7l2.1 2.1M2 12h3M19 12h3M4.2 19.8l2.1-2.1M17.7 6.3l2.1-2.1"/>'),
  help: svg('<circle cx="12" cy="12" r="10"/><path d="M9.1 9a3 3 0 0 1 5.8 1c0 2-3 3-3 3M12 17h.01"/>'),
  bolt: svg('<path d="M13 2L3 14h9l-1 8 10-12h-9z"/>'),
  chart: svg('<path d="M3 17l6-6 4 4 8-8M14 7h7v7"/>'),
  health: svg('<path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1-1.1a5.5 5.5 0 0 0-7.8 7.8L12 21l8.8-8.6a5.5 5.5 0 0 0 0-7.8z"/>'),
};
/** A headline card: label, a note under it, the number, a sparkline and a line of context. */
const hk = (id, label, note, color, icon) => `<div class="hk" style="--c:${color}"><div class="hk-h"><div><span class="lbl">${label}</span><small class="note">${note}</small></div><span class="badge">${icon}</span></div><div class="val" id="h${id}">–</div><div class="spark"><canvas id="s${id}" data-color="${color}"></canvas></div><small class="ctx" id="h${id}Ctx">&nbsp;</small></div>`;
const kpi = (id, label, color) => `<div class="kpi"><div class="top"><span class="lbl">${label}</span><span class="trend flat" id="t${id}">–</span></div><div class="val" id="v${id}">–</div><div class="spark"><canvas id="s${id}" data-color="${color}"></canvas></div></div>`;
const stat = (id, label, hint = '') => `<div class="kpi stat"><span class="lbl">${label}</span><div class="val" id="${id}">–</div><small>${hint}</small></div>`;
const card = (title, body, em = '', cls = '') => `<div class="card ${cls}"><h3>${title}${em ? ` <em>${em}</em>` : ''}</h3>${body}</div>`;
const chart = (id) => `<div class="chart"><canvas id="${id}"></canvas></div>`;
const sect = (title, color, small = '') => `<div class="sect"><span style="background:${color};color:${color}"></span><h2>${title}</h2>${small ? `<small>${small}</small>` : ''}</div>`;
const langs = (field, tag, max) => [['fa', 'فارسی', ''], ['en', 'English', ' ltr'], ['ru', 'Русский', ' ltr'], ['zh', '中文', ' ltr']]
  .map(([l, name, dir]) => `<div><span class="lng">${name}</span>${tag === 'input'
    ? `<input class="in${dir}" id="n${field}_${l}" maxlength="${max}">`
    : `<textarea class="in${dir}" id="n${field}_${l}" maxlength="${max}"></textarea>`}</div>`).join('');

export const dashboardPage = (tag) => `${head('پنل مدیریت دکتر وی پی ان')}
<style>
/* layout: a top bar, a side menu (right, RTL) and the content */
.top{position:sticky;top:0;z-index:30;height:58px;display:flex;align-items:center;gap:10px;padding:0 16px;background:var(--topbar);color:#fff;box-shadow:0 2px 14px rgba(0,0,0,.12)}
.top .brand{display:flex;align-items:center;gap:10px;color:#fff;text-decoration:none}
.top .brand b{font-size:15px;display:block;line-height:1.2}.top .brand small{font-size:11px;opacity:.8}
.top .logo{width:34px;height:34px;border-radius:10px;background:rgba(255,255,255,.22);box-shadow:none}
.top .ico{background:rgba(255,255,255,.14);border-color:rgba(255,255,255,.22);color:#fff;backdrop-filter:none}
.top .ico:hover{background:rgba(255,255,255,.24);color:#fff}
.top select.pick{background:rgba(255,255,255,.14);border-color:rgba(255,255,255,.22);color:#fff}
.top select.pick option{color:#111;background:#fff}
.menu{display:none!important}
.app{display:flex;align-items:flex-start;gap:18px;max-width:1700px;margin:auto;padding:18px}
.side{position:sticky;top:76px;width:218px;flex:none;background:var(--card);border:1px solid var(--line);border-radius:18px;padding:12px 10px;box-shadow:var(--shadow);max-height:calc(100vh - 94px);overflow:auto;backdrop-filter:var(--blur);-webkit-backdrop-filter:var(--blur)}
.side .grp{font-size:11px;font-weight:800;color:var(--faint);margin:14px 10px 6px;letter-spacing:.3px}
.side .grp:first-child{margin-top:4px}
.side button,.side a{display:flex;align-items:center;gap:10px;width:100%;border:0;background:transparent;color:var(--muted);padding:9px 10px;border-radius:11px;cursor:pointer;font-weight:600;font-size:13.5px;text-decoration:none;text-align:right;margin-bottom:2px}
.side button:hover,.side a:hover{background:var(--card2);color:var(--text)}
.side button.on{background:color-mix(in srgb,var(--blue) 13%,transparent);color:var(--text)}
.side .ni{display:grid;place-items:center;width:28px;height:28px;border-radius:9px;background:var(--card2);color:var(--blue);flex:none}
.side button.on .ni{background:var(--blue);color:#fff}
.shade{display:none}
main{flex:1;min-width:0}
/* headline cards */
.hks{display:grid;grid-template-columns:repeat(4,1fr);gap:14px}
.hk{background:var(--card);border:1px solid var(--line);border-radius:var(--radius);padding:14px 16px 10px;box-shadow:var(--shadow);position:relative;overflow:hidden;min-width:0;backdrop-filter:var(--blur);-webkit-backdrop-filter:var(--blur)}
.hk::after{content:"";position:absolute;inset:0;background:linear-gradient(120deg,transparent 55%,color-mix(in srgb,var(--c) 9%,transparent));pointer-events:none}
.hk-h{display:flex;justify-content:space-between;align-items:flex-start;gap:8px}
.hk .lbl{display:block;font-size:12px;font-weight:800;color:var(--muted);letter-spacing:.2px}
.hk .note{display:flex;align-items:center;gap:5px;font-size:11px;color:var(--faint);margin-top:3px}
.hk .note .dot{width:6px;height:6px;box-shadow:none}
.hk .badge{display:grid;place-items:center;width:38px;height:38px;border-radius:12px;background:var(--c);color:#fff;box-shadow:0 6px 16px color-mix(in srgb,var(--c) 40%,transparent);flex:none}
.hk .val{font-size:27px;font-weight:800;margin-top:6px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;font-variant-numeric:tabular-nums}
.hk .val small{font-size:16px;color:var(--faint);font-weight:700}
.hk .spark{height:46px;margin:2px -6px 0}
.hk .ctx{display:inline-block;font-size:11.5px;color:var(--faint);margin-top:2px;padding:2px 8px;border-radius:99px;background:var(--card2)}
.hk .ctx.up{color:var(--green);background:color-mix(in srgb,var(--green) 13%,transparent)}
.hk .ctx.down{color:var(--red);background:color-mix(in srgb,var(--red) 13%,transparent)}
/* trend cards: a colored top edge, an icon title, a footer of numbers */
.trendcard{border-top:3px solid var(--accent)!important}
.trendcard h3{align-items:flex-start}
.ttl{display:flex;align-items:center;gap:10px}
.ttl small{display:block;font-size:11.5px;color:var(--faint);font-weight:500;margin-top:2px}
.tic{display:grid;place-items:center;width:34px;height:34px;border-radius:10px;background:color-mix(in srgb,var(--c) 14%,transparent);color:var(--c);flex:none}
.foot{display:grid;grid-template-columns:repeat(auto-fit,minmax(120px,1fr));gap:10px;margin:14px -16px -16px;padding:12px 16px;border-top:1px solid var(--line);background:var(--card2);border-radius:0 0 var(--radius) var(--radius)}
.foot div{display:flex;gap:8px;align-items:center;min-width:0}
.foot .fi{color:var(--accent);flex:none;display:grid}
.foot small{display:block;font-size:10.5px;color:var(--faint);font-weight:700}
.foot b{font-size:14px;white-space:nowrap}
.deltas{margin:12px -16px -16px;padding:8px 16px 12px;border-top:1px solid var(--line)}
.deltas div{display:flex;justify-content:space-between;align-items:center;padding:5px 0;font-size:13px;color:var(--muted)}
.deltas span.v{font-size:12px;font-weight:700;padding:2px 9px;border-radius:99px}
.deltas .up{color:var(--green);background:color-mix(in srgb,var(--green) 13%,transparent)}
.deltas .down{color:var(--red);background:color-mix(in srgb,var(--red) 13%,transparent)}
.deltas .flat{color:var(--faint);background:var(--card2)}
.cmp2{display:flex;gap:14px;align-items:center}
.gauge{position:relative;width:130px;flex:none;text-align:center}
.gauge canvas{width:130px!important;height:130px!important}
.gtxt{position:absolute;top:0;left:0;width:130px;height:130px;display:grid;place-content:center;text-align:center}
.gtxt b{font-size:17px}.gtxt small{font-size:10.5px;color:var(--faint)}
.gauge>.sub{display:block;margin-top:6px;font-size:11px}
.acts{display:flex;gap:6px}
.ico.sm{height:30px;min-width:0;font-size:12px;padding:0 10px}
.health{display:grid;grid-template-columns:repeat(auto-fit,minmax(170px,1fr));gap:12px}
.health .h{padding:12px;border-radius:12px;background:var(--card2);border:1px solid var(--line)}
.health .h small{display:block;color:var(--faint);font-size:11.5px;font-weight:700}
.health .h b{display:block;font-size:20px;margin:4px 0 2px}
.health .h span{font-size:12px;color:var(--muted)}
.health .h.good b{color:var(--green)}.health .h.warn b{color:var(--amber)}.health .h.bad b{color:var(--red)}
.sep{color:var(--faint)}
.bar0 .seg,.bar0 .ico{flex:none}
#pause{display:inline-flex;align-items:center;gap:2px;white-space:nowrap}
.seg{display:flex;background:var(--card2);border:1px solid var(--line);border-radius:12px;padding:3px;gap:2px}
.seg button{border:0;background:transparent;color:var(--muted);padding:7px 13px;border-radius:9px;cursor:pointer;font-size:13px;font-weight:600}
.seg button.on{background:var(--card);color:var(--text);box-shadow:0 1px 3px rgba(15,23,42,.12)}
main{max-width:1320px;margin:auto;padding:16px 18px 30px}
.bar0{display:flex;flex-wrap:wrap;align-items:center;gap:10px;margin-bottom:14px}
.live{display:flex;flex-wrap:wrap;align-items:center;gap:8px;color:var(--muted);font-size:13px;flex:1;min-width:260px}
.live b{color:var(--text)}
.dot{width:8px;height:8px;border-radius:50%;background:var(--green);box-shadow:0 0 0 4px color-mix(in srgb,var(--green) 22%,transparent);animation:pulse 2s infinite}
@keyframes pulse{50%{box-shadow:0 0 0 7px transparent}}
.filter{display:flex;align-items:center;gap:6px;font-size:13px;color:var(--muted);flex-wrap:wrap}
.filter .chip{display:none;align-items:center;gap:4px;padding:4px 6px 4px 12px;border-radius:99px;background:color-mix(in srgb,var(--blue) 15%,transparent);color:var(--text);border:1px solid color-mix(in srgb,var(--blue) 40%,transparent);font-weight:600}
.filter.on .chip{display:inline-flex}
.filter .chip button{border:0;background:transparent;color:var(--muted);cursor:pointer;font-size:14px;padding:0 6px}
.banner{margin:0 0 14px;padding:11px 14px;border-radius:12px;font-size:13px;line-height:1.8}
.banner[hidden]{display:none}
.banner.err{background:rgba(220,38,38,.1);color:var(--red);border:1px solid rgba(220,38,38,.25)}
.banner.warn{background:rgba(245,158,11,.12);color:var(--amber);border:1px solid rgba(245,158,11,.3)}
.tab{display:none}
.tab.on{display:block;animation:fade .25s ease}
@keyframes fade{from{opacity:0;transform:translateY(4px)}}
.kpis{display:grid;grid-template-columns:repeat(auto-fit,minmax(190px,1fr));gap:14px}
.kpi{background:var(--card);border:1px solid var(--line);border-radius:var(--radius);padding:15px 15px 6px;box-shadow:var(--shadow);position:relative;overflow:hidden;min-width:0;backdrop-filter:var(--blur);-webkit-backdrop-filter:var(--blur)}
.kpi .top{display:flex;align-items:flex-start;justify-content:space-between;gap:8px}
.kpi .lbl{color:var(--muted);font-size:13px;font-weight:600}
.kpi .val{font-size:28px;font-weight:800;letter-spacing:-.5px;margin-top:8px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.kpi .spark{height:44px;margin:4px -8px -2px}
.kpi.stat{padding-bottom:14px}.kpi.stat small{display:block;color:var(--faint);font-size:12px;margin-top:4px;line-height:1.6}
.trend{font-size:12px;font-weight:700;padding:3px 8px;border-radius:999px;white-space:nowrap}
.trend.up{color:var(--green);background:color-mix(in srgb,var(--green) 14%,transparent)}
.trend.down{color:var(--red);background:color-mix(in srgb,var(--red) 14%,transparent)}
.trend.flat{color:var(--faint);background:var(--card2)}
.sect{display:flex;align-items:center;gap:8px;margin:24px 2px 12px;flex-wrap:wrap}
.sect h2{font-size:15px;margin:0;font-weight:700}
.sect span{width:7px;height:16px;border-radius:4px;background:var(--blue)}
.sect small{color:var(--faint);font-size:12px;font-weight:500}
.grid{display:grid;grid-template-columns:2fr 1fr;gap:14px}
.grid3{display:grid;grid-template-columns:repeat(3,1fr);gap:14px}
.grid2{display:grid;grid-template-columns:repeat(2,1fr);gap:14px}
.mt{margin-top:14px}
.chart{position:relative;height:280px}
.small .chart{height:230px}
.bar{height:6px;border-radius:6px;background:linear-gradient(90deg,#93c5fd,#2563eb);margin-top:6px}
.scroll{max-height:420px;overflow:auto}
.note{color:var(--faint);font-size:12px;margin:26px 0 8px;text-align:center;line-height:1.9}
.pill{display:inline-block;padding:1px 8px;border-radius:99px;font-size:11px;font-weight:700;background:var(--card2);color:var(--muted);margin-inline-start:6px;vertical-align:middle}
.pill.dc{background:color-mix(in srgb,var(--amber) 16%,transparent);color:var(--amber)}
.rate{display:inline-block;min-width:52px;text-align:center;padding:2px 8px;border-radius:8px;font-weight:700;font-size:13px}
.sub{color:var(--faint);font-size:12px;font-weight:500}
/* live feed: a terminal */
.feed{font-family:"JetBrains Mono",Vazirmatn,monospace;font-size:12.5px;background:#02050b;color:#9fffd6;border-radius:12px;padding:10px 14px;height:440px;overflow:auto;border:1px solid rgba(57,255,159,.18);box-shadow:inset 0 0 40px rgba(57,255,159,.05)}
.feed .row{display:flex;flex-wrap:wrap;gap:4px 10px;padding:5px 0;border-bottom:1px dashed rgba(57,255,159,.08);align-items:center}
.feed .row.new{animation:flash 2s ease}
@keyframes flash{from{background:rgba(57,255,159,.2)}}
.feed .t{color:#4d8f74;direction:ltr;min-width:44px}
.feed .k{color:#00e5ff}.feed .m{color:#ff7ae8}.feed .w{color:#ffc53d}.feed .x{color:#ff3b6b}.feed .g{color:#6b8a9e}
.feed .cur{display:inline-block;width:8px;height:14px;background:#39ff9f;vertical-align:middle;animation:blink 1s steps(1) infinite;margin-top:6px}
@keyframes blink{50%{opacity:0}}
.pulse-dot{border-radius:50%;background:rgba(0,229,255,.8);animation:ring 1.8s infinite}
@keyframes ring{0%{box-shadow:0 0 0 0 rgba(0,229,255,.7)}100%{box-shadow:0 0 0 18px rgba(0,229,255,0)}}
/* cohorts: a heat map */
.cohort td,.cohort th{text-align:center;padding:8px 4px;font-size:13px;font-variant-numeric:tabular-nums}
.cohort td:first-child,.cohort th:first-child{text-align:right;white-space:nowrap}
.cohort td.h{border-radius:6px;font-weight:700}
/* announcement editor */
.langs{display:grid;grid-template-columns:repeat(2,1fr);gap:10px}
.langs .lng{font-size:12px;color:var(--faint);margin-bottom:4px;display:block}
textarea.in{min-height:84px;resize:vertical;line-height:1.7}
.phone{width:280px;max-width:100%;min-height:420px;margin:auto;border-radius:34px;border:8px solid #0b1220;background:#0f172a;padding:18px 12px 26px;box-shadow:0 20px 50px rgba(0,0,0,.45),0 0 30px rgba(0,229,255,.12)}
.phone .notice{background:linear-gradient(160deg,#1e3a8a,#312e81);color:#fff;border-radius:18px;padding:14px;margin-top:40px}
.phone .notice b{display:block;font-size:15px;margin-bottom:6px}
.phone .notice p{margin:0;font-size:13px;line-height:1.8;opacity:.92;white-space:pre-wrap}
.phone .notice span{display:inline-block;margin-top:10px;color:#fff;background:rgba(255,255,255,.16);padding:6px 12px;border-radius:9px;font-size:12px}
.phone .off{color:#94a3b8;text-align:center;font-size:13px;margin-top:150px;line-height:1.8}
.leaflet-container{background:var(--card2)!important;font-family:Vazirmatn,sans-serif}
:fullscreen{overflow:auto}
@media (max-width:1200px){.hks{grid-template-columns:repeat(2,1fr)}}
@media (max-width:1000px){.grid,.grid3,.grid2{grid-template-columns:1fr}
.menu{display:grid!important}.app{padding:12px}
.side{position:fixed;top:0;right:0;bottom:0;z-index:50;max-height:none;border-radius:0;transform:translateX(110%);transition:transform .25s;width:250px;padding-top:20px}
body.nav-open .side{transform:none}body.nav-open .shade{display:block;position:fixed;inset:0;z-index:40;background:rgba(0,0,0,.35)}}
@media (max-width:620px){.top .brand small,.top select.pick{display:none}.cmp2{flex-direction:column}.hks{grid-template-columns:1fr 1fr}.hk .val{font-size:21px}.seg button{padding:7px 9px}.kpis{grid-template-columns:repeat(2,1fr)}.kpi .val{font-size:22px}.langs{grid-template-columns:1fr}main{padding:12px}}
</style>
<script src="https://cdnjs.cloudflare.com/ajax/libs/Chart.js/4.4.1/chart.umd.min.js"></script>
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.min.css">
<script src="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.min.js"></script>
</head>
<body class="dash">
<header class="top">
<button class="ico menu" id="menu" title="منو"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><path d="M4 6h16M4 12h16M4 18h16"/></svg></button>
<a class="brand" href="/">${logo}<div><b>دکتر وی پی ان</b><small>پنل مدیریت</small></div></a>
<div class="sp"></div>
<select class="pick" id="theme" title="تم">${themeOptions}</select>
<a class="ico" href="/settings" title="تنظیمات">${ICON.gear}</a>
<a class="ico" href="/logout" title="خروج"><svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4M10 17l-5-5 5-5M5 12h12"/></svg></a>
</header>
<div class="app">
<aside class="side" id="side">
<nav id="tabs">
<div class="grp">مدیریت</div>
${TABS.slice(0, 8).map(([k, t], i) => `<button data-tab="${k}"${i ? '' : ' class="on"'}><span class="ni">${NAV_ICON[k]}</span>${t}</button>`).join('')}
<div class="grp">انتشار</div>
${TABS.slice(8).map(([k, t]) => `<button data-tab="${k}"><span class="ni">${NAV_ICON[k]}</span>${t}</button>`).join('')}
<a href="/settings"><span class="ni">${NAV_ICON.settings}</span>تنظیمات</a>
<div class="grp">راهنما</div>
<a href="https://github.com/DrvpnTM/app" target="_blank" rel="noopener"><span class="ni">${NAV_ICON.help}</span>مخزن گیت‌هاب</a>
</nav>
</aside>
<div class="shade" id="shade"></div>
<main>
<div class="bar0">
<div class="live"><span class="dot" id="liveDot"></span><span id="liveText">زنده</span><span class="sub" id="upd"></span><span class="sep">·</span><span>امروز <b id="aDay">–</b></span><span>هفته <b id="aWeek">–</b></span><span>ماه <b id="aMonth">–</b></span><span>۱ ساعت <b id="aHour">–</b></span></div>
<div class="filter" id="filter">
<select class="pick" id="fDim" title="فیلتر"><option value="">همهٔ کاربران</option><option value="country">کشور…</option><option value="region">منطقهٔ اپ…</option><option value="isp">اپراتور…</option><option value="version">نسخه…</option><option value="server">نوع سرور…</option><option value="hop">حلقهٔ اضافه…</option><option value="brand">برند گوشی…</option><option value="android">اندروید…</option></select>
<select class="pick" id="fVal" hidden></select>
<span class="chip" id="fChip"><span id="fText"></span><button id="fClear" title="حذف فیلتر">✕</button></span>
</div>
<div class="seg" id="seg"><button data-d="7">۷ روز</button><button data-d="30" class="on">۳۰ روز</button><button data-d="90">۹۰ روز</button><button data-d="180">۱۸۰ روز</button></div>
<button class="ico" id="pause" title="توقف به‌روزرسانی خودکار"><span id="pauseIcon">❚❚</span>&nbsp;<span id="pauseText">توقف</span></button>
<button class="ico" id="refresh" title="به‌روزرسانی (R)">${ICON.refresh}</button>
<a class="ico" id="csv" title="دانلود CSV" href="#">CSV</a>
<button class="ico" id="full" title="تمام‌صفحه (F)">${ICON.full}</button>
</div>
<div id="loadError" class="banner err" role="alert" hidden></div>
<div id="staleNote" class="banner warn" role="status" hidden></div>

<section class="tab on" data-tab="overview">
<section class="hks">
${hk('Online', 'کاربران آنلاین', '<i class="dot"></i> ۱ ساعت اخیر', '#16a34a', NAV_ICON.users)}
${hk('Active', 'کاربران فعال', '<span id="hActiveRange"></span>', '#2563eb', NAV_ICON.live)}
${hk('Usage', 'حجم مصرفی', '<span id="hUsageRange"></span>', '#7c3aed', NAV_ICON.traffic)}
${hk('Peak', 'شلوغ‌ترین روز', '<span id="hPeakRange"></span>', '#f59e0b', NAV_ICON.bolt)}
</section>
<section class="grid mt">
<div class="card trendcard" style="--accent:#7c3aed">
<h3><span class="ttl"><span class="tic" style="--c:#7c3aed">${NAV_ICON.traffic}</span><span>روند مصرف<small id="usageRangeLbl"></small></span></span><em>میانگین ۷ روزه · مصرف روزانه</em></h3>
${chart('cUsageTrend')}
<div class="foot" id="usageFoot"></div>
</div>
<div class="card trendcard" style="--accent:#f59e0b">
<h3><span class="ttl"><span class="tic" style="--c:#f59e0b">${NAV_ICON.chart}</span><span>مقایسهٔ مصرف<small>امروز و دیروز در برابر میانگین روزانهٔ هفته و ماه</small></span></span></h3>
<div class="chart" style="height:230px"><canvas id="cUsageCmp"></canvas></div>
<div class="deltas" id="usageDeltas"></div>
</div>
</section>
<section class="grid mt">
<div class="card trendcard" style="--accent:#2563eb">
<h3><span class="ttl"><span class="tic" style="--c:#2563eb">${NAV_ICON.users}</span><span>روند کاربران<small id="usersRangeLbl"></small></span></span><em>میانگین ۷ روزه · کاربران فعال در روز</em></h3>
${chart('cUsersTrend')}
<div class="foot" id="usersFoot"></div>
</div>
<div class="card trendcard" style="--accent:#16a34a">
<h3><span class="ttl"><span class="tic" style="--c:#16a34a">${NAV_ICON.users}</span><span>مقایسهٔ کاربران<small>میانگین کاربران روزانه، فقط روزهایی که کسی آمده</small></span></span></h3>
<div class="cmp2"><div class="gauge"><canvas id="cGauge"></canvas><div class="gtxt"><b id="gaugeTxt">–</b><small>آنلاین الان</small></div><small class="sub">۱ ساعت اخیر از کاربران ماه</small></div><div class="chart" style="height:230px;flex:1;min-width:0"><canvas id="cUsersCmp"></canvas></div></div>
<div class="deltas" id="usersDeltas"></div>
</div>
</section>
<div class="card trendcard mt" style="--accent:#ea580c">
<h3><span class="ttl"><span class="tic" style="--c:#ea580c">${NAV_ICON.health}</span><span>سلامت سرویس<small>اتصال کاربران، رسیدن گزارش‌ها و سهمیهٔ پایگاه‌داده</small></span></span><span class="acts"><button class="ico sm" data-go="conn">جزئیات اتصال</button><button class="ico sm" data-go="live">جریان زنده</button></span></h3>
<div class="health" id="healthRow"></div>
</div>
<section class="grid mt">
${card('فعالیت ۲۴ ساعت اخیر', chart('cHourly'), 'گزارش‌های رسیده در هر ساعت · وقت تهران')}
${card('نسخه‌های برتر', '<div id="tTopVersions"></div>')}
</section>
<section class="grid2 small mt">
${card('منطقه', chart('cRegion'))}
${card('کاربران فعال و جدید', chart('cDaily'), 'در روز')}
</section>
</section>

<section class="tab" data-tab="live">
<section class="kpis">
${stat('lQuarter', 'گزارش در ۱۵ دقیقهٔ اخیر')}
${stat('lHour', 'گزارش در ۱ ساعت اخیر')}
${stat('lDay', 'گزارش در ۲۴ ساعت اخیر')}
${stat('lClock', 'ساعت تهران', '<span id="lAgo">به‌روزرسانی هر ۳۰ ثانیه</span>')}
</section>
<section class="grid mt">
${card('نقشهٔ زنده', '<div id="cLiveMap" style="height:440px;border-radius:12px;overflow:hidden;z-index:0"></div>', 'نواحی‌ای که در ۱ ساعت اخیر گزارش داده‌اند')}
${card('جریان زنده', '<div class="feed" id="lFeed"><span class="cur"></span></div>', 'بدون شناسه، شهر یا مدل گوشی')}
</section>
</section>

<section class="tab" data-tab="users">
<section class="kpis">
${stat('uW1', 'بازگشت هفتهٔ اول', 'از نصب‌های ۷ تا ۱۳ روز پیش، چند درصد در ۷ روز اخیر برگشته‌اند')}
${stat('uStick', 'چسبندگی (DAU/MAU)', 'میانگین کاربر روزانهٔ ۷ روز اخیر ÷ کاربران ماه')}
${stat('uReturning', 'کاربران قدیمی فعال', 'در بازه فعال بوده‌اند و پیش از آن نصب کرده‌اند')}
${stat('uLost', 'ریزش', 'در ۳۰ روز اخیر بوده‌اند ولی ۷ روز است نیامده‌اند')}
</section>
${sect('کوهورت‌های هفتگی', 'var(--violet)', 'هر ردیف: نصب‌های یک هفته · هر ستون: چند درصدشان در هفتهٔ n بعد از نصب دوباره آمده‌اند')}
${card('نگهداشت کاربران', '<div id="uCohort" class="scroll"><div class="empty">در حال بارگذاری…</div></div>', '<span id="uCohortAt"></span>')}
<section class="grid2 mt">
${card('کاربران جدید و قدیمی', chart('cNewOld'), 'در روز')}
${card('نسخه‌ها در طول زمان', chart('cVersions'), '۴۵ روز اخیر · سهم هر نسخه')}
</section>
</section>

<section class="tab" data-tab="conn">
<section class="kpis">
${stat('cRate', 'نرخ موفقیت اتصال', 'اتصال موفق ÷ همهٔ تلاش‌ها (از نسخهٔ ۰٫۲٫۸)')}
${stat('cOkN', 'اتصال موفق')}
${stat('cFailN', 'اتصال ناموفق')}
${stat('cDeadN', 'وصل ولی بی‌ترافیک', 'کاربرانی که وصل شدند ولی کمتر از ۲۰۰ کیلوبایت رد و بدل کردند')}
</section>
${sect('کدام روش کار می‌کند؟', 'var(--green)', 'بر پایهٔ آخرین روش هر کاربر در بازه')}
<section class="grid">
${card('اتصال‌ها و نرخ موفقیت در روز', chart('cConnDaily'))}
${card('موفقیت به تفکیک حلقهٔ اضافه', chart('cConnHop'), '٪ موفق')}
</section>
<section class="grid3 small mt">
${card('موفقیت به تفکیک نوع سرور', chart('cConnServer'), '٪ موفق')}
${card('کشورها', '<div id="tConnCountry" class="scroll"></div>', 'نرخ موفقیت')}
${card('اپراتورها', '<div id="tConnIsp" class="scroll"></div>', 'نرخ موفقیت')}
</section>
<section class="grid2 mt">
${card('وصل ولی بی‌ترافیک', '<div id="tDead"></div>', 'به تفکیک روش · نشانهٔ فیلتر شدن')}
${card('موفقیت به تفکیک نسخه', '<div id="tConnVersion"></div>')}
</section>
${sect('روش‌های استفاده‌شده', 'var(--pink)')}
<section class="grid3 small">
${card('حلقهٔ اضافه', chart('cHop'))}
${card('نوع سرور', chart('cServer'), 'از نسخهٔ ۰٫۲٫۶')}
${card('حالت گیمینگ', chart('cGaming'))}
</section>
</section>

<section class="tab" data-tab="traffic">
${sect('حجم مصرفی', 'var(--amber)', 'فقط مجموع بایت‌های عبوری از VPN، بدون مقصد و محتوا · از نسخهٔ ۰٫۲٫۸')}
<section class="grid">
${card('حجم مصرفی در روز', chart('cVolume'), 'روز رسیدن گزارش (مصرف حدود ۲۴ ساعت قبلش)')}
${card('خلاصهٔ حجم', '<div id="tVolume" class="empty">–</div>')}
</section>
<section class="grid3 small mt">
${card('حجم بر پایهٔ نوع سرور', chart('cVolServer'))}
${card('حجم بر پایهٔ حلقهٔ اضافه', chart('cVolHop'))}
${card('پراکندگی مصرف کاربران', chart('cVolUsers'), 'تعداد کاربر')}
</section>
<section class="grid3 mt">
${card('کشورها', '<div id="tVolCountries" class="scroll"></div>', 'بر پایهٔ حجم')}
${card('اپراتورها', '<div id="tVolIsp" class="scroll"></div>', 'بر پایهٔ حجم')}
${card('منطقه', chart('cVolRegion'), 'بر پایهٔ حجم', 'small')}
</section>
</section>

<section class="tab" data-tab="geo">
${sect('نقشهٔ جهان', 'var(--green)', 'موقعیت تقریبی از روی IP (گرد‌شده به ~۱۱ کیلومتر) · گزارش‌هایی که از راه VPN رسیده‌اند کنار گذاشته می‌شوند')}
<section class="grid">
${card('کاربران روی نقشه', '<div id="cMap" style="height:420px;border-radius:12px;overflow:hidden;z-index:0"></div>', 'با غلتک ماوس زوم، با کشیدن جابه‌جا')}
${card('کشورها', '<div id="tCountries" class="scroll"></div>', 'بر پایهٔ کاربر')}
</section>
<section class="grid mt">
${card('اپراتورها و شبکه‌ها', '<div id="tIsp" class="scroll"></div>', 'از روی شبکهٔ IP · «دیتاسنتر» یعنی احتمالاً از پشت VPN یا پروکسی')}
${card('شهرها', '<div id="tCities" class="scroll"></div>', 'فقط تعداد')}
</section>
<section class="grid2 small mt">
${card('منطقهٔ انتخاب‌شده در اپ', chart('cRegion2'))}
${card('مسیر رسیدن گزارش', chart('cVia'), 'مستقیم یا از راه VPN (از نسخهٔ ۰٫۲٫۸)')}
</section>
</section>

<section class="tab" data-tab="devices">
<section class="grid">
${card('پرکاربردترین گوشی‌ها', '<div id="tModels"></div>', 'بر پایهٔ کاربر')}
${card('برند گوشی', chart('cBrand'))}
</section>
<section class="grid2 small mt">
${card('نسخهٔ اندروید', chart('cAndroid'))}
${card('نسخهٔ برنامه', chart('cVersion'))}
</section>
</section>

<section class="tab" data-tab="downloads">
<section class="kpis">
${stat('dTotal', 'کل دانلودهای APK')}
${stat('dLatest', 'دانلود آخرین نسخه', '<span id="dLatestTag"></span>')}
${stat('dToday', 'دانلود ۲۴ ساعت اخیر', 'از روی عکس روزانهٔ آمار گیت‌هاب')}
${stat('dPerActive', 'نصب فعال ÷ دانلود', 'چند درصد دانلودها در ۳۰ روز اخیر فعال بوده‌اند')}
</section>
<section class="grid mt">
${card('دانلود هر نسخه', chart('cDl'), '۱۵ نسخهٔ اخیر')}
${card('دانلود در روز', chart('cDlDay'), 'از وقتی پنل دانلودها را می‌خواند')}
</section>
${card('همهٔ نسخه‌ها', '<div id="tDl" class="scroll"></div>', '<span id="dRepo"></span>', 'mt')}
</section>

<section class="tab" data-tab="notice">
<section class="grid">
<div class="card">
<h3>اعلان داخل برنامه <em id="nState"></em></h3>
<div id="nNoToken" class="banner warn" hidden>برای ذخیره از همین‌جا، یک توکن گیت‌هاب در <a href="/settings#github">تنظیمات</a> بگذارید. تا آن وقت اعلان فعلی فقط نمایش داده می‌شود.</div>
<div style="display:flex;gap:22px;flex-wrap:wrap"><label class="switch"><input type="checkbox" id="nEnabled"> نمایش اعلان به کاربران</label>
<label class="switch" title="یک بار در نوار اعلان‌های گوشی هم نشان داده می‌شود (از نسخهٔ ۰٫۲٫۸ اپ؛ هر ۶ ساعت بررسی می‌شود)"><input type="checkbox" id="nNotify"> ارسال به‌صورت نوتیفیکیشن هم</label></div>
<label class="f">عنوان</label>
<div class="langs">${langs('Title', 'input', 120)}</div>
<label class="f">متن</label>
<div class="langs">${langs('Message', 'textarea', 1200)}</div>
<label class="f" for="nLink">لینک دکمه (اختیاری)</label><input class="in ltr" id="nLink" placeholder="https://">
<div style="margin-top:14px"><label class="switch"><input type="checkbox" id="nRenew" checked> به کسانی که اعلان قبلی را بسته‌اند هم دوباره نشان بده</label></div>
<div style="display:flex;gap:10px;margin-top:18px;flex-wrap:wrap"><button class="btn" id="nSave">ذخیره و انتشار</button><button class="btn ghost" id="nReload">بارگذاری دوباره</button></div>
<div class="msg" id="nMsg"></div>
<p style="color:var(--faint);font-size:12px;line-height:1.8;margin:14px 0 0">اعلان در فایل <code>notice.json</code> مخزن گیت‌هاب ذخیره می‌شود و برنامه‌ها چند دقیقه بعد آن را می‌بینند (کش گیت‌هاب تا ۵ دقیقه). نوتیفیکیشن را اپ (از نسخهٔ ۰٫۲٫۸) هر ۶ ساعت یک بار بررسی می‌کند و هر اعلان را فقط یک بار نشان می‌دهد؛ برای فرستادن دوباره، گزینهٔ «دوباره نشان بده» را روشن بگذارید. زبان‌های دیگر برنامه متن فارسی را نشان می‌دهند. نشانی آمار (stats_url) دست نمی‌خورد.</p>
</div>
<div class="card"><h3>پیش‌نمایش <em>صفحهٔ اول برنامه</em></h3>
<div class="seg" id="nLang" style="width:max-content;margin:0 auto 14px"><button data-l="fa" class="on">فا</button><button data-l="en">EN</button><button data-l="ru">RU</button><button data-l="zh">中</button></div>
<div class="phone"><div id="nPreview"></div></div>
</div>
</section>
</section>

<p class="note">فقط آمار ناشناس: یک شناسهٔ تصادفی برای هر نصب، بدون IP؛ از ترافیک فقط مجموع حجم (بدون مقصد و محتوا).<br>هر ۵ دقیقه به‌روز می‌شود و عددها حداکثر ۱۵ دقیقه قدیمی‌اند (برای ماندن در سهمیهٔ رایگان پایگاه‌داده). میان‌برها: <code>1</code>…<code>9</code> زبانه‌ها · <code>R</code> تازه‌سازی · <code>F</code> تمام‌صفحه</p>
</main>
</div>
<script src="/app.js?v=${tag}"></script>
</body></html>`;

export const settingsPage = (tag) => `${head('تنظیمات · پنل دکتر وی پی ان')}
<style>
.hdr .in{max-width:760px}
main{max-width:760px;margin:auto;padding:18px}
main .card{margin-bottom:16px;padding:20px}
.card h3{margin:0 0 4px;font-size:15px}
.card p{margin:0 0 14px;color:var(--muted);font-size:13px;line-height:1.8}
.row{display:flex;gap:10px;flex-wrap:wrap;align-items:flex-end}
.btn{margin-top:14px}
.danger-zone{border-color:rgba(220,38,38,.35)!important}
.ok{color:var(--green)}.bad{color:var(--red)}
td.n{width:auto}
</style></head>
<body>
<header class="hdr"><div class="in">
<a class="brand" href="/">${logo}<div><b>تنظیمات</b><small>پنل دکتر وی پی ان</small></div></a>
<div class="sp"></div>
<a class="ico" href="/">بازگشت به داشبورد</a>
</div></header>
<main id="settingsPage">

<div class="card">
<h3>تم</h3>
<p>ظاهر پنل. روی همین مرورگر ذخیره می‌شود.</p>
<select class="pick" id="theme">${themeOptions}</select>
</div>

<div class="card" id="telegram">
<h3>ربات تلگرام <span id="tgState" style="font-size:12px"></span></h3>
<p>هر روز ساعت ۰۷:۴۷ تهران گزارش دیروز را می‌فرستد، و اگر تعداد کاربران ناگهان نصف شود یا بیشتر اتصال‌ها ناموفق باشد هشدار می‌دهد (هر هشدار حداکثر هر ۶ ساعت یک بار).<br>
۱) در تلگرام به <code>@BotFather</code> پیام دهید، <code>/newbot</code> بزنید و توکن را بگیرید. ۲) به ربات خودتان یک پیام بدهید (یا آن را مدیر کانال کنید). ۳) شناسهٔ عددی خودتان را از <code>@userinfobot</code> بگیرید (برای کانال: <code>@نام_کانال</code>).</p>
<label class="f" for="tgToken">توکن ربات</label><input class="in ltr" id="tgToken" placeholder="123456789:AA..." autocomplete="off">
<label class="f" for="tgChat">شناسهٔ چت</label><input class="in ltr" id="tgChat" placeholder="123456789 / -100... / @channel">
<div class="row" style="margin-top:14px;gap:22px"><label class="switch"><input type="checkbox" id="tgDaily" checked> گزارش روزانه</label><label class="switch"><input type="checkbox" id="tgAlerts" checked> هشدارها</label></div>
<div class="row"><button class="btn" id="tgSave">ذخیره</button><button class="btn ghost" id="tgTest">ارسال پیام آزمایشی</button><button class="btn ghost" id="tgClear">حذف</button></div>
<div class="msg" id="tgMsg"></div>
</div>

<div class="card" id="github">
<h3>توکن گیت‌هاب <span id="ghState" style="font-size:12px"></span></h3>
<p>برای ویرایش اعلان داخل برنامه از پنل، و سقف بالاتر خواندن آمار دانلود. در گیت‌هاب: Settings ← Developer settings ← Fine-grained tokens ← Generate new token؛ فقط مخزن <code>DrvpnTM/app</code>، دسترسی <b>Contents: Read and write</b>.</p>
<label class="f" for="ghToken">توکن</label><input class="in ltr" id="ghToken" placeholder="github_pat_..." autocomplete="off">
<div class="row"><button class="btn" id="ghSave">ذخیره و بررسی</button><button class="btn ghost" id="ghClear">حذف</button></div>
<div class="msg" id="ghMsg"></div>
</div>

<div class="card">
<h3>تغییر رمز عبور</h3>
<p>رمز جدید باید حداقل ۸ نویسه باشد. با تغییر رمز، همهٔ دستگاه‌های واردشده خارج می‌شوند.</p>
<label class="f" for="cur">رمز فعلی</label><input class="in ltr" id="cur" type="password" autocomplete="current-password">
<label class="f" for="n1">رمز جدید</label><input class="in ltr" id="n1" type="password" autocomplete="new-password">
<label class="f" for="n2">تکرار رمز جدید</label><input class="in ltr" id="n2" type="password" autocomplete="new-password">
<button class="btn" id="pwBtn">ذخیرهٔ رمز</button>
<div class="msg" id="pwMsg"></div>
</div>

<div class="card">
<h3>ورودهای اخیر</h3>
<p id="lockNote">ورودها و تلاش‌های ناموفق اخیر.</p>
<table id="logins"><tr><td>در حال بارگذاری…</td></tr></table>
<button class="btn ghost" id="loginsClear">پاک کردن فهرست</button>
</div>

<div class="card">
<h3>سلامت پایگاه‌داده</h3>
<p>پایگاه‌داده (Cloudflare D1) در طرح رایگان روزی ۵ میلیون ردیف خواندن دارد؛ اگر تمام شود، تا ساعت ۰۰:۰۰ UTC هیچ آماری ثبت و نشان داده نمی‌شود.</p>
<table id="health"><tr><td>در حال بارگذاری…</td></tr></table>
</div>

<div class="card">
<h3>اطلاعات</h3>
<p>نشان «نصب فعال» (عمومی):<br><code id="badgeUrl"></code></p>
<p>آمار فقط ناشناس است: شناسهٔ تصادفی هر نصب، بدون IP ذخیره‌شده؛ از ترافیک فقط مجموع حجم مصرفی (بدون مقصد و محتوا)؛ از شبکه فقط نام اپراتور. موقعیت روی نقشه تقریبی و از روی IP است (گرد‌شده به ~۱۱ کیلومتر).</p>
</div>

<div class="card">
<h3>حذف خودکار دادهٔ قدیمی</h3>
<p>هر شب دادهٔ قدیمی‌تر از این تعداد روز پاک می‌شود تا پایگاه‌داده سبک بماند. «کل کاربران» و روند روزهای گذشته حفظ می‌شود. ۰ یعنی خاموش.</p>
<div class="row">
<div><label class="f" for="adays">نگه‌داشتن (روز)</label><input class="in ltr" id="adays" type="number" value="0" min="0" max="365" style="width:120px"></div>
<button class="btn ghost" id="autoBtn">ذخیره</button>
</div>
<div class="msg" id="autoMsg"></div>
</div>

<div class="card danger-zone">
<h3>مدیریت داده</h3>
<p>حذف دادهٔ قدیمی برای سبک ماندن، یا پاک کردن کامل آمار (برگشت‌ناپذیر).</p>
<div class="row">
<div><label class="f" for="pdays">حذف دادهٔ قدیمی‌تر از (روز)</label><input class="in ltr" id="pdays" type="number" value="90" min="7" max="365" style="width:120px"></div>
<button class="btn ghost" id="pruneBtn">حذف قدیمی‌ها</button>
</div>
<div><button class="btn danger" id="resetBtn">پاک کردن کامل آمار</button></div>
<div class="msg" id="dataMsg"></div>
</div>

</main>
<script src="/app.js?v=${tag}"></script>
</body></html>`;
