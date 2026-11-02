#!/usr/bin/env python3
"""Writes the picture header (3D icons, big buttons, 4-step guide) at the top of every README.

Run from the repository root:  python3 docs/make-readme-headers.py
The text under the header (everything after the "<!-- end of header -->" marker, or, the first
time, after the README's "# " title) is kept as it is; only the header and the download section
are rewritten.

The desktop (Windows / macOS / Linux) buttons link to the newest DrvpnTM/satelite-proxy release
saved in docs/desktop.json. The "Download count" workflow refreshes that file every hour and runs
this script again, so the links follow new desktop versions on their own.
"""
import json
import re
from urllib.parse import quote

DOWNLOAD = "https://github.com/DrvpnTM/app/releases/latest/download/DrVPN.apk"
SITE = "https://drvpn.net/"
TELEGRAM = "https://t.me/drVPN_net"
LANG_BAR = "🌐 [فارسی](README.md) | [English](README-en.md) | [中文](README-zh.md) | [Русский](README-ru.md) | [မြန်မာ](README-my.md) | [Español](README-es.md)"
END = "<!-- end of header -->"
DESKTOP_REPO = "DrvpnTM/satelite-proxy"
BADGES = """<a href="https://github.com/DrvpnTM/app/releases"><img src="https://img.shields.io/endpoint?url=https%3A%2F%2Fraw.githubusercontent.com%2FDrvpnTM%2Fapp%2Fmain%2Fdocs%2Fdownloads.json&style=for-the-badge" alt="downloads"></a>
<a href="https://github.com/DrvpnTM/app/releases/latest"><img src="https://img.shields.io/endpoint?url=https%3A%2F%2Fdrvpn-stats.cdn-1-4cf.workers.dev%2Fbadge&style=for-the-badge" alt="active installs"></a>
<a href="https://github.com/DrvpnTM/app/releases/latest"><img src="https://img.shields.io/badge/VirusTotal-scanned-394EFF?style=for-the-badge&logo=virustotal&logoColor=white" alt="VirusTotal"></a>"""

