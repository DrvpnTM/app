// Dr VPN admin panel in the browser: the dashboard and the settings page (served at /app.js).
(() => {
'use strict';

// ---------- Shared ----------

const $ = (id) => document.getElementById(id);
const fa = (n) => Number(n || 0).toLocaleString('fa-IR');
const pct = (a, b) => (b ? Math.round((a / b) * 100) : 0);
const esc = (s) => String(s ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const UNITS = ['بایت', 'کیلوبایت', 'مگابایت', 'گیگابایت', 'ترابایت', 'پتابایت'];
/** Bytes in words: "۱٫۴ گیگابایت". */
function vol(b) {
  b = Number(b || 0);
  let i = 0; while (b >= 1024 && i < UNITS.length - 1) { b /= 1024; i++; }
  return Number(b.toFixed(b >= 100 || i === 0 ? 0 : 1)).toLocaleString('fa-IR') + ' ' + UNITS[i];
}
const tehran = (ms, opts) => new Date(ms).toLocaleString('fa-IR', Object.assign({ timeZone: 'Asia/Tehran' }, opts));
const hm = (ms) => tehran(ms, { hour: '2-digit', minute: '2-digit' });

const REGION = { ir: 'ایران', cn: 'چین', ru: 'روسیه', other: 'سایر کشورها' };
const HOP = { off: 'بدون حلقه', lantern: 'جادهٔ ابریشم', shield: 'دیوار چین', psiphon: 'PS Pro', tunnel: 'تونل دکتر', byedpi: 'ضد DPI دکتر', tor: 'CDN سیاه نا امن', emergency: 'حالت اضطراری' };
const SERVER = { xray: 'سرور Xray', aether: 'Aether WARP', openvpn: 'Dr Open VPN', serverless: 'Dr Serverless', custom: 'کانفیگ دستی', group: 'گروه/زنجیر', none: 'بدون سرور', '?': 'نسخهٔ قدیمی' };
const VIA = { direct: 'مستقیم', vpn: 'از راه VPN', '?': 'نسخهٔ قدیمی' };
const SERVER_COLOR = { xray: '#2563eb', aether: '#f59e0b', openvpn: '#16a34a', serverless: '#7c3aed', custom: '#0d9488', group: '#db2777', '?': '#94a3b8' };
const HOP_COLOR = { off: '#94a3b8', lantern: '#0d9488', shield: '#c62828', psiphon: '#f59e0b', tunnel: '#2563eb', byedpi: '#ea580c', tor: '#334155', emergency: '#db2777' };
const PALETTE = ['#2563eb', '#7c3aed', '#16a34a', '#f59e0b', '#db2777', '#0d9488', '#64748b', '#ea580c', '#0891b2'];
const NEON = ['#00e5ff', '#ff2bd6', '#39ff9f', '#ffc53d', '#b26bff', '#ff7a45', '#4d9fff', '#ff3b6b', '#7fffd4'];
const CNAME = { IR: 'ایران', CN: 'چین', RU: 'روسیه', US: 'آمریکا', DE: 'آلمان', TR: 'ترکیه', AE: 'امارات', GB: 'انگلیس', FR: 'فرانسه', NL: 'هلند', CA: 'کانادا', IN: 'هند', IQ: 'عراق', AF: 'افغانستان', PK: 'پاکستان', SE: 'سوئد', FI: 'فنلاند', IT: 'ایتالیا', ES: 'اسپانیا', PL: 'لهستان', AZ: 'آذربایجان', AM: 'ارمنستان', GE: 'گرجستان', QA: 'قطر', OM: 'عمان', KW: 'کویت', SA: 'عربستان', MY: 'مالزی', ID: 'اندونزی', JP: 'ژاپن', KR: 'کره جنوبی', AU: 'استرالیا', BR: 'برزیل', UA: 'اوکراین', BY: 'بلاروس', CH: 'سوئیس', AT: 'اتریش', BE: 'بلژیک', NO: 'نروژ', DK: 'دانمارک', CZ: 'چک', RO: 'رومانی', KZ: 'قزاقستان', UZ: 'ازبکستان', TM: 'ترکمنستان', TJ: 'تاجیکستان', BH: 'بحرین', LB: 'لبنان', SY: 'سوریه', JO: 'اردن', EG: 'مصر', HK: 'هنگ‌کنگ', TW: 'تایوان', SG: 'سنگاپور', MM: 'میانمار', VN: 'ویتنام', TH: 'تایلند', BD: 'بنگلادش', HU: 'مجارستان', BG: 'بلغارستان', GR: 'یونان', CY: 'قبرس', IE: 'ایرلند', PT: 'پرتغال', LT: 'لیتوانی', LV: 'لتونی', EE: 'استونی', RS: 'صربستان', MD: 'مولداوی', KG: 'قرقیزستان', MN: 'مغولستان', NZ: 'نیوزیلند', MX: 'مکزیک', AR: 'آرژانتین', ZA: 'آفریقای جنوبی', NG: 'نیجریه', KE: 'کنیا', MA: 'مراکش', DZ: 'الجزایر', TN: 'تونس', IL: 'اسرائیل' };
/** Well-known networks by AS number, by the names people know them. */
const ASN = {
  197207: 'همراه اول', 44244: 'ایرانسل', 58224: 'مخابرات', 12880: 'زیرساخت', 57218: 'رایتل', 31549: 'شاتل',
  16322: 'پارس آنلاین', 43754: 'آسیاتک', 25184: 'افرانت', 50810: 'مبین‌نت', 48159: 'TIC', 39074: 'صبانت',
  4134: 'China Telecom', 4837: 'China Unicom', 9808: 'China Mobile', 56046: 'China Mobile', 12389: 'Rostelecom',
  8359: 'MTS', 31133: 'MegaFon', 3216: 'Beeline', 13335: 'Cloudflare',
};
const HOSTING = /hosting|host|cloud|data ?cent|datacenter|server|hetzner|ovh|digitalocean|amazon|aws|google|microsoft|azure|oracle|m247|contabo|linode|akamai|vultr|choopa|leaseweb|scaleway|alibaba|tencent|cdn77|datacamp|g-core|gcore|stark|clouvider|psychz|colocrossing|hostinger|ionos|kamatera|frantech|buyvm|aeza|timeweb|selectel|serverius|worldstream|packet|cogent/i;
const flag = (cc) => (cc && /^[A-Z]{2}$/.test(cc)) ? String.fromCodePoint(...[...cc].map((c) => 0x1f1e6 + c.charCodeAt(0) - 65)) : '🏳️';
const cname = (cc) => CNAME[cc] || cc;
/** "44244|Iran Cell Service..." as "ایرانسل · AS44244", marked when it is a data center. */
function ispLabel(key, plain) {
  const [asn, ...rest] = String(key).split('|');
  const org = rest.join('|');
  const name = ASN[asn] || org || 'AS' + asn;
  if (plain) return name;
  return esc(name) + ' <span class="sub">AS' + esc(asn) + '</span>' + (!ASN[asn] && HOSTING.test(org) ? '<span class="pill dc">دیتاسنتر</span>' : '');
}

const theme = () => document.documentElement.getAttribute('data-theme') || 'auto';
function isDark() { const t = theme(); if (t === 'dark' || t === 'cyber') return true; if (t === 'light' || t === 'glass') return false; return matchMedia('(prefers-color-scheme:dark)').matches; }
function setTheme(v) {
  try { localStorage.setItem('drvpn_theme', v); } catch (e) {}
  if (v === 'auto') document.documentElement.removeAttribute('data-theme'); else document.documentElement.setAttribute('data-theme', v);
}
function themePicker(onChange) {
  const sel = $('theme');
  try { sel.value = localStorage.getItem('drvpn_theme') || 'light'; } catch (e) { sel.value = 'light'; }
  sel.addEventListener('change', () => { setTheme(sel.value); if (onChange) onChange(); });
}
async function getJson(url, init) {
  const r = await fetch(url, Object.assign({ credentials: 'same-origin' }, init));
  if (r.status === 401) { location.reload(); throw new Error('login'); }
  let j = null; try { j = await r.json(); } catch (e) {}
  if (!r.ok || !j || j.ok === false) { const err = new Error('HTTP ' + r.status); err.detail = j && j.error; throw err; }
  return j;
}
function show(el, ok, text) { el.className = 'msg ' + (ok ? 'ok' : 'err'); el.textContent = text; }

if ($('settingsPage')) { settingsPage(); return; }

// ---------- Charts ----------

const css = (n) => getComputedStyle(document.documentElement).getPropertyValue(n).trim();
const palette = () => (theme() === 'cyber' ? NEON : PALETTE);
const neon = (c, i) => (theme() === 'cyber' ? NEON[i % NEON.length] : c);
function chartTheme() {
  Chart.defaults.color = css('--muted') || '#64748b';
  Chart.defaults.borderColor = css('--line') || '#e5e9f2';
}
Chart.defaults.font.family = 'Vazirmatn, system-ui, sans-serif';
Chart.defaults.font.size = 12;
Chart.defaults.plugins.legend.labels.usePointStyle = true;
Chart.defaults.plugins.legend.labels.boxWidth = 8;
Chart.defaults.plugins.legend.labels.padding = 14;
Chart.defaults.plugins.tooltip.rtl = true;
Chart.defaults.plugins.tooltip.boxPadding = 6;
Chart.defaults.plugins.tooltip.callbacks.label = (c) => ' ' + (c.dataset.label ? c.dataset.label + ': ' : c.label + ': ') + fa(c.parsed.y ?? c.parsed);
chartTheme();

const charts = {};
function put(id, config) {
  const el = $(id);
  if (!el) return;
  const has = config.data.datasets.some((d) => d.data.some((v) => (v && typeof v === 'object' ? v.y : v) > 0));
  el.parentElement.querySelector('.empty')?.remove();
  el.style.display = has ? '' : 'none';
  if (!has) { el.insertAdjacentHTML('afterend', '<div class="empty">هنوز داده‌ای نیست</div>'); if (charts[id]) { charts[id].destroy(); delete charts[id]; } return; }
  if (charts[id] && charts[id].config.type === config.type) { charts[id].data = config.data; charts[id].options = Object.assign({ maintainAspectRatio: false }, config.options); charts[id].update('none'); return; }
  if (charts[id]) charts[id].destroy();
  charts[id] = new Chart(el, Object.assign({}, config, { options: Object.assign({ maintainAspectRatio: false }, config.options) }));
}
const doughnut = (rows, names, colors) => ({
  type: 'doughnut',
  data: { labels: rows.map((r) => (names && names[r.k]) || r.k), datasets: [{ data: rows.map((r) => r.n), backgroundColor: rows.map((r, i) => (theme() === 'cyber' ? NEON[i % NEON.length] : (colors && colors[r.k]) || PALETTE[i % PALETTE.length])), borderWidth: theme() === 'cyber' ? 2 : 0, borderColor: css('--bg'), hoverOffset: 6 }] },
  options: { cutout: '68%', plugins: { legend: { position: 'bottom' } } },
});
const bars = (labels, data, color, label = 'کاربر') => ({
  type: 'bar',
  data: { labels, datasets: [{ label, data, backgroundColor: neon(color, 0), borderRadius: 7, maxBarThickness: 46 }] },
  options: { plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true, ticks: { precision: 0, callback: fa } }, x: { grid: { display: false } } } },
});
/** Tooltips and the value axis in bytes. */
const volTip = { callbacks: { label: (c) => ' ' + (c.dataset.label || c.label) + ': ' + vol(c.parsed.y ?? c.parsed) } };
const volDoughnut = (rows, names, colors) => { const c = doughnut(rows, names, colors); c.options.plugins.tooltip = volTip; return c; };
/** Success rate per group as horizontal bars, with the counts in the tooltip. */
function rateBars(rows, names) {
  rows = rows.slice(0, 10);
  return {
    type: 'bar',
    data: {
      labels: rows.map((r) => (names && names[r.k]) || r.k),
      datasets: [{ label: 'موفقیت', data: rows.map((r) => pct(r.ok, r.ok + r.fail)), backgroundColor: rows.map((r) => rateColor(pct(r.ok, r.ok + r.fail))), borderRadius: 6, maxBarThickness: 26 }],
    },
    options: {
      indexAxis: 'y',
      plugins: { legend: { display: false }, tooltip: { callbacks: { label: (c) => { const r = rows[c.dataIndex]; return ' ' + fa(c.parsed.x) + '٪ موفق · ' + fa(r.ok) + ' موفق، ' + fa(r.fail) + ' ناموفق · ' + fa(r.users) + ' کاربر'; } } } },
      scales: { x: { min: 0, max: 100, ticks: { callback: (v) => fa(v) + '٪' } }, y: { grid: { display: false } } },
    },
  };
}
function rateColor(p) { return p >= 80 ? css('--green') || '#16a34a' : p >= 50 ? css('--amber') || '#f59e0b' : css('--red') || '#dc2626'; }
function spark(id, data, kind = 'line') {
  const el = $(id);
  const color = neon(el.dataset.color || '#2563eb', ['sOnline', 'sActive', 'sUsage', 'sPeak'].indexOf(id) + 1);
  if (charts[id] && charts[id].config.type !== kind) { charts[id].destroy(); delete charts[id]; }
  if (charts[id]) { charts[id].data.datasets[0].data = data; charts[id].data.labels = data.map((_, i) => i); charts[id].data.datasets[0].borderColor = color; charts[id].data.datasets[0].backgroundColor = color + '22'; charts[id].update('none'); return; }
  charts[id] = new Chart(el, {
    type: kind,
    data: { labels: data.map((_, i) => i), datasets: [kind === 'bar'
      ? { data, backgroundColor: color + 'aa', borderColor: color, borderRadius: 1, barPercentage: .7, categoryPercentage: 1 }
      : { data, borderColor: color, backgroundColor: color + '22', fill: true, tension: .4, pointRadius: 0, borderWidth: 2 }] },
    options: { maintainAspectRatio: false, plugins: { legend: { display: false }, tooltip: { enabled: false } }, scales: { x: { display: false }, y: { display: false } }, animation: false },
  });
}
function trend(id, cur, prev, good = 1) {
  const el = $(id);
  if (cur == null || (cur === 0 && prev === 0)) { el.className = 'trend flat'; el.textContent = '—'; return; }
  const p = prev > 0 ? Math.round(((cur - prev) / prev) * 100) : (cur > 0 ? 100 : 0);
  el.className = 'trend ' + ((p >= 0) === (good > 0) ? 'up' : 'down');
  el.textContent = (p >= 0 ? '▲ ' : '▼ ') + fa(Math.abs(p)) + '٪';
}
/** A ranked list with a bar under each name. */
function rows(el, data, label, color, format) {
  if (!el) return;
  const max = Math.max(1, ...data.map((r) => r.n));
  el.innerHTML = data.length
    ? '<table>' + data.map((r, i) => '<tr><td class="rank">' + fa(i + 1) + '</td><td>' + label(r) + '<div class="bar" style="width:' + Math.round((r.n / max) * 100) + '%;background:' + (color || 'linear-gradient(90deg,#93c5fd,#2563eb)') + '"></div></td><td class="n"' + (format ? ' style="width:auto"' : '') + '>' + (format || fa)(r.n) + '</td></tr>').join('') + '</table>'
    : '<div class="empty">هنوز داده‌ای نیست</div>';
}
/** Success rate per group as a table. */
function rateTable(el, data, label) {
  el.innerHTML = data.length
    ? '<table><tr><th></th><th>موفقیت</th><th>تلاش</th></tr>' + data.map((r) => { const p = pct(r.ok, r.ok + r.fail); return '<tr><td>' + label(r) + '</td><td><span class="rate" style="background:color-mix(in srgb,' + rateColor(p) + ' 18%,transparent);color:' + rateColor(p) + '">' + fa(p) + '٪</span></td><td class="n">' + fa(r.ok + r.fail) + '</td></tr>'; }).join('') + '</table>'
    : '<div class="empty">هنوز داده‌ای نیست (از نسخهٔ ۰٫۲٫۸)</div>';
}

// ---------- Maps ----------

const tileUrl = () => (isDark()
  ? 'https://server.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Dark_Gray_Base/MapServer/tile/{z}/{y}/{x}'
  : 'https://server.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Light_Gray_Base/MapServer/tile/{z}/{y}/{x}');
const maps = {};
function mapFor(id) {
  const el = $(id);
  if (typeof L === 'undefined') { el.innerHTML = '<div class="empty">نقشه بارگذاری نشد</div>'; return null; }
  let m = maps[id];
  if (!m) {
    m = maps[id] = { map: L.map(el, { worldCopyJump: true, minZoom: 2 }).setView([30, 45], 3), layer: null, dark: null, fitted: false };
    m.markers = L.layerGroup().addTo(m.map);
  }
  // Tiles only swap when the theme actually changed (keeps the admin's current view).
  if (m.dark !== isDark()) {
    if (m.layer) m.map.removeLayer(m.layer);
    m.dark = isDark();
    m.layer = L.tileLayer(tileUrl(), { maxZoom: 16, attribution: 'Tiles &copy; Esri' }).addTo(m.map);
  }
  setTimeout(() => m.map.invalidateSize(), 60);
  return m;
}
function renderMap(points) {
  const m = mapFor('cMap'); if (!m) return;
  m.markers.clearLayers();
  const c = theme() === 'cyber' ? '#00e5ff' : '#2563eb';
  const bounds = [];
  for (const p of points || []) {
    L.circleMarker([p.lat, p.lon], { radius: 4 + Math.min(16, Math.sqrt(p.n) * 2.4), color: c, weight: 1, fillColor: c, fillOpacity: 0.4 })
      .bindTooltip('کاربران این ناحیه: ' + fa(p.n), { direction: 'top' }).addTo(m.markers);
    bounds.push([p.lat, p.lon]);
  }
  // Fit to the data once; later refreshes keep whatever the admin is looking at.
  if (bounds.length && !m.fitted) { m.map.fitBounds(bounds, { padding: [40, 40], maxZoom: 6 }); m.fitted = true; }
}
function renderLiveMap(spots) {
  const m = mapFor('cLiveMap'); if (!m) return;
  m.markers.clearLayers();
  for (const p of spots || []) {
    const size = 8 + Math.min(14, Math.sqrt(p.n) * 3);
    L.marker([p.lat, p.lon], { icon: L.divIcon({ className: '', html: '<div class="pulse-dot" style="width:' + size + 'px;height:' + size + 'px"></div>', iconSize: [size, size] }) })
      .bindTooltip('گزارش در ۱ ساعت اخیر: ' + fa(p.n), { direction: 'top' }).addTo(m.markers);
  }
  if (spots && spots.length && !m.fitted) { m.map.fitBounds(spots.map((p) => [p.lat, p.lon]), { padding: [40, 40], maxZoom: 5 }); m.fitted = true; }
}

// ---------- State ----------

const state = { days: 30, tab: 'overview', f: '', v: '', stats: null, base: null, users: null, live: null, downloads: null, notice: null };
// The view is in the address (#tab=conn&f=country&v=IR&d=7), so it can be bookmarked and reloaded.
(function readHash() {
  const h = new URLSearchParams(location.hash.slice(1));
  if ([7, 30, 90].includes(Number(h.get('d')))) state.days = Number(h.get('d'));
  if (h.get('tab') && document.querySelector('.tab[data-tab="' + h.get('tab') + '"]')) state.tab = h.get('tab');
  if (h.get('f')) { state.f = h.get('f'); state.v = h.get('v') || ''; }
})();
function writeHash() {
  const h = new URLSearchParams();
  if (state.tab !== 'overview') h.set('tab', state.tab);
  if (state.days !== 30) h.set('d', state.days);
  if (state.f && state.v) { h.set('f', state.f); h.set('v', state.v); }
  history.replaceState(null, '', h.toString() ? '#' + h : location.pathname);
}

// ---------- Dashboard numbers ----------

let loadSequence = 0, lastLoad = 0;
async function load(fresh) {
  lastLoad = Date.now();
  const sequence = ++loadSequence;
  const btn = $('refresh'); btn.classList.add('spin');
  const error = $('loadError'), staleNote = $('staleNote');
  error.hidden = true;
  try {
    const q = '/stats?days=' + state.days + (state.f && state.v ? '&f=' + encodeURIComponent(state.f) + '&v=' + encodeURIComponent(state.v) : '') + (fresh ? '&fresh=1' : '');
    const s = await getJson(q);
    if (sequence !== loadSequence) return;
    state.stats = s;
    if (!s.filter) state.base = s;
    const at = (t) => hm(t);
    staleNote.hidden = !s.stale && !s.limited;
    if (s.limited) staleNote.textContent = 'سهمیهٔ امروزِ پنل برای خواندن پایگاه‌داده تمام شده؛ عددهای ساعت ' + at(s.cachedAt) + ' نشان داده می‌شود و فردا دوباره تازه می‌شود. ثبت آمار برنامه ادامه دارد.';
    if (s.stale) staleNote.textContent = 'پایگاه‌داده الان جواب نمی‌دهد؛ آخرین عددهای سالم (ساعت ' + at(s.cachedAt) + ') نشان داده می‌شود. ' + (s.error || '');
    $('upd').textContent = '· به‌روزرسانی ' + tehran(Date.parse(s.updated), { hour: '2-digit', minute: '2-digit', second: '2-digit' });
    render(s);
    if (!state.base) loadBase();
    else fillFilterValues();
    if (state.tab === 'users') loadUsers();
  } catch (e) {
    if (sequence === loadSequence) {
      error.textContent = e.detail ? 'بارگذاری آمار ناموفق بود: ' + e.detail : 'بارگذاری آمار ناموفق بود. اتصال اینترنت را بررسی کنید و دوباره تلاش کنید.';
      error.hidden = false;
      console.error('Failed to load dashboard statistics:', e);
    }
  } finally {
    if (sequence === loadSequence) btn.classList.remove('spin');
  }
}
/** The unfiltered numbers, for the filter's list of values, when the page opened filtered. */
async function loadBase() {
  try { state.base = await getJson('/stats?days=' + state.days); fillFilterValues(); } catch (e) {}
}

function fillDays(series, n) {
  const map = Object.fromEntries(series.map((d) => [d.day, d]));
  const out = [];
  for (let i = n - 1; i >= 0; i--) {
    const iso = new Date(Date.now() - i * 864e5).toISOString().slice(0, 10);
    const r = map[iso] || {};
    out.push({ day: iso, label: new Date(iso).toLocaleDateString('fa-IR', { month: 'short', day: 'numeric' }), active: r.active || 0, newUsers: r.newUsers || 0, connects: r.connects || 0, fails: r.fails || 0, up: r.up || 0, down: r.down || 0 });
  }
  return out;
}

let lastRows = [];
function render(s) {
  const d = fillDays(s.series, s.rangeDays); lastRows = d;
  const bd = s.breakdown;
  const labels = d.map((x) => x.label), skip = { ticks: { maxTicksLimit: s.rangeDays > 40 ? 8 : 10 } };
  const P = palette();

  // Top bar
  $('aDay').textContent = fa(s.active.day);
  $('aWeek').textContent = fa(s.active.week);
  $('aMonth').textContent = fa(s.active.month);

  const conn = s.conn || { reporting: 0, byHop: [], byServer: [], byVersion: [], byCountry: [], byIsp: [], deadByHop: [], deadByServer: [] };
  const okSum = conn.byHop.reduce((a, r) => a + r.ok, 0), failSum = conn.byHop.reduce((a, r) => a + r.fail, 0);
  const okPct = okSum + failSum ? pct(okSum, okSum + failSum) : null;
  const volNow = (s.period.up || 0) + (s.period.down || 0), volPrev = (s.prev.up || 0) + (s.prev.down || 0);
  renderOverview(s, d);

  // Overview charts
  put('cDaily', { type: 'line', data: { labels, datasets: [
    { label: 'فعال', data: d.map((x) => x.active), borderColor: P[0], backgroundColor: P[0] + '22', fill: true, tension: .35, pointRadius: 0, borderWidth: 2.5 },
    { label: 'جدید', data: d.map((x) => x.newUsers), borderColor: P[2], backgroundColor: 'transparent', tension: .35, pointRadius: 0, borderWidth: 2 },
  ] }, options: { interaction: { mode: 'index', intersect: false }, plugins: { legend: { position: 'top', align: 'end' } }, scales: { y: { beginAtZero: true, ticks: { precision: 0, callback: fa } }, x: Object.assign({ grid: { display: false } }, skip) } } });
  put('cRegion', doughnut(bd.byRegion, REGION));
  // The last 24 hours, one bar per hour, in Tehran time.
  const hourNow = Math.floor(Date.now() / 3600000), byHour = Object.fromEntries((s.hourly || []).map((h) => [h.at / 3600, h.n]));
  const hours = []; for (let i = 23; i >= 0; i--) hours.push(hourNow - i);
  put('cHourly', { type: 'bar', data: { labels: hours.map((h) => tehran(h * 3600000, { hour: '2-digit', minute: '2-digit' })), datasets: [{ label: 'گزارش', data: hours.map((h) => byHour[h] || 0), backgroundColor: hours.map((h, i) => (i === 23 ? P[1] : P[0] + 'aa')), borderRadius: 4, maxBarThickness: 18 }] },
    options: { plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true, ticks: { precision: 0, callback: fa } }, x: { grid: { display: false }, ticks: { maxTicksLimit: 12 } } } } });
  rows($('tTopVersions'), bd.byVersion.slice(0, 8), (r) => 'v' + esc(r.k), 'linear-gradient(90deg,' + P[4] + '55,' + P[4] + ')');

  // Users tab (the parts that come with the dashboard numbers)
  const ret = s.retention || { cohort: 0, back: 0 };
  $('uW1').textContent = ret.cohort ? fa(pct(ret.back, ret.cohort)) + '٪' : '–';
  const u = s.users || {};
  $('uStick').textContent = u.dauMau != null ? fa(Math.round(u.dauMau * 100)) + '٪' : '–';
  $('uReturning').textContent = fa(u.returning);
  $('uLost').textContent = fa(u.lost);
  put('cNewOld', { type: 'bar', data: { labels, datasets: [
    { label: 'قدیمی', data: d.map((x) => Math.max(0, x.active - x.newUsers)), backgroundColor: P[0], borderRadius: 3, stack: 'u', maxBarThickness: 22 },
    { label: 'جدید', data: d.map((x) => x.newUsers), backgroundColor: P[2], borderRadius: 3, stack: 'u', maxBarThickness: 22 },
  ] }, options: { interaction: { mode: 'index', intersect: false }, plugins: { legend: { position: 'top', align: 'end' } }, scales: { y: { stacked: true, beginAtZero: true, ticks: { precision: 0, callback: fa } }, x: Object.assign({ stacked: true, grid: { display: false } }, skip) } } });

  // Connection tab
  $('cRate').textContent = okPct == null ? '–' : fa(okPct) + '٪';
  $('cOkN').textContent = fa(okSum);
  $('cFailN').textContent = fa(failSum);
  const deadN = conn.deadByHop.reduce((a, r) => a + r.dead, 0), deadAll = conn.deadByHop.reduce((a, r) => a + r.n, 0);
  $('cDeadN').textContent = deadAll ? fa(deadN) + ' (' + fa(pct(deadN, deadAll)) + '٪)' : '–';
  put('cConnDaily', { type: 'bar', data: { labels, datasets: [
    { type: 'line', label: 'موفقیت ٪', data: d.map((x) => (x.fails ? pct(x.connects, x.connects + x.fails) : null)), borderColor: P[2], backgroundColor: 'transparent', yAxisID: 'p', tension: .3, pointRadius: 2, spanGaps: true },
    { label: 'موفق', data: d.map((x) => x.connects), backgroundColor: P[0] + 'cc', borderRadius: 3, stack: 'c', maxBarThickness: 20 },
    { label: 'ناموفق', data: d.map((x) => x.fails), backgroundColor: css('--red') || '#dc2626', borderRadius: 3, stack: 'c', maxBarThickness: 20 },
  ] }, options: { interaction: { mode: 'index', intersect: false }, plugins: { legend: { position: 'top', align: 'end' } },
    scales: { y: { stacked: true, beginAtZero: true, ticks: { precision: 0, callback: fa } }, p: { position: 'left', min: 0, max: 100, grid: { display: false }, ticks: { callback: (v) => fa(v) + '٪' } }, x: Object.assign({ stacked: true, grid: { display: false } }, skip) } } });
  put('cConnHop', rateBars(conn.byHop, HOP));
  put('cConnServer', rateBars(conn.byServer, SERVER));
  rateTable($('tConnCountry'), conn.byCountry, (r) => flag(r.k) + ' ' + esc(cname(r.k)));
  rateTable($('tConnIsp'), conn.byIsp, (r) => ispLabel(r.k));
  rateTable($('tConnVersion'), conn.byVersion.slice(0, 10), (r) => 'v' + esc(r.k));
  $('tDead').innerHTML = conn.deadByHop.length
    ? '<table><tr><th>روش</th><th>کاربر وصل‌شده</th><th>بی‌ترافیک</th></tr>' + conn.deadByHop.map((r) => { const p = pct(r.dead, r.n); return '<tr><td>' + esc(HOP[r.k] || r.k) + '</td><td>' + fa(r.n) + '</td><td class="n"><span class="rate" style="background:color-mix(in srgb,' + rateColor(100 - p) + ' 18%,transparent);color:' + rateColor(100 - p) + '">' + fa(p) + '٪</span></td></tr>'; }).join('') + '</table>'
    : '<div class="empty">هنوز داده‌ای نیست (از نسخهٔ ۰٫۲٫۸)</div>';
  put('cHop', doughnut(bd.byHop, HOP, HOP_COLOR));
  put('cServer', doughnut(bd.byServer || [], SERVER, SERVER_COLOR));
  put('cGaming', doughnut(bd.gaming.map((r) => ({ n: r.n, k: r.k ? 'روشن' : 'خاموش' })), null, { 'روشن': '#7c3aed', 'خاموش': isDark() ? '#334155' : '#cbd5e1' }));

  // Traffic tab
  const tr = s.traffic || { reporting: 0, byServer: [], byHop: [], byRegion: [], byCountry: [], byIsp: [], perUser: [] };
  $('aHour').textContent = state.live ? fa(state.live.counts.hour) : '–';
  put('cVolume', { type: 'bar', data: { labels, datasets: [
    { label: 'دانلود', data: d.map((x) => x.down), backgroundColor: P[3], borderRadius: 4, maxBarThickness: 22, stack: 'v' },
    { label: 'آپلود', data: d.map((x) => x.up), backgroundColor: P[5], borderRadius: 4, maxBarThickness: 22, stack: 'v' },
  ] }, options: { interaction: { mode: 'index', intersect: false }, plugins: { legend: { position: 'top', align: 'end' }, tooltip: volTip },
    scales: { y: { stacked: true, beginAtZero: true, ticks: { callback: vol } }, x: Object.assign({ stacked: true, grid: { display: false } }, skip) } } });
  const tVol = $('tVolume');
  if (volNow > 0) {
    const peak = d.reduce((m, x) => (x.up + x.down > m.up + m.down ? x : m), d[0]);
    const line = (k, v) => '<tr><td style="color:var(--muted)">' + k + '</td><td class="n" style="width:auto">' + v + '</td></tr>';
    tVol.className = '';
    tVol.innerHTML = '<table>'
      + line('کل حجم بازه', vol(volNow)) + line('دانلود', vol(s.period.down)) + line('آپلود', vol(s.period.up))
      + line('بازهٔ قبل', vol(volPrev)) + line('میانگین روزانه', vol(volNow / s.rangeDays))
      + line('پرمصرف‌ترین روز', peak.label + ' · ' + vol(peak.up + peak.down))
      + line('کاربران گزارش‌دهنده', fa(tr.reporting) + ' از ' + fa(s.period.users))
      + line('میانگین هر کاربر', tr.reporting ? vol(volNow / tr.reporting) : '–')
      + line('میانگین هر اتصال', s.period.connects ? vol(volNow / s.period.connects) : '–')
      + '</table>';
  } else { tVol.className = 'empty'; tVol.textContent = 'هنوز حجمی گزارش نشده (از نسخهٔ ۰٫۲٫۸)'; }
  put('cVolServer', volDoughnut(tr.byServer, SERVER, SERVER_COLOR));
  put('cVolHop', volDoughnut(tr.byHop, HOP, HOP_COLOR));
  put('cVolRegion', volDoughnut(tr.byRegion, REGION));
  const VOL_BUCKET = { '0': 'بدون مصرف', '<100MB': 'زیر ۱۰۰ مگ', '100MB-1GB': '۱۰۰ مگ تا ۱ گیگ', '1-5GB': '۱ تا ۵ گیگ', '5-20GB': '۵ تا ۲۰ گیگ', '>20GB': 'بالای ۲۰ گیگ' };
  put('cVolUsers', bars(tr.perUser.map((r) => VOL_BUCKET[r.k] || r.k), tr.perUser.map((r) => r.n), '#f59e0b'));
  rows($('tVolCountries'), tr.byCountry, (r) => flag(r.k) + '&nbsp; ' + esc(cname(r.k)), 'linear-gradient(90deg,#fcd34d,#f59e0b)', vol);
  rows($('tVolIsp'), tr.byIsp || [], (r) => ispLabel(r.k), 'linear-gradient(90deg,#fcd34d,#f59e0b)', vol);

  // Geography tab
  rows($('tCountries'), bd.byCountry || [], (r) => flag(r.k) + '&nbsp; ' + esc(cname(r.k)), 'linear-gradient(90deg,#86efac,#16a34a)');
  rows($('tCities'), bd.byCity || [], (r) => { const p = r.k.split('|'); return flag(p[0]) + '&nbsp; ' + esc(p[1] || '?') + ' <span class="sub">· ' + esc(cname(p[0])) + '</span>'; }, 'linear-gradient(90deg,#c4b5fd,#7c3aed)');
  rows($('tIsp'), bd.byIsp || [], (r) => ispLabel(r.k), 'linear-gradient(90deg,#67e8f9,#0891b2)');
  put('cRegion2', doughnut(bd.byRegion, REGION));
  put('cVia', doughnut(bd.byVia || [], VIA, { direct: '#16a34a', vpn: '#f59e0b', '?': '#94a3b8' }));
  if (state.tab === 'geo') renderMap(s.points);

  // Devices tab
  put('cBrand', doughnut(bd.byBrand.map((r) => ({ n: r.n, k: r.k === '?' ? 'نامشخص' : r.k }))));
  put('cAndroid', bars(bd.byAndroid.map((r) => (r.k === '?' ? 'نامشخص' : 'اندروید ' + r.k)), bd.byAndroid.map((r) => r.n), '#16a34a'));
  put('cVersion', doughnut(bd.byVersion.map((r) => ({ n: r.n, k: 'v' + r.k }))));
  const top = bd.byModel.filter((r) => !r.k.includes('?'));
  if (top.length) rows($('tModels'), top, (r) => esc(r.k));
  else $('tModels').innerHTML = '<div class="empty">هنوز داده‌ای نیست</div>';
}

