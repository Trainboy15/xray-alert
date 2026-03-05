package io.github.trainboy15.xrayalert;

import io.github.trainboy15.xrayalert.commands.XrayAlertCommand;
import io.github.trainboy15.xrayalert.listeners.BlockBreakListener;
import org.bukkit.plugin.java.JavaPlugin;

public class XrayAlert extends JavaPlugin {

    private OreTracker oreTracker;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        oreTracker = new OreTracker(this);

        getServer().getPluginManager().registerEvents(new BlockBreakListener(this), this);

        XrayAlertCommand commandExecutor = new XrayAlertCommand(this);
        getCommand("xrayalert").setExecutor(commandExecutor);
        getCommand("xrayalert").setTabCompleter(commandExecutor);

        getLogger().info("XrayAlert v" + getDescription().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("XrayAlert disabled.");
    }

    public OreTracker getOreTracker() {
        return oreTracker;
    }
}
