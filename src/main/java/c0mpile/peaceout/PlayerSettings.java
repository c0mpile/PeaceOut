package c0mpile.peaceout;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerSettings {

    private final PeaceOut plugin;
    private final UUID uuid;

    public PlayerSettings(PeaceOut plugin, UUID uuid) {
        this.plugin = plugin;
        this.uuid = uuid;
    }

    private String path() {
        return "players." + uuid;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void initialize(Player player) {
        String base = path();
        boolean changed = false;

        String currentName = plugin.getConfig().getString(
                base + ".name",
                ""
        );

        if (!currentName.equals(player.getName())) {
            plugin.getConfig().set(
                    base + ".name",
                    player.getName()
            );
            changed = true;
        }

        if (!plugin.getConfig().contains(base + ".enabled")) {
            plugin.getConfig().set(
                    base + ".enabled",
                    plugin.getConfig().getBoolean(
                            "default-enabled",
                            true
                    )
            );
            changed = true;
        }

        String[] booleanSettings = {
                "drowning",
                "targeting",
                "hunger",
                "regeneration",
                "fall",
                "lava",
                "fire",
                "durability",
                "fireworks",
                "keep-inventory",
                "drop-vacuum",
                "trash",
                "backpack",
                "backpack-pickup",
                "backpack-sticky"
        };

        for (String key : booleanSettings) {
            if (!plugin.getConfig().contains(base + "." + key)) {
                plugin.getConfig().set(
                        base + "." + key,
                        plugin.getConfig().getBoolean(
                                "defaults." + key,
                                false
                        )
                );
                changed = true;
            }
        }

        if (!plugin.getConfig().contains(
                base + ".experience-multiplier"
        )) {
            plugin.getConfig().set(
                    base + ".experience-multiplier",
                    plugin.getConfig().getDouble(
                            "defaults.experience-multiplier",
                            1.0
                    )
            );
            changed = true;
        }

        if (!plugin.getConfig().contains(
                base + ".block-break-speed"
        )) {
            plugin.getConfig().set(
                    base + ".block-break-speed",
                    plugin.getConfig().getDouble(
                            "defaults.block-break-speed",
                            1.0
                    )
            );
            changed = true;
        }

        if (changed) {
            plugin.saveConfig();
        }
    }

    public String getName() {
        return plugin.getConfig().getString(
                path() + ".name",
                uuid.toString()
        );
    }

    public boolean isMasterEnabled() {
        return plugin.getConfig().getBoolean(
                path() + ".enabled",
                true
        );
    }

    public void setMasterEnabled(boolean enabled) {
        plugin.getConfig().set(
                path() + ".enabled",
                enabled
        );
        plugin.saveConfig();
    }

    public boolean isEnabled(String key) {
        return plugin.getConfig().getBoolean(
                path() + "." + key,
                false
        );
    }

    public void setEnabled(String key, boolean enabled) {
        plugin.getConfig().set(
                path() + "." + key,
                enabled
        );
        plugin.saveConfig();
    }

    public boolean toggle(String key) {
        boolean enabled = !isEnabled(key);
        setEnabled(key, enabled);
        return enabled;
    }

    public double getMultiplier(String key) {
        return plugin.getConfig().getDouble(
                path() + "." + key,
                1.0
        );
    }

    public void setMultiplier(String key, double value) {
        plugin.getConfig().set(
                path() + "." + key,
                value
        );
        plugin.saveConfig();
    }

    public Map<UUID, String> getRecordedPlayers() {
        Map<UUID, String> players = new LinkedHashMap<>();

        ConfigurationSection section =
                plugin.getConfig().getConfigurationSection(
                        "players"
                );

        if (section == null) {
            return players;
        }

        for (String key : section.getKeys(false)) {
            try {
                UUID recordedUuid = UUID.fromString(key);

                players.put(
                        recordedUuid,
                        plugin.getConfig().getString(
                                "players." + key + ".name",
                                recordedUuid.toString()
                        )
                );
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning(
                        "Ignoring invalid UUID in config.yml: " + key
                );
            }
        }

        return players;
    }
}