// ---------- Dashboard overview ----------

/** A moving average of the last [n] values (fewer at the start). */
const movingAvg = (arr, n = 7) => arr.map((_, i) => { const w = arr.slice(Math.max(0, i - n + 1), i + 1); return w.reduce((a, b) => a + b, 0) / w.length; });
const avg = (arr) => (arr.length ? arr.reduce((a, b) => a + b, 0) / arr.length : 0);
/** "▲ ۱۲٪" with up/down/flat; [base] 0 gives flat. */
function delta(cur, base) {
  if (!base) return { cls: 'flat', text: '—' };
  const p = Math.round(((cur - base) / base) * 100);
  return { cls: p > 0 ? 'up' : p < 0 ? 'down' : 'flat', text: (p > 0 ? '▲ ' : p < 0 ? '▼ ' : '') + fa(Math.abs(p)) + '٪' };
}
const footItem = (icon, label, value) => '<div><span class="fi">' + icon + '</span><span><small>' + label + '</small><b>' + value + '</b></span></div>';
const I = {
  db: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><ellipse cx="12" cy="5" rx="9" ry="3"/><path d="M3 5v14c0 1.7 4 3 9 3s9-1.3 9-3V5M3 12c0 1.7 4 3 9 3s9-1.3 9-3"/></svg>',
  cal: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2"/><path d="M16 2v4M8 2v4M3 10h18"/></svg>',
  bolt: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M13 2L3 14h9l-1 8 10-12h-9z"/></svg>',
  user: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="8" r="4"/><path d="M4 21a8 8 0 0 1 16 0"/></svg>',
  ok: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M8 12l3 3 5-6"/></svg>',
};
function setCtx(id, text, cls) { const el = $(id); el.textContent = text; el.className = 'ctx' + (cls ? ' ' + cls : ''); }

