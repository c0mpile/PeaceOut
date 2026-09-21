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
import java.util.HashMap;
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

    private static final String TRASH_TITLE =
            ChatColor.DARK_RED + "PeaceOut Trash";

    private static final String BACKPACK_SELECTOR_TITLE =
            ChatColor.DARK_GREEN + "Choose Backpack";

    private static final String BACKPACK_TITLE_PREFIX =
            ChatColor.DARK_GREEN + "Backpack #";

    private static final List<String> PERSONAL_SETTINGS = List.of(
            "targeting",
            "hunger",
            "regeneration",
            "fall",
            "durability",
            "fireworks",
            "keep-inventory",
            "experience-multiplier",
            "block-break-speed",
            "drop-vacuum",
            "drowning",
            "lava",
            "fire",
            "trash",
            "backpack"
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

    private final Map<UUID, UUID> adminTargets =
            new HashMap<>();

    public PeaceOutMenu(PeaceOut plugin) {
        this.plugin = plugin;
        plugin.getServer()
                .getPluginManager()
                .registerEvents(this, plugin);
    }

    public void openPersonalMenu(Player player) {
        PlayerSettings settings = plugin.getSettings(player);

        Inventory inventory = Bukkit.createInventory(
                null,
                54,
                PERSONAL_TITLE
        );

        fillBackground(inventory);

        for (int index = 0;
                index < PERSONAL_SETTINGS.size();
                index++) {
            String key = PERSONAL_SETTINGS.get(index);

            if (key.equals("experience-multiplier")
                    || key.equals("block-break-speed")) {
                inventory.setItem(
                        index,
                        createMultiplierItem(settings, key)
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
                        ChatColor.GOLD
                                + "PeaceOut Status",
                        List.of(
                                ChatColor.GRAY
                                        + "Master switch: "
                                        + status(
                                        settings.isMasterEnabled()
                                ),
                                "",
                                ChatColor.YELLOW
                                        + "Click to view status."
                        )
                )
        );

        inventory.setItem(
                53,
                createItem(
                        Material.BARRIER,
                        ChatColor.RED + "Close",
                        List.of(
                                ChatColor.GRAY
                                        + "Close this menu."
                        )
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

        List<Map.Entry<UUID, String>> players =
                plugin.getSettings(admin)
                        .getRecordedPlayers()
                        .entrySet()
                        .stream()
                        .sorted(
                                Map.Entry.comparingByValue(
                                        String.CASE_INSENSITIVE_ORDER
                                )
                        )
                        .toList();

        for (int slot = 0;
                slot < Math.min(players.size(), 45);
                slot++) {
            UUID uuid = players.get(slot).getKey();
            String name = players.get(slot).getValue();

            OfflinePlayer offlinePlayer =
                    Bukkit.getOfflinePlayer(uuid);

            String displayName =
                    offlinePlayer.getName() != null
                            ? offlinePlayer.getName()
                            : name;

            inventory.setItem(
                    slot,
                    createItem(
                            Material.PLAYER_HEAD,
                            ChatColor.YELLOW + displayName,
                            List.of(
                                    ChatColor.GRAY + "UUID:",
                                    ChatColor.DARK_GRAY
                                            + uuid.toString(),
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
                        List.of(
                                ChatColor.GRAY
                                        + "Close this menu."
                        )
                )
        );

        admin.openInventory(inventory);
    }

    private void openAdminPlayerMenu(
            Player admin,
            UUID targetUuid
    ) {
        PlayerSettings settings =
                plugin.getSettings(targetUuid);

        adminTargets.put(
                admin.getUniqueId(),
                targetUuid
        );

        Inventory inventory = Bukkit.createInventory(
                null,
                54,
                ADMIN_PLAYER_PREFIX
                        + settings.getName()
        );

        fillBackground(inventory);

        for (int index = 0;
                index < PERSONAL_SETTINGS.size();
                index++) {
            String key = PERSONAL_SETTINGS.get(index);

            if (key.equals("experience-multiplier")
                    || key.equals("block-break-speed")) {
                inventory.setItem(
                        index,
                        createMultiplierItem(settings, key)
                );
            } else {
                inventory.setItem(
                        index,
                        createToggleItem(null, settings, key)
                );
            }
        }

        inventory.setItem(
                45,
                createMasterSwitchItem(settings)
        );

        inventory.setItem(
                48,
                createItem(
                        Material.ARROW,
                        ChatColor.YELLOW + "Back",
                        List.of(
                                ChatColor.GRAY
                                        + "Return to player list."
                        )
                )
        );

        inventory.setItem(
                53,
                createItem(
                        Material.BARRIER,
                        ChatColor.RED + "Close",
                        List.of(
                                ChatColor.GRAY
                                        + "Close this menu."
                        )
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

        String name = key.equals(
                "experience-multiplier"
        )
                ? "Experience multiplier"
                : "Block-break speed";

        Inventory inventory = Bukkit.createInventory(
                null,
                27,
                MULTIPLIER_TITLE_PREFIX + name
        );

        for (int index = 0;
                index < MULTIPLIER_VALUES.size();
                index++) {
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
                                    ChatColor.GRAY
                                            + "Click to select."
                            )
                    )
            );
        }

        inventory.setItem(
                22,
                createItem(
                        Material.ARROW,
                        ChatColor.YELLOW + "Back",
                        List.of(
                                ChatColor.GRAY
                                        + "Return to settings."
                        )
                )
        );

        viewer.openInventory(inventory);
    }

    public void openTrash(Player player) {
        Inventory inventory = Bukkit.createInventory(
                null,
                54,
                TRASH_TITLE
        );

        player.openInventory(inventory);
    }

    public void openBackpackSelector(Player player) {
        int count = getBackpackCount(player);

        Inventory inventory = Bukkit.createInventory(
                null,
                27,
                BACKPACK_SELECTOR_TITLE
        );

        for (int number = 1;
                number <= Math.min(count, 10);
                number++) {
            inventory.setItem(
                    number + 8,
                    createItem(
                            Material.CHEST,
                            ChatColor.GREEN
                                    + "Backpack #"
                                    + number,
                            List.of(
                                    ChatColor.GRAY
                                            + "Click to open."
                            )
                    )
            );
        }

        player.openInventory(inventory);
    }

    public void openBackpack(
            Player player,
            int backpackNumber
    ) {
        Inventory inventory = Bukkit.createInventory(
                null,
                54,
                BACKPACK_TITLE_PREFIX
                        + backpackNumber
        );

        String path = backpackPath(
                player.getUniqueId(),
                backpackNumber
        );

        List<?> storedItems =
                plugin.getConfig().getList(path);

        if (storedItems != null) {
            for (int slot = 0;
                    slot < Math.min(
                            storedItems.size(),
                            inventory.getSize()
                    );
                    slot++) {
                Object value = storedItems.get(slot);

                if (value instanceof ItemStack itemStack) {
                    inventory.setItem(slot, itemStack);
                }
            }
        }

        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        String title = event.getView().getTitle();

        if (title.equals(TRASH_TITLE)) {
            return;
        }

        if (title.startsWith(BACKPACK_TITLE_PREFIX)) {
            return;
        }

        if (title.equals(BACKPACK_SELECTOR_TITLE)) {
            event.setCancelled(true);

            if (event.getClickedInventory() == null
                    || event.getClickedInventory()
                    != event.getView().getTopInventory()) {
                return;
            }

            int slot = event.getRawSlot();

            if (slot < 9 || slot > 18) {
                return;
            }

            int backpackNumber = slot - 8;
            int count = getBackpackCount(player);

            if (backpackNumber < 1
                    || backpackNumber > count
                    || backpackNumber > 10) {
                return;
            }

            openBackpack(player, backpackNumber);
            return;
        }

        if (!isPeaceOutMenu(title)) {
            return;
        }

        event.setCancelled(true);

        if (event.getClickedInventory() == null
                || event.getClickedInventory()
                != event.getView().getTopInventory()) {
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
            handleAdminPlayerClick(player, slot);
            return;
        }

        if (title.startsWith(MULTIPLIER_TITLE_PREFIX)) {
            handleMultiplierClick(player, title, slot);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        String title = event.getView().getTitle();

        if (!title.startsWith(BACKPACK_TITLE_PREFIX)) {
            return;
        }

        int backpackNumber;

        try {
            backpackNumber = Integer.parseInt(
                    title.substring(
                            BACKPACK_TITLE_PREFIX.length()
                    )
            );
        } catch (NumberFormatException exception) {
            return;
        }

        if (backpackNumber < 1 || backpackNumber > 10) {
            return;
        }

        List<ItemStack> contents = new ArrayList<>();

        for (ItemStack item :
                event.getView().getTopInventory().getContents()) {
            contents.add(item);
        }

        plugin.getConfig().set(
                backpackPath(
                        player.getUniqueId(),
                        backpackNumber
                ),
                contents
        );

        plugin.saveConfig();
    }

    private void handlePersonalClick(
            Player player,
            int slot
    ) {
        if (slot == 53) {
            player.closeInventory();
            return;
        }

        if (slot == 49) {
            player.closeInventory();
            sendStatus(
                    player,
                    plugin.getSettings(player)
            );
            return;
        }

        if (slot == 45) {
            PlayerSettings settings =
                    plugin.getSettings(player);

            settings.setMasterEnabled(
                    !settings.isMasterEnabled()
            );

            openPersonalMenu(player);
            return;
        }

        if (slot < 0
                || slot >= PERSONAL_SETTINGS.size()) {
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

        if (!canUseFeature(player, key)) {
            player.sendMessage(
                    ChatColor.RED
                            + "You do not have permission "
                            + "to enable this feature."
            );
            return;
        }

        PlayerSettings settings =
                plugin.getSettings(player);

        settings.toggle(key);
        openPersonalMenu(player);
    }

    private void handleAdminListClick(
            Player admin,
            int slot
    ) {
        if (slot == 49) {
            admin.closeInventory();
            return;
        }

        if (slot < 0 || slot >= 45) {
            return;
        }

        List<Map.Entry<UUID, String>> players =
                plugin.getSettings(admin)
                        .getRecordedPlayers()
                        .entrySet()
                        .stream()
                        .sorted(
                                Map.Entry.comparingByValue(
                                        String.CASE_INSENSITIVE_ORDER
                                )
                        )
                        .toList();

        if (slot >= players.size()) {
            return;
        }

        openAdminPlayerMenu(
                admin,
                players.get(slot).getKey()
        );
    }

    private void handleAdminPlayerClick(
            Player admin,
            int slot
    ) {
        UUID targetUuid =
                adminTargets.get(admin.getUniqueId());

        if (targetUuid == null) {
            admin.closeInventory();
            return;
        }

        if (slot == 48) {
            openAdminMenu(admin);
            return;
        }

        if (slot == 53) {
            admin.closeInventory();
            return;
        }

        PlayerSettings settings =
                plugin.getSettings(targetUuid);

        if (slot == 45) {
            settings.setMasterEnabled(
                    !settings.isMasterEnabled()
            );

            openAdminPlayerMenu(admin, targetUuid);
            applyOnlineSettings(targetUuid);
            return;
        }

        if (slot < 0
                || slot >= PERSONAL_SETTINGS.size()) {
            return;
        }

        String key = PERSONAL_SETTINGS.get(slot);

        if (key.equals("experience-multiplier")
                || key.equals("block-break-speed")) {
            openMultiplierMenu(
                    admin,
                    targetUuid,
                    key,
                    true
            );
            return;
        }

        settings.toggle(key);
        openAdminPlayerMenu(admin, targetUuid);
        applyOnlineSettings(targetUuid);
    }

    private void handleMultiplierClick(
            Player viewer,
            String title,
            int slot
    ) {
        if (slot == 22) {
            UUID targetUuid =
                    adminTargets.get(viewer.getUniqueId());

            if (targetUuid == null) {
                viewer.closeInventory();
            } else {
                openAdminPlayerMenu(
                        viewer,
                        targetUuid
                );
            }

            return;
        }

        if (slot < 9 || slot > 17) {
            return;
        }

        int valueIndex = slot - 9;

        if (valueIndex >= MULTIPLIER_VALUES.size()) {
            return;
        }

        UUID targetUuid =
                adminTargets.getOrDefault(
                        viewer.getUniqueId(),
                        viewer.getUniqueId()
                );

        String key = title.contains("Experience")
                ? "experience-multiplier"
                : "block-break-speed";

        PlayerSettings settings =
                plugin.getSettings(targetUuid);

        double value =
                MULTIPLIER_VALUES.get(valueIndex);

        settings.setMultiplier(key, value);

        Player online = Bukkit.getPlayer(targetUuid);

        if (online != null && online.isOnline()
                && key.equals("block-break-speed")) {
            plugin.getListener()
                    .applyBlockSpeedModifier(online);
        }

        viewer.sendMessage(
                ChatColor.AQUA + "[PeaceOut] "
                        + ChatColor.GREEN
                        + formatSettingName(key)
                        + " set to "
                        + formatMultiplier(value)
                        + "."
        );

        if (adminTargets.containsKey(viewer.getUniqueId())) {
            openAdminPlayerMenu(viewer, targetUuid);
        } else {
            openPersonalMenu(viewer);
        }
    }

    private boolean canUseFeature(
            Player player,
            String key
    ) {
        if (key.equals("trash")) {
            return player.hasPermission(
                    "peaceout.trash"
            );
        }

        if (key.equals("backpack")) {
            return player.hasPermission(
                            "peaceout.backpack"
                    )
                    && getBackpackCount(player) > 0;
        }

        return true;
    }

    private int getBackpackCount(Player player) {
        for (int value = 10; value >= 1; value--) {
            if (player.hasPermission(
                    "peaceout.backpacks." + value
            )) {
                return value;
            }
        }

        return 0;
    }

    private void applyOnlineSettings(UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);

        if (online != null && online.isOnline()) {
            plugin.getListener()
                    .applyBlockSpeedModifier(online);
        }
    }

    private boolean isPeaceOutMenu(String title) {
        return title.equals(PERSONAL_TITLE)
                || title.equals(ADMIN_TITLE)
                || title.startsWith(ADMIN_PLAYER_PREFIX)
                || title.startsWith(MULTIPLIER_TITLE_PREFIX);
    }

    private String backpackPath(
            UUID uuid,
            int number
    ) {
        return "players."
                + uuid
                + ".backpacks."
                + number;
    }

    private ItemStack createToggleItem(
            Player player,
            PlayerSettings settings,
            String key
    ) {
        boolean permitted =
                player == null
                        || canUseFeature(player, key);

        boolean enabled =
                settings.isEnabled(key);

        Material material;

        if (!permitted) {
            material = Material.BARRIER;
        } else if (enabled) {
            material = Material.LIME_DYE;
        } else {
            material = Material.GRAY_DYE;
        }

        String state;

        if (!permitted) {
            state = ChatColor.RED
                    + "Permission required";
        } else {
            state = enabled
                    ? ChatColor.GREEN + "Enabled"
                    : ChatColor.RED + "Disabled";
        }

        return createItem(
                material,
                (enabled && permitted
                        ? ChatColor.GREEN
                        : ChatColor.RED)
                        + formatSettingName(key),
                List.of(
                        ChatColor.GRAY + "Status: " + state,
                        "",
                        permitted
                                ? ChatColor.YELLOW
                                + "Click to toggle."
                                : ChatColor.DARK_RED
                                + "You cannot use this feature."
                )
        );
    }

    private ItemStack createMultiplierItem(
            PlayerSettings settings,
            String key
    ) {
        return createItem(
                Material.COMPARATOR,
                ChatColor.GOLD
                        + formatSettingName(key),
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
        boolean enabled =
                settings.isMasterEnabled();

        return createItem(
                enabled
                        ? Material.EMERALD
                        : Material.REDSTONE,
                ChatColor.GOLD + "Master switch",
                List.of(
                        ChatColor.GRAY + "Status: "
                                + (enabled
                                ? ChatColor.GREEN
                                + "Enabled"
                                : ChatColor.RED
                                + "Disabled"),
                        "",
                        ChatColor.YELLOW
                                + "Click to toggle.",
                        ChatColor.DARK_GRAY
                                + "Affects ordinary protections.",
                        ChatColor.DARK_GRAY
                                + "XP and block speed remain independent."
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

        for (int slot = 0;
                slot < inventory.getSize();
                slot++) {
            inventory.setItem(slot, filler);
        }
    }

    private void sendStatus(
            Player player,
            PlayerSettings settings
    ) {
        player.sendMessage(
                ChatColor.AQUA
                        + "[PeaceOut] "
                        + ChatColor.GOLD
                        + "Current settings"
        );

        player.sendMessage(
                ChatColor.GRAY + "Master switch: "
                        + status(
                        settings.isMasterEnabled()
                )
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
                                + status(
                                settings.isEnabled(key)
                        )
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
        return String.format(
                Locale.US,
                "%.2fx",
                value
        );
    }

    private String formatSettingName(String key) {
        return switch (key) {
            case "targeting" ->
                    "Mob targeting protection";
            case "hunger" -> "No food drain";
            case "regeneration" ->
                    "Increased health regeneration";
            case "fall" -> "No fall damage";
            case "durability" ->
                    "Infinite durability";
            case "fireworks" ->
                    "Infinite fireworks";
            case "keep-inventory" ->
                    "Keep inventory on death";
            case "experience-multiplier" ->
                    "Experience multiplier";
            case "block-break-speed" ->
                    "Block break speed";
            case "drop-vacuum" ->
                    "Drop vacuum";
            case "drowning" ->
                    "Drowning protection";
            case "lava" ->
                    "Lava protection";
            case "fire" ->
                    "Fire protection";
            case "trash" ->
                    "Trash can";
            case "backpack" ->
                    "Backpacks";
            default -> key;
        };
    }

    private boolean nearlyEqual(
            double first,
            double second
    ) {
        return Math.abs(first - second) < 0.001;
    }
}
