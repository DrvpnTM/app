"""Search texts for the website: page titles, descriptions and the home page FAQ, per language.

Titles stay under ~60 characters and descriptions under ~160, so Google shows them in full.
"""

OG_LOCALE = {"fa": "fa_IR", "en": "en_US", "zh": "zh_CN", "ru": "ru_RU", "my": "my_MM", "es": "es_ES"}

# (title, description) for the home page, the buying guide and the privacy page.
PAGES = {
    "fa": {
        "home": ("خرید فیلترشکن پرسرعت | دانلود دکتر وی پی ان برای اندروید",
                 "دانلود رایگان فیلترشکن دکتر وی پی ان (Dr VPN) برای اندروید با سرور رایگان، مخصوص نت ملی "
                 "و اینترنت ایران. خرید اشتراک فیلترشکن پرسرعت و کانفیگ v2ray و vless."),
        "article": ("راهنمای خرید فیلترشکن و اشتراک وی پی ان | ۷ نکته مهم",
                    "قبل از خرید فیلترشکن و اشتراک وی پی ان این ۷ نکته را بدانید: پروتکل، تست رایگان، حجم، "
                    "تعداد کاربر و پشتیبانی. به همراه معرفی برنامه رایگان دکتر وی پی ان."),
        "privacy": ("سیاست حریم خصوصی دکتر وی پی ان",
                    "دکتر وی پی ان هیچ اطلاعات شخصی جمع‌آوری نمی‌کند و ترافیک شما را ثبت نمی‌کند. "
                    "سیاست حریم خصوصی برنامه فیلترشکن دکتر وی پی ان."),
    },
    "en": {
        "home": ("Dr VPN — Free, Fast VPN for Android with a Free Server",
                 "Download Dr VPN, a free open-source VPN for Android and Android TV with a built-in free "
                 "server. Works with VLESS, VMess, Trojan, Hysteria2 and WireGuard configs."),
        "article": ("How to Buy a VPN Subscription: 7 Things to Check",
                    "Before you buy a VPN subscription, check protocols, free trial, data and time limits, "
                    "devices and support. Plus a quick look at the free Dr VPN app."),
        "privacy": ("Privacy Policy — Dr VPN",
                    "Dr VPN collects no personal data and keeps no traffic logs. Read the privacy policy "
                    "of the Dr VPN app for Android."),
    },
    "zh": {
        "home": ("Dr VPN — 免费高速安卓 VPN，内置免费服务器",
                 "下载 Dr VPN：免费开源的安卓及安卓电视 VPN，内置免费服务器，支持 VLESS、VMess、Trojan、"
                 "Hysteria2 和 WireGuard 配置。"),
        "article": ("如何购买 VPN 订阅：购买前要检查的 7 件事",
                    "购买 VPN 订阅前，请检查协议、免费试用、流量和时长、设备数量与客服支持，并了解免费的 Dr VPN 应用。"),
        "privacy": ("隐私政策 — Dr VPN",
                    "Dr VPN 不收集任何个人数据，也不记录流量日志。查看 Dr VPN 安卓应用的隐私政策。"),
    },
    "ru": {
        "home": ("Dr VPN — бесплатный быстрый VPN для Android",
                 "Скачайте Dr VPN — бесплатный VPN с открытым кодом для Android и Android TV со встроенным "
                 "бесплатным сервером. Поддержка VLESS, VMess, Trojan, Hysteria2 и WireGuard."),
        "article": ("Как купить VPN-подписку: 7 вещей, которые стоит проверить",
                    "Перед покупкой VPN-подписки проверьте протоколы, пробный период, трафик и срок, число "
                    "устройств и поддержку. И короткий обзор бесплатного Dr VPN."),
        "privacy": ("Политика конфиденциальности — Dr VPN",
                    "Dr VPN не собирает личные данные и не ведёт журналы трафика. Политика "
                    "конфиденциальности приложения Dr VPN для Android."),
    },
    "my": {
        "home": ("Dr VPN — Android အတွက် အခမဲ့ မြန်ဆန်သော VPN",
                 "Dr VPN ကို ဒေါင်းလုဒ်လုပ်ပါ — Android နှင့် Android TV အတွက် အခမဲ့ open-source VPN၊ "
                 "အခမဲ့ server ပါဝင်သည်။ VLESS, VMess, Trojan, Hysteria2, WireGuard ကို ပံ့ပိုးသည်။"),
        "article": ("VPN subscription ဝယ်နည်း — စစ်ဆေးရမည့် အချက် ၇ ချက်",
                    "VPN subscription မဝယ်မီ protocol၊ အခမဲ့စမ်းသပ်မှု၊ data နှင့် ကာလ၊ စက်အရေအတွက်နှင့် "
                    "support ကို စစ်ဆေးပါ။ အခမဲ့ Dr VPN app ကိုလည်း မိတ်ဆက်ထားသည်။"),
        "privacy": ("Privacy Policy — Dr VPN",
                    "Dr VPN သည် ကိုယ်ရေးအချက်အလက် မစုဆောင်းပါ၊ traffic log မသိမ်းပါ။ "
                    "Dr VPN Android app ၏ privacy policy။"),
    },
    "es": {
        "home": ("Dr VPN — VPN gratis y rápida para Android con servidor gratis",
                 "Descarga Dr VPN, una VPN gratuita y de código abierto para Android y Android TV con "
                 "servidor gratis incluido. Compatible con VLESS, VMess, Trojan, Hysteria2 y WireGuard."),
        "article": ("Cómo comprar una suscripción VPN: 7 cosas que revisar",
                    "Antes de comprar una suscripción VPN, revisa protocolos, prueba gratis, datos y duración, "
                    "dispositivos y soporte. Y conoce la app gratuita Dr VPN."),
        "privacy": ("Política de privacidad — Dr VPN",
                    "Dr VPN no recopila datos personales ni guarda registros de tráfico. Lee la política de "
                    "privacidad de la app Dr VPN para Android."),
    },
}