function renderOverview(s, d) {
  const P = palette();
  const range = fa(s.rangeDays) + ' روز اخیر';
  const labels = d.map((x) => x.label), skip = { ticks: { maxTicksLimit: s.rangeDays > 40 ? 8 : 10 } };
  const usage = d.map((x) => x.up + x.down), users = d.map((x) => x.active);
  const today = d[d.length - 1], yday = d[d.length - 2] || { up: 0, down: 0, active: 0 };
  const last7 = d.slice(-7), last30 = d.slice(-30);
  const volNow = usage.reduce((a, b) => a + b, 0), volPrev = (s.prev.up || 0) + (s.prev.down || 0);

  // Headline cards
  const live = state.live && state.live.counts;
  $('hOnline').innerHTML = (live ? fa(live.hour) : '–') + ' <small>/ ' + fa(s.active.month) + '</small>';
  setCtx('hOnlineCtx', 'گزارش در ۱ ساعت اخیر از کاربران ماه');
  const hourNow = Math.floor(Date.now() / 3600000), byHour = Object.fromEntries((s.hourly || []).map((h) => [h.at / 3600, h.n]));
  spark('sOnline', Array.from({ length: 24 }, (_, i) => byHour[hourNow - 23 + i] || 0));
  $('hActive').textContent = fa(s.period.users);
  $('hActiveRange').textContent = range;
  const du = delta(s.period.users, s.prev.users);
  setCtx('hActiveCtx', du.text + ' نسبت به بازهٔ قبل · ' + fa(s.period.newUsers) + ' جدید', du.cls);
  spark('sActive', users);
  $('hUsage').textContent = vol(volNow);
  $('hUsageRange').textContent = range;
  const dv = delta(volNow, volPrev);
  setCtx('hUsageCtx', volNow ? dv.text + ' نسبت به بازهٔ قبل' : 'از نسخهٔ ۰٫۲٫۸ ثبت می‌شود', volNow ? dv.cls : '');
  spark('sUsage', usage);
  // Busiest day: by usage once there is any, else by users.
  const byUsage = volNow > 0, key = (x) => (byUsage ? x.up + x.down : x.active);
  const peak = d.reduce((m, x) => (key(x) > key(m) ? x : m), d[0]);
  $('hPeak').textContent = byUsage ? vol(key(peak)) : fa(key(peak)) + ' کاربر';
  $('hPeakRange').textContent = range + (byUsage ? ' · بر پایهٔ حجم' : ' · بر پایهٔ کاربر');
  setCtx('hPeakCtx', peak.label);
  spark('sPeak', d.map(key), 'bar');

  // Usage trend and comparison
  $('usageRangeLbl').textContent = range;
  put('cUsageTrend', { type: 'bar', data: { labels, datasets: [
    { type: 'line', label: 'میانگین ۷ روزه', data: movingAvg(usage), borderColor: P[3], borderDash: [5, 4], borderWidth: 2, pointRadius: 0, tension: .3, fill: false },
    { label: 'مصرف روزانه', data: usage, backgroundColor: P[1], borderRadius: 4, maxBarThickness: 18 },
  ] }, options: { interaction: { mode: 'index', intersect: false }, plugins: { legend: { position: 'top', align: 'end' }, tooltip: volTip },
    scales: { y: { beginAtZero: true, ticks: { callback: vol } }, x: Object.assign({ grid: { display: false } }, skip) } } });
  $('usageFoot').innerHTML = footItem(I.db, 'کل حجم', vol(volNow)) + footItem(I.cal, 'حجم ۳۰ روز', vol(last30.reduce((a, x) => a + x.up + x.down, 0)))
    + footItem(I.cal, 'حجم ۷ روز', vol(last7.reduce((a, x) => a + x.up + x.down, 0))) + footItem(I.bolt, 'پرمصرف‌ترین روز', volNow ? vol(peak.up + peak.down) + ' · ' + peak.label : '–');
  const uMonth = avg(last30.map((x) => x.up + x.down)), uWeek = avg(last7.map((x) => x.up + x.down));
  const uToday = today.up + today.down, uYday = yday.up + yday.down;
  put('cUsageCmp', { type: 'bar', data: { labels: ['میانگین ماه', 'میانگین هفته', 'دیروز', 'امروز'], datasets: [{ label: 'حجم', data: [uMonth, uWeek, uYday, uToday], backgroundColor: [P[1] + 'cc', P[1] + 'cc', P[1], P[0]], borderRadius: 6, maxBarThickness: 46 }] },
    options: { plugins: { legend: { display: false }, tooltip: volTip }, scales: { y: { beginAtZero: true, ticks: { callback: vol } }, x: { grid: { display: false } } } } });
  const dl = (label, x) => '<div><span>' + label + '</span><span class="v ' + x.cls + '">' + x.text + '</span></div>';
  $('usageDeltas').innerHTML = dl('امروز نسبت به دیروز', delta(uToday, uYday)) + dl('دیروز نسبت به میانگین هفته', delta(uYday, uWeek)) + dl('این بازه نسبت به بازهٔ قبل', delta(volNow, volPrev));

  // Users trend and comparison
  $('usersRangeLbl').textContent = range;
  put('cUsersTrend', { type: 'line', data: { labels, datasets: [
    { label: 'میانگین ۷ روزه', data: movingAvg(users), borderColor: P[3], borderDash: [5, 4], borderWidth: 2, pointRadius: 0, tension: .3, fill: false },
    { label: 'کاربران فعال در روز', data: users, borderColor: P[0], backgroundColor: P[0] + '22', fill: true, tension: .35, pointRadius: 0, borderWidth: 2.5 },
  ] }, options: { interaction: { mode: 'index', intersect: false }, plugins: { legend: { position: 'top', align: 'end' } }, scales: { y: { beginAtZero: true, ticks: { precision: 0, callback: fa } }, x: Object.assign({ grid: { display: false } }, skip) } } });
  $('usersFoot').innerHTML = footItem(I.user, 'آنلاین / ماه', (live ? fa(live.hour) : '–') + ' / ' + fa(s.active.month)) + footItem(I.ok, 'امروز', fa(s.active.day))
    + footItem(I.cal, 'دیروز', fa(yday.active)) + footItem(I.cal, 'هفته', fa(s.active.week)) + footItem(I.cal, 'ماه', fa(s.active.month));
  // Average daily users, counting only days when someone came.
  const seen = (rows) => avg(rows.map((x) => x.active).filter((n) => n > 0));
  put('cUsersCmp', { type: 'bar', data: { labels: ['ماه', 'هفته', 'دیروز', 'امروز'], datasets: [{ label: 'کاربر', data: [seen(last30), seen(last7), yday.active, today.active].map((n) => Math.round(n)), backgroundColor: P[2], borderRadius: 6, maxBarThickness: 46 }] },
    options: { plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true, ticks: { precision: 0, callback: fa } }, x: { grid: { display: false } } } } });
  const on = live ? live.hour : 0, of = Math.max(1, s.active.month);
  $('gaugeTxt').textContent = fa(on) + ' / ' + fa(s.active.month);
  put('cGauge', { type: 'doughnut', data: { labels: ['آنلاین', 'بقیه'], datasets: [{ data: [on, Math.max(0, of - on)], backgroundColor: [P[2], css('--line') || '#e5e7eb'], borderWidth: 0 }] },
    options: { cutout: '80%', rotation: 0, plugins: { legend: { display: false }, tooltip: { enabled: false } }, animation: false } });
  if (!on) { const el = $('cGauge'); el.style.display = ''; el.parentElement.querySelector('.empty')?.remove(); }
  $('usersDeltas').innerHTML = dl('امروز نسبت به دیروز', delta(today.active, yday.active)) + dl('کاربران یکتا ۷ روز', { cls: 'flat', text: fa(s.active.week) }) + dl('کاربران یکتا ۳۰ روز', { cls: 'flat', text: fa(s.active.month) });
  renderHealth();
}

