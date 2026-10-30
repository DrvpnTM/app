#!/usr/bin/env python3
"""Builds the Dr VPN website from the README, article and privacy pages.

Run from the repository root:  python3 site/build.py
Needs a GitHub token in GH_TOKEN or GITHUB_TOKEN: the Markdown is turned into HTML by GitHub
itself, so the site looks just like the repository page. The result goes to _site/.

Search Console: put Google's verification file (google....html) in site/, or set the
GOOGLE_SITE_VERIFICATION environment variable to the code from the "HTML tag" method.
"""
import datetime
import html
import json
import os
import re
import shutil
import urllib.request

BASE = os.environ.get("SITE_URL", "https://drvpntm.github.io/app/")
REPO = "DrvpnTM/app"
BLOB = f"https://github.com/{REPO}/blob/main/"
OUT = "_site"
VERIFY = os.environ.get("GOOGLE_SITE_VERIFICATION", "").strip()

# source file -> (page, language, right-to-left)
PAGES = {
    "README.md": ("", "fa", True),
    "README-en.md": ("en.html", "en", False),
    "README-zh.md": ("zh.html", "zh", False),
    "README-ru.md": ("ru.html", "ru", False),
    "README-my.md": ("my.html", "my", False),
    "README-es.md": ("es.html", "es", False),
    "ARTICLE-fa.md": ("article-fa.html", "fa", True),
    "ARTICLE-en.md": ("article-en.html", "en", False),
    "ARTICLE-zh.md": ("article-zh.html", "zh", False),
    "ARTICLE-ru.md": ("article-ru.html", "ru", False),
    "ARTICLE-my.md": ("article-my.html", "my", False),
    "ARTICLE-es.md": ("article-es.html", "es", False),
    "PRIVACY.md": ("privacy.html", "fa", True),
}
HOME_GROUP = [f for f in PAGES if f.startswith("README")]
ARTICLE_GROUP = [f for f in PAGES if f.startswith("ARTICLE")]
FOOTER = {
    "fa": ("مقاله", "حریم خصوصی", "کد منبع"),
    "en": ("Article", "Privacy", "Source code"),
    "zh": ("文章", "隐私", "源代码"),
    "ru": ("Статья", "Конфиденциальность", "Исходный код"),
    "my": ("ဆောင်းပါး", "Privacy", "Source code"),
    "es": ("Artículo", "Privacidad", "Código fuente"),
}


def render(text):
    token = os.environ.get("GH_TOKEN") or os.environ.get("GITHUB_TOKEN")
    req = urllib.request.Request(
        "https://api.github.com/markdown",
        data=json.dumps({"text": text, "mode": "gfm", "context": REPO}).encode(),
        headers={"Accept": "application/vnd.github+json",
                 **({"Authorization": f"Bearer {token}"} if token else {})},
    )
    with urllib.request.urlopen(req) as r:
        return r.read().decode()


def fix_link(m):
    attr, url = m.group(1), m.group(2)
    if re.match(r"^(https?:|mailto:|#|//)", url):
        return m.group(0)
    path, _, frag = url.partition("#")
    if path in PAGES:
        page = PAGES[path][0] or "./"
    elif path.startswith(("docs/icons/", "android/app/src/main/res/drawable-nodpi/")):
        page = path  # copied next to the pages
    else:
        page = BLOB + path
    return f'{attr}="{page}{"#" + frag if frag else ""}"'


def plain(s):
    s = re.sub(r"<[^>]+>", " ", s)
    return re.sub(r"\s+", " ", html.unescape(s)).strip()


def describe(body):
    """The first real paragraph, for the search result snippet."""
    for p in re.findall(r"<p[^>]*>(.*?)</p>", body, re.S):
        t = plain(p)
        if len(t) > 60:
            return t if len(t) <= 160 else t[:157].rsplit(" ", 1)[0] + "…"
    return "Dr VPN — fast, safe, open-source VPN for Android."


def title_of(src, body):
    if src.startswith("README"):
        h3 = re.search(r"<h3[^>]*>(.*?)</h3>", body, re.S)
        return f"Dr VPN — {plain(h3.group(1))}" if h3 else "Dr VPN"
    h1 = re.search(r"<h1[^>]*>(.*?)</h1>", body, re.S)
    return plain(h1.group(1)) if h1 else "Dr VPN"


