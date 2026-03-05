package io.github.trainboy15.xrayalert;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks how many of each ore type each player has mined within a sliding time window
 * and manages per-ore alert cooldowns.
 */
public class OreTracker {

    private final XrayAlert plugin;

    /** player UUID → ore type → timestamps (ms) of recent breaks within the window */
    private final Map<UUID, Map<Material, Deque<Long>>> breakTimes = new HashMap<>();

    /** player UUID → ore type → timestamp (ms) of the last alert sent for that ore */
    private final Map<UUID, Map<Material, Long>> lastAlertTime = new HashMap<>();

    public OreTracker(XrayAlert plugin) {
        this.plugin = plugin;
    }

    /**
     * Records a single ore break for the given player and ore type.
     *
     * @param player   the player who mined the ore
     * @param material the ore {@link Material} that was broken
     * @return the number of times this player has mined this ore within the current window,
     *         or {@code -1} if this ore type is not monitored
     */
    public int recordBreak(Player player, Material material) {
        int threshold = getThreshold(material);
        if (threshold < 0) {
            return -1;
        }

        long now = System.currentTimeMillis();
        long windowMillis = plugin.getConfig().getLong("time-window", 60) * 1000L;

        Deque<Long> times = breakTimes
                .computeIfAbsent(player.getUniqueId(), k -> new EnumMap<>(Material.class))
                .computeIfAbsent(material, k -> new ArrayDeque<>());

        // Evict entries older than the window
        times.removeIf(t -> (now - t) > windowMillis);

        times.addLast(now);
        return times.size();
    }

    /**
     * Returns {@code true} if an alert should be sent for this player and ore type right now.
     * An alert is eligible when the mined count within the window has reached or exceeded the
     * threshold AND the per-player, per-ore cooldown has elapsed since the last alert.
     *
     * @param player   the player
     * @param material the ore type
     * @param count    the current count returned by {@link #recordBreak}
     * @return whether an alert should be dispatched
     */
    public boolean shouldAlert(Player player, Material material, int count) {
        int threshold = getThreshold(material);
        if (threshold < 0 || count < threshold) {
            return false;
        }

        long now = System.currentTimeMillis();
        long cooldownMillis = plugin.getConfig().getLong("alert-cooldown", 30) * 1000L;

        Map<Material, Long> playerCooldowns = lastAlertTime
                .computeIfAbsent(player.getUniqueId(), k -> new EnumMap<>(Material.class));

        Long lastAlert = playerCooldowns.get(material);
        if (lastAlert != null && (now - lastAlert) < cooldownMillis) {
            return false;
        }

        playerCooldowns.put(material, now);
        return true;
    }

    /**
     * Returns the configured alert threshold for the given ore material,
     * or {@code -1} if the ore is not configured or monitoring is disabled for it.
     */
    public int getThreshold(Material material) {
        return plugin.getConfig().getInt("thresholds." + material.name(), -1);
    }

    /**
     * Returns the current break count for the given player and material within the active
     * time window without recording a new break.  Useful for testing/inspection.
     */
    public int getCount(Player player, Material material) {
        long now = System.currentTimeMillis();
        long windowMillis = plugin.getConfig().getLong("time-window", 60) * 1000L;

        Map<Material, Deque<Long>> playerMap = breakTimes.get(player.getUniqueId());
        if (playerMap == null) {
            return 0;
        }
        Deque<Long> times = playerMap.get(material);
        if (times == null) {
            return 0;
        }
        times.removeIf(t -> (now - t) > windowMillis);
        return times.size();
    }

    /** Clears all tracked data (called on config reload so stale data does not persist). */
    public void reset() {
        breakTimes.clear();
        lastAlertTime.clear();
    }
}
