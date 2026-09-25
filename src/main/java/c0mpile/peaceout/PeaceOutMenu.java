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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PeaceOutMenu implements Listener {

  private static final int MAX_BACKPACKS = 24;
  private static final int BACKPACK_FOOTER_SLOT = 49;

  private static final String PERSONAL_TITLE = ChatColor.DARK_AQUA + "PeaceOut Settings";

  private static final String ADMIN_TITLE = ChatColor.DARK_RED + "PeaceOut Admin";

  private static final String ADMIN_PLAYER_PREFIX = ChatColor.DARK_RED + "Player: ";

  private static final String MULTIPLIER_TITLE_PREFIX = ChatColor.DARK_PURPLE + "Choose ";

  private static final String TRASH_TITLE = ChatColor.DARK_RED + "PeaceOut Trash";

  private static final String BACKPACK_SELECTOR_TITLE = ChatColor.DARK_GREEN + "Choose Backpack";

  private static final String BACKPACK_TITLE_PREFIX = ChatColor.DARK_GREEN + "Backpack #";

  private static final List<String> SETTINGS = List.of(
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
      "backpack-pickup",
      "backpack-sticky");

  private static final List<Double> MULTIPLIERS = List.of(
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
  private final Set<UUID> changingBackpackMenus = new HashSet<>();

  public PeaceOutMenu(PeaceOut plugin) {
    this.plugin = plugin;

    plugin.getServer()
        .getPluginManager()
        .registerEvents(this, plugin);
  }

  public void openPersonalMenu(Player player) {
    Inventory inventory = Bukkit.createInventory(
        null,
        54,
        PERSONAL_TITLE);

    PlayerSettings settings = plugin.getSettings(player);

    for (int slot = 0; slot < SETTINGS.size(); slot++) {
      String key = SETTINGS.get(slot);

      inventory.setItem(
          slot,
          isMultiplier(key)
              ? multiplierItem(settings, key)
              : toggleItem(player, settings, key));
    }

    inventory.setItem(
        45,
        item(
            settings.isMasterEnabled()
                ? Material.EMERALD
                : Material.REDSTONE,
            ChatColor.GOLD + "Master Switch",
            List.of(
                ChatColor.GRAY + "Status: "
                    + (settings.isMasterEnabled()
                        ? ChatColor.GREEN + "Enabled"
                        : ChatColor.RED + "Disabled"),
                "",
                ChatColor.YELLOW
                    + "Click to toggle.")));

    inventory.setItem(
        47,
        item(
            Material.BOOK,
            ChatColor.GOLD + "View Status",
            List.of(
                ChatColor.YELLOW
                    + "Click to view status.")));

    inventory.setItem(
        53,
        item(
            Material.BARRIER,
            ChatColor.RED + "Close",
            List.of()));

    adminTargets.remove(player.getUniqueId());
    player.openInventory(inventory);
  }

  public void openAdminMenu(Player player) {
    Inventory inventory = Bukkit.createInventory(
        null,
        54,
        ADMIN_TITLE);

    List<Map.Entry<UUID, String>> players = plugin.getSettings(player)
        .getRecordedPlayers()
        .entrySet()
        .stream()
        .sorted(
            Map.Entry.comparingByValue(
                String.CASE_INSENSITIVE_ORDER))
        .toList();

    for (int slot = 0; slot < Math.min(players.size(), 45); slot++) {

      UUID uuid = players.get(slot).getKey();
      OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);

      String name = offline.getName() != null
          ? offline.getName()
          : players.get(slot).getValue();

      inventory.setItem(
          slot,
          item(
              Material.PLAYER_HEAD,
              ChatColor.YELLOW + name,
              List.of(
                  ChatColor.GRAY + "UUID:",
                  ChatColor.DARK_GRAY
                      + uuid.toString(),
                  "",
                  ChatColor.GREEN
                      + "Click to edit.")));
    }

    inventory.setItem(
        49,
        item(
            Material.BARRIER,
            ChatColor.RED + "Close",
            List.of()));

    player.openInventory(inventory);
  }

  private void openAdminPlayerMenu(
      Player admin,
      UUID target) {
    PlayerSettings settings = plugin.getSettings(target);

    Inventory inventory = Bukkit.createInventory(
        null,
        54,
        ADMIN_PLAYER_PREFIX + settings.getName());

    for (int slot = 0; slot < SETTINGS.size(); slot++) {
      String key = SETTINGS.get(slot);

      inventory.setItem(
          slot,
          isMultiplier(key)
              ? multiplierItem(settings, key)
              : toggleItem(null, settings, key));
    }

    inventory.setItem(
        45,
        item(
            settings.isMasterEnabled()
                ? Material.EMERALD
                : Material.REDSTONE,
            ChatColor.GOLD + "Master Switch",
            List.of(
                ChatColor.GRAY + "Status: "
                    + (settings.isMasterEnabled()
                        ? ChatColor.GREEN + "Enabled"
                        : ChatColor.RED + "Disabled"),
                "",
                ChatColor.YELLOW
                    + "Click to toggle.")));

    inventory.setItem(
        48,
        item(
            Material.ARROW,
            ChatColor.YELLOW + "Back",
            List.of()));

    inventory.setItem(
        53,
        item(
            Material.BARRIER,
            ChatColor.RED + "Close",
            List.of()));

    adminTargets.put(admin.getUniqueId(), target);
    admin.openInventory(inventory);
  }

  private void openMultiplierMenu(
      Player player,
      UUID target,
      String key,
      boolean admin) {
    String displayTitle = key.equals("experience-multiplier")
        ? "Experience Multiplier"
        : "Block-Break Speed";

    Inventory inventory = Bukkit.createInventory(
        null,
        27,
        MULTIPLIER_TITLE_PREFIX + displayTitle);

    PlayerSettings settings = plugin.getSettings(target);

    for (int index = 0; index < MULTIPLIERS.size(); index++) {

      double value = MULTIPLIERS.get(index);

      boolean selected = Math.abs(
          settings.getMultiplier(key) - value) < 0.001;

      Material icon = key.equals("experience-multiplier")
          ? Material.EXPERIENCE_BOTTLE
          : Material.DIAMOND_PICKAXE;

      inventory.setItem(
          index + 9,
          item(
              icon,
              (selected
                  ? ChatColor.GREEN
                  : ChatColor.YELLOW)
                  + formatMultiplier(value),
              List.of(
                  selected
                      ? ChatColor.GREEN
                          + "Currently selected."
                      : ChatColor.YELLOW
                          + "Click to select.")));
    }

    inventory.setItem(
        22,
        item(
            Material.ARROW,
            ChatColor.YELLOW + "Back",
            List.of()));

    if (admin) {
      adminTargets.put(player.getUniqueId(), target);
    } else {
      adminTargets.remove(player.getUniqueId());
    }

    player.openInventory(inventory);
  }

  public void openTrash(Player player) {
    Inventory inventory = Bukkit.createInventory(
        null,
        54,
        TRASH_TITLE);

    inventory.setItem(
        49,
        item(
            Material.CAULDRON,
            ChatColor.DARK_RED + "Trash Can",
            List.of(
                ChatColor.GRAY
                    + "Items are discarded when closed.")));

    player.openInventory(inventory);
  }

  public void openBackpackSelector(Player player) {
    Inventory inventory = Bukkit.createInventory(
        null,
        27,
        BACKPACK_SELECTOR_TITLE);

    int count = Math.min(
        getBackpackCount(player),
        MAX_BACKPACKS);

    for (int number = 1; number <= count; number++) {
      inventory.setItem(
          number - 1,
          item(
              Material.ENDER_CHEST,
              ChatColor.GREEN
                  + "Backpack #" + number,
              List.of(
                  ChatColor.YELLOW
                      + "Click to open.")));
    }

    inventory.setItem(
        26,
        item(
            Material.BARRIER,
            ChatColor.RED + "Close",
            List.of()));

    player.openInventory(inventory);
  }

  public void openBackpack(
      Player player,
      int number) {
    if (number < 1
        || number > MAX_BACKPACKS
        || number > getBackpackCount(player)) {
      return;
    }

    Inventory inventory = Bukkit.createInventory(
        null,
        54,
        BACKPACK_TITLE_PREFIX + number);
    inventory.setMaxStackSize(PeaceOut.MAX_STACK_SIZE);

    List<ItemStack> contents = getBackpackContents(
        player.getUniqueId(),
        number);

    for (int slot = 0; slot < 54; slot++) {
      inventory.setItem(slot, contents.get(slot));
    }

    inventory.setItem(
        BACKPACK_FOOTER_SLOT,
        item(
            Material.ENDER_CHEST,
            ChatColor.GREEN
                + "Backpack #" + number,
            List.of(
                ChatColor.YELLOW
                    + "Click to choose another backpack.",
                ChatColor.GRAY
                    + "Contents save before switching.")));

    player.openInventory(inventory);
  }

  public List<ItemStack> getBackpackContents(
      UUID uuid,
      int number) {
    List<ItemStack> contents = new ArrayList<>();

    for (int slot = 0; slot < 54; slot++) {
      contents.add(null);
    }

    List<?> stored = plugin.getConfig().getList(
        backpackPath(uuid, number));

    if (stored != null) {
      for (int slot = 0; slot < Math.min(stored.size(), 54); slot++) {

        Object value = stored.get(slot);

        if (value instanceof ItemStack item) {
          PeaceOut.applyMaxStackSize(item);
          contents.set(slot, item);
        }
      }
    }

    contents.set(BACKPACK_FOOTER_SLOT, null);

    return contents;
  }

  public void saveBackpackContents(
      UUID uuid,
      int number,
      List<ItemStack> contents) {
    List<ItemStack> safe = new ArrayList<>();

    for (int slot = 0; slot < 54; slot++) {
      ItemStack item = slot < contents.size()
          ? contents.get(slot)
          : null;
      if (item != null) {
        PeaceOut.applyMaxStackSize(item);
      }
      safe.add(item);
    }

    safe.set(BACKPACK_FOOTER_SLOT, null);

    plugin.getConfig().set(
        backpackPath(uuid, number),
        safe);

    plugin.saveConfig();
  }

  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player player)) {
      return;
    }

    String title = event.getView().getTitle();

    if (title.startsWith(BACKPACK_TITLE_PREFIX)) {
      int slot = event.getRawSlot();

      if (slot == BACKPACK_FOOTER_SLOT
          && event.getClickedInventory() == event.getView().getTopInventory()) {

        event.setCancelled(true);

        saveCurrentBackpack(player);
        changingBackpackMenus.add(player.getUniqueId());
        openBackpackSelector(player);
      }

      return;
    }

    if (title.equals(TRASH_TITLE)) {
      return;
    }

    if (title.equals(BACKPACK_SELECTOR_TITLE)) {
      event.setCancelled(true);

      if (event.getClickedInventory() != event.getView().getTopInventory()) {
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

      int number = slot + 1;

      if (number <= getBackpackCount(player)) {
        openBackpack(player, number);
      }

      return;
    }

    if (!isPeaceOutTitle(title)) {
      return;
    }

    event.setCancelled(true);

    if (event.getClickedInventory() != event.getView().getTopInventory()) {
      return;
    }

    int slot = event.getRawSlot();

    if (title.equals(PERSONAL_TITLE)) {
      handlePersonalClick(player, slot);
    } else if (title.equals(ADMIN_TITLE)) {
      handleAdminClick(player, slot);
    } else if (title.startsWith(ADMIN_PLAYER_PREFIX)) {
      handleAdminPlayerClick(player, slot);
    } else if (title.startsWith(MULTIPLIER_TITLE_PREFIX)) {
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

    if (changingBackpackMenus.remove(player.getUniqueId())) {
      return;
    }

    saveCurrentBackpack(player);
  }

  private void saveCurrentBackpack(Player player) {
    String title = player.getOpenInventory().getTitle();

    if (!title.startsWith(BACKPACK_TITLE_PREFIX)) {
      return;
    }

    int number;

    try {
      number = Integer.parseInt(
          title.substring(BACKPACK_TITLE_PREFIX.length()));
    } catch (NumberFormatException exception) {
      return;
    }

    List<ItemStack> contents = new ArrayList<>();

    for (ItemStack item : player.getOpenInventory()
        .getTopInventory()
        .getContents()) {
      contents.add(item);
    }

    saveBackpackContents(
        player.getUniqueId(),
        number,
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

    if (slot < 0 || slot >= SETTINGS.size()) {
      return;
    }

    String key = SETTINGS.get(slot);

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
              + "You do not have permission.");
      return;
    }

    plugin.getSettings(player).toggle(key);
    openPersonalMenu(player);
  }

  private void handleAdminClick(
      Player player,
      int slot) {
    if (slot == 49) {
      player.closeInventory();
      return;
    }

    List<Map.Entry<UUID, String>> players = plugin.getSettings(player)
        .getRecordedPlayers()
        .entrySet()
        .stream()
        .sorted(
            Map.Entry.comparingByValue(
                String.CASE_INSENSITIVE_ORDER))
        .toList();

    if (slot >= 0
        && slot < 45
        && slot < players.size()) {

      openAdminPlayerMenu(
          player,
          players.get(slot).getKey());
    }
  }

  private void handleAdminPlayerClick(
      Player admin,
      int slot) {
    UUID target = adminTargets.get(
        admin.getUniqueId());

    if (target == null) {
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

    PlayerSettings settings = plugin.getSettings(target);

    if (slot == 45) {
      settings.setMasterEnabled(
          !settings.isMasterEnabled());

      openAdminPlayerMenu(admin, target);
      return;
    }

    if (slot < 0 || slot >= SETTINGS.size()) {
      return;
    }

    String key = SETTINGS.get(slot);

    if (isMultiplier(key)) {
      openMultiplierMenu(
          admin,
          target,
          key,
          true);
      return;
    }

    settings.toggle(key);
    openAdminPlayerMenu(admin, target);
  }

  private void handleMultiplierClick(
      Player player,
      String title,
      int slot) {
    if (slot == 22) {
      UUID target = adminTargets.get(
          player.getUniqueId());

      if (target == null) {
        openPersonalMenu(player);
      } else {
        openAdminPlayerMenu(player, target);
      }

      return;
    }

    if (slot < 9 || slot > 17) {
      return;
    }

    int index = slot - 9;

    if (index >= MULTIPLIERS.size()) {
      return;
    }

    UUID target = adminTargets.getOrDefault(
        player.getUniqueId(),
        player.getUniqueId());

    String key = title.contains("Experience")
        ? "experience-multiplier"
        : "block-break-speed";

    plugin.getSettings(target).setMultiplier(
        key,
        MULTIPLIERS.get(index));

    if (key.equals("block-break-speed")
        && target.equals(player.getUniqueId())) {
      plugin.getListener()
          .applyBlockSpeedModifier(player);
    }

    if (adminTargets.containsKey(player.getUniqueId())) {
      openAdminPlayerMenu(player, target);
    } else {
      openPersonalMenu(player);
    }
  }

  private boolean canUseFeature(
      Player player,
      String key) {
    if (key.equals("trash")) {
      return player.hasPermission("peaceout.trash");
    }

    if (key.equals("backpack")
        || key.equals("backpack-pickup")
        || key.equals("backpack-sticky")) {
      return player.hasPermission("peaceout.backpack")
          && getBackpackCount(player) > 0;
    }

    return true;
  }

  private ItemStack toggleItem(
      Player player,
      PlayerSettings settings,
      String key) {
    boolean permitted = player == null
        || canUseFeature(player, key);

    boolean enabled = settings.isEnabled(key);

    String status;

    if (!permitted) {
      status = ChatColor.RED + "Locked";
    } else if (enabled) {
      status = ChatColor.GREEN + "Enabled";
    } else {
      status = ChatColor.GRAY + "Disabled";
    }

    return item(
        featureMaterial(key),
        ChatColor.WHITE + displayName(key),
        List.of(
            ChatColor.GRAY + "Status: " + status,
            "",
            permitted
                ? ChatColor.YELLOW
                    + "Click to toggle."
                : ChatColor.RED
                    + "Permission required."));
  }

  private ItemStack multiplierItem(
      PlayerSettings settings,
      String key) {
    return item(
        key.equals("experience-multiplier")
            ? Material.EXPERIENCE_BOTTLE
            : Material.DIAMOND_PICKAXE,
        ChatColor.GOLD + displayName(key),
        List.of(
            ChatColor.GRAY + "Current: "
                + ChatColor.WHITE
                + formatMultiplier(
                    settings.getMultiplier(key)),
            "",
            ChatColor.YELLOW
                + "Click to choose."));
  }

  private Material featureMaterial(String key) {
    return switch (key) {
      case "targeting" -> Material.SHIELD;
      case "hunger" -> Material.GOLDEN_APPLE;
      case "regeneration" -> Material.POTION;
      case "fall" -> Material.FEATHER;
      case "drowning" -> Material.WATER_BUCKET;
      case "fire" -> Material.FLINT_AND_STEEL;
      case "lava" -> Material.MAGMA_CREAM;
      case "durability" -> Material.ANVIL;
      case "fireworks" -> Material.FIREWORK_ROCKET;
      case "keep-inventory" -> Material.TOTEM_OF_UNDYING;
      case "drop-vacuum" -> Material.HOPPER;
      case "trash" -> Material.CAULDRON;
      case "backpack" -> Material.ENDER_CHEST;
      case "backpack-pickup" -> Material.CHEST_MINECART;
      case "backpack-sticky" -> Material.HOPPER;
      default -> Material.BOOK;
    };
  }

  private String description(String key) {
    return switch (key) {
      case "targeting" ->
        "Stops hostile mobs targeting you.";
      case "hunger" ->
        "Prevents food level loss.";
      case "regeneration" ->
        "Improves natural health regeneration.";
      case "fall" ->
        "Prevents damage from falling.";
      case "drowning" ->
        "Prevents damage from drowning.";
      case "fire" ->
        "Prevents fire damage.";
      case "lava" ->
        "Prevents lava damage.";
      case "durability" ->
        "Prevents equipped items losing durability.";
      case "fireworks" ->
        "Prevents fireworks being consumed.";
      case "keep-inventory" ->
        "Keeps items and experience after death.";
      case "drop-vacuum" ->
        "Pulls nearby items and experience to you.";
      case "trash" ->
        "Provides access to a disposable trash can.";
      case "backpack" ->
        "Unlocks your personal backpacks.";
      case "backpack-pickup" ->
        "Stores overflow items in your backpacks.";
      case "backpack-sticky" ->
        "Routes items to backpacks already holding them.";
      case "experience-multiplier" ->
        "Changes experience gained from orbs.";
      case "block-break-speed" ->
        "Changes your block-breaking speed.";
      default ->
        "PeaceOut setting.";
    };
  }

  private ItemStack item(
      Material material,
      String name,
      List<String> lore) {
    ItemStack result = new ItemStack(material);
    ItemMeta meta = result.getItemMeta();

    if (meta != null) {
      meta.setDisplayName(name);
      meta.setLore(lore);
      result.setItemMeta(meta);
    }

    return result;
  }

  private boolean isPeaceOutTitle(String title) {
    return title.equals(PERSONAL_TITLE)
        || title.equals(ADMIN_TITLE)
        || title.startsWith(ADMIN_PLAYER_PREFIX)
        || title.startsWith(MULTIPLIER_TITLE_PREFIX);
  }

  private boolean isMultiplier(String key) {
    return key.equals("experience-multiplier")
        || key.equals("block-break-speed");
  }

  private int getBackpackCount(Player player) {
    for (int number = MAX_BACKPACKS; number >= 1; number--) {

      if (player.hasPermission(
          "peaceout.backpacks." + number)) {
        return number;
      }
    }

    return 0;
  }

  private String backpackPath(
      UUID uuid,
      int number) {
    return "players."
        + uuid
        + ".backpacks."
        + number;
  }

  private String displayName(String key) {
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
      case "backpack-sticky" ->
        "Smart Backpack Sorting";
      default ->
        key;
    };
  }

  private String formatMultiplier(double value) {
    return String.format(
        Locale.US,
        "%.2fx",
        value);
  }

  private void sendStatus(
      Player player,
      PlayerSettings settings) {
    player.sendMessage(
        ChatColor.AQUA + "[PeaceOut] "
            + ChatColor.GOLD + "Status");

    player.sendMessage(
        ChatColor.GRAY + "Master: "
            + (settings.isMasterEnabled()
                ? ChatColor.GREEN + "ON"
                : ChatColor.RED + "OFF"));

    for (String key : SETTINGS) {
      String value;

      if (isMultiplier(key)) {
        value = ChatColor.WHITE
            + formatMultiplier(
                settings.getMultiplier(key));
      } else {
        value = settings.isEnabled(key)
            ? ChatColor.GREEN + "ON"
            : ChatColor.RED + "OFF";
      }

      player.sendMessage(
          ChatColor.GRAY
              + displayName(key)
              + ": "
              + value);
    }
  }
}
