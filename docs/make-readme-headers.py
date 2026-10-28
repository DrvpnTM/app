#!/usr/bin/env python3
"""Writes the picture header (3D icons, big buttons, 4-step guide) at the top of every README.

Run from the repository root:  python3 docs/make-readme-headers.py
The text under the header (everything after the "<!-- end of header -->" marker, or, the first
time, after the README's "# " title) is kept as it is; only the header and the download section
are rewritten.
"""
import re

DOWNLOAD = "https://github.com/DrvpnTM/app/releases/latest/download/DrVPN.apk"
SITE = "https://drvpn.net/"
TELEGRAM = "https://t.me/drVPN_net"
LANG_BAR = "🌐 [فارسی](README.md) | [English](README-en.md) | [中文](README-zh.md) | [Русский](README-ru.md) | [မြန်မာ](README-my.md) | [Español](README-es.md)"
END = "<!-- end of header -->"

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
        download_section="Toca el botón verde **DOWNLOAD** arriba de esta página, o abre [Releases](https://github.com/DrvpnTM/app/releases/latest) y descarga `DrVPN.apk`.",
    ),
}

STEP_ICONS = ["install", "country", "tap", "connected"]
FEAT_ICONS = ["free", "fast", "shield", "tv", "website"]


def cell(icon, label, width=84, number=None, share="25%"):
    num = f'<img src="docs/icons/step{number}.png" width="34" alt="{number}"><br>' if number else ""
    return f'<td align="center" width="{share}">{num}<img src="docs/icons/{icon}.png" width="{width}" alt=""><br><b>{label}</b></td>'


def header(t):
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

<b>{t["hint"]}</b>

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

{END}
"""


def body_of(text):
    if END in text:
        return text.split(END, 1)[1]
    # First run: drop the old language bar and title, keep the rest.
    m = re.search(r"^# .*$", text, re.M)
    return "\n" + text[m.end():] if m else text


def with_new_download(body, sentence):
    # The download section: from its "## ...📥" heading to the next "## " heading.
    return re.sub(r"(^## [^\n]*📥[^\n]*\n)(.*?)(?=^## )", lambda m: m.group(1) + "\n" + sentence + "\n\n", body, flags=re.S | re.M)


for name, t in T.items():
    text = open(name, encoding="utf-8").read()
    body = with_new_download(body_of(text), t["download_section"])
    open(name, "w", encoding="utf-8").write(header(t) + body)
    print("wrote", name)
