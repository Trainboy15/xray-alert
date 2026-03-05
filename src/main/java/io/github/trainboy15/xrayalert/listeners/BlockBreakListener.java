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

import java.util.Set;

public class BlockBreakListener implements Listener {

    private final XrayAlert plugin;
    private final Set<Material> monitoredOres;

    public BlockBreakListener(XrayAlert plugin) {
        this.plugin = plugin;
        this.monitoredOres = plugin.getVersionManager().getMonitoredOres();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Material material = event.getBlock().getType();
        if (!monitoredOres.contains(material)) {
            return;
        }

        Player player = event.getPlayer();

        // Players with the bypass permission are not monitored
        if (player.hasPermission("xrayalerts.bypass")) {
            return;
        }

        OreTracker tracker = plugin.getOreTracker();
        int count = tracker.recordVeinBreak(player, event.getBlock(), material);

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
