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

## Update after a change

```bash
cd stats-worker
npx wrangler deploy
```

The database updates itself on the first request (missing columns, the `installs` and `daily`
tables); there is no SQL to run by hand. `deploy` also turns on the nightly job in `wrangler.toml`
that deletes old data when "auto-delete" is set in the panel's settings (off by default).

## Staying in the free plan

D1's free plan reads at most 5 million rows a day; past that every query fails until 00:00 UTC,
the app's pings included. So the panel's numbers are computed in a few cheap queries, kept for
15 minutes, and the panel refreshes every 5 minutes only while its tab is in view. The panel may
read at most 2 million rows a day (`STATS_DAILY_READS` in `worker.js`); past that it shows its kept
numbers until midnight. The badge is recomputed every 6 hours. The settings page shows how much the
panel read today, the database size and today's pings.

## See the numbers

- `https://drvpn-stats.<you>.workers.dev/stats`: active installs (today, 7 days, 30 days), all-time installs,
  and the last 7, 30 or 90 days by version, region, extra hop, server type, gaming mode, phone and place, plus
  connections per day and week-1 retention.
- Badge for the README: `https://img.shields.io/endpoint?url=https://drvpn-stats.<you>.workers.dev/badge&style=for-the-badge`