/** The service at a glance: connections, reports arriving, and the database's daily reads. */
let healthInfo = null;
async function loadHealth() { try { healthInfo = await getJson('/health'); renderHealth(); } catch (e) {} }
function renderHealth() {
  const s = state.stats; if (!s) return;
  const conn = s.conn || { byHop: [], deadByHop: [] };
  const ok = conn.byHop.reduce((a, r) => a + r.ok, 0), fail = conn.byHop.reduce((a, r) => a + r.fail, 0);
  const rate = ok + fail ? pct(ok, ok + fail) : null;
  const dead = conn.deadByHop.reduce((a, r) => a + r.dead, 0), conned = conn.deadByHop.reduce((a, r) => a + r.n, 0);
  const live = state.live;
  const last = live && live.feed[0] ? Math.round((Date.now() / 1000 - live.feed[0].at) / 60) : null;
  const box = (cls, label, value, note) => '<div class="h ' + cls + '"><small>' + label + '</small><b>' + value + '</b><span>' + note + '</span></div>';
  const reads = healthInfo ? pct(healthInfo.readsToday, healthInfo.readsBudget) : null;
  $('healthRow').innerHTML =
    box(rate == null ? '' : rate >= 80 ? 'good' : rate >= 50 ? 'warn' : 'bad', 'موفقیت اتصال', rate == null ? '–' : fa(rate) + '٪', rate == null ? 'از نسخهٔ ۰٫۲٫۸' : fa(ok) + ' موفق · ' + fa(fail) + ' ناموفق')
    + box(!conned ? '' : pct(dead, conned) <= 10 ? 'good' : pct(dead, conned) <= 30 ? 'warn' : 'bad', 'وصل ولی بی‌ترافیک', conned ? fa(pct(dead, conned)) + '٪' : '–', conned ? fa(dead) + ' از ' + fa(conned) + ' کاربر' : 'از نسخهٔ ۰٫۲٫۸')
    + box(last == null ? '' : last <= 30 ? 'good' : last <= 180 ? 'warn' : 'bad', 'آخرین گزارش', last == null ? '–' : last < 1 ? 'همین الان' : fa(last) + ' دقیقه پیش', live ? fa(live.counts.day) + ' گزارش در ۲۴ ساعت' : '')
    + box(reads == null ? '' : reads < 60 ? 'good' : reads < 90 ? 'warn' : 'bad', 'سهمیهٔ خواندن امروز', reads == null ? '–' : fa(reads) + '٪', healthInfo ? fa(healthInfo.readsToday) + ' از ' + fa(healthInfo.readsBudget) + ' ردیف' : '')
    + box(healthInfo && healthInfo.ok === false ? 'bad' : healthInfo ? 'good' : '', 'پایگاه‌داده', healthInfo ? (healthInfo.ok ? 'سالم' : 'خطا') : '–', healthInfo ? fa(healthInfo.installs) + ' نصب · ' + fa(healthInfo.todayPings) + ' گزارش امروز' : '');
}

