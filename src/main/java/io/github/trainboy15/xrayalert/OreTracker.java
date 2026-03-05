package io.github.trainboy15.xrayalert;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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

        /** player UUID → ore type → vein block key → timestamp (ms) of when that vein was counted */
        private final Map<UUID, Map<Material, Map<BlockKey, Long>>> countedVeinBlocks = new HashMap<>();

        private static final BlockFace[] ADJACENT_FACES = new BlockFace[] {
            BlockFace.UP,
            BlockFace.DOWN,
            BlockFace.NORTH,
            BlockFace.SOUTH,
            BlockFace.EAST,
            BlockFace.WEST
        };

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
        return recordEvent(player, material);
    }

    /**
     * Records an ore vein event (rather than individual block breaks) for the given player.
     * Multiple blocks from the same connected vein are counted as a single event.
     *
     * @param player      the player who mined the ore
     * @param brokenBlock the broken block from the event
     * @param material    the ore {@link Material} that was broken
     * @return the number of veins this player has mined for this ore within the current window,
     *         or {@code -1} if this ore type is not monitored
     */
    public int recordVeinBreak(Player player, Block brokenBlock, Material material) {
        int threshold = getThreshold(material);
        if (threshold < 0) {
            return -1;
        }

        long now = System.currentTimeMillis();
        Set<BlockKey> veinBlocks = collectConnectedVein(brokenBlock, material);

        long cacheMillis = plugin.getConfig().getLong("vein-cache-seconds", 300) * 1000L;
        Map<BlockKey, Long> seenBlocks = countedVeinBlocks
                .computeIfAbsent(player.getUniqueId(), k -> new EnumMap<>(Material.class))
                .computeIfAbsent(material, k -> new HashMap<>());

        seenBlocks.entrySet().removeIf(entry -> (now - entry.getValue()) > cacheMillis);

        boolean alreadyCounted = veinBlocks.stream().anyMatch(seenBlocks::containsKey);
        if (alreadyCounted) {
            return getCount(player, material);
        }

        for (BlockKey key : veinBlocks) {
            seenBlocks.put(key, now);
        }

        return recordEvent(player, material);
    }

    private int recordEvent(Player player, Material material) {
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

    private Set<BlockKey> collectConnectedVein(Block start, Material material) {
        Set<BlockKey> vein = new HashSet<>();
        if (start == null) {
            return vein;
        }

        int maxVeinScanBlocks = plugin.getConfig().getInt("max-vein-scan-blocks", 128);
        maxVeinScanBlocks = Math.max(1, maxVeinScanBlocks);

        Set<BlockKey> visited = new HashSet<>();
        Deque<Block> queue = new ArrayDeque<>();

        BlockKey startKey = new BlockKey(start);
        visited.add(startKey);
        vein.add(startKey);

        // On some server versions, MONITOR handlers may observe the broken block as air.
        // Seed BFS from neighboring blocks of the same ore material.
        for (BlockFace face : ADJACENT_FACES) {
            Block adjacent = start.getRelative(face);
            if (adjacent.getType() == material) {
                queue.add(adjacent);
            }
        }

        while (!queue.isEmpty() && vein.size() < maxVeinScanBlocks) {
            Block block = queue.removeFirst();
            BlockKey key = new BlockKey(block);
            if (!visited.add(key)) {
                continue;
            }
            if (block.getType() != material) {
                continue;
            }

            vein.add(key);
            for (BlockFace face : ADJACENT_FACES) {
                Block adjacent = block.getRelative(face);
                BlockKey adjacentKey = new BlockKey(adjacent);
                if (!visited.contains(adjacentKey)) {
                    queue.addLast(adjacent);
                }
            }
        }

        return vein;
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
        countedVeinBlocks.clear();
    }

    private static final class BlockKey {
        private final UUID worldId;
        private final int x;
        private final int y;
        private final int z;

        private BlockKey(Block block) {
            this.worldId = block.getWorld().getUID();
            this.x = block.getX();
            this.y = block.getY();
            this.z = block.getZ();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            BlockKey blockKey = (BlockKey) o;
            return x == blockKey.x && y == blockKey.y && z == blockKey.z && Objects.equals(worldId, blockKey.worldId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(worldId, x, y, z);
        }
    }
}
