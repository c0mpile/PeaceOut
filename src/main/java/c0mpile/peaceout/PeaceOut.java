package c0mpile.peaceout;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PeaceOut extends JavaPlugin {

    private final Map<UUID, PlayerSettings> settings = new HashMap<>();

    private PeaceOutMenu menu;
    private PeaceOutListener listener;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        menu = new PeaceOutMenu(this);
        listener = new PeaceOutListener(this);

        getServer().getPluginManager().registerEvents(listener, this);

        PeaceOutCommand command = new PeaceOutCommand(this);

        if (getCommand("peaceout") == null) {
            getLogger().severe(
                    "The peaceout command is missing from plugin.yml."
            );
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getCommand("peaceout").setExecutor(command);
        getCommand("peaceout").setTabCompleter(command);

        for (Player player : getServer().getOnlinePlayers()) {
            getSettings(player);
        }

        getLogger().info("PeaceOut enabled.");
    }

    @Override
    public void onDisable() {
        for (Player player : getServer().getOnlinePlayers()) {
            listener.removeBlockSpeedModifier(player);
        }

        saveConfig();
        getLogger().info("PeaceOut disabled.");
    }

    public PlayerSettings getSettings(Player player) {
        return getSettings(player.getUniqueId(), player);
    }

    public PlayerSettings getSettings(UUID uuid, Player player) {
        PlayerSettings playerSettings = settings.computeIfAbsent(
                uuid,
                key -> new PlayerSettings(this, key)
        );

        playerSettings.initialize(player);
        return playerSettings;
    }

    public PlayerSettings getSettings(UUID uuid) {
        return settings.computeIfAbsent(
                uuid,
                key -> new PlayerSettings(this, key)
        );
    }

    public PeaceOutMenu getMenu() {
        return menu;
    }

    public PeaceOutListener getListener() {
        return listener;
    }
}