// ---------- Filter ----------

const FILTER_NAMES = { country: 'کشور', region: 'منطقه', isp: 'اپراتور', version: 'نسخه', server: 'سرور', hop: 'حلقه', brand: 'برند', android: 'اندروید' };
/** The values of a dimension, from the unfiltered numbers: [value, label]. */
function filterValues(dim) {
  const bd = state.base && state.base.breakdown;
  if (!bd) return [];
  const lists = {
    country: () => bd.byCountry.map((r) => [r.k, flag(r.k) + ' ' + cname(r.k) + ' (' + fa(r.n) + ')']),
    region: () => bd.byRegion.map((r) => [r.k, (REGION[r.k] || r.k) + ' (' + fa(r.n) + ')']),
    isp: () => (bd.byIsp || []).map((r) => [r.k.split('|')[0], ispLabel(r.k, true) + ' (' + fa(r.n) + ')']),
    version: () => bd.byVersion.map((r) => [r.k, 'v' + r.k + ' (' + fa(r.n) + ')']),
    server: () => (bd.byServer || []).map((r) => [r.k, (SERVER[r.k] || r.k) + ' (' + fa(r.n) + ')']),
    hop: () => bd.byHop.map((r) => [r.k, (HOP[r.k] || r.k) + ' (' + fa(r.n) + ')']),
    brand: () => bd.byBrand.map((r) => [r.k, r.k + ' (' + fa(r.n) + ')']),
    android: () => bd.byAndroid.map((r) => [r.k, 'اندروید ' + r.k + ' (' + fa(r.n) + ')']),
  };
  return lists[dim] ? lists[dim]() : [];
}
function fillFilterValues() {
  const dim = $('fDim'), val = $('fVal'), box = $('filter');
  dim.value = state.f && state.v ? state.f : (dim.value || '');
  const list = filterValues(dim.value);
  val.hidden = !dim.value || (state.f && state.v);
  val.innerHTML = '<option value="">انتخاب کنید…</option>' + list.map(([v, t]) => '<option value="' + esc(v) + '">' + esc(t) + '</option>').join('');
  box.classList.toggle('on', !!(state.f && state.v));
  if (state.f && state.v) {
    const label = (filterValues(state.f).find(([v]) => String(v) === String(state.v)) || [state.v, state.v])[1].replace(/ \([^)]*\)$/, '');
    $('fText').textContent = FILTER_NAMES[state.f] + ': ' + label;
    dim.hidden = true;
  } else dim.hidden = false;
}
$('fDim').addEventListener('change', () => { state.f = ''; state.v = ''; fillFilterValues(); });
$('fVal').addEventListener('change', () => {
  if (!$('fVal').value) return;
  state.f = $('fDim').value; state.v = $('fVal').value;
  writeHash(); fillFilterValues(); load();
});
$('fClear').addEventListener('click', () => { state.f = ''; state.v = ''; $('fDim').value = ''; writeHash(); fillFilterValues(); load(); });

