package io.github.trainboy15.xrayalert.listeners;

import io.github.trainboy15.xrayalert.OreTracker;
import io.github.trainboy15.xrayalert.XrayAlert;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.EnumSet;
import java.util.Set;

public class BlockBreakListener implements Listener {

    /** All ore materials present in Minecraft 1.18.2 that this plugin monitors. */
    private static final Set<Material> ORES = EnumSet.of(
            Material.COAL_ORE,
            Material.DEEPSLATE_COAL_ORE,
            Material.IRON_ORE,
            Material.DEEPSLATE_IRON_ORE,
            Material.COPPER_ORE,
            Material.DEEPSLATE_COPPER_ORE,
            Material.GOLD_ORE,
            Material.DEEPSLATE_GOLD_ORE,
            Material.REDSTONE_ORE,
            Material.DEEPSLATE_REDSTONE_ORE,
            Material.EMERALD_ORE,
            Material.DEEPSLATE_EMERALD_ORE,
            Material.LAPIS_ORE,
            Material.DEEPSLATE_LAPIS_ORE,
            Material.DIAMOND_ORE,
            Material.DEEPSLATE_DIAMOND_ORE,
            Material.NETHER_GOLD_ORE,
            Material.NETHER_QUARTZ_ORE,
            Material.ANCIENT_DEBRIS
    );

    private final XrayAlert plugin;

    public BlockBreakListener(XrayAlert plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Material material = event.getBlock().getType();
        if (!ORES.contains(material)) {
            return;
        }

        Player player = event.getPlayer();

        // Players with the bypass permission are not monitored
        if (player.hasPermission("xrayalerts.bypass")) {
            return;
        }

        OreTracker tracker = plugin.getOreTracker();
        int count = tracker.recordBreak(player, material);

        if (count < 0) {
            // Ore type is not configured / disabled
            return;
        }

        if (tracker.shouldAlert(player, material, count)) {
            sendAlert(player, material, count, tracker.getThreshold(material));
        }
    }

    private void sendAlert(Player miner, Material material, int count, int threshold) {
        String raw = plugin.getConfig().getString(
                "alert-message",
                "&c[XrayAlert] &f{player} &cmined &f{count}x {ore} &cin &f{window}s &c(threshold: &f{threshold}&c)"
        );

        long window = plugin.getConfig().getLong("time-window", 60);

        String message = ChatColor.translateAlternateColorCodes('&', raw
                .replace("{player}", miner.getName())
                .replace("{ore}", material.name())
                .replace("{count}", String.valueOf(count))
                .replace("{threshold}", String.valueOf(threshold))
                .replace("{window}", String.valueOf(window))
        );

        plugin.getServer().getOnlinePlayers().stream()
                .filter(p -> p.hasPermission("xrayalerts.alert"))
                .forEach(p -> p.sendMessage(message));

        plugin.getLogger().info(ChatColor.stripColor(message));
    }
}
