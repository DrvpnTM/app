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
read at most 2 million rows a day (`STATS_DAILY_READS` in `db.js`); past that it shows its kept
numbers until midnight. The badge is recomputed every 6 hours. The settings page shows how much the
panel read today, the database size and today's pings.

## The panel

Open `https://drvpn-stats.<you>.workers.dev/` and sign in. Pages (side menu, or keys 1–9):

- **Dashboard**: online now (reports in the last hour) against the month's users, active users, data used and
  the busiest day; usage and user trends with a 7-day average; today and yesterday against the week's and
  month's daily average; a health card (connection success, "connected but no traffic", last report, the
  database's daily reads); the last 24 hours by hour.
- **Live**: reports of the last 15 minutes, hour and day, a map of the areas seen in the last hour, and a feed
  of the newest reports (country, version, method, server, maker; no ID, city or phone model).
- **Users**: week-1 return, DAU/MAU, returning and lost users, weekly cohorts, new vs returning per day, and
  the versions in use over the last 45 days.
- **Connections** (app 0.2.8 on): success rate overall, per day, and by extra hop, server type, version,
  country and network; users who connected but moved under 200 KB, by method (a sign a method is filtered).
- **Data used** (app 0.2.8 on): per day, per user, by server type, extra hop, region, country and network.
- **Geography and networks**: the map, countries, cities and networks (e.g. Irancell, MCI; data centers are
  marked). Reports sent through the VPN are left out here: their place and network are the VPN server's.
- **Devices**, **Downloads** (APK downloads per release from GitHub) and **Announcement** (edit the in-app
  notice: `notice.json` in the repo, through GitHub's API; needs a GitHub token, see Settings).

Every page can be narrowed to one country, region, network, version, server type, extra hop, phone maker or
Android version, and the range is 7, 30, 90 or 180 days. The address keeps the view (`#tab=conn&f=country&v=IR`).

Settings: theme (light mint, cyber, dark, glass, auto), Telegram bot (a daily report at 07:47 Tehran, and alerts
when reports halve or most connections fail), GitHub token, password, recent logins (5 wrong passwords from one
address lock it out for 15 minutes), database health and old-data clean-up.

Optional secrets instead of the settings page: `GITHUB_TOKEN`, `TELEGRAM_BOT_TOKEN`, `TELEGRAM_CHAT_ID`
(`npx wrangler secret put NAME`).

## Files

`worker.js` routes and login · `stats.js` the panel's numbers · `extras.js` GitHub, announcement, Telegram ·
`db.js` schema and settings · `pages.js` HTML and CSS · `client/panel.js` the pages' script (served at `/app.js`).

## Data used, and the badge

- Data used (from app 0.2.8): per day (download and upload), per user on average, by server type, extra hop,
  region and country, and how many users used how much. The app sends only two byte totals, counted since its
  last ping (the same numbers as its Usage screen), so a day's bar is what that day's pings carried: about the
  24 hours before. Versions before 0.2.8 send none and are left out of the averages.
- Badge for the README: `https://img.shields.io/endpoint?url=https://drvpn-stats.<you>.workers.dev/badge&style=for-the-badge`
