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
  PRIMARY KEY (day, id)
);
CREATE INDEX IF NOT EXISTS pings_day ON pings(day);