T = {
    "README.md": dict(
        rtl=True, name="دکتر وی پی ان",
        tagline="فیلترشکن سریع و امن برای اندروید",
        free="🎁 همراه با سرور رایگان",
        hint="👆 برای دانلود، روی دکمه سبز بزنید",
        steps_title="📱 خیلی ساده، در ۴ قدم",
        steps=["نصب کنید", "کشورتان را انتخاب کنید", "دکمه بزرگ را بزنید", "سبز شد یعنی وصل هستید"],
        feat_title="✨ چرا دکتر وی پی ان؟",
        feats=["سرور رایگان", "پرسرعت", "امن", "تلویزیون اندروید", "۱۱ زبان"],
        buy_title="🚀 اشتراک پرسرعت می‌خواهید؟",
        desktop_title="💻 نسخه کامپیوتر", installer="نصب‌کننده", portable="قابل‌حمل", apple="Apple silicon (M1 به بالا)", intel="Intel", version="نسخه", all_versions="همه نسخه‌ها",
        download_section="روی دکمه سبز **DOWNLOAD** بالای همین صفحه بزنید، یا از بخش [Releases](https://github.com/DrvpnTM/app/releases/latest) فایل `DrVPN.apk` را دانلود کنید.",
    ),
    "README-en.md": dict(
        rtl=False, name="Dr VPN",
        tagline="Fast, safe VPN for Android",
        free="🎁 Free server built in",
        hint="👆 Tap the green button to download",
        steps_title="📱 Easy: 4 steps",
        steps=["Install", "Pick your country", "Tap the big button", "Green = connected"],
        feat_title="✨ Why Dr VPN?",
        feats=["Free server", "Fast", "Safe", "Android TV", "11 languages"],
        buy_title="🚀 Want a high-speed subscription?",
        desktop_title="💻 Desktop version", installer="Installer", portable="Portable", apple="Apple silicon (M1+)", intel="Intel", version="Version", all_versions="All versions",
        download_section="Tap the green **DOWNLOAD** button at the top of this page, or open [Releases](https://github.com/DrvpnTM/app/releases/latest) and download `DrVPN.apk`.",
    ),
    "README-zh.md": dict(
        rtl=False, name="Dr VPN",
        tagline="安卓快速、安全的 VPN",
        free="🎁 内置免费服务器",
        hint="👆 点击绿色按钮下载",
        steps_title="📱 简单 4 步",
        steps=["安装", "选择国家", "点击大按钮", "绿色 = 已连接"],
        feat_title="✨ 为什么选择 Dr VPN？",
        feats=["免费服务器", "高速", "安全", "安卓电视", "11 种语言"],
        buy_title="🚀 想要高速订阅？",
        desktop_title="💻 电脑版", installer="安装版", portable="便携版", apple="Apple 芯片 (M1+)", intel="Intel", version="版本", all_versions="所有版本",
        download_section="点击本页顶部的绿色 **DOWNLOAD** 按钮，或打开 [Releases](https://github.com/DrvpnTM/app/releases/latest) 下载 `DrVPN.apk`。",
    ),
    "README-ru.md": dict(
        rtl=False, name="Dr VPN",
        tagline="Быстрый и безопасный VPN для Android",
        free="🎁 Бесплатный сервер внутри",
        hint="👆 Нажмите зелёную кнопку, чтобы скачать",
        steps_title="📱 Всего 4 шага",
        steps=["Установите", "Выберите страну", "Нажмите большую кнопку", "Зелёный = подключено"],
        feat_title="✨ Почему Dr VPN?",
        feats=["Бесплатный сервер", "Быстро", "Безопасно", "Android TV", "11 языков"],
        buy_title="🚀 Нужна скоростная подписка?",
        desktop_title="💻 Версия для компьютера", installer="Установщик", portable="Портативная", apple="Apple silicon (M1+)", intel="Intel", version="Версия", all_versions="Все версии",
        download_section="Нажмите зелёную кнопку **DOWNLOAD** вверху страницы или откройте [Releases](https://github.com/DrvpnTM/app/releases/latest) и скачайте `DrVPN.apk`.",
    ),
    "README-my.md": dict(
        rtl=False, name="Dr VPN",
        tagline="Android အတွက် မြန်ဆန်ပြီး လုံခြုံသော VPN",
        free="🎁 အခမဲ့ server ပါဝင်သည်",
        hint="👆 ဒေါင်းလုဒ်ရန် အစိမ်းရောင်ခလုတ်ကို နှိပ်ပါ",
        steps_title="📱 လွယ်ကူသော အဆင့် ၄ ဆင့်",
        steps=["ထည့်သွင်းပါ", "နိုင်ငံ ရွေးပါ", "ခလုတ်ကြီးကို နှိပ်ပါ", "အစိမ်း = ချိတ်ဆက်ပြီး"],
        feat_title="✨ Dr VPN ကို ဘာကြောင့် ရွေးသင့်သလဲ",
        feats=["အခမဲ့ server", "မြန်ဆန်", "လုံခြုံ", "Android TV", "ဘာသာစကား ၁၁ မျိုး"],
        buy_title="🚀 မြန်နှုန်းမြင့် subscription လိုချင်ပါသလား",
        desktop_title="💻 ကွန်ပျူတာ ဗားရှင်း", installer="Installer", portable="Portable", apple="Apple silicon (M1+)", intel="Intel", version="ဗားရှင်း", all_versions="ဗားရှင်းအားလုံး",
        download_section="ဤစာမျက်နှာ၏ ထိပ်ရှိ အစိမ်းရောင် **DOWNLOAD** ခလုတ်ကို နှိပ်ပါ၊ သို့မဟုတ် [Releases](https://github.com/DrvpnTM/app/releases/latest) ကိုဖွင့်ပြီး `DrVPN.apk` ကို ဒေါင်းလုဒ်လုပ်ပါ။",
    ),
    "README-es.md": dict(
        rtl=False, name="Dr VPN",
        tagline="VPN rápida y segura para Android",
        free="🎁 Servidor gratis incluido",
        hint="👆 Toca el botón verde para descargar",
        steps_title="📱 Fácil: 4 pasos",
        steps=["Instala", "Elige tu país", "Toca el botón grande", "Verde = conectado"],
        feat_title="✨ ¿Por qué Dr VPN?",
        feats=["Servidor gratis", "Rápida", "Segura", "Android TV", "11 idiomas"],
        buy_title="🚀 ¿Quieres una suscripción de alta velocidad?",
        desktop_title="💻 Versión para computadora", installer="Instalador", portable="Portátil", apple="Apple silicon (M1+)", intel="Intel", version="Versión", all_versions="Todas las versiones",
        download_section="Toca el botón verde **DOWNLOAD** arriba de esta página, o abre [Releases](https://github.com/DrvpnTM/app/releases/latest) y descarga `DrVPN.apk`.",
    ),
}

