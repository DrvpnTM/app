-- One row per install per day. No IP address, no traffic content or destinations, no servers: only a
-- random install ID the app makes (not tied to the person or the device), the app version, what the
-- user chose, and how much was used (two byte totals).
CREATE TABLE IF NOT EXISTS pings (
  day      TEXT NOT NULL,      -- YYYY-MM-DD (UTC)
  id       TEXT NOT NULL,      -- random install ID
  version  TEXT NOT NULL,
  region   TEXT NOT NULL,      -- the region picked in the app: ir, cn, ru, other
  hop      TEXT NOT NULL,      -- extra hop in use: off, lantern, shield, psiphon, tunnel, byedpi, tor, emergency
  gaming   INTEGER NOT NULL,   -- gaming mode on
  connects INTEGER NOT NULL,   -- connections since the app's last ping (0.2.8 on; before: that UTC day so far)
  brand    TEXT NOT NULL DEFAULT '?',  -- phone maker (Samsung, Xiaomi, ...)
  model    TEXT NOT NULL DEFAULT '?',  -- phone model
  android  TEXT NOT NULL DEFAULT '?',  -- Android version (14, 15, ...)
  country  TEXT NOT NULL DEFAULT '??', -- IP country (Cloudflare), level only, no city/address
  lat      REAL,                       -- approximate IP latitude, rounded to ~11 km (for the map)
  lon      REAL,                       -- approximate IP longitude, rounded to ~11 km
  city     TEXT NOT NULL DEFAULT '?',  -- IP city (Cloudflare), only shown as a per-city count, never per user
  server   TEXT NOT NULL DEFAULT '?',  -- kind of server in use: xray, aether, openvpn, serverless, custom, group
  up       INTEGER,                    -- bytes sent through the VPN since the app's last ping (NULL before 0.2.8)
  down     INTEGER,                    -- bytes received through the VPN since the app's last ping (NULL before 0.2.8)
  fails    INTEGER,                    -- connections that failed to start since the last ping (NULL before 0.2.8)
  asn      INTEGER,                    -- the network's AS number (Cloudflare), never the address
  isp      TEXT NOT NULL DEFAULT '?',  -- the network's name, e.g. "Iran Cell Service and Communication Company"
  at       INTEGER,                    -- when the ping came (unix seconds)
  via      TEXT NOT NULL DEFAULT '?',  -- direct, or vpn (then place and network are the VPN server's)
  PRIMARY KEY (day, id)
);
CREATE INDEX IF NOT EXISTS pings_day ON pings(day);
CREATE INDEX IF NOT EXISTS pings_at ON pings(at);

-- Panel settings and kept results: the admin password (PBKDF2, never plain), the panel's last
-- numbers (stats_<days>), the badge count, the auto-delete setting, the Telegram and GitHub tokens
-- set in the panel, the last 30 logins (time, country, city, browser; no address).
CREATE TABLE IF NOT EXISTS settings (
  key   TEXT PRIMARY KEY,
  value TEXT NOT NULL
);

-- Each install's first day, so "new users" needs no scan of every ping.
CREATE TABLE IF NOT EXISTS installs (
  id    TEXT PRIMARY KEY,
  first TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS installs_first ON installs(first);

-- Past days rolled up once: they never change, so the panel does not count them again.
CREATE TABLE IF NOT EXISTS daily (
  day      TEXT PRIMARY KEY,
  active   INTEGER NOT NULL,
  connects INTEGER NOT NULL,
  up       INTEGER NOT NULL DEFAULT 0,
  down     INTEGER NOT NULL DEFAULT 0,
  fails    INTEGER NOT NULL DEFAULT 0
);

-- Past days per value of one column (versions in use per day), rolled up once.
CREATE TABLE IF NOT EXISTS daily_dim (
  day TEXT NOT NULL, dim TEXT NOT NULL, k TEXT NOT NULL, n INTEGER NOT NULL,
  PRIMARY KEY (day, dim, k)
);
