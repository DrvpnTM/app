🌐 [فارسی](README.md) | [English](README-en.md) | [中文](README-zh.md) | [Русский](README-ru.md) | [မြန်မာ](README-my.md) | [Español](README-es.md)

# Dr VPN

**Dr VPN** es una aplicación VPN sencilla y de código abierto para Android, con un aspecto estilo Hiddify: un gran botón de conexión, la tarjeta de la suscripción activa y la tarjeta del servidor seleccionado.

- 🌐 Sitio web: [drvpn.net](https://drvpn.net/)
- 📢 Canal de Telegram: [@drVPN_net](https://t.me/drVPN_net)
- 📄 Artículo: [Cómo comprar una suscripción VPN + conoce Dr VPN](ARTICLE-es.md)

## ✨ Características

- Conexión con un toque desde el gran botón de la pantalla principal
- Configuración inicial: elige tu país (los sitios locales se abren directamente) y el idioma de la app
- VLESS, VMess, Trojan, Shadowsocks, Hysteria2, WireGuard, SOCKS y HTTP
- Añade suscripciones o configuraciones desde un enlace, el portapapeles, un código QR o un archivo
- Los servidores se prueban (ping) automáticamente; solo se muestran los que funcionan (marca una casilla para ver todos)
- Una bandera del país junto a cada servidor
- Proxy por aplicación, reglas de enrutamiento, modo oscuro y varios temas
- Modo avanzado (internet nacional): escáner de IP limpia

## 📥 Descarga

Cada cambio se compila automáticamente con GitHub Actions: abre la pestaña **Actions** → **Build Dr VPN APK** → última ejecución correcta → **Artifacts** → `DrVPN-universal`.

> La compilación actual es de prueba (debug).

## 📱 Cómo usar

1. Instala y abre la app, elige tu país y tu idioma.
2. Toca **+** arriba e importa tu enlace de suscripción o configuración (portapapeles o código QR).
3. Los servidores que funcionan aparecen en la pestaña **Proxies**: elige uno.
4. Vuelve a **Inicio** y toca el gran botón. Verde significa conectado.

## 📜 Licencia y créditos

Código abierto bajo **GPL-3.0** ([LICENSE](LICENSE)); usa el motor [Xray-core](https://github.com/XTLS/Xray-core).
