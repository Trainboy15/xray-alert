# xray-alert

A Minecraft **1.18.2** Spigot plugin that alerts staff members when a player mines an unusually large number of ores within a configurable time window — a common sign of X-Ray usage.

---

## Features

- Monitors all 1.18.2 ores (diamond, deepslate diamond, ancient debris, emerald, gold, iron, coal, copper, lapis, redstone, nether gold, nether quartz, and their deepslate variants)
- Per-ore configurable thresholds and a global time window
- Alerts all online players with the `xrayalerts.alert` permission
- Per-player, per-ore alert cooldown to avoid spam
- Bypass permission for trusted players
- Fully customisable alert message with placeholders
- `/xrayalert reload` command to hot-reload the configuration

---

## Permissions

| Permission           | Default | Description |
|----------------------|---------|-------------|
| `xrayalerts.alert`   | op      | Receive xray alerts |
| `xrayalerts.admin`   | op      | Use `/xrayalert` commands |
| `xrayalerts.bypass`  | false   | Skip xray monitoring entirely |

---

## Commands

| Command               | Description |
|-----------------------|-------------|
| `/xrayalert reload`   | Reload `config.yml` and reset tracked data |

---

## Configuration (`config.yml`)

```yaml
# Sliding time window in seconds
time-window: 60

# Minimum seconds between repeated alerts for the same player + ore
alert-cooldown: 30

# Alert message (supports & colour codes)
# Placeholders: {player}, {ore}, {count}, {threshold}, {window}
alert-message: "&c[XrayAlert] &f{player} &cmined &f{count}x {ore} &cin &f{window}s &c(threshold: &f{threshold}&c)"

thresholds:
  DIAMOND_ORE: 5
  DEEPSLATE_DIAMOND_ORE: 5
  ANCIENT_DEBRIS: 3
  # ... (all ores are listed in the default config)
```

---

## Building

Requirements: **JDK 17**, **Maven 3**, Spigot API 1.18.2 (install via [BuildTools](https://www.spigotmc.org/wiki/buildtools/)).

```bash
mvn clean package
```

The compiled jar will be in `target/xray-alert-1.0.0.jar`.

---

## Installation

1. Drop the jar into your server's `plugins/` folder.
2. Start or restart the server — `plugins/XrayAlert/config.yml` is created automatically.
3. Edit `config.yml` to suit your thresholds, then run `/xrayalert reload`.