def alternates(group):
    links = [f'<link rel="alternate" hreflang="{PAGES[f][1]}" href="{BASE}{PAGES[f][0]}">' for f in group]
    links.append(f'<link rel="alternate" hreflang="x-default" href="{BASE}{PAGES[group[1]][0]}">')
    return "\n".join(links)


def page(src):
    out, lang, rtl = PAGES[src]
    with open(src, encoding="utf-8") as f:
        body = render(f.read())
    body = re.sub(r'\b(href|src)="([^"]*)"', fix_link, body)
    title, desc = title_of(src, body), describe(body)
    url = BASE + out
    group = HOME_GROUP if src in HOME_GROUP else ARTICLE_GROUP if src in ARTICLE_GROUP else None
    art, priv, code = FOOTER[lang]
    art_page = PAGES[f"ARTICLE-{lang}.md"][0]
    ld = {"@context": "https://schema.org", "@type": "SoftwareApplication", "name": "Dr VPN",
          "operatingSystem": "Android", "applicationCategory": "UtilitiesApplication",
          "offers": {"@type": "Offer", "price": "0", "priceCurrency": "USD"},
          "downloadUrl": f"https://github.com/{REPO}/releases/latest/download/DrVPN.apk",
          "url": url, "description": desc}
    return f"""<!doctype html>
<html lang="{lang}" dir="{"rtl" if rtl else "ltr"}">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>{html.escape(title)}</title>
<meta name="description" content="{html.escape(desc)}">
{f'<meta name="google-site-verification" content="{html.escape(VERIFY)}">' if VERIFY else ""}
<link rel="canonical" href="{url}">
{alternates(group) if group else ""}
<meta property="og:type" content="website">
<meta property="og:title" content="{html.escape(title)}">
<meta property="og:description" content="{html.escape(desc)}">
<meta property="og:url" content="{url}">
<meta property="og:image" content="{BASE}logo.png">
<meta name="twitter:card" content="summary">
<meta name="theme-color" content="#0f9d58">
<link rel="icon" href="logo.png">
<link rel="stylesheet" href="style.css">
{'<script type="application/ld+json">' + json.dumps(ld, ensure_ascii=False) + '</script>' if src in HOME_GROUP else ""}
</head>
<body>
<main class="markdown-body">
{body}
</main>
<footer>
<a href="{art_page}">{art}</a> · <a href="privacy.html">{priv}</a> ·
<a href="https://github.com/{REPO}">{code}</a> · <a href="https://drvpn.net/">drvpn.net</a> ·
<a href="https://t.me/drVPN_net">Telegram</a>
</footer>
</body>
</html>
"""


def main():
    shutil.rmtree(OUT, ignore_errors=True)
    os.makedirs(OUT)
    shutil.copytree("docs/icons", f"{OUT}/docs/icons")
    logo_dir = "android/app/src/main/res/drawable-nodpi"
    os.makedirs(f"{OUT}/{logo_dir}")
    shutil.copy(f"{logo_dir}/drvpn_logo.png", f"{OUT}/{logo_dir}/")
    shutil.copy(f"{logo_dir}/drvpn_logo.png", f"{OUT}/logo.png")
    for name in os.listdir("site"):
        if name.endswith((".css", ".html", ".txt")):
            shutil.copy(f"site/{name}", OUT)

    for src, (out, _, _) in PAGES.items():
        with open(f"{OUT}/{out or 'index.html'}", "w", encoding="utf-8") as f:
            f.write(page(src))
        print("built", out or "index.html")

    today = datetime.date.today().isoformat()
    urls = "".join(f"<url><loc>{BASE}{out}</loc><lastmod>{today}</lastmod></url>\n"
                   for out, _, _ in PAGES.values())
    with open(f"{OUT}/sitemap.xml", "w") as f:
        f.write('<?xml version="1.0" encoding="UTF-8"?>\n'
                f'<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n{urls}</urlset>\n')
    with open(f"{OUT}/robots.txt", "w") as f:
        f.write(f"User-agent: *\nAllow: /\n\nSitemap: {BASE}sitemap.xml\n")
    open(f"{OUT}/.nojekyll", "w").close()


if __name__ == "__main__":
    main()
