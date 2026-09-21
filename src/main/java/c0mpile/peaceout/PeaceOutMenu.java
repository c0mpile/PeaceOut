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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class PeaceOutMenu implements Listener {

  private static final int MAX_BACKPACKS = 24;

  private static final String PERSONAL_TITLE = ChatColor.DARK_AQUA + "PeaceOut Settings";

  private static final String ADMIN_TITLE = ChatColor.DARK_RED + "PeaceOut Admin";

  private static final String ADMIN_PLAYER_PREFIX = ChatColor.DARK_RED + "Player: ";

  private static final String MULTIPLIER_TITLE_PREFIX = ChatColor.DARK_PURPLE + "Choose ";

  private static final String TRASH_TITLE = ChatColor.DARK_RED + "PeaceOut Trash";

  private static final String BACKPACK_SELECTOR_TITLE = ChatColor.DARK_GREEN + "Choose Backpack";

  private static final String BACKPACK_TITLE_PREFIX = ChatColor.DARK_GREEN + "Backpack #";

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
      "backpack",
      "backpack-pickup");

  private static final List<Double> MULTIPLIER_VALUES = List.of(
      0.25,
      0.50,
      0.75,
      1.00,
      1.50,
      2.00,
      3.00,
      5.00,
      10.00);

  private final PeaceOut plugin;

  private final Map<UUID, UUID> adminTargets = new HashMap<>();

  public PeaceOutMenu(PeaceOut plugin) {
    this.plugin = plugin;

    plugin.getServer()
        .getPluginManager()
        .registerEvents(this, plugin);
  }

  public void openPersonalMenu(Player player) {
    PlayerSettings settings = plugin.getSettings(player);

    adminTargets.remove(player.getUniqueId());

    Inventory inventory = Bukkit.createInventory(
        null,
        54,
        PERSONAL_TITLE);

    fillBackground(inventory);

    for (int index = 0; index < PERSONAL_SETTINGS.size(); index++) {
      String key = PERSONAL_SETTINGS.get(index);

      if (isMultiplier(key)) {
        inventory.setItem(
            index,
            createMultiplierItem(settings, key));
      } else {
        inventory.setItem(
            index,
            createToggleItem(
                player,
                settings,
                key));
      }
    }

    inventory.setItem(
        45,
        createMasterSwitchItem(settings));

    inventory.setItem(
        47,
        createItem(
            Material.BOOK,
            ChatColor.GOLD + "View Status",
            List.of(
                ChatColor.GRAY
                    + "Review your current settings.",
                "",
                ChatColor.YELLOW
                    + "Click to view status.")));

    inventory.setItem(
        49,
        createItem(
            Material.NETHER_STAR,
            ChatColor.AQUA + "PeaceOut",
            List.of(
                ChatColor.GRAY
                    + "Personal quality-of-life settings.")));

    inventory.setItem(
        53,
        createItem(
            Material.BARRIER,
            ChatColor.RED + "Close",
            List.of(
                ChatColor.GRAY
                    + "Close this menu.")));

    player.openInventory(inventory);
  }

  public void openAdminMenu(Player admin) {
    Inventory inventory = Bukkit.createInventory(
        null,
        54,
        ADMIN_TITLE);

    List<Map.Entry<UUID, String>> players = plugin.getSettings(admin)
        .getRecordedPlayers()
        .entrySet()
        .stream()
        .sorted(
            Map.Entry.comparingByValue(
                String.CASE_INSENSITIVE_ORDER))
        .toList();

    for (int slot = 0; slot < Math.min(players.size(), 45); slot++) {
      UUID uuid = players.get(slot).getKey();
      String storedName = players.get(slot).getValue();

      OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);

      String displayName = offlinePlayer.getName() != null
          ? offlinePlayer.getName()
          : storedName;

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
                      + "Click to edit settings.")));
    }

    inventory.setItem(
        49,
        createItem(
            Material.BARRIER,
            ChatColor.RED + "Close",
            List.of(
                ChatColor.GRAY
                    + "Close this menu.")));

    admin.openInventory(inventory);
  }

  private void openAdminPlayerMenu(
      Player admin,
      UUID targetUuid) {
    PlayerSettings settings = plugin.getSettings(targetUuid);

    adminTargets.put(
        admin.getUniqueId(),
        targetUuid);

    Inventory inventory = Bukkit.createInventory(
        null,
        54,
        ADMIN_PLAYER_PREFIX + settings.getName());

    fillBackground(inventory);

    for (int index = 0; index < PERSONAL_SETTINGS.size(); index++) {
      String key = PERSONAL_SETTINGS.get(index);

      if (isMultiplier(key)) {
        inventory.setItem(
            index,
            createMultiplierItem(settings, key));
      } else {
        inventory.setItem(
            index,
            createToggleItem(null, settings, key));
      }
    }

    inventory.setItem(
        45,
        createMasterSwitchItem(settings));

    inventory.setItem(
        48,
        createItem(
            Material.ARROW,
            ChatColor.YELLOW + "Back",
            List.of(
                ChatColor.GRAY
                    + "Return to the player list.")));

    inventory.setItem(
        49,
        createItem(
            Material.NAME_TAG,
            ChatColor.GOLD + settings.getName(),
            List.of(
                ChatColor.GRAY
                    + "Editing this player's settings.")));

    inventory.setItem(
        53,
        createItem(
            Material.BARRIER,
            ChatColor.RED + "Close",
            List.of(
                ChatColor.GRAY
                    + "Close this menu.")));

    admin.openInventory(inventory);
  }

  private void openMultiplierMenu(
      Player viewer,
      UUID targetUuid,
      String key,
      boolean adminMenu) {
    PlayerSettings settings = plugin.getSettings(targetUuid);

    if (adminMenu) {
      adminTargets.put(
          viewer.getUniqueId(),
          targetUuid);
    } else {
      adminTargets.remove(viewer.getUniqueId());
    }

    String name = key.equals("experience-multiplier")
        ? "Experience Multiplier"
        : "Block-Break Speed";

    Inventory inventory = Bukkit.createInventory(
        null,
        27,
        MULTIPLIER_TITLE_PREFIX + name);

    fillBackground(inventory);

    for (int index = 0; index < MULTIPLIER_VALUES.size(); index++) {
      double value = MULTIPLIER_VALUES.get(index);

      boolean selected = nearlyEqual(
          settings.getMultiplier(key),
          value);

      inventory.setItem(
          index + 9,
          createItem(
              selected
                  ? Material.LIME_DYE
                  : Material.COMPARATOR,
              (selected
                  ? ChatColor.GREEN
                  : ChatColor.YELLOW)
                  + formatMultiplier(value),
              List.of(
                  selected
                      ? ChatColor.GREEN
                          + "Currently selected."
                      : ChatColor.GRAY
                          + "Click to select.")));
    }

    inventory.setItem(
        22,
        createItem(
            Material.ARROW,
            ChatColor.YELLOW + "Back",
            List.of(
                ChatColor.GRAY
                    + "Return to settings.")));

    viewer.openInventory(inventory);
  }

  public void openTrash(Player player) {
    Inventory inventory = Bukkit.createInventory(
        null,
        54,
        TRASH_TITLE);

    fillBackground(inventory);

    for (int slot = 0; slot < 45; slot++) {
      inventory.setItem(slot, null);
    }

    inventory.setItem(
        49,
        createItem(
            Material.CAULDRON,
            ChatColor.DARK_RED + "Trash Can",
            List.of(
                ChatColor.GRAY
                    + "Items placed here are discarded",
                ChatColor.GRAY
                    + "when this menu is closed.")));

    player.openInventory(inventory);
  }

  public void openBackpackSelector(Player player) {
    int count = getBackpackCount(player);

    Inventory inventory = Bukkit.createInventory(
        null,
        27,
        BACKPACK_SELECTOR_TITLE);

    fillBackground(inventory);

    /*
     * Slots 0 through 23 provide exactly 24 backpack choices.
     * Slots 24 through 26 are reserved for the footer.
     */
    for (int number = 1; number <= Math.min(count, MAX_BACKPACKS); number++) {
      inventory.setItem(
          number - 1,
          createItem(
              Material.CHEST,
              ChatColor.GREEN
                  + "Backpack #" + number,
              List.of(
                  ChatColor.GRAY
                      + "Private storage.",
                  "",
                  ChatColor.YELLOW
                      + "Click to open.")));
    }

    inventory.setItem(
        24,
        createItem(
            Material.BOOK,
            ChatColor.GOLD + "Backpack Access",
            List.of(
                ChatColor.GRAY
                    + "Available: "
                    + ChatColor.WHITE
                    + count)));

    inventory.setItem(
        26,
        createItem(
            Material.BARRIER,
            ChatColor.RED + "Close",
            List.of(
                ChatColor.GRAY
                    + "Close this menu.")));

    player.openInventory(inventory);
  }

  public void openBackpack(
      Player player,
      int backpackNumber) {
    if (backpackNumber < 1
        || backpackNumber > MAX_BACKPACKS
        || backpackNumber > getBackpackCount(player)) {
      return;
    }

    Inventory inventory = Bukkit.createInventory(
        null,
        54,
        BACKPACK_TITLE_PREFIX + backpackNumber);

    List<ItemStack> contents = getBackpackContents(
        player.getUniqueId(),
        backpackNumber);

    for (int slot = 0; slot < Math.min(
        contents.size(),
        inventory.getSize()); slot++) {
      inventory.setItem(slot, contents.get(slot));
    }

    inventory.setItem(
        49,
        createItem(
            Material.CHEST,
            ChatColor.GREEN
                + "Backpack #" + backpackNumber,
            List.of(
                ChatColor.GRAY
                    + "Contents save when closed.")));

    player.openInventory(inventory);
  }

  public List<ItemStack> getBackpackContents(
      UUID uuid,
      int backpackNumber) {
    List<ItemStack> contents = new ArrayList<>();

    for (int slot = 0; slot < 54; slot++) {
      contents.add(null);
    }

    List<?> stored = plugin.getConfig().getList(
        backpackPath(uuid, backpackNumber));

    if (stored == null) {
      return contents;
    }

    for (int slot = 0; slot < Math.min(stored.size(), 54); slot++) {
      Object value = stored.get(slot);

      if (value instanceof ItemStack itemStack) {
        contents.set(slot, itemStack);
      }
    }

    return contents;
  }

  public void saveBackpackContents(
      UUID uuid,
      int backpackNumber,
      List<ItemStack> contents) {
    List<ItemStack> safeContents = new ArrayList<>();

    for (int slot = 0; slot < 54; slot++) {
      if (slot < contents.size()) {
        safeContents.add(contents.get(slot));
      } else {
        safeContents.add(null);
      }
    }

    plugin.getConfig().set(
        backpackPath(uuid, backpackNumber),
        safeContents);

    plugin.saveConfig();
  }

  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player player)) {
      return;
    }

    String title = event.getView().getTitle();

    /*
     * Trash is intentionally not cancelled. Players need to be able
     * to place and remove items normally. Nothing is persisted.
     */
    if (title.equals(TRASH_TITLE)) {
      return;
    }

    /*
     * Backpack inventories are normal storage inventories.
     */
    if (title.startsWith(BACKPACK_TITLE_PREFIX)) {
      return;
    }

    if (title.equals(BACKPACK_SELECTOR_TITLE)) {
      event.setCancelled(true);

      if (event.getClickedInventory() == null
          || event.getClickedInventory() != event.getView().getTopInventory()) {
        return;
      }

      int slot = event.getRawSlot();

      if (slot == 26) {
        player.closeInventory();
        return;
      }

      if (slot < 0 || slot > 23) {
        return;
      }

      int backpackNumber = slot + 1;
      int count = getBackpackCount(player);

      if (backpackNumber > count) {
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
          title.substring(BACKPACK_TITLE_PREFIX.length()));
    } catch (NumberFormatException exception) {
      return;
    }

    if (backpackNumber < 1
        || backpackNumber > MAX_BACKPACKS) {
      return;
    }

    List<ItemStack> contents = new ArrayList<>();

    for (ItemStack item : event.getView()
        .getTopInventory()
        .getContents()) {
      contents.add(item);
    }

    saveBackpackContents(
        player.getUniqueId(),
        backpackNumber,
        contents);
  }

  private void handlePersonalClick(
      Player player,
      int slot) {
    if (slot == 53) {
      player.closeInventory();
      return;
    }

    if (slot == 47) {
      player.closeInventory();
      sendStatus(
          player,
          plugin.getSettings(player));
      return;
    }

    if (slot == 45) {
      PlayerSettings settings = plugin.getSettings(player);

      settings.setMasterEnabled(
          !settings.isMasterEnabled());

      openPersonalMenu(player);
      return;
    }

    if (slot < 0
        || slot >= PERSONAL_SETTINGS.size()) {
      return;
    }

    String key = PERSONAL_SETTINGS.get(slot);

    if (isMultiplier(key)) {
      openMultiplierMenu(
          player,
          player.getUniqueId(),
          key,
          false);
      return;
    }

    if (!canUseFeature(player, key)) {
      player.sendMessage(
          ChatColor.RED
              + "You do not have permission "
              + "to enable this feature.");
      return;
    }

    PlayerSettings settings = plugin.getSettings(player);

    settings.toggle(key);
    openPersonalMenu(player);
  }

  private void handleAdminListClick(
      Player admin,
      int slot) {
    if (slot == 49) {
      admin.closeInventory();
      return;
    }

    if (slot < 0 || slot >= 45) {
      return;
    }

    List<Map.Entry<UUID, String>> players = plugin.getSettings(admin)
        .getRecordedPlayers()
        .entrySet()
        .stream()
        .sorted(
            Map.Entry.comparingByValue(
                String.CASE_INSENSITIVE_ORDER))
        .toList();

    if (slot >= players.size()) {
      return;
    }

    openAdminPlayerMenu(
        admin,
        players.get(slot).getKey());
  }

  private void handleAdminPlayerClick(
      Player admin,
      int slot) {
    UUID targetUuid = adminTargets.get(admin.getUniqueId());

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

    PlayerSettings settings = plugin.getSettings(targetUuid);

    if (slot == 45) {
      settings.setMasterEnabled(
          !settings.isMasterEnabled());

      openAdminPlayerMenu(admin, targetUuid);
      applyOnlineSettings(targetUuid);
      return;
    }

    if (slot < 0
        || slot >= PERSONAL_SETTINGS.size()) {
      return;
    }

    String key = PERSONAL_SETTINGS.get(slot);

    if (isMultiplier(key)) {
      openMultiplierMenu(
          admin,
          targetUuid,
          key,
          true);
      return;
    }

    settings.toggle(key);
    openAdminPlayerMenu(admin, targetUuid);
    applyOnlineSettings(targetUuid);
  }

  private void handleMultiplierClick(
      Player viewer,
      String title,
      int slot) {
    if (slot == 22) {
      UUID targetUuid = adminTargets.get(viewer.getUniqueId());

      if (targetUuid == null) {
        openPersonalMenu(viewer);
      } else {
        openAdminPlayerMenu(viewer, targetUuid);
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

    UUID targetUuid = adminTargets.getOrDefault(
        viewer.getUniqueId(),
        viewer.getUniqueId());

    String key = title.contains("Experience")
        ? "experience-multiplier"
        : "block-break-speed";

    PlayerSettings settings = plugin.getSettings(targetUuid);

    double value = MULTIPLIER_VALUES.get(valueIndex);

    settings.setMultiplier(key, value);

    Player online = Bukkit.getPlayer(targetUuid);

    if (online != null
        && online.isOnline()
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
            + ".");

    if (adminTargets.containsKey(viewer.getUniqueId())) {
      openAdminPlayerMenu(viewer, targetUuid);
    } else {
      openPersonalMenu(viewer);
    }
  }

  private boolean canUseFeature(
      Player player,
      String key) {
    if (player == null) {
      return true;
    }

    if (key.equals("trash")) {
      return player.hasPermission("peaceout.trash");
    }

    if (key.equals("backpack")
        || key.equals("backpack-pickup")) {
      return player.hasPermission("peaceout.backpack")
          && getBackpackCount(player) > 0;
    }

    return true;
  }

  private int getBackpackCount(Player player) {
    for (int value = MAX_BACKPACKS; value >= 1; value--) {
      if (player.hasPermission(
          "peaceout.backpacks." + value)) {
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

  private boolean isMultiplier(String key) {
    return key.equals("experience-multiplier")
        || key.equals("block-break-speed");
  }

  private String backpackPath(
      UUID uuid,
      int number) {
    return "players."
        + uuid
        + ".backpacks."
        + number;
  }

  private ItemStack createToggleItem(
      Player player,
      PlayerSettings settings,
      String key) {
    boolean permitted = player == null
        || canUseFeature(player, key);

    boolean enabled = settings.isEnabled(key);

    Material icon = featureIcon(key);

    if (!permitted) {
      icon = Material.BARRIER;
    }

    ChatColor stateColor = enabled && permitted
        ? ChatColor.GREEN
        : ChatColor.RED;

    String state = !permitted
        ? ChatColor.RED + "Permission required"
        : enabled
            ? ChatColor.GREEN + "Enabled"
            : ChatColor.GRAY + "Disabled";

    List<String> lore = new ArrayList<>();

    lore.add(
        ChatColor.GRAY + "Status: " + state);

    lore.add("");

    if (permitted) {
      lore.add(
          ChatColor.YELLOW + "Click to toggle.");
    } else {
      lore.add(
          ChatColor.DARK_RED
              + "You cannot use this feature.");
    }

    if (key.equals("backpack-pickup")) {
      lore.add("");
      lore.add(
          ChatColor.DARK_GRAY
              + "Sends overflow pickups to backpacks.");
    }

    return createItem(
        icon,
        stateColor + formatSettingName(key),
        lore);
  }

  private Material featureIcon(String key) {
    return switch (key) {
      case "targeting" -> Material.SHIELD;
      case "hunger" -> Material.GOLDEN_APPLE;
      case "regeneration" -> Material.POTION;
      case "fall" -> Material.FEATHER;
      case "durability" -> Material.ANVIL;
      case "fireworks" -> Material.FIREWORK_ROCKET;
      case "keep-inventory" -> Material.ENDER_CHEST;
      case "drop-vacuum" -> Material.HOPPER;
      case "drowning" -> Material.WATER_BUCKET;
      case "lava" -> Material.LAVA_BUCKET;
      case "fire" -> Material.FIRE_CHARGE;
      case "trash" -> Material.CAULDRON;
      case "backpack" -> Material.CHEST;
      case "backpack-pickup" -> Material.BUNDLE;
      default -> Material.PAPER;
    };
  }

  private ItemStack createMultiplierItem(
      PlayerSettings settings,
      String key) {
    Material material = key.equals(
        "experience-multiplier")
            ? Material.EXPERIENCE_BOTTLE
            : Material.DIAMOND_PICKAXE;

    return createItem(
        material,
        ChatColor.GOLD + formatSettingName(key),
        List.of(
            ChatColor.GRAY + "Current: "
                + ChatColor.WHITE
                + formatMultiplier(
                    settings.getMultiplier(key)),
            "",
            ChatColor.YELLOW
                + "Click to choose a value."));
  }

  private ItemStack createMasterSwitchItem(
      PlayerSettings settings) {
    boolean enabled = settings.isMasterEnabled();

    return createItem(
        enabled
            ? Material.EMERALD
            : Material.REDSTONE,
        ChatColor.GOLD + "Master Switch",
        List.of(
            ChatColor.GRAY + "Status: "
                + (enabled
                    ? ChatColor.GREEN + "Enabled"
                    : ChatColor.RED + "Disabled"),
            "",
            ChatColor.YELLOW
                + "Click to toggle.",
            ChatColor.DARK_GRAY
                + "Controls ordinary protections.",
            ChatColor.DARK_GRAY
                + "XP and block speed are independent."));
  }

  private ItemStack createItem(
      Material material,
      String name,
      List<String> lore) {
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
        Material.GRAY_STAINED_GLASS_PANE,
        " ",
        List.of());

    for (int slot = 0; slot < inventory.getSize(); slot++) {
      inventory.setItem(slot, filler);
    }
  }

  private void sendStatus(
      Player player,
      PlayerSettings settings) {
    player.sendMessage(
        ChatColor.AQUA
            + "[PeaceOut] "
            + ChatColor.GOLD
            + "Current Settings");

    player.sendMessage(
        ChatColor.GRAY + "Master switch: "
            + status(
                settings.isMasterEnabled()));

    for (String key : PERSONAL_SETTINGS) {
      if (isMultiplier(key)) {
        player.sendMessage(
            ChatColor.GRAY
                + formatSettingName(key)
                + ": "
                + ChatColor.WHITE
                + formatMultiplier(
                    settings.getMultiplier(key)));
      } else {
        player.sendMessage(
            ChatColor.GRAY
                + formatSettingName(key)
                + ": "
                + status(
                    settings.isEnabled(key)));
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
        value);
  }

  private String formatSettingName(String key) {
    return switch (key) {
      case "targeting" ->
        "Mob Targeting Protection";
      case "hunger" ->
        "No Hunger Drain";
      case "regeneration" ->
        "Health Regeneration";
      case "fall" ->
        "No Fall Damage";
      case "durability" ->
        "Infinite Durability";
      case "fireworks" ->
        "Infinite Fireworks";
      case "keep-inventory" ->
        "Keep Inventory";
      case "experience-multiplier" ->
        "Experience Multiplier";
      case "block-break-speed" ->
        "Block-Break Speed";
      case "drop-vacuum" ->
        "Drop Vacuum";
      case "drowning" ->
        "Drowning Protection";
      case "lava" ->
        "Lava Protection";
      case "fire" ->
        "Fire Protection";
      case "trash" ->
        "Trash Can";
      case "backpack" ->
        "Backpacks";
      case "backpack-pickup" ->
        "Automatic Backpack Pickup";
      default -> key;
    };
  }

  private boolean nearlyEqual(
      double first,
      double second) {
    return Math.abs(first - second) < 0.001;
  }
}
