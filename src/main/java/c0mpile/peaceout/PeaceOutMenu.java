package c0mpile.peaceout;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class PeaceOutMenu implements Listener {

    private static final String PERSONAL_TITLE =
            ChatColor.DARK_AQUA + "PeaceOut Settings";

    private static final String ADMIN_TITLE =
            ChatColor.DARK_RED + "PeaceOut Admin";

    private static final String ADMIN_PLAYER_PREFIX =
            ChatColor.DARK_RED + "Player: ";

    private static final String MULTIPLIER_TITLE_PREFIX =
            ChatColor.DARK_PURPLE + "Choose ";

    private static final List<String> PERSONAL_SETTINGS = List.of(
            "targeting",
            "hunger",
            "regeneration",
            "fall",
            "durability",
            "fireworks",
            "vein-miner",
            "tree-chopper",
            "keep-inventory",
            "experience-multiplier",
            "block-break-speed",
            "drop-vacuum",
            "drowning",
            "lava",
            "fire"
    );

    private static final List<String> ORDINARY_TOGGLES = List.of(
            "targeting",
            "hunger",
            "regeneration",
            "fall",
            "durability",
            "fireworks",
            "keep-inventory",
            "drop-vacuum",
            "drowning",
            "lava",
            "fire"
    );

    private static final List<Double> MULTIPLIER_VALUES = List.of(
            0.25,
            0.50,
            0.75,
            1.00,
            1.50,
            2.00,
            3.00,
            5.00,
            10.00
    );

    private final PeaceOut plugin;

    public PeaceOutMenu(PeaceOut plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void openPersonalMenu(Player player) {
        PlayerSettings settings = plugin.getSettings(player);

        Inventory inventory = Bukkit.createInventory(
                null,
                54,
                PERSONAL_TITLE
        );

        fillBackground(inventory);

        for (int index = 0; index < PERSONAL_SETTINGS.size(); index++) {
            String key = PERSONAL_SETTINGS.get(index);

            if (key.equals("experience-multiplier")
                    || key.equals("block-break-speed")) {
                inventory.setItem(
                        index,
                        createMultiplierItem(player, settings, key)
                );
            } else {
                inventory.setItem(
                        index,
                        createToggleItem(player, settings, key)
                );
            }
        }

        inventory.setItem(
                45,
                createMasterSwitchItem(settings)
        );

        inventory.setItem(
                49,
                createItem(
                        Material.BOOK,
                        ChatColor.GOLD + "PeaceOut Status",
                        List.of(
                                ChatColor.GRAY + "Master switch: "
                                        + status(
                                        settings.isMasterEnabled()
                                ),
                                ChatColor.GRAY + "Click to view status in chat."
                        )
                )
        );

        inventory.setItem(
                53,
                createItem(
                        Material.BARRIER,
                        ChatColor.RED + "Close",
                        List.of(ChatColor.GRAY + "Close this menu.")
                )
        );

        player.openInventory(inventory);
    }

    public void openAdminMenu(Player admin) {
        Inventory inventory = Bukkit.createInventory(
                null,
                54,
                ADMIN_TITLE
        );

        fillBackground(inventory);

        List<Map.Entry<UUID, String>> recordedPlayers =
                getRecordedPlayers().entrySet().stream()
                        .sorted(
                                Map.Entry.comparingByValue(
                                        String.CASE_INSENSITIVE_ORDER
                                )
                        )
                        .toList();

        int slot = 0;

        for (Map.Entry<UUID, String> entry : recordedPlayers) {
            if (slot >= 45) {
                break;
            }

            UUID uuid = entry.getKey();
            String name = entry.getValue();

            OfflinePlayer offlinePlayer =
                    Bukkit.getOfflinePlayer(uuid);

            String displayName = offlinePlayer.getName() != null
                    ? offlinePlayer.getName()
                    : name;

            inventory.setItem(
                    slot++,
                    createItem(
                            Material.PLAYER_HEAD,
                            ChatColor.YELLOW + displayName,
                            List.of(
                                    ChatColor.GRAY + "UUID:",
                                    ChatColor.DARK_GRAY + uuid.toString(),
                                    "",
                                    ChatColor.GREEN
                                            + "Click to edit settings."
                            )
                    )
            );
        }

        inventory.setItem(
                49,
                createItem(
                        Material.BARRIER,
                        ChatColor.RED + "Close",
                        List.of(ChatColor.GRAY + "Close this menu.")
                )
        );

        admin.openInventory(inventory);
    }

    private void openAdminPlayerMenu(
            Player admin,
            UUID targetUuid
    ) {
        PlayerSettings targetSettings =
                plugin.getSettings(targetUuid);

        String targetName = targetSettings.getName();

        Inventory inventory = Bukkit.createInventory(
                null,
                54,
                ADMIN_PLAYER_PREFIX + targetName
        );

        fillBackground(inventory);

        for (int index = 0; index < PERSONAL_SETTINGS.size(); index++) {
            String key = PERSONAL_SETTINGS.get(index);

            if (key.equals("experience-multiplier")
                    || key.equals("block-break-speed")) {
                inventory.setItem(
                        index,
                        createMultiplierItem(
                                targetSettings,
                                key
                        )
                );
            } else {
                inventory.setItem(
                        index,
                        createToggleItem(
                                targetSettings,
                                key
                        )
                );
            }
        }

        inventory.setItem(
                45,
                createMasterSwitchItem(targetSettings)
        );

        inventory.setItem(
                48,
                createItem(
                        Material.ARROW,
                        ChatColor.YELLOW + "Back",
                        List.of(ChatColor.GRAY + "Return to player list.")
                )
        );

        inventory.setItem(
                53,
                createItem(
                        Material.BARRIER,
                        ChatColor.RED + "Close",
                        List.of(ChatColor.GRAY + "Close this menu.")
                )
        );

        admin.openInventory(inventory);
    }

    private void openMultiplierMenu(
            Player viewer,
            UUID targetUuid,
            String key,
            boolean adminMenu
    ) {
        PlayerSettings settings =
                plugin.getSettings(targetUuid);

        String name = key.equals("experience-multiplier")
                ? "Experience multiplier"
                : "Block-break speed";

        Inventory inventory = Bukkit.createInventory(
                null,
                27,
                MULTIPLIER_TITLE_PREFIX + name
        );

        fillBackground(inventory);

        for (int index = 0; index < MULTIPLIER_VALUES.size(); index++) {
            double value = MULTIPLIER_VALUES.get(index);

            ChatColor color = nearlyEqual(
                    settings.getMultiplier(key),
                    value
            )
                    ? ChatColor.GREEN
                    : ChatColor.YELLOW;

            inventory.setItem(
                    index + 9,
                    createItem(
                            Material.COMPARATOR,
                            color + formatMultiplier(value),
                            List.of(
                                    ChatColor.GRAY + "Click to select."
                            )
                    )
            );
        }

        inventory.setItem(
                22,
                createItem(
                        Material.ARROW,
                        ChatColor.YELLOW + "Back",
                        List.of(ChatColor.GRAY + "Return to settings.")
                )
        );

        viewer.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        String title = event.getView().getTitle();

        if (!title.equals(PERSONAL_TITLE)
                && !title.equals(ADMIN_TITLE)
                && !title.startsWith(ADMIN_PLAYER_PREFIX)
                && !title.startsWith(MULTIPLIER_TITLE_PREFIX)) {
            return;
        }

        event.setCancelled(true);

        if (event.getClickedInventory() == null
                || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }

        int slot = event.getRawSlot();

        if (title.equals(PERSONAL_TITLE)) {
            handlePersonalClick(player, slot);
            return;
        }

        if (title.equals(ADMIN_TITLE)) {
            handleAdminListClick(player, slot);
            return;
        }

        if (title.startsWith(ADMIN_PLAYER_PREFIX)) {
            handleAdminPlayerClick(player, title, slot);
            return;
        }

        if (title.startsWith(MULTIPLIER_TITLE_PREFIX)) {
            handleMultiplierClick(player, title, slot);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        String title = event.getView().getTitle();

        if (title.equals(PERSONAL_TITLE)
                || title.equals(ADMIN_TITLE)
                || title.startsWith(ADMIN_PLAYER_PREFIX)
                || title.startsWith(MULTIPLIER_TITLE_PREFIX)) {
            plugin.getLogger().fine(
                    "Closed PeaceOut menu for "
                            + event.getPlayer().getName()
            );
        }
    }

    private void handlePersonalClick(Player player, int slot) {
        if (slot == 53) {
            player.closeInventory();
            return;
        }

        if (slot == 49) {
            player.closeInventory();
            sendStatus(player, plugin.getSettings(player));
            return;
        }

        if (slot == 45) {
            PlayerSettings settings = plugin.getSettings(player);
            settings.setMasterEnabled(!settings.isMasterEnabled());
            openPersonalMenu(player);
            return;
        }

        if (slot < 0 || slot >= PERSONAL_SETTINGS.size()) {
            return;
        }

        String key = PERSONAL_SETTINGS.get(slot);

        if (key.equals("experience-multiplier")
                || key.equals("block-break-speed")) {
            openMultiplierMenu(
                    player,
                    player.getUniqueId(),
                    key,
                    false
            );
            return;
        }

        toggleSetting(player, plugin.getSettings(player), key);
        openPersonalMenu(player);
    }

    private void handleAdminListClick(Player admin, int slot) {
        if (slot == 49) {
            admin.closeInventory();
            return;
        }

        if (slot < 0 || slot >= 45) {
            return;
        }

        List<Map.Entry<UUID, String>> recordedPlayers =
                getRecordedPlayers().entrySet().stream()
                        .sorted(
                                Map.Entry.comparingByValue(
                                        String.CASE_INSENSITIVE_ORDER
                                )
                        )
                        .toList();

        if (slot >= recordedPlayers.size()) {
            return;
        }

        openAdminPlayerMenu(
                admin,
                recordedPlayers.get(slot).getKey()
        );
    }

    private void handleAdminPlayerClick(
            Player admin,
            String title,
            int slot
    ) {
        if (slot == 48) {
            openAdminMenu(admin);
            return;
        }

        if (slot == 53) {
            admin.closeInventory();
            return;
        }

        UUID targetUuid = findUuidFromAdminTitle(title);

        if (targetUuid == null) {
            admin.sendMessage(
                    ChatColor.RED
                            + "Could not identify that player."
            );
            return;
        }

        PlayerSettings targetSettings =
                plugin.getSettings(targetUuid);

        if (slot == 45) {
            targetSettings.setMasterEnabled(
                    !targetSettings.isMasterEnabled()
            );
            openAdminPlayerMenu(admin, targetUuid);
            applyOnlineSettings(targetUuid);
            return;
        }

        if (slot < 0 || slot >= PERSONAL_SETTINGS.size()) {
            return;
        }

        String key = PERSONAL_SETTINGS.get(slot);

        if (key.equals("experience-multiplier")
                || key.equals("block-break-speed")) {
            openMultiplierMenu(admin, targetUuid, key, true);
            return;
        }

        targetSettings.setEnabled(
                key,
                !targetSettings.isEnabled(key)
        );

        openAdminPlayerMenu(admin, targetUuid);
        applyOnlineSettings(targetUuid);
    }

    
    private void handleMultiplierClick(
            Player viewer,
            String title,
            int slot
    ) {
        if (slot == 22) {
            viewer.closeInventory();
            openPersonalMenu(viewer);
            return;
        }

        if (slot < 9 || slot > 17) {
            return;
        }

        int valueIndex = slot - 9;

        if (valueIndex >= MULTIPLIER_VALUES.size()) {
            return;
        }

        String key = title.contains("Experience")
                ? "experience-multiplier"
                : "block-break-speed";

        UUID targetUuid = viewer.getUniqueId();

        PlayerSettings settings =
                plugin.getSettings(targetUuid);

        settings.setMultiplier(
                key,
                MULTIPLIER_VALUES.get(valueIndex)
        );

        applyOnlineSettings(targetUuid);

        viewer.sendMessage(
                ChatColor.AQUA + "[PeaceOut] "
                        + ChatColor.GREEN
                        + formatSettingName(key)
                        + " set to "
                        + formatMultiplier(
                        MULTIPLIER_VALUES.get(valueIndex)
                )
                        + "."
        );

        openPersonalMenu(viewer);
    }

    private void toggleSetting(
            Player player,
            PlayerSettings settings,
            String key
    ) {
        boolean value = !settings.isEnabled(key);
        settings.setEnabled(key, value);

        player.sendMessage(
                ChatColor.AQUA + "[PeaceOut] "
                        + ChatColor.WHITE
                        + formatSettingName(key)
                        + ChatColor.GRAY
                        + ": "
                        + (value
                        ? ChatColor.GREEN + "enabled."
                        : ChatColor.RED + "disabled.")
        );
    }

    private ItemStack createToggleItem(
            Player player,
            PlayerSettings settings,
            String key
    ) {
        return createToggleItem(settings, key);
    }

    private ItemStack createToggleItem(
            PlayerSettings settings,
            String key
    ) {
        boolean enabled = settings.isEnabled(key);

        Material material = enabled
                ? Material.LIME_DYE
                : Material.GRAY_DYE;

        return createItem(
                material,
                (enabled ? ChatColor.GREEN : ChatColor.RED)
                        + formatSettingName(key),
                List.of(
                        ChatColor.GRAY + "Status: "
                                + (enabled
                                ? ChatColor.GREEN + "Enabled"
                                : ChatColor.RED + "Disabled"),
                        "",
                        ChatColor.YELLOW + "Click to toggle."
                )
        );
    }

    private ItemStack createMultiplierItem(
            Player player,
            PlayerSettings settings,
            String key
    ) {
        return createMultiplierItem(settings, key);
    }

    private ItemStack createMultiplierItem(
            PlayerSettings settings,
            String key
    ) {
        return createItem(
                Material.COMPARATOR,
                ChatColor.GOLD + formatSettingName(key),
                List.of(
                        ChatColor.GRAY + "Current: "
                                + ChatColor.WHITE
                                + formatMultiplier(
                                settings.getMultiplier(key)
                        ),
                        "",
                        ChatColor.YELLOW
                                + "Click to choose a value."
                )
        );
    }

    private ItemStack createMasterSwitchItem(
            PlayerSettings settings
    ) {
        boolean enabled = settings.isMasterEnabled();

        return createItem(
                enabled ? Material.EMERALD : Material.REDSTONE,
                ChatColor.GOLD + "Master switch",
                List.of(
                        ChatColor.GRAY + "Status: "
                                + (enabled
                                ? ChatColor.GREEN + "Enabled"
                                : ChatColor.RED + "Disabled"),
                        "",
                        ChatColor.YELLOW + "Click to toggle.",
                        ChatColor.DARK_GRAY
                                + "Does not affect XP, block speed,",
                        ChatColor.DARK_GRAY
                                + "vein miner, or tree chopper."
                )
        );
    }

    private ItemStack createItem(
            Material material,
            String name,
            List<String> lore
    ) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    private void fillBackground(Inventory inventory) {
        ItemStack filler = createItem(
                Material.BLACK_STAINED_GLASS_PANE,
                " ",
                List.of()
        );

        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }
    }

    private Map<UUID, String> getRecordedPlayers() {
        Map<UUID, String> players = new java.util.LinkedHashMap<>();

        if (plugin.getConfig().getConfigurationSection("players")
                == null) {
            return players;
        }

        for (String key : plugin.getConfig()
                .getConfigurationSection("players")
                .getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);

                String name = plugin.getConfig().getString(
                        "players." + key + ".name",
                        uuid.toString()
                );

                players.put(uuid, name);
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning(
                        "Ignoring invalid UUID in config.yml: " + key
                );
            }
        }

        return players;
    }

    private UUID findUuidFromAdminTitle(String title) {
        String name = ChatColor.stripColor(title)
                .replace("Player: ", "")
                .trim();

        for (Map.Entry<UUID, String> entry : getRecordedPlayers()
                .entrySet()) {
            if (entry.getValue().equalsIgnoreCase(name)) {
                return entry.getKey();
            }
        }

        return null;
    }

    private void applyOnlineSettings(UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);

        if (online != null && online.isOnline()) {
            plugin.getListener().applyBlockSpeedModifier(online);
        }
    }

    private void sendStatus(
            Player player,
            PlayerSettings settings
    ) {
        player.sendMessage(
                ChatColor.AQUA + "[PeaceOut] "
                        + ChatColor.GOLD
                        + "Current settings"
        );

        player.sendMessage(
                ChatColor.GRAY + "Master switch: "
                        + status(settings.isMasterEnabled())
        );

        for (String key : PERSONAL_SETTINGS) {
            if (key.equals("experience-multiplier")
                    || key.equals("block-break-speed")) {
                player.sendMessage(
                        ChatColor.GRAY
                                + formatSettingName(key)
                                + ": "
                                + ChatColor.WHITE
                                + formatMultiplier(
                                settings.getMultiplier(key)
                        )
                );
            } else {
                player.sendMessage(
                        ChatColor.GRAY
                                + formatSettingName(key)
                                + ": "
                                + status(settings.isEnabled(key))
                );
            }
        }
    }

    private String status(boolean enabled) {
        return enabled
                ? ChatColor.GREEN + "ON"
                : ChatColor.RED + "OFF";
    }

    private String formatMultiplier(double value) {
        return String.format(Locale.US, "%.2fx", value);
    }

    private boolean nearlyEqual(double first, double second) {
        return Math.abs(first - second) < 0.001;
    }

    private String formatSettingName(String key) {
        return switch (key) {
            case "targeting" -> "Maximum entity targets";
            case "hunger" -> "No food drain";
            case "regeneration" -> "Increased health regeneration";
            case "fall" -> "No fall damage";
            case "durability" -> "Infinite durability";
            case "fireworks" -> "Infinite fireworks";
            case "vein-miner" -> "Instant vein miner";
            case "tree-chopper" -> "Instant tree chopper";
            case "keep-inventory" -> "Keep inventory on death";
            case "experience-multiplier" -> "Experience multiplier";
            case "block-break-speed" -> "Block break speed";
            case "drop-vacuum" -> "Drop vacuum";
            case "drowning" -> "Drowning protection";
            case "lava" -> "Lava protection";
            case "fire" -> "Fire protection";
            default -> key;
        };
    }
}
