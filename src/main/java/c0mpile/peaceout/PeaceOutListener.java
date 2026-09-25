package c0mpile.peaceout;

import com.destroystokyo.paper.event.player.PlayerElytraBoostEvent;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

public final class PeaceOutListener implements Listener {

  private static final int MAX_BACKPACKS = 24;

  private static final double DROP_VACUUM_RADIUS = 8.0;
  private static final double DROP_VACUUM_RADIUS_SQUARED = DROP_VACUUM_RADIUS * DROP_VACUUM_RADIUS;

  private static final NamespacedKey BLOCK_SPEED_MODIFIER_KEY = new NamespacedKey(
      "c0mpile",
      "peaceout-block-break-speed");

  private final PeaceOut plugin;
  private final BukkitTask vacuumTask;

  public PeaceOutListener(PeaceOut plugin) {
    this.plugin = plugin;

    this.vacuumTask = Bukkit.getScheduler().runTaskTimer(
        plugin,
        this::scanVacuumTargets,
        1L,
        2L);
  }

  public void stop() {
    vacuumTask.cancel();
  }

  // =========================================================================
  // Player join / quit
  // =========================================================================

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();

    plugin.getSettings(player).initialize(player);
    applyBlockSpeedModifier(player);

    for (ItemStack item : player.getInventory().getContents()) {
      PeaceOut.applyMaxStackSize(item);
    }
    for (ItemStack item : player.getEnderChest().getContents()) {
      PeaceOut.applyMaxStackSize(item);
    }
  }

  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent event) {
    removeBlockSpeedModifier(event.getPlayer());
  }

  // =========================================================================
  // Inventory stack size handling
  // =========================================================================

  @EventHandler(priority = EventPriority.LOWEST)
  public void onInventoryOpen(InventoryOpenEvent event) {
    for (ItemStack item : event.getInventory().getContents()) {
      PeaceOut.applyMaxStackSize(item);
    }
    for (ItemStack item : event.getPlayer().getInventory().getContents()) {
      PeaceOut.applyMaxStackSize(item);
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onInventoryClick(InventoryClickEvent event) {
    PeaceOut.applyMaxStackSize(event.getCurrentItem());
    PeaceOut.applyMaxStackSize(event.getCursor());
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onInventoryDrag(InventoryDragEvent event) {
    PeaceOut.applyMaxStackSize(event.getOldCursor());
    PeaceOut.applyMaxStackSize(event.getCursor());
    for (ItemStack item : event.getNewItems().values()) {
      PeaceOut.applyMaxStackSize(item);
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onPrepareCraft(PrepareItemCraftEvent event) {
    ItemStack result = event.getInventory().getResult();
    if (result != null) {
      PeaceOut.applyMaxStackSize(result);
    }
  }

  // =========================================================================
  // Protection events
  // =========================================================================

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onEntityTarget(
      EntityTargetLivingEntityEvent event) {
    if (!(event.getTarget() instanceof Player player)) {
      return;
    }

    if (ordinaryFeatureEnabled(player, "targeting")) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onEntityDamage(EntityDamageEvent event) {
    if (!(event.getEntity() instanceof Player player)) {
      return;
    }

    EntityDamageEvent.DamageCause cause = event.getCause();

    if (cause == EntityDamageEvent.DamageCause.DROWNING
        && ordinaryFeatureEnabled(player, "drowning")) {
      event.setCancelled(true);
      return;
    }

    if (cause == EntityDamageEvent.DamageCause.FALL
        && ordinaryFeatureEnabled(player, "fall")) {
      event.setCancelled(true);
      return;
    }

    if (cause == EntityDamageEvent.DamageCause.LAVA
        && ordinaryFeatureEnabled(player, "lava")) {
      event.setCancelled(true);
      return;
    }

    if ((cause == EntityDamageEvent.DamageCause.FIRE
        || cause == EntityDamageEvent.DamageCause.FIRE_TICK)
        && ordinaryFeatureEnabled(player, "fire")) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onFoodLevelChange(FoodLevelChangeEvent event) {
    if (!(event.getEntity() instanceof Player player)) {
      return;
    }

    if (ordinaryFeatureEnabled(player, "hunger")
        && event.getFoodLevel() < player.getFoodLevel()) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onPlayerItemDamage(PlayerItemDamageEvent event) {
    if (ordinaryFeatureEnabled(
        event.getPlayer(),
        "durability")) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onElytraBoost(PlayerElytraBoostEvent event) {
    if (ordinaryFeatureEnabled(
        event.getPlayer(),
        "fireworks")) {
      event.setShouldConsume(false);
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onPlayerExperienceChange(
      PlayerExpChangeEvent event) {
    Player player = event.getPlayer();

    if (!player.hasPermission("peaceout.use")) {
      return;
    }

    double multiplier = plugin.getSettings(player)
        .getMultiplier("experience-multiplier");

    event.setAmount(Math.max(
        0,
        (int) Math.round(
            event.getAmount() * multiplier)));
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onPlayerDeath(PlayerDeathEvent event) {
    if (ordinaryFeatureEnabled(
        event.getEntity(),
        "keep-inventory")) {
      event.setKeepInventory(true);
      event.setKeepLevel(true);
      event.getDrops().clear();
      event.setDroppedExp(0);
    }
  }

  // =========================================================================
  // Block drops – max stack size and vacuum
  // =========================================================================

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onBlockDropItem(BlockDropItemEvent event) {
    for (Item item : event.getItems()) {
      if (item.isValid()) {
        PeaceOut.applyMaxStackSize(item.getItemStack());
      }
    }

    Player player = event.getPlayer();

    if (!ordinaryFeatureEnabled(player, "drop-vacuum")) {
      return;
    }

    boolean backpackPickup = backpackPickupEnabled(player);

    for (Item item : new ArrayList<>(event.getItems())) {
      if (!item.isValid()) {
        continue;
      }

      if (backpackPickup) {
        if (routeItemToInventoryAndBackpack(player, item)) {
          if (!item.isValid() || item.isDead()) {
            event.getItems().remove(item);
          }
        }
        continue;
      }

      ItemStack original = item.getItemStack().clone();

      InsertResult result = addToInventory(
          player.getInventory(),
          original);

      if (result.added() <= 0) {
        continue;
      }

      if (result.remaining() <= 0) {
        item.remove();
        event.getItems().remove(item);
        continue;
      }

      ItemStack remainder = original.clone();
      remainder.setAmount(result.remaining());
      item.setItemStack(remainder);
    }
  }

  // =========================================================================
  // Pickup event – max stack size and backpack routing
  // =========================================================================

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onEntityPickupItem(EntityPickupItemEvent event) {
    Item item = event.getItem();
    if (item.isValid() && !item.isDead()) {
      PeaceOut.applyMaxStackSize(item.getItemStack());
    }

    if (!(event.getEntity() instanceof Player player)) {
      return;
    }

    if (!backpackPickupEnabled(player)) {
      return;
    }

    if (!item.isValid() || item.isDead()) {
      return;
    }

    boolean handled = routeItemToInventoryAndBackpack(
        player,
        item);

    if (handled) {
      event.setCancelled(true);
    }
  }

  // =========================================================================
  // Vacuum scan task
  // =========================================================================

  private void scanVacuumTargets() {
    for (Player player : Bukkit.getOnlinePlayers()) {
      if (!player.isOnline() || player.isDead()) {
        continue;
      }

      scanNearbyItems(player);
      scanNearbyExperience(player);
    }
  }

  private void scanNearbyItems(Player player) {
    boolean backpackPickup = backpackPickupEnabled(player);
    boolean dropVacuum = ordinaryFeatureEnabled(
        player,
        "drop-vacuum");

    if (!backpackPickup && !dropVacuum) {
      return;
    }

    for (Entity entity : player.getNearbyEntities(
        DROP_VACUUM_RADIUS,
        DROP_VACUUM_RADIUS,
        DROP_VACUUM_RADIUS)) {
      if (!(entity instanceof Item item)) {
        continue;
      }

      if (!item.isValid() || item.isDead()) {
        continue;
      }

      if (item.getPickupDelay() > 0) {
        continue;
      }

      if (player.getLocation().distanceSquared(
          item.getLocation()) > DROP_VACUUM_RADIUS_SQUARED) {
        continue;
      }

      PeaceOut.applyMaxStackSize(item.getItemStack());

      if (backpackPickup) {
        routeItemToInventoryAndBackpack(
            player,
            item);
      } else {
        collectItemForDropVacuum(
            player,
            item);
      }
    }
  }

  private void scanNearbyExperience(Player player) {
    if (!ordinaryFeatureEnabled(player, "drop-vacuum")) {
      return;
    }

    for (Entity entity : player.getNearbyEntities(
        DROP_VACUUM_RADIUS,
        DROP_VACUUM_RADIUS,
        DROP_VACUUM_RADIUS)) {
      if (!(entity instanceof ExperienceOrb orb)) {
        continue;
      }

      if (!orb.isValid() || orb.isDead()) {
        continue;
      }

      if (player.getLocation().distanceSquared(
          orb.getLocation()) > DROP_VACUUM_RADIUS_SQUARED) {
        continue;
      }

      collectExperience(player, orb);
    }
  }

  private void collectItemForDropVacuum(
      Player player,
      Item item) {
    if (!item.isValid() || item.isDead()) {
      return;
    }

    ItemStack original = item.getItemStack().clone();

    InsertResult result = addToInventory(
        player.getInventory(),
        original);

    if (result.added() <= 0) {
      return;
    }

    if (result.remaining() <= 0) {
      item.remove();
      return;
    }

    ItemStack remainder = original.clone();
    remainder.setAmount(result.remaining());
    item.setItemStack(remainder);
  }

  private void collectExperience(
      Player player,
      ExperienceOrb orb) {
    if (!orb.isValid() || orb.isDead()) {
      return;
    }

    int amount = orb.getExperience();

    if (amount <= 0) {
      orb.remove();
      return;
    }

    double multiplier = plugin.getSettings(player)
        .getMultiplier("experience-multiplier");

    int adjustedAmount = Math.max(
        0,
        (int) Math.round(amount * multiplier));

    player.giveExp(adjustedAmount);
    orb.remove();
  }

  // =========================================================================
  // Item routing – inventory + backpacks
  // =========================================================================

  private boolean routeItemToInventoryAndBackpack(
      Player player,
      Item item) {
    if (!item.isValid() || item.isDead()) {
      return false;
    }

    ItemStack original = item.getItemStack().clone();
    int originalAmount = original.getAmount();
    int backpackCount = getBackpackCount(player);

    int matchedBackpack = -1;
    if (backpackStickyEnabled(player)) {
      matchedBackpack = findMatchingBackpack(
          player,
          original,
          backpackCount);
    }

    if (matchedBackpack != -1) {
      ItemStack remaining = addToBackpack(
          player,
          matchedBackpack,
          original);

      if (remaining == null || remaining.getAmount() <= 0) {
        item.remove();
        return true;
      }

      InsertResult backpackResult = addToBackpacks(
          player,
          remaining,
          backpackCount,
          matchedBackpack);

      if (backpackResult.remaining() <= 0) {
        item.remove();
        return true;
      }

      ItemStack invRemainder = remaining.clone();
      invRemainder.setAmount(backpackResult.remaining());

      InsertResult inventoryResult = addToInventory(
          player.getInventory(),
          invRemainder);

      int totalRemaining = inventoryResult.remaining();
      int totalAdded = originalAmount - totalRemaining;

      if (totalAdded <= 0) {
        return false;
      }

      if (totalRemaining <= 0) {
        item.remove();
        return true;
      }

      ItemStack finalRemainder = original.clone();
      finalRemainder.setAmount(totalRemaining);
      item.setItemStack(finalRemainder);
      return true;
    }

    InsertResult inventoryResult = addToInventory(
        player.getInventory(),
        original);

    if (inventoryResult.remaining() <= 0) {
      item.remove();
      return true;
    }

    ItemStack remainder = original.clone();
    remainder.setAmount(inventoryResult.remaining());

    InsertResult backpackResult = addToBackpacks(
        player,
        remainder,
        backpackCount);

    if (backpackResult.added() <= 0) {
      if (inventoryResult.added() > 0) {
        item.setItemStack(remainder);
        return true;
      }

      return false;
    }

    if (backpackResult.remaining() <= 0) {
      item.remove();
      return true;
    }

    ItemStack finalRemainder = remainder.clone();
    finalRemainder.setAmount(
        backpackResult.remaining());

    item.setItemStack(finalRemainder);
    return true;
  }

  // =========================================================================
  // addToInventory – respects up to 99 stack size
  // =========================================================================

  private InsertResult addToInventory(
      PlayerInventory inventory,
      ItemStack stack) {
    int originalAmount = stack.getAmount();
    int remaining = originalAmount;

    int cap = effectiveCap(stack);

    // Pass 1 – merge into existing partial stacks (slots 0-35).
    for (int slot = 0; slot <= 35 && remaining > 0; slot++) {
      ItemStack existing = inventory.getItem(slot);

      if (existing == null
          || existing.getType().isAir()
          || !existing.isSimilar(stack)) {
        continue;
      }

      PeaceOut.applyMaxStackSize(existing);

      int free = cap - existing.getAmount();

      if (free <= 0) {
        continue;
      }

      int moved = Math.min(free, remaining);
      existing.setAmount(existing.getAmount() + moved);
      remaining -= moved;
    }

    // Pass 2 – fill empty slots (slots 0-35).
    for (int slot = 0; slot <= 35 && remaining > 0; slot++) {
      ItemStack existing = inventory.getItem(slot);

      if (existing != null && !existing.getType().isAir()) {
        continue;
      }

      int moved = Math.min(remaining, cap);
      ItemStack placed = stack.clone();
      placed.setAmount(moved);
      PeaceOut.applyMaxStackSize(placed);
      inventory.setItem(slot, placed);
      remaining -= moved;
    }

    return new InsertResult(originalAmount - remaining, remaining);
  }

  // =========================================================================
  // addToBackpacks – respects up to 99 stack size and skips slot 49
  // =========================================================================

  private InsertResult addToBackpacks(
      Player player,
      ItemStack stack,
      int count) {
    return addToBackpacks(player, stack, count, -1);
  }

  private InsertResult addToBackpacks(
      Player player,
      ItemStack stack,
      int count,
      int skipBackpack) {
    int originalAmount = stack.getAmount();
    ItemStack remaining = stack.clone();

    for (int number = 1; number <= Math.min(count, MAX_BACKPACKS); number++) {
      if (number == skipBackpack) {
        continue;
      }

      ItemStack next = addToBackpack(
          player,
          number,
          remaining);

      if (next == null || next.getAmount() <= 0) {
        return new InsertResult(
            originalAmount,
            0);
      }

      remaining = next;
    }

    return new InsertResult(
        originalAmount - remaining.getAmount(),
        remaining.getAmount());
  }

  private int findMatchingBackpack(
      Player player,
      ItemStack stack,
      int count) {
    int max = Math.min(count, MAX_BACKPACKS);
    for (int number = 1; number <= max; number++) {
      List<ItemStack> contents = plugin.getMenu().getBackpackContents(
          player.getUniqueId(),
          number);

      for (int slot = 0; slot < contents.size(); slot++) {
        if (slot == 49) {
          continue;
        }

        ItemStack stored = contents.get(slot);

        if (stored != null
            && !stored.getType().isAir()
            && stored.isSimilar(stack)) {
          return number;
        }
      }
    }

    return -1;
  }

  private ItemStack addToBackpack(
      Player player,
      int number,
      ItemStack incoming) {
    List<ItemStack> contents = plugin.getMenu().getBackpackContents(
        player.getUniqueId(),
        number);

    ItemStack remaining = incoming.clone();
    boolean changed = false;

    int cap = effectiveCap(remaining);

    for (int slot = 0; slot < contents.size(); slot++) {
      if (slot == 49) {
        continue;
      }

      ItemStack stored = contents.get(slot);

      if (stored == null
          || stored.getType().isAir()
          || !stored.isSimilar(remaining)) {
        continue;
      }

      PeaceOut.applyMaxStackSize(stored);

      int free = cap - stored.getAmount();

      if (free <= 0) {
        continue;
      }

      int moved = Math.min(
          free,
          remaining.getAmount());

      stored.setAmount(
          stored.getAmount() + moved);

      remaining.setAmount(
          remaining.getAmount() - moved);

      changed = true;

      if (remaining.getAmount() <= 0) {
        break;
      }
    }

    for (int slot = 0; slot < contents.size(); slot++) {
      if (slot == 49
          || remaining.getAmount() <= 0) {
        continue;
      }

      ItemStack stored = contents.get(slot);

      if (stored != null
          && !stored.getType().isAir()) {
        continue;
      }

      int moved = Math.min(
          remaining.getAmount(),
          cap);

      ItemStack placed = remaining.clone();
      placed.setAmount(moved);
      PeaceOut.applyMaxStackSize(placed);
      contents.set(slot, placed);

      remaining.setAmount(
          remaining.getAmount() - moved);

      changed = true;
    }

    if (changed) {
      plugin.getMenu().saveBackpackContents(
          player.getUniqueId(),
          number,
          contents);
    }

    return remaining.getAmount() <= 0
        ? null
        : remaining;
  }

  private static int effectiveCap(ItemStack stack) {
    return stack.getType().getMaxStackSize() <= 1 ? 1 : PeaceOut.MAX_STACK_SIZE;
  }

  // =========================================================================
  // Permission / feature checks
  // =========================================================================

  private boolean backpackPickupEnabled(Player player) {
    if (!player.hasPermission("peaceout.use")
        || !player.hasPermission("peaceout.backpack")
        || getBackpackCount(player) <= 0) {
      return false;
    }

    PlayerSettings settings = plugin.getSettings(player);

    return settings.isEnabled("backpack")
        && settings.isEnabled("backpack-pickup");
  }

  private boolean backpackStickyEnabled(Player player) {
    if (!backpackPickupEnabled(player)) {
      return false;
    }

    return plugin.getSettings(player).isEnabled("backpack-sticky");
  }

  private boolean ordinaryFeatureEnabled(
      Player player,
      String key) {
    if (!player.hasPermission("peaceout.use")) {
      return false;
    }

    PlayerSettings settings = plugin.getSettings(player);

    return settings.isMasterEnabled()
        && settings.isEnabled(key);
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

  // =========================================================================
  // Block-break speed modifier
  // =========================================================================

  public void applyBlockSpeedModifier(Player player) {
    AttributeInstance attribute = player.getAttribute(Attribute.BLOCK_BREAK_SPEED);

    if (attribute == null) {
      return;
    }

    removeBlockSpeedModifier(player);

    double multiplier = plugin.getSettings(player)
        .getMultiplier("block-break-speed");

    if (Math.abs(multiplier - 1.0) < 0.001) {
      return;
    }

    attribute.addModifier(
        new AttributeModifier(
            BLOCK_SPEED_MODIFIER_KEY,
            multiplier - 1.0,
            AttributeModifier.Operation.MULTIPLY_SCALAR_1));
  }

  public void removeBlockSpeedModifier(Player player) {
    AttributeInstance attribute = player.getAttribute(Attribute.BLOCK_BREAK_SPEED);

    if (attribute != null) {
      attribute.removeModifier(
          BLOCK_SPEED_MODIFIER_KEY);
    }
  }

  // =========================================================================
  // Internal value type
  // =========================================================================

  private record InsertResult(
      int added,
      int remaining) {
  }
}