// ---------- Users: cohorts and versions over time ----------

async function loadUsers(fresh) {
  try {
    const u = await getJson('/users' + (fresh ? '?fresh=1' : ''));
    state.users = u;
    $('uCohortAt').textContent = 'محاسبه: ' + hm(u.cachedAt || Date.parse(u.updated)) + ' · هر ۶ ساعت';
    const cells = {};
    for (const r of u.cohorts) (cells[r.c] = cells[r.c] || {})[r.w] = r.n;
    const WEEKS = 8;
    let html = '<table class="cohort"><tr><th>هفتهٔ نصب</th><th>کاربر</th>' + Array.from({ length: WEEKS }, (_, w) => '<th>' + (w ? 'هفتهٔ ' + fa(w) : 'هفتهٔ اول') + '</th>').join('') + '</tr>';
    for (let c = WEEKS - 1; c >= 0; c--) {
      const row = cells[c] || {}, size = row[0] || 0;
      const from = new Date(Date.now() - (c * 7 + 6) * 864e5), to = new Date(Date.now() - c * 7 * 864e5);
      const lbl = from.toLocaleDateString('fa-IR', { month: 'short', day: 'numeric' }) + ' – ' + to.toLocaleDateString('fa-IR', { month: 'short', day: 'numeric' });
      html += '<tr><td>' + lbl + '</td><td>' + fa(size) + '</td>';
      for (let w = 0; w < WEEKS; w++) {
        if (w > c || !size) { html += '<td></td>'; continue; }
        const p = pct(row[w] || 0, size);
        html += '<td class="h" style="background:color-mix(in srgb,var(--blue) ' + Math.max(4, p) + '%,transparent);color:' + (p > 55 ? '#fff' : 'var(--text)') + '" title="' + fa(row[w] || 0) + ' کاربر">' + fa(p) + '٪</td>';
      }
      html += '</tr>';
    }
    $('uCohort').innerHTML = html + '</table>';

    // Versions per day as each version's share.
    const byDay = {}, totals = {};
    for (const r of u.versions) { (byDay[r.day] = byDay[r.day] || {})[r.k] = r.n; totals[r.k] = (totals[r.k] || 0) + r.n; }
    const top = Object.entries(totals).sort((a, b) => b[1] - a[1]).slice(0, 6).map(([k]) => k);
    const days = []; for (let i = u.days - 1; i >= 0; i--) days.push(new Date(Date.now() - i * 864e5).toISOString().slice(0, 10));
    const share = (dd, k) => { const m = byDay[dd] || {}, all = Object.values(m).reduce((a, n) => a + n, 0); if (!all) return 0; return Math.round(((k ? m[k] || 0 : Object.entries(m).filter(([v]) => !top.includes(v)).reduce((a, [, n]) => a + n, 0)) / all) * 1000) / 10; };
    const P = palette();
    put('cVersions', { type: 'line', data: { labels: days.map((x) => new Date(x).toLocaleDateString('fa-IR', { month: 'short', day: 'numeric' })), datasets: [
      ...top.map((k, i) => ({ label: 'v' + k, data: days.map((x) => share(x, k)), borderColor: P[i % P.length], backgroundColor: P[i % P.length] + '66', fill: true, stack: 'v', tension: .3, pointRadius: 0, borderWidth: 1.5 })),
      { label: 'سایر', data: days.map((x) => share(x, null)), borderColor: '#94a3b8', backgroundColor: '#94a3b855', fill: true, stack: 'v', tension: .3, pointRadius: 0, borderWidth: 1 },
    ] }, options: { interaction: { mode: 'index', intersect: false }, plugins: { legend: { position: 'top', align: 'end' }, tooltip: { callbacks: { label: (c) => ' ' + c.dataset.label + ': ' + fa(c.parsed.y) + '٪' } } },
      scales: { y: { stacked: true, min: 0, max: 100, ticks: { callback: (v) => fa(v) + '٪' } }, x: { grid: { display: false }, ticks: { maxTicksLimit: 8 } } } } });
  } catch (e) {
    $('uCohort').innerHTML = '<div class="empty">' + esc(e.detail || 'بارگذاری نشد') + '</div>';
  }
}

// ---------- Live ----------

let lastFeedAt = 0;
async function loadLive() {
  try {
    const l = await getJson('/live');
    state.live = l;
    $('aHour').textContent = fa(l.counts.hour);
    $('lQuarter').textContent = fa(l.counts.quarter);
    $('lHour').textContent = fa(l.counts.hour);
    $('lDay').textContent = fa(l.counts.day);
    $('lAgo').textContent = 'به‌روزرسانی ' + hm(Date.now()) + ' · هر ۳۰ ثانیه';
    const feed = $('lFeed');
    feed.innerHTML = (l.feed.length ? l.feed.map((x) => {
      const fresh = x.at > lastFeedAt && lastFeedAt > 0;
      const net = x.isp && x.isp !== '?' ? '<span class="g">' + esc(ASN[x.asn] || x.isp).slice(0, 26) + '</span>' : (x.via === 'vpn' ? '<span class="g">از راه VPN</span>' : '');
      const traffic = x.up != null || x.down != null ? '<span class="w">' + vol((x.up || 0) + (x.down || 0)) + '</span>' : '';
      const fails = x.fails ? '<span class="x">✕' + fa(x.fails) + '</span>' : '';
      return '<div class="row' + (fresh ? ' new' : '') + '"><span class="t">' + hm(x.at * 1000) + '</span><span>' + flag(x.country) + '</span><span class="k">v' + esc(x.version) + '</span><span class="m">' + esc(HOP[x.hop] || x.hop) + '</span><span>' + esc(SERVER[x.server] || x.server) + '</span><span class="g">' + esc(x.brand) + '</span>' + net + (x.connects ? '<span>⚡' + fa(x.connects) + '</span>' : '') + fails + traffic + '</div>';
    }).join('') : '<div class="row g">در ۲۴ ساعت اخیر گزارشی نرسیده (زمان گزارش‌ها از همین نسخهٔ پنل ثبت می‌شود).</div>') + '<span class="cur"></span>';
    lastFeedAt = Math.max(lastFeedAt, ...l.feed.map((x) => x.at), 1);
    if (state.tab === 'live') renderLiveMap(l.spots);
    if (state.stats) { renderOverview(state.stats, lastRows); }
  } catch (e) { console.error('live failed', e); }
}
setInterval(() => { $('lClock').textContent = hm(Date.now()); }, 1000);
$('lClock').textContent = hm(Date.now());

// ---------- Downloads ----------

