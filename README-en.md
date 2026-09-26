🌐 [فارسی](README.md) | [English](README-en.md) | [中文](README-zh.md) | [Русский](README-ru.md) | [မြန်မာ](README-my.md) | [Español](README-es.md)

# Dr VPN

**Dr VPN** is a simple, open-source VPN app for Android with a Hiddify-style look: one big connect button, the active subscription card and the selected server card.

- 🌐 Website: [drvpn.net](https://drvpn.net/)
- 📢 Telegram channel: [@drVPN_net](https://t.me/drVPN_net)
- 📄 Article: [How to buy a VPN subscription + meet Dr VPN](ARTICLE-en.md)

## ✨ Features

- One-tap connect from the big button on the home screen
- First-run setup: choose your country (local sites open directly) and the app language
- VLESS, VMess, Trojan, Shadowsocks, Hysteria2, WireGuard, SOCKS and HTTP
- Add subscriptions or configs from a link, clipboard, QR code or file
- Servers are pinged automatically; only working servers are shown (tick a box to show all)
- A country flag next to every server
- Per-app proxy, routing rules, dark mode

## 📥 Download

Every change is built automatically by GitHub Actions: open the **Actions** tab → **Build Dr VPN APK** → latest successful run → **Artifacts** → `DrVPN-universal`.

> The current build is a test (debug) build.

## 📱 How to use

1. Install and open the app, then pick your country and language.
2. Tap **+** at the top and import your subscription link or config (clipboard or QR code).
3. Working servers appear in the **Proxies** tab — pick one.
4. Go back to **Home** and tap the big button. Green means connected.

## 📜 License and credits

Open source under **GPL-3.0** ([LICENSE](LICENSE)). Dr VPN is based on [v2rayNG](https://github.com/2dust/v2rayNG) and uses [Xray-core](https://github.com/XTLS/Xray-core) and [hev-socks5-tunnel](https://github.com/heiher/hev-socks5-tunnel). Many thanks to their authors.
