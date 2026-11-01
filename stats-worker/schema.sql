-- One row per install per day. No IP address, no traffic, no servers: only a random install ID
-- the app makes (not tied to the person or the device), the app version and what the user chose.
CREATE TABLE IF NOT EXISTS pings (
  day      TEXT NOT NULL,      -- YYYY-MM-DD (UTC)
  id       TEXT NOT NULL,      -- random install ID
  version  TEXT NOT NULL,
  region   TEXT NOT NULL,      -- the region picked in the app: ir, cn, ru, other
  hop      TEXT NOT NULL,      -- extra hop in use: off, lantern, shield, psiphon, tunnel, tor, emergency
  gaming   INTEGER NOT NULL,   -- gaming mode on
  connects INTEGER NOT NULL,   -- connections that day
  brand    TEXT NOT NULL DEFAULT '?',  -- phone maker (Samsung, Xiaomi, ...)
  model    TEXT NOT NULL DEFAULT '?',  -- phone model
  android  TEXT NOT NULL DEFAULT '?',  -- Android version (14, 15, ...)
  country  TEXT NOT NULL DEFAULT '??', -- IP country (Cloudflare), level only, no city/address
  lat      REAL,                       -- approximate IP latitude, rounded to ~11 km (for the map)
  lon      REAL,                       -- approximate IP longitude, rounded to ~11 km
  city     TEXT NOT NULL DEFAULT '?',  -- IP city (Cloudflare), only shown as a per-city count, never per user
  PRIMARY KEY (day, id)
);
CREATE INDEX IF NOT EXISTS pings_day ON pings(day);

-- Panel settings (currently just the admin password the owner sets in the panel; PBKDF2, never plain).
CREATE TABLE IF NOT EXISTS settings (
  key   TEXT PRIMARY KEY,
  value TEXT NOT NULL
);