async function loadDownloads(fresh) {
  try {
    const dl = await getJson('/downloads' + (fresh ? '?fresh=1' : ''));
    state.downloads = dl;
    $('dTotal').textContent = fa(dl.total);
    const latest = dl.releases.find((r) => r.edition === 'main');
    $('dLatest').textContent = latest ? fa(latest.n) : '–';
    $('dLatestTag').textContent = latest ? latest.tag + ' · ' + new Date(latest.at).toLocaleDateString('fa-IR') : '';
    $('dRepo').innerHTML = '<a href="https://github.com/' + esc(dl.repo) + '/releases" target="_blank" rel="noopener">' + esc(dl.repo) + '</a>' + (dl.stale ? ' · <span style="color:var(--amber)">گیت‌هاب جواب نداد؛ عدد قبلی</span>' : '');
    const hist = Object.entries(dl.history || {}).sort();
    const perDay = hist.slice(1).map(([k, v], i) => ({ k, n: Math.max(0, v - hist[i][1]) }));
    $('dToday').textContent = perDay.length ? fa(perDay[perDay.length - 1].n) : '–';
    const month = state.base ? state.base.active.month : null;
    $('dPerActive').textContent = month != null && dl.total ? fa(pct(month, dl.total)) + '٪' : '–';
    const recent = dl.releases.slice(0, 15).reverse();
    const P = palette();
    put('cDl', { type: 'bar', data: { labels: recent.map((r) => r.tag.replace('countries-', 'کشوری ')), datasets: [{ label: 'دانلود', data: recent.map((r) => r.n), backgroundColor: recent.map((r) => (r.edition === 'main' ? P[0] : P[4])), borderRadius: 6, maxBarThickness: 34 }] },
      options: { plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true, ticks: { precision: 0, callback: fa } }, x: { grid: { display: false } } } } });
    put('cDlDay', bars(perDay.map((x) => new Date(x.k).toLocaleDateString('fa-IR', { month: 'short', day: 'numeric' })), perDay.map((x) => x.n), P[2], 'دانلود'));
    $('tDl').innerHTML = '<table><tr><th>نسخه</th><th>تاریخ</th><th>بیشترین فایل</th><th>دانلود</th></tr>' + dl.releases.map((r) => '<tr><td>' + esc(r.tag) + (r.edition === 'countries' ? '<span class="pill">کشوری</span>' : '') + '</td><td>' + new Date(r.at).toLocaleDateString('fa-IR') + '</td><td class="sub" style="direction:ltr;text-align:right">' + esc((r.assets[0] || {}).name || '') + '</td><td class="n">' + fa(r.n) + '</td></tr>').join('') + '</table>';
  } catch (e) {
    $('tDl').innerHTML = '<div class="empty">' + esc(e.detail || 'آمار دانلود از گیت‌هاب خوانده نشد') + '</div>';
  }
}

// ---------- Announcement ----------

const LANGS = ['fa', 'en', 'ru', 'zh'];
let previewLang = 'fa', noticeSha = null;
function noticeForm() {
  const pick = (field) => Object.fromEntries(LANGS.map((l) => [l, $('n' + field + '_' + l).value.trim()]).filter(([, v]) => v));
  return { enabled: $('nEnabled').checked, notify: $('nNotify').checked, title: pick('Title'), message: pick('Message'), link: $('nLink').value.trim(), renew: $('nRenew').checked, sha: noticeSha };
}
function preview() {
  const n = noticeForm();
  const t = n.title[previewLang] || n.title.fa || n.title.en || '', m = n.message[previewLang] || n.message.fa || n.message.en || '';
  const rtl = previewLang === 'fa';
  $('nPreview').innerHTML = n.enabled && (t || m)
    ? '<div class="notice" dir="' + (rtl ? 'rtl' : 'ltr') + '"><b>' + esc(t) + '</b><p>' + esc(m) + '</p>' + (n.link ? '<span>' + (rtl ? 'باز کردن' : 'Open') + ' ↗</span>' : '') + '</div>'
    : '<div class="off">اعلان خاموش است؛<br>کاربران چیزی نمی‌بینند.</div>';
  $('nState').textContent = n.enabled ? (n.notify ? 'روشن · با نوتیفیکیشن' : 'روشن') : 'خاموش';
  if (n.enabled && n.notify && (t || m)) {
    $('nPreview').insertAdjacentHTML('afterbegin', '<div class="notice" style="margin-top:6px;background:#1e293b;padding:10px 12px;border-radius:14px" dir="' + (rtl ? 'rtl' : 'ltr') + '"><b style="font-size:13px">🔔 ' + esc(t || 'Dr VPN') + '</b><p style="font-size:12px;opacity:.85">' + esc(m).slice(0, 140) + '</p></div>');
  }
}
async function loadNotice() {
  try {
    const r = await getJson('/notice');
    state.notice = r;
    noticeSha = r.sha;
    const n = r.notice || {};
    $('nEnabled').checked = !!n.enabled;
    $('nNotify').checked = !!n.notify;
    for (const l of LANGS) { $('nTitle_' + l).value = (n.title || {})[l] || ''; $('nMessage_' + l).value = (n.message || {})[l] || ''; }
    $('nLink').value = n.link || '';
    $('nNoToken').hidden = !!r.canEdit;
    $('nSave').disabled = !r.canEdit;
    preview();
  } catch (e) { show($('nMsg'), false, 'اعلان خوانده نشد: ' + (e.detail || e.message)); }
}
document.querySelector('[data-tab="notice"]').addEventListener('input', preview);
$('nLang').addEventListener('click', (e) => { const b = e.target.closest('button'); if (!b) return; previewLang = b.dataset.l; $('nLang').querySelectorAll('button').forEach((x) => x.classList.toggle('on', x === b)); preview(); });
$('nReload').addEventListener('click', loadNotice);
$('nSave').addEventListener('click', async () => {
  const btn = $('nSave'), msg = $('nMsg'), n = noticeForm();
  if (n.enabled && !n.title.fa && !n.message.fa && !n.title.en && !n.message.en) return show(msg, false, 'برای اعلان روشن، دست‌کم عنوان یا متن فارسی یا انگلیسی لازم است.');
  if (!confirm(n.enabled ? (n.notify ? 'اعلان منتشر و برای همهٔ کاربران نوتیفیکیشن فرستاده شود؟' : 'اعلان برای همهٔ کاربران منتشر شود؟') : 'اعلان خاموش شود؟')) return;
  btn.disabled = true;
  try {
    const r = await getJson('/notice', { method: 'POST', headers: { 'content-type': 'application/json' }, body: JSON.stringify(n) });
    noticeSha = r.sha;
    msg.className = 'msg ok';
    msg.innerHTML = 'ذخیره شد' + (r.commit ? ' · <a href="' + esc(r.commit) + '" target="_blank" rel="noopener">تغییر در گیت‌هاب</a>' : '') + '. برنامه‌ها حداکثر چند دقیقهٔ دیگر آن را می‌بینند.';
  } catch (e) { show(msg, false, e.detail || 'ذخیره نشد'); } finally { btn.disabled = false; }
});

// ---------- Tabs, controls, shortcuts ----------

const loaded = {};
function openTab(tab) {
  state.tab = tab;
  document.querySelectorAll('#tabs button').forEach((b) => b.classList.toggle('on', b.dataset.tab === tab));
  document.querySelectorAll('.tab').forEach((t) => t.classList.toggle('on', t.dataset.tab === tab));
  writeHash();
  if (tab === 'geo' && state.stats) renderMap(state.stats.points);
  if (tab === 'live') { loadLive(); }
  if (tab === 'users' && !loaded.users) { loaded.users = true; loadUsers(); }
  if (tab === 'downloads' && !loaded.downloads) { loaded.downloads = true; loadDownloads(); }
  if (tab === 'notice' && !loaded.notice) { loaded.notice = true; loadNotice(); }
}
$('tabs').addEventListener('click', (e) => { const b = e.target.closest('button'); if (b) { openTab(b.dataset.tab); document.body.classList.remove('nav-open'); scrollTo({ top: 0 }); } });
document.querySelectorAll('[data-go]').forEach((b) => b.addEventListener('click', () => openTab(b.dataset.go)));
$('menu').addEventListener('click', () => document.body.classList.toggle('nav-open'));
$('shade').addEventListener('click', () => document.body.classList.remove('nav-open'));
let paused = false;
$('pause').addEventListener('click', () => {
  paused = !paused;
  $('pauseIcon').textContent = paused ? '▶' : '❚❚';
  $('pauseText').textContent = paused ? 'ادامه' : 'توقف';
  $('liveDot').style.animationPlayState = paused ? 'paused' : '';
  $('liveDot').style.background = paused ? 'var(--faint)' : '';
  $('liveText').textContent = paused ? 'متوقف' : 'زنده';
});
$('seg').querySelectorAll('button').forEach((b) => b.classList.toggle('on', Number(b.dataset.d) === state.days));
$('seg').addEventListener('click', (e) => {
  const b = e.target.closest('button'); if (!b) return;
  $('seg').querySelectorAll('button').forEach((x) => x.classList.toggle('on', x === b));
  state.days = Number(b.dataset.d); state.base = null; writeHash(); load();
});
function refresh() {
  load(true);
  if (state.tab === 'users') loadUsers(true);
  if (state.tab === 'live') loadLive();
  if (state.tab === 'downloads') loadDownloads(true);
  if (state.tab === 'notice') loadNotice();
}
$('refresh').addEventListener('click', refresh);
themePicker(() => {
  Object.values(charts).forEach((c) => c.destroy());
  for (const k in charts) delete charts[k];
  chartTheme();
  if (state.stats) render(state.stats);
  if (state.users) loadUsers();
  if (state.downloads) loadDownloads();
  if (state.tab === 'live' && state.live) renderLiveMap(state.live.spots);
});
$('full').addEventListener('click', () => (document.fullscreenElement ? document.exitFullscreen() : document.documentElement.requestFullscreen()).catch(() => {}));
$('csv').addEventListener('click', (e) => {
  e.preventDefault();
  const out = [['date', 'active_users', 'new_users', 'connects', 'failed_connects', 'download_bytes', 'upload_bytes'], ...lastRows.map((x) => [x.day, x.active, x.newUsers, x.connects, x.fails, x.down, x.up])];
  const url = URL.createObjectURL(new Blob([out.map((r) => r.join(',')).join('\n')], { type: 'text/csv' }));
  const a = document.createElement('a'); a.href = url; a.download = 'drvpn-stats-' + state.days + 'd' + (state.f ? '-' + state.f + '-' + state.v : '') + '.csv'; a.click(); URL.revokeObjectURL(url);
});
document.addEventListener('keydown', (e) => {
  if (e.ctrlKey || e.metaKey || e.altKey || /INPUT|TEXTAREA|SELECT/.test(document.activeElement.tagName)) return;
  const tabs = [...document.querySelectorAll('#tabs button')];
  const n = parseInt(e.key);
  if (n >= 1 && n <= tabs.length) openTab(tabs[n - 1].dataset.tab);
  else if (e.key === 'r' || e.key === 'R') refresh();
  else if (e.key === 'f' || e.key === 'F') $('full').click();
});

