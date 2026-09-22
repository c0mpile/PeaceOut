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
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
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

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();

    plugin.getSettings(player).initialize(player);
    applyBlockSpeedModifier(player);
  }

  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent event) {
    removeBlockSpeedModifier(event.getPlayer());
  }

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

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onBlockDropItem(BlockDropItemEvent event) {
    Player player = event.getPlayer();

    if (!ordinaryFeatureEnabled(player, "drop-vacuum")) {
      return;
    }

    for (Item item : new ArrayList<>(event.getItems())) {
      if (!item.isValid()) {
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

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onEntityPickupItem(EntityPickupItemEvent event) {
    if (!(event.getEntity() instanceof Player player)) {
      return;
    }

    if (!backpackPickupEnabled(player)) {
      return;
    }

    Item item = event.getItem();

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

  private boolean routeItemToInventoryAndBackpack(
      Player player,
      Item item) {
    if (!item.isValid() || item.isDead()) {
      return false;
    }

    ItemStack original = item.getItemStack().clone();

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
        getBackpackCount(player));

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

  private InsertResult addToInventory(
      PlayerInventory inventory,
      ItemStack stack) {
    int originalAmount = stack.getAmount();

    HashMap<Integer, ItemStack> leftovers = inventory.addItem(stack.clone());

    int remainingAmount = leftovers.values()
        .stream()
        .mapToInt(ItemStack::getAmount)
        .sum();

    return new InsertResult(
        originalAmount - remainingAmount,
        remainingAmount);
  }

  private InsertResult addToBackpacks(
      Player player,
      ItemStack stack,
      int count) {
    int originalAmount = stack.getAmount();
    ItemStack remaining = stack.clone();

    for (int number = 1; number <= Math.min(count, MAX_BACKPACKS); number++) {
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

  private ItemStack addToBackpack(
      Player player,
      int number,
      ItemStack incoming) {
    List<ItemStack> contents = plugin.getMenu().getBackpackContents(
        player.getUniqueId(),
        number);

    ItemStack remaining = incoming.clone();
    boolean changed = false;

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

      int max = Math.min(
          stored.getMaxStackSize(),
          remaining.getMaxStackSize());

      int free = max - stored.getAmount();

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
          remaining.getMaxStackSize());

      ItemStack placed = remaining.clone();
      placed.setAmount(moved);
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

  private record InsertResult(
      int added,
      int remaining) {
  }
}