STEP_ICONS = ["install", "country", "tap", "connected"]
FEAT_ICONS = ["free", "fast", "shield", "tv", "website"]


def cell(icon, label, width=84, number=None, share="25%"):
    num = f'<img src="docs/icons/step{number}.png" width="34" alt="{number}"><br>' if number else ""
    return f'<td align="center" width="{share}">{num}<img src="docs/icons/{icon}.png" width="{width}" alt=""><br><b>{label}</b></td>'


def load_desktop():
    try:
        with open("docs/desktop.json", encoding="utf-8") as f:
            return json.load(f)
    except FileNotFoundError:
        return {"tag": "", "assets": []}


def desktop_section(t, desktop):
    tag = desktop.get("tag", "")
    assets = desktop.get("assets", [])
    page = f"https://github.com/{DESKTOP_REPO}/releases/latest"

    def link(suffix):
        name = next((a for a in assets if a.endswith(suffix)), None)
        if not name:
            return page
        return f"https://github.com/{DESKTOP_REPO}/releases/download/{quote(tag, safe='')}/{quote(name, safe='')}"

    platforms = [
        ("windows", "Windows", [(t["installer"], "_x64-setup.exe"), (t["portable"], "_x64_portable.zip")]),
        ("macos", "macOS", [(t["apple"], "_aarch64.dmg"), (t["intel"], "_x64.dmg")]),
        ("linux", "Linux", [("AppImage", ".AppImage")]),
    ]
    cells = []
    for icon, label, files in platforms:
        main = link(files[0][1])
        choices = " · ".join(f'<a href="{link(suffix)}">{text}</a>' for text, suffix in files)
        cells.append(
            f'<td align="center" width="33%"><a href="{main}"><img src="docs/icons/platforms/{icon}.svg" width="64" alt="{label}"></a>'
            f"<br><b>{label}</b><br>{choices}</td>"
        )
    shown = f'{t["version"]} {tag} · ' if tag else ""
    return f"""<h2>{t["desktop_title"]}</h2>

<table>
<tr>
{chr(10).join(cells)}
</tr>
</table>

<sub>{shown}<a href="https://github.com/{DESKTOP_REPO}/releases">{t["all_versions"]}</a></sub>"""


def header(t, desktop):
    d = ' dir="rtl"' if t["rtl"] else ""
    steps = "\n".join(cell(i, s, number=n + 1) for n, (i, s) in enumerate(zip(STEP_ICONS, t["steps"])))
    feats = "\n".join(cell(i, s, width=64, share="20%") for i, s in zip(FEAT_ICONS, t["feats"]))
    return f"""{LANG_BAR}

<div align="center"{d}>

<img src="android/app/src/main/res/drawable-nodpi/drvpn_logo.png" width="128" alt="Dr VPN">

<h1>{t["name"]}</h1>

<h3>{t["tagline"]}</h3>

<b>{t["free"]}</b>

<br><br>

<a href="{DOWNLOAD}"><img src="docs/icons/download-button.svg" width="340" alt="Download Dr VPN"></a>

{BADGES}

<b>{t["hint"]}</b>

<br><br>

{desktop_section(t, desktop)}

<br><br>

<h2>{t["steps_title"]}</h2>

<table>
<tr>
{steps}
</tr>
</table>

<h2>{t["feat_title"]}</h2>

<table>
<tr>
{feats}
</tr>
</table>

<h2>{t["buy_title"]}</h2>

<a href="{SITE}"><img src="docs/icons/buy-button.svg" width="300" alt="drvpn.net"></a>
&nbsp;
<a href="{TELEGRAM}"><img src="docs/icons/telegram-button.svg" width="300" alt="Telegram @drVPN_net"></a>

</div>

{END}"""


def body_of(text):
    if END in text:
        return text.split(END, 1)[1]
    # First run: drop the old language bar and title, keep the rest.
    m = re.search(r"^# .*$", text, re.M)
    return "\n" + text[m.end():] if m else text


def with_new_download(body, sentence):
    # The download section: from its "## ...📥" heading to the next "## " heading.
    return re.sub(r"(^## [^\n]*📥[^\n]*\n)(.*?)(?=^## )", lambda m: m.group(1) + "\n" + sentence + "\n\n", body, flags=re.S | re.M)


desktop = load_desktop()
for name, t in T.items():
    text = open(name, encoding="utf-8").read()
    body = with_new_download(body_of(text), t["download_section"])
    open(name, "w", encoding="utf-8").write(header(t, desktop) + body)
    print("wrote", name)