// Every 5 minutes (the live feed every 30 s), and only while the page is in view: each load reads
// the database, which has a free daily limit.
openTab(state.tab);
load();
if (state.tab !== 'live') loadLive();
loadHealth();
setInterval(() => { if (document.visibilityState === 'visible' && !paused) { load(); loadHealth(); } }, 5 * 60000);
setInterval(() => { if (document.visibilityState === 'visible' && !paused && (state.tab === 'live' || state.tab === 'overview')) loadLive(); }, state.tab === 'live' ? 30000 : 30000);
document.addEventListener('visibilitychange', () => {
  if (document.visibilityState === 'visible' && !paused && Date.now() - lastLoad > 5 * 60000) load();
});

// ---------- Settings page ----------

function settingsPage() {
  themePicker();
  $('badgeUrl').textContent = location.origin + '/badge';
  const post = async (path, fields) => {
    const f = new FormData();
    for (const [k, v] of Object.entries(fields || {})) f.set(k, v);
    const r = await fetch(path, { method: 'POST', body: f, credentials: 'same-origin' });
    let j = {}; try { j = await r.json(); } catch (e) {}
    return j;
  };
  const button = (id, msgId, run) => $(id).addEventListener('click', async (e) => {
    const btn = e.currentTarget, msg = $(msgId);
    btn.disabled = true;
    try { await run(msg); } catch (err) { show(msg, false, 'ارتباط با سرور برقرار نشد. دوباره تلاش کنید.'); console.error(err); } finally { btn.disabled = false; }
  });

  const size = (b) => (b == null ? '–' : b >= 1048576 ? fa((b / 1048576).toFixed(1)) + ' مگابایت' : fa(Math.round(b / 1024)) + ' کیلوبایت');
  async function loadHealth() {
    const t = $('health');
    try {
      const h = await getJson('/health');
      const lc = h.lastCompute;
      const row = (k, v) => '<tr><td style="color:var(--muted)">' + k + '</td><td class="n">' + v + '</td></tr>';
      t.innerHTML = (h.ok ? row('وضعیت', '<span class="ok">سالم</span>') : row('وضعیت', '<span class="bad">' + esc(h.error || 'خطا') + '</span>'))
        + row('کل نصب‌ها', fa(h.installs))
        + row('گزارش‌های امروز', fa(h.todayPings))
        + row('قدیمی‌ترین روز ذخیره‌شده', h.oldestDay || '–')
        + row('حجم پایگاه‌داده', size(h.dbBytes))
        + row('آخرین محاسبه', lc ? new Date(lc.at).toLocaleString('fa-IR') + ' · ' + fa(lc.rowsRead) + ' ردیف' : '–')
        + row('خواندن امروزِ پنل', fa(h.readsToday) + ' از ' + fa(h.readsBudget) + ' ردیف (' + fa(pct(h.readsToday, h.readsBudget)) + '٪)')
        + row('تازه‌سازی آمار', 'هر ' + fa(h.statsTtlMinutes) + ' دقیقه');
      $('adays').value = h.keepDays || 0;
      $('tgState').innerHTML = h.telegram.set ? '<span class="ok">● وصل</span>' : '<span class="bad">● تنظیم نشده</span>';
      $('tgChat').value = h.telegram.chat || '';
      $('tgDaily').checked = h.telegram.daily;
      $('tgAlerts').checked = h.telegram.alerts;
      if (h.telegram.set) $('tgToken').placeholder = 'ذخیره شده · برای عوض کردن، توکن تازه را بنویسید';
      $('ghState').innerHTML = h.github.set ? '<span class="ok">● تنظیم شده' + (h.github.fromSecret ? ' (secret)' : '') + '</span>' : '<span class="bad">● تنظیم نشده</span>';
      if (h.github.set) $('ghToken').placeholder = 'ذخیره شده · برای عوض کردن، توکن تازه را بنویسید';
      $('lockNote').textContent = 'ورودها و تلاش‌های ناموفق اخیر. بعد از ' + fa(h.lock.tries) + ' رمز اشتباه از یک نشانی، ورود از آن‌جا ' + fa(h.lock.minutes) + ' دقیقه بسته می‌شود.';
      $('logins').innerHTML = h.logins.length
        ? '<tr><th>زمان</th><th>نتیجه</th><th>از کجا</th><th>دستگاه</th></tr>' + h.logins.map((l) => '<tr><td>' + new Date(l.at).toLocaleString('fa-IR') + '</td><td>' + (l.ok ? '<span class="ok">ورود</span>' : '<span class="bad">رمز اشتباه</span>') + '</td><td>' + flag(l.cc) + ' ' + esc(l.city || cname(l.cc)) + '</td><td class="sub" style="direction:ltr;text-align:right">' + esc(l.dev) + '</td></tr>').join('')
        : '<tr><td style="color:var(--faint)">هنوز ورودی ثبت نشده.</td></tr>';
    } catch (err) {
      t.innerHTML = '<tr><td class="bad">سلامت پایگاه‌داده خوانده نشد.</td></tr>';
    }
  }
  loadHealth();

  button('tgSave', 'tgMsg', async (msg) => {
    const j = await post('/settings/telegram', { token: $('tgToken').value, chat: $('tgChat').value, daily: $('tgDaily').checked ? '1' : '0', alerts: $('tgAlerts').checked ? '1' : '0' });
    show(msg, !!j.ok, j.ok ? 'ذخیره شد. با «ارسال پیام آزمایشی» امتحانش کنید.' : j.error || 'خطا');
    if (j.ok) { $('tgToken').value = ''; loadHealth(); }
  });
  button('tgTest', 'tgMsg', async (msg) => {
    const j = await post('/settings/telegram/test');
    show(msg, !!j.ok, j.ok ? 'پیام فرستاده شد؛ تلگرام را ببینید.' : 'نرسید: ' + (j.error || 'خطا') + ' (به ربات یک پیام داده‌اید؟ اگر کانال است، ربات مدیر آن هست؟)');
  });
  button('tgClear', 'tgMsg', async (msg) => {
    if (!confirm('تنظیمات تلگرام حذف شود؟')) return;
    const j = await post('/settings/telegram', { clear: '1' });
    show(msg, !!j.ok, j.ok ? 'حذف شد.' : j.error || 'خطا'); loadHealth();
  });
  button('ghSave', 'ghMsg', async (msg) => {
    const j = await post('/settings/github', { token: $('ghToken').value.trim() });
    show(msg, !!j.ok, j.ok ? 'توکن کار می‌کند؛ حالا اعلان را می‌توانید از زبانهٔ «اعلان» عوض کنید.' : j.error || 'خطا');
    if (j.ok) { $('ghToken').value = ''; loadHealth(); }
  });
  button('ghClear', 'ghMsg', async (msg) => {
    if (!confirm('توکن گیت‌هاب حذف شود؟')) return;
    const j = await post('/settings/github', { clear: '1' });
    show(msg, !!j.ok, j.ok ? 'حذف شد.' : j.error || 'خطا'); loadHealth();
  });
  button('loginsClear', 'pwMsg', async () => { await post('/settings/logins/clear'); loadHealth(); });
  button('pwBtn', 'pwMsg', async (msg) => {
    const cur = $('cur').value, n1 = $('n1').value, n2 = $('n2').value;
    if (n1 !== n2) return show(msg, false, 'رمز جدید و تکرارش یکی نیستند');
    if (n1.length < 8) return show(msg, false, 'رمز جدید باید حداقل ۸ نویسه باشد');
    const j = await post('/settings/password', { current: cur, next: n1 });
    if (j.ok) { show(msg, true, 'رمز با موفقیت عوض شد.'); $('cur').value = $('n1').value = $('n2').value = ''; } else show(msg, false, j.error || 'خطا در تغییر رمز');
  });
  button('autoBtn', 'autoMsg', async (msg) => {
    const j = await post('/settings/autoprune', { days: $('adays').value });
    if (j.ok) { $('adays').value = j.keepDays; show(msg, true, j.keepDays ? 'هر شب دادهٔ قدیمی‌تر از ' + fa(j.keepDays) + ' روز پاک می‌شود.' : 'حذف خودکار خاموش شد.'); } else show(msg, false, j.error || 'خطا');
  });
  button('pruneBtn', 'dataMsg', async (msg) => {
    const days = $('pdays').value;
    if (!confirm('دادهٔ قدیمی‌تر از ' + days + ' روز حذف شود؟')) return;
    const j = await post('/settings/prune', { days });
    show(msg, !!j.ok, j.ok ? fa(j.deleted) + ' ردیف قدیمی حذف شد.' : j.error || 'خطا');
  });
  button('resetBtn', 'dataMsg', async (msg) => {
    if (!confirm('همهٔ آمار برای همیشه پاک شود؟ این کار برگشت‌ناپذیر است.')) return;
    if (!confirm('مطمئنید؟ تمام داده‌ها حذف می‌شود.')) return;
    const j = await post('/settings/reset');
    show(msg, !!j.ok, j.ok ? fa(j.deleted) + ' ردیف حذف شد. آمار از صفر شروع می‌شود.' : j.error || 'خطا');
  });
  if (location.hash) setTimeout(() => document.querySelector(location.hash)?.scrollIntoView({ behavior: 'smooth' }), 200);
}
})();