# Home page FAQ: (heading, [(question, answer), ...]). Answers are Markdown.
FAQ = {
    "fa": ("سؤالات پرتکرار", [
        ("آیا فیلترشکن دکتر وی پی ان رایگان است؟",
         "بله. برنامه رایگان و متن‌باز است و یک سرور رایگان هم داخلش دارد. برای سرعت بیشتر می‌توانید از "
         "[drvpn.net](https://drvpn.net/) اشتراک پرسرعت بخرید."),
        ("دکتر وی پی ان از چه کانفیگ‌هایی پشتیبانی می‌کند؟",
         "VLESS (با Reality)، VMess، Trojan، Shadowsocks، Hysteria2، WireGuard، SOCKS و HTTP؛ "
         "به همراه لینک اشتراک (Subscription) و کیوآر کد."),
        ("روی تلویزیون اندروید هم کار می‌کند؟",
         "بله. دکتر وی پی ان روی گوشی، تبلت و تلویزیون اندروید (Android TV) کار می‌کند."),
        ("چطور فیلترشکن دکتر وی پی ان را نصب کنم؟",
         "فایل `DrVPN.apk` را با دکمه دانلود همین صفحه بگیرید و بازش کنید. اگر گوشی پرسید، اجازه نصب از "
         "این منبع را بدهید. بعد کشور را انتخاب کنید و دکمه بزرگ اتصال را بزنید."),
    ]),
    "en": ("Frequently asked questions", [
        ("Is Dr VPN free?",
         "Yes. The app is free and open source, and it comes with a free server built in. For higher "
         "speed you can buy a subscription at [drvpn.net](https://drvpn.net/)."),
        ("Which configs does Dr VPN support?",
         "VLESS (with Reality), VMess, Trojan, Shadowsocks, Hysteria2, WireGuard, SOCKS and HTTP, "
         "plus subscription links and QR codes."),
        ("Does it work on Android TV?",
         "Yes. Dr VPN works on Android phones, tablets and Android TV."),
        ("How do I install Dr VPN?",
         "Download `DrVPN.apk` with the button at the top of this page and open it. If your phone asks, "
         "allow installs from this source. Then pick your country and tap the big connect button."),
    ]),
    "zh": ("常见问题", [
        ("Dr VPN 是免费的吗？",
         "是的。应用免费且开源，并内置一个免费服务器。如需更高速度，可在 [drvpn.net](https://drvpn.net/) 购买订阅。"),
        ("Dr VPN 支持哪些配置？",
         "VLESS（含 Reality）、VMess、Trojan、Shadowsocks、Hysteria2、WireGuard、SOCKS 和 HTTP，"
         "以及订阅链接和二维码。"),
        ("能在安卓电视上使用吗？",
         "可以。Dr VPN 支持安卓手机、平板和安卓电视（Android TV）。"),
        ("如何安装 Dr VPN？",
         "点击本页顶部的下载按钮获取 `DrVPN.apk` 并打开。如果手机提示，请允许从此来源安装。"
         "然后选择国家，点击大的连接按钮。"),
    ]),
    "ru": ("Частые вопросы", [
        ("Dr VPN бесплатный?",
         "Да. Приложение бесплатное и с открытым кодом, в нём есть встроенный бесплатный сервер. "
         "Для большей скорости можно купить подписку на [drvpn.net](https://drvpn.net/)."),
        ("Какие конфиги поддерживает Dr VPN?",
         "VLESS (с Reality), VMess, Trojan, Shadowsocks, Hysteria2, WireGuard, SOCKS и HTTP, "
         "а также ссылки на подписку и QR-коды."),
        ("Работает ли он на Android TV?",
         "Да. Dr VPN работает на телефонах, планшетах и Android TV."),
        ("Как установить Dr VPN?",
         "Скачайте `DrVPN.apk` кнопкой вверху страницы и откройте файл. Если телефон спросит, разрешите "
         "установку из этого источника. Затем выберите страну и нажмите большую кнопку подключения."),
    ]),
    "my": ("မေးလေ့ရှိသော မေးခွန်းများ", [
        ("Dr VPN က အခမဲ့လား။",
         "ဟုတ်ပါတယ်။ App သည် အခမဲ့ open source ဖြစ်ပြီး အခမဲ့ server ပါဝင်သည်။ ပိုမြန်ချင်ပါက "
         "[drvpn.net](https://drvpn.net/) တွင် subscription ဝယ်နိုင်သည်။"),
        ("Dr VPN က ဘယ် config တွေကို ပံ့ပိုးသလဲ။",
         "VLESS (Reality အပါအဝင်)၊ VMess၊ Trojan၊ Shadowsocks၊ Hysteria2၊ WireGuard၊ SOCKS နှင့် HTTP၊ "
         "subscription link နှင့် QR code များ။"),
        ("Android TV မှာ အသုံးပြုလို့ရလား။",
         "ရပါတယ်။ Dr VPN သည် Android ဖုန်း၊ tablet နှင့် Android TV တွင် အလုပ်လုပ်သည်။"),
        ("Dr VPN ကို ဘယ်လို ထည့်သွင်းရမလဲ။",
         "ဤစာမျက်နှာ ထိပ်ရှိ ဒေါင်းလုဒ်ခလုတ်ဖြင့် `DrVPN.apk` ကို ရယူပြီး ဖွင့်ပါ။ ဖုန်းက မေးပါက ဤရင်းမြစ်မှ "
         "ထည့်သွင်းခွင့်ပြုပါ။ ထို့နောက် နိုင်ငံရွေးပြီး ချိတ်ဆက်ခလုတ်ကြီးကို နှိပ်ပါ။"),
    ]),
    "es": ("Preguntas frecuentes", [
        ("¿Dr VPN es gratis?",
         "Sí. La app es gratuita y de código abierto, e incluye un servidor gratis. Para más velocidad "
         "puedes comprar una suscripción en [drvpn.net](https://drvpn.net/)."),
        ("¿Qué configuraciones admite Dr VPN?",
         "VLESS (con Reality), VMess, Trojan, Shadowsocks, Hysteria2, WireGuard, SOCKS y HTTP, "
         "además de enlaces de suscripción y códigos QR."),
        ("¿Funciona en Android TV?",
         "Sí. Dr VPN funciona en teléfonos, tabletas y Android TV."),
        ("¿Cómo instalo Dr VPN?",
         "Descarga `DrVPN.apk` con el botón de arriba de esta página y ábrelo. Si el teléfono lo pide, "
         "permite instalar desde este origen. Luego elige tu país y toca el botón grande de conexión."),
    ]),
}
