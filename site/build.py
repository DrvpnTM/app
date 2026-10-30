#!/usr/bin/env python3
"""Builds the Dr VPN website (MkDocs Material) from the README, article and privacy pages.

Run from the repository root:  python3 site/build.py
Needs:  pip install mkdocs-material mkdocs-static-i18n
The result goes to _site/. The site address comes from SITE_URL (default: drvpntm.github.io).

Search Console: set GOOGLE_SITE_VERIFICATION to the code from the "HTML tag" method, or put
Google's verification file (google....html) in site/static/.
"""
import os
import re
import shutil
import subprocess

SITE_URL = os.environ.get("SITE_URL", "https://drvpntm.github.io/")
REPO = "DrvpnTM/app"
BLOB = f"https://github.com/{REPO}/blob/main/"
DOWNLOAD = f"https://github.com/{REPO}/releases/latest/download/DrVPN.apk"
WORK = "_mkdocs"
DOCS = f"{WORK}/docs"
END = "<!-- end of header -->"

# Words for the home page come from the README header script, so both stay the same.
_src = open("docs/make-readme-headers.py", encoding="utf-8").read().split("\nSTEP_ICONS")[0]
_ns = {}
exec(_src, _ns)
T = _ns["T"]

LANGS = [  # code, name, README, article
    ("fa", "فارسی", "README.md", "ARTICLE-fa.md"),
    ("en", "English", "README-en.md", "ARTICLE-en.md"),
    ("zh", "中文", "README-zh.md", "ARTICLE-zh.md"),
    ("ru", "Русский", "README-ru.md", "ARTICLE-ru.md"),
    ("my", "မြန်မာ", "README-my.md", "ARTICLE-my.md"),
    ("es", "Español", "README-es.md", "ARTICLE-es.md"),
]
WORDS = {  # home, article, privacy, download, buy, more
    "fa": ("خانه", "راهنمای خرید", "حریم خصوصی", "دانلود برای اندروید", "خرید اشتراک", "درباره دکتر وی پی ان"),
    "en": ("Home", "Buying guide", "Privacy", "Download for Android", "Buy a subscription", "About Dr VPN"),
    "zh": ("首页", "购买指南", "隐私", "下载安卓版", "购买订阅", "关于 Dr VPN"),
    "ru": ("Главная", "Как купить", "Конфиденциальность", "Скачать для Android", "Купить подписку", "О Dr VPN"),
    "my": ("ပင်မ", "ဝယ်ယူနည်း", "Privacy", "Android အတွက် ဒေါင်းလုဒ်", "Subscription ဝယ်ရန်", "Dr VPN အကြောင်း"),
    "es": ("Inicio", "Guía de compra", "Privacidad", "Descargar para Android", "Comprar suscripción", "Sobre Dr VPN"),
}
STEP_ICONS = ["install", "country", "tap", "connected"]
FEAT_ICONS = ["free", "fast", "shield", "tv", "website"]


def clean(text, lang):
    """Markdown from the repository, ready for the site."""
    text = text.split(END, 1)[1] if END in text else text
    text = re.sub(r'^\s*<div dir="rtl">\s*$|^\s*</div>\s*$', "", text, flags=re.M)
    text = re.sub(r"^🌐 \[.*$", "", text, flags=re.M)  # language bar

    def link(m):
        label, url = m.group(1), m.group(2)
        if re.match(r"^(https?:|mailto:|#)", url):
            return m.group(0)
        if url.startswith(("README", "ARTICLE", "PRIVACY")):
            url = {"R": "index.md", "A": "article.md", "P": "privacy.md"}[url[0]]
        else:
            url = BLOB + url
        return f"[{label}]({url})"

    text = re.sub(r"\[([^\]]*)\]\(([^)\s]+)\)", link, text)
    # The download section points at "the green button at the top of this page" — that's the hero now.
    return text.strip() + "\n"


def home(lang, readme):
    t = T[readme]
    w = WORDS[lang]
    steps = "\n".join(
        f'<div class="dr-step"><img src="{SITE_URL}assets/icons/step{n + 1}.png" alt="{n + 1}" width="30">'
        f'<img src="{SITE_URL}assets/icons/{i}.png" alt="" width="72"><b>{s}</b></div>'
        for n, (i, s) in enumerate(zip(STEP_ICONS, t["steps"])))
    feats = "\n".join(
        f'<div class="dr-feat"><img src="{SITE_URL}assets/icons/{i}.png" alt="" width="56"><b>{s}</b></div>'
        for i, s in zip(FEAT_ICONS, t["feats"]))
    body = clean(open(readme, encoding="utf-8").read(), lang)
    title = re.sub(r"^\W+\s*", "", t["tagline"])
    return f"""---
title: {t["name"]} — {title}
description: {t["tagline"]}. {t["free"].lstrip("🎁 ")}.
hide:
  - navigation
  - toc
---

<div class="dr-hero" markdown>
<div class="dr-hero-text" markdown>

# {t["name"]}

<p class="dr-tagline">{t["tagline"]}</p>
<p class="dr-free">{t["free"]}</p>

[:material-android: {w[3]}]({DOWNLOAD}){{ .md-button .md-button--primary }}
[:material-cart: {w[4]}](https://drvpn.net/){{ .md-button }}
[:fontawesome-brands-telegram: Telegram](https://t.me/drVPN_net){{ .md-button }}

<div class="dr-badges">
<img src="https://img.shields.io/github/downloads/{REPO}/total?label=Downloads&color=0057d9" alt="Downloads">
<img src="https://img.shields.io/github/v/release/{REPO}?label=Version&color=0057d9" alt="Version">
<img src="https://img.shields.io/github/stars/{REPO}?label=Stars&color=0057d9" alt="Stars">
</div>
</div>
<div class="dr-hero-logo"><img src="{SITE_URL}assets/logo.png" alt="Dr VPN" width="220"></div>
</div>

## {t["steps_title"]}

<div class="dr-steps">
{steps}
</div>

## {t["feat_title"]}

<div class="dr-feats">
{feats}
</div>

<div class="dr-buy" markdown>

## {t["buy_title"]}

[:material-web: drvpn.net](https://drvpn.net/){{ .md-button .md-button--primary }}
[:fontawesome-brands-telegram: @drVPN_net](https://t.me/drVPN_net){{ .md-button }}

</div>

---

{body}"""


