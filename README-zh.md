🌐 [فارسی](README.md) | [English](README-en.md) | [中文](README-zh.md) | [Русский](README-ru.md) | [မြန်မာ](README-my.md) | [Español](README-es.md)

# Dr VPN

**Dr VPN** 是一款简单、开源的 Android VPN 应用，界面类似 Hiddify：一个大的连接按钮、当前订阅卡片和已选节点卡片。

- 🌐 网站：[drvpn.net](https://drvpn.net/)
- 📢 Telegram 频道：[@drVPN_net](https://t.me/drVPN_net)
- 📄 文章：[如何购买 VPN 订阅 + Dr VPN 介绍](ARTICLE-zh.md)

## ✨ 功能

- 主屏大按钮，一键连接
- 首次启动设置：选择国家（本地网站直连）和应用语言
- 支持 VLESS、VMess、Trojan、Shadowsocks、Hysteria2、WireGuard、SOCKS、HTTP
- 通过链接、剪贴板、二维码或文件添加订阅/配置
- 自动测试节点延迟，只显示可用节点（勾选即可显示全部）
- 每个节点旁显示国家旗帜
- 分应用代理、路由规则、深色模式

## 📥 下载

每次更新都会由 GitHub Actions 自动构建：打开 **Actions** → **Build Dr VPN APK** → 最近一次成功的运行 → **Artifacts** → `DrVPN-universal`。

> 当前为测试（debug）版本。

## 📱 使用方法

1. 安装并打开应用，选择国家和语言。
2. 点击顶部 **+**，从剪贴板或二维码导入订阅链接或配置。
3. 可用节点会出现在 **节点** 标签页，选择一个。
4. 回到 **首页**，点击大按钮。变绿即表示已连接。

## 📜 许可与致谢

本项目以 **GPL-3.0** 开源（[LICENSE](LICENSE)）。Dr VPN 基于 [v2rayNG](https://github.com/2dust/v2rayNG)，并使用 [Xray-core](https://github.com/XTLS/Xray-core) 和 [hev-socks5-tunnel](https://github.com/heiher/hev-socks5-tunnel)。感谢这些项目的作者。
