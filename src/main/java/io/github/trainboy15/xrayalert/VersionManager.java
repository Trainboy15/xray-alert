package io.github.trainboy15.xrayalert;

import org.bukkit.Bukkit;
import org.bukkit.Material;

import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages Minecraft version detection and provides version-specific ore lists.
 * Supports Minecraft versions 1.8 through 1.21.
 */
public class VersionManager {

    private final String serverVersion;
    private final MinecraftVersion detectedVersion;

    public VersionManager() {
        this.serverVersion = Bukkit.getServer().getVersion();
        this.detectedVersion = parseVersion(serverVersion);
    }

    /**
     * Parses the server version string to extract the major and minor version numbers.
     *
     * @param versionString the server version string (e.g., "git-Spigot-1234567-1.18.2")
     * @return a MinecraftVersion object representing the detected version
     */
    private MinecraftVersion parseVersion(String versionString) {
        // Extract version from string like "git-Spigot-xxxxx-1.18.2"
        Pattern pattern = Pattern.compile("(\\d+)\\.(\\d+)(?:\\.(\\d+))?");
        Matcher matcher = pattern.matcher(versionString);

        if (matcher.find()) {
            int major = Integer.parseInt(matcher.group(1));
            int minor = Integer.parseInt(matcher.group(2));
            int patch = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;
            return new MinecraftVersion(major, minor, patch);
        }

        // Default to 1.18.2 if parsing fails
        return new MinecraftVersion(1, 18, 2);
    }

    /**
     * Gets the set of ore materials that should be monitored for this server version.
     *
     * @return a Set of Material objects representing ores to monitor
     */
    public Set<Material> getMonitoredOres() {
        int major = detectedVersion.major;
        int minor = detectedVersion.minor;

        if (major < 1 || (major == 1 && minor < 8)) {
            // Not supported
            return EnumSet.noneOf(Material.class);
        } else if (major == 1 && minor < 13) {
            // MC 1.8 - 1.12: Pre-flattening
            return getOres_1_8_to_1_12();
        } else if (major == 1 && minor == 13) {
            // MC 1.13: Flattening update
            return getOres_1_13();
        } else if (major == 1 && minor < 17) {
            // MC 1.14 - 1.16
            return getOres_1_14_to_1_16();
        } else if (major == 1 && minor == 17) {
            // MC 1.17: Caves & Cliffs Part 1 (copper ore)
            return getOres_1_17();
        } else if (major == 1 && minor == 18) {
            // MC 1.18+: Caves & Cliffs Part 2 (deepslate ores)
            return getOres_1_18_plus();
        } else if (major > 1 || (major == 1 && minor > 18)) {
            // MC 1.19 - 1.21 and beyond: Use 1.18+ ore list
            return getOres_1_18_plus();
        }

        return EnumSet.noneOf(Material.class);
    }

    /**
     * Minecraft 1.8 - 1.12: Pre-flattening era
     */
    private Set<Material> getOres_1_8_to_1_12() {
        Set<Material> ores = EnumSet.of(
                Material.COAL_ORE,
                Material.IRON_ORE,
                Material.GOLD_ORE,
                Material.DIAMOND_ORE,
                Material.EMERALD_ORE,
                Material.LAPIS_ORE,
                Material.REDSTONE_ORE
        );

        // Quartz ore was named QUARTZ_ORE before flattening and NETHER_QUARTZ_ORE after.
        addIfPresent(ores, "QUARTZ_ORE", "NETHER_QUARTZ_ORE");
        return ores;
    }

    private void addIfPresent(Set<Material> target, String... names) {
        for (String name : names) {
            Material material = Material.getMaterial(name);
            if (material != null) {
                target.add(material);
                return;
            }
        }
    }

    /**
     * Minecraft 1.13: The Flattening (block data restructure)
     */
    private Set<Material> getOres_1_13() {
        return EnumSet.of(
                Material.COAL_ORE,
                Material.IRON_ORE,
                Material.GOLD_ORE,
                Material.DIAMOND_ORE,
                Material.EMERALD_ORE,
                Material.LAPIS_ORE,
                Material.REDSTONE_ORE,
                Material.NETHER_QUARTZ_ORE
        );
    }

    /**
     * Minecraft 1.14 - 1.16: Pre-Caves & Cliffs
     */
    private Set<Material> getOres_1_14_to_1_16() {
        return EnumSet.of(
                Material.COAL_ORE,
                Material.IRON_ORE,
                Material.GOLD_ORE,
                Material.DIAMOND_ORE,
                Material.EMERALD_ORE,
                Material.LAPIS_ORE,
                Material.REDSTONE_ORE,
                Material.NETHER_QUARTZ_ORE,
                Material.NETHER_GOLD_ORE  // Added in 1.16
        );
    }

    /**
     * Minecraft 1.17: Caves & Cliffs Part 1 (adds copper ore)
     */
    private Set<Material> getOres_1_17() {
        Set<Material> ores = getOres_1_14_to_1_16();
        ores.add(Material.COPPER_ORE);
        return ores;
    }

    /**
     * Minecraft 1.18+: Caves & Cliffs Part 2 (deepslate ores)
     */
    private Set<Material> getOres_1_18_plus() {
        return EnumSet.of(
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
    }

    /**
     * Returns the detected Minecraft version.
     */
    public MinecraftVersion getDetectedVersion() {
        return detectedVersion;
    }

    /**
     * Returns the full server version string.
     */
    public String getServerVersion() {
        return serverVersion;
    }

    /**
     * Checks if the server version is at least the specified major and minor version.
     *
     * @param major the major version number
     * @param minor the minor version number
     * @return true if the server version is >= specified version
     */
    public boolean isVersionAtLeast(int major, int minor) {
        if (detectedVersion.major > major) {
            return true;
        }
        if (detectedVersion.major == major) {
            return detectedVersion.minor >= minor;
        }
        return false;
    }

    /**
     * Internal class to represent a Minecraft version.
     */
    public static class MinecraftVersion {
        public final int major;
        public final int minor;
        public final int patch;

        public MinecraftVersion(int major, int minor, int patch) {
            this.major = major;
            this.minor = minor;
            this.patch = patch;
        }

        @Override
        public String toString() {
            if (patch == 0) {
                return String.format("%d.%d", major, minor);
            }
            return String.format("%d.%d.%d", major, minor, patch);
        }
    }
}
