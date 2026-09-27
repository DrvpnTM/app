# لینک یک‌لمسی افزودن اشتراک به Dr VPN / One-tap subscription links

<div dir="rtl">

با این لینک‌ها کاربر روی یک دکمه در سایت شما می‌زند و اشتراکش مستقیم به برنامه Dr VPN اضافه می‌شود؛ دیگر لازم نیست لینک را کپی کند.

## ساختار لینک

```
drvpn://install-sub?url=<لینک اشتراک به‌صورت URL-encoded>#<نام اشتراک>
```

- `url`: لینک اشتراک کاربر، که باید **URL-encode** شود (مثلاً `:` به `%3A` و `/` به `%2F`).
- بعد از `#`: نامی که برای اشتراک در برنامه نمایش داده می‌شود (اختیاری).
- برای افزودن یک کانفیگ تکی (مثلاً `vless://...`) به‌جای `install-sub` از `install-config` استفاده کنید.
- لینک‌های قدیمی `v2rayng://install-sub?...` هم کار می‌کنند.

### مثال

لینک اشتراک:
```
https://panel.drvpn.net/sub/abc123
```
لینک یک‌لمسی:
```
drvpn://install-sub?url=https%3A%2F%2Fpanel.drvpn.net%2Fsub%2Fabc123#Dr%20VPN
```

## دکمه آماده برای سایت drvpn.net

این کد را در صفحه اشتراک کاربر بگذارید (به‌جای `SUB_URL` لینک اشتراک همان کاربر را قرار دهید):

```html
<a id="drvpn-add" class="drvpn-btn" href="#">افزودن به Dr VPN</a>
<script>
  const subUrl = "SUB_URL";
  document.getElementById("drvpn-add").href =
    "drvpn://install-sub?url=" + encodeURIComponent(subUrl) + "#" + encodeURIComponent("Dr VPN");
</script>
<style>
  .drvpn-btn { display:inline-block; padding:14px 28px; border-radius:14px;
    background:#007AFF; color:#fff; font-weight:600; text-decoration:none; }
</style>
```

## تلگرام

تلگرام لینک‌هایی با `drvpn://` را در پیام‌ها قابل کلیک نمی‌کند. در کانال یا ربات، کاربر را به یک صفحه در سایت خودتان (مثلاً `https://drvpn.net/add?sub=...`) بفرستید و در آن صفحه دکمه بالا را نشان دهید، یا صفحه را خودکار به لینک `drvpn://` هدایت کنید.

</div>

---

## English

Format: `drvpn://install-sub?url=<URL-encoded subscription link>#<name>` (use `install-config` for a single `vless://`/`vmess://`… config). Legacy `v2rayng://` links also work. Telegram does not make custom schemes clickable, so link to a page on your site that shows the button (or redirects to the `drvpn://` link).
