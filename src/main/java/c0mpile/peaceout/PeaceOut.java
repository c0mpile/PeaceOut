package c0mpile.peaceout;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PeaceOut extends JavaPlugin {

    public static final int MAX_STACK_SIZE = 99;

    public static void applyMaxStackSize(ItemStack stack) {
        if (stack == null
                || stack.getType().isAir()
                || stack.getType().getMaxStackSize() <= 1) {
            return;
        }

        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return;
        }

        if (!meta.hasMaxStackSize() || meta.getMaxStackSize() != MAX_STACK_SIZE) {
            meta.setMaxStackSize(MAX_STACK_SIZE);
            stack.setItemMeta(meta);
        }
    }

    private final Map<UUID, PlayerSettings> settings =
            new HashMap<>();

    private PeaceOutMenu menu;
    private PeaceOutListener listener;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        menu = new PeaceOutMenu(this);
        listener = new PeaceOutListener(this);

        getServer()
                .getPluginManager()
                .registerEvents(listener, this);

        PeaceOutCommand command =
                new PeaceOutCommand(this);

        if (getCommand("peaceout") == null
                || getCommand("trash") == null
                || getCommand("bp") == null) {
            getLogger().severe(
                    "PeaceOut commands are missing from plugin.yml."
            );

            getServer()
                    .getPluginManager()
                    .disablePlugin(this);

            return;
        }

        getCommand("peaceout")
                .setExecutor(command);

        getCommand("peaceout")
                .setTabCompleter(command);

        getCommand("trash")
                .setExecutor(command);

        getCommand("trash")
                .setTabCompleter(command);

        getCommand("bp")
                .setExecutor(command);

        getCommand("bp")
                .setTabCompleter(command);

        for (Player player :
                getServer().getOnlinePlayers()) {
            getSettings(player);
        }

        getLogger().info("PeaceOut enabled.");
    }

    @Override
    public void onDisable() {
        if (listener != null) {
            listener.stop();

            for (Player player :
                    getServer().getOnlinePlayers()) {
                listener.removeBlockSpeedModifier(player);
            }
        }

        saveConfig();
        getLogger().info("PeaceOut disabled.");
    }

    public PlayerSettings getSettings(Player player) {
        return getSettings(
                player.getUniqueId(),
                player
        );
    }

    public PlayerSettings getSettings(
            UUID uuid,
            Player player
    ) {
        PlayerSettings result =
                settings.computeIfAbsent(
                        uuid,
                        key -> new PlayerSettings(this, key)
                );

        result.initialize(player);
        return result;
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
