# Dr VPN stats (Cloudflare Worker)

Counts active installs from the app's anonymous daily ping. Free on Cloudflare's free plan.
No IP addresses are stored: see `schema.sql` for everything that is kept.

## Deploy (once, about 5 minutes)

You need a free Cloudflare account and Node.js.

```bash
cd stats-worker
npx wrangler login                       # opens the browser to sign in to Cloudflare
npx wrangler d1 create drvpn-stats       # prints a database_id
# put that database_id in wrangler.toml
npx wrangler d1 execute drvpn-stats --remote --file=schema.sql
npx wrangler deploy                      # prints the address, e.g. https://drvpn-stats.<you>.workers.dev
```

## Turn it on in the app

In `notice.json` (repo root), set:

```json
"stats_url": "https://drvpn-stats.<you>.workers.dev/ping"
```

and push. Apps read it from there, so no new version is needed. Until it is set, the app sends nothing.

## See the numbers

- `https://drvpn-stats.<you>.workers.dev/stats`: active installs (today, 7 days, 30 days), all-time installs,
  and the last 30 days by version, region, extra hop and gaming mode, plus connections per day.
- Badge for the README: `https://img.shields.io/endpoint?url=https://drvpn-stats.<you>.workers.dev/badge&style=for-the-badge`
