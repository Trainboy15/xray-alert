# xray-alert

A Spigot plugin that alerts staff members when a player mines an unusually large number of ores (or ore veins) within a configurable time window, helping detect possible X-Ray usage.

---

## Features

- Supports Minecraft **1.8 through 1.21+** with version-aware monitored ore lists
- Monitors major overworld and nether ores for each supported version (including deepslate variants where available)
- Vein-aware tracking: connected ore blocks from the same vein are counted once
- Per-ore configurable thresholds and a global sliding time window
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

# Vein cache window in seconds (how long counted vein blocks remain cached)
vein-cache-seconds: 300

# Safety cap for connected-block vein scanning
max-vein-scan-blocks: 128

# Alert message (supports & colour codes)
# Placeholders: {player}, {ore}, {count}, {threshold}, {window}
alert-message: "&c[XrayAlert] &f{player} &cmined &f{count}x {ore} &cin &f{window}s &c(threshold: &f{threshold}&c)"

thresholds:
  DIAMOND_ORE: 5
  DEEPSLATE_DIAMOND_ORE: 5
  ANCIENT_DEBRIS: 3
  # Set any ore to -1 to disable monitoring for that ore
  # ... (all ores are listed in the default config)
```

---

## Building

Requirements: **JDK 17**, **Maven 3**, and Spigot API 1.18.2 in your local Maven repository (install via [BuildTools](https://www.spigotmc.org/wiki/buildtools/)).

```bash
mvn clean package
```

The compiled jar will be in `target/xray-alert-1.0.0.jar`.

---

## Installation

1. Drop the jar into your server's `plugins/` folder.
2. Start or restart the server — `plugins/XrayAlert/config.yml` is created automatically.
3. Edit `config.yml` to suit your thresholds, then run `/xrayalert reload`.