def page(lang, src, desc):
    text = clean(open(src, encoding="utf-8").read(), lang)
    return f"---\ndescription: {desc}\n---\n\n{text}"


def config():
    langs = "\n".join(f"""        - locale: {code}
          name: {name}
          build: true{'''
          default: true''' if code == "fa" else ""}
          site_name: {T[readme]["name"]}
          nav_translations:
            Home: {WORDS[code][0]}
            Article: {WORDS[code][1]}
            Privacy: {WORDS[code][2]}""" for code, name, readme, _ in LANGS)
    verify = os.environ.get("GOOGLE_SITE_VERIFICATION", "").strip()
    return f"""site_name: Dr VPN
site_url: {SITE_URL}
site_description: Dr VPN — fast, safe, open-source VPN for Android with a free server built in.
repo_url: https://github.com/{REPO}
repo_name: {REPO}
edit_uri: ""
copyright: Dr VPN · open source (GPL-3.0)
docs_dir: docs
theme:
  name: material
  custom_dir: overrides
  language: fa
  logo: assets/logo.png
  favicon: assets/logo.png
  icon:
    repo: fontawesome/brands/github
  font:
    text: Vazirmatn
    code: Roboto Mono
  palette:
    - media: "(prefers-color-scheme: light)"
      scheme: default
      primary: custom
      accent: custom
      toggle:
        icon: material/weather-night
        name: Dark mode
    - media: "(prefers-color-scheme: dark)"
      scheme: slate
      primary: custom
      accent: custom
      toggle:
        icon: material/weather-sunny
        name: Light mode
  features:
    - navigation.tabs
    - navigation.top
    - navigation.footer
    - search.highlight
    - toc.follow
    - content.code.copy
extra_css:
  - assets/style.css
extra:
  google_site_verification: "{verify}"
  social:
    - icon: fontawesome/brands/telegram
      link: https://t.me/drVPN_net
    - icon: fontawesome/brands/github
      link: https://github.com/{REPO}
    - icon: material/web
      link: https://drvpn.net/
markdown_extensions:
  - attr_list
  - md_in_html
  - admonition
  - pymdownx.details
  - pymdownx.superfences
  - pymdownx.emoji:
      emoji_index: !!python/name:material.extensions.emoji.twemoji
      emoji_generator: !!python/name:material.extensions.emoji.to_svg
  - toc:
      permalink: true
plugins:
  - search
  - i18n:
      docs_structure: folder
      reconfigure_material: true
      reconfigure_search: true
      languages:
{langs}
nav:
  - Home: index.md
  - Article: article.md
  - Privacy: privacy.md
"""


def main():
    shutil.rmtree(WORK, ignore_errors=True)
    shutil.rmtree("_site", ignore_errors=True)
    os.makedirs(f"{DOCS}/assets")
    shutil.copytree("docs/icons", f"{DOCS}/assets/icons")
    shutil.copy("android/app/src/main/res/drawable-nodpi/drvpn_logo.png", f"{DOCS}/assets/logo.png")
    shutil.copy("site/style.css", f"{DOCS}/assets/style.css")
    shutil.copytree("site/overrides", f"{WORK}/overrides")
    if os.path.isdir("site/static"):
        shutil.copytree("site/static", DOCS, dirs_exist_ok=True)
    with open(f"{DOCS}/robots.txt", "w") as f:
        f.write(f"User-agent: *\nAllow: /\n\nSitemap: {SITE_URL}sitemap.xml\n")

    for code, _, readme, article in LANGS:
        os.makedirs(f"{DOCS}/{code}")
        t = T[readme]
        with open(f"{DOCS}/{code}/index.md", "w", encoding="utf-8") as f:
            f.write(home(code, readme))
        with open(f"{DOCS}/{code}/article.md", "w", encoding="utf-8") as f:
            f.write(page(code, article, t["tagline"]))
        with open(f"{DOCS}/{code}/privacy.md", "w", encoding="utf-8") as f:
            f.write(page(code, "PRIVACY.md", "Dr VPN privacy policy"))
    with open(f"{WORK}/mkdocs.yml", "w", encoding="utf-8") as f:
        f.write(config())

    subprocess.run(["mkdocs", "build", "--strict", "-f", f"{WORK}/mkdocs.yml", "-d", "../_site"], check=True)
    open("_site/.nojekyll", "w").close()


if __name__ == "__main__":
    main()
