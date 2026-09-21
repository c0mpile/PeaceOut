package c0mpile.peaceout;

import com.destroystokyo.paper.event.player.PlayerElytraBoostEvent;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public final class PeaceOutListener implements Listener {

    private static final int MAX_BACKPACKS = 24;

    private static final NamespacedKey BLOCK_SPEED_MODIFIER_KEY =
            new NamespacedKey(
                    "c0mpile",
                    "peaceout-block-break-speed"
            );

    private final PeaceOut plugin;

    public PeaceOutListener(PeaceOut plugin) {
        this.plugin = plugin;
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

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onEntityTarget(
            EntityTargetLivingEntityEvent event
    ) {
        if (!(event.getTarget() instanceof Player player)) {
            return;
        }

        if (ordinaryFeatureEnabled(player, "targeting")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
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

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (ordinaryFeatureEnabled(player, "hunger")
                && event.getFoodLevel() < player.getFoodLevel()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onPlayerItemDamage(PlayerItemDamageEvent event) {
        if (ordinaryFeatureEnabled(
                event.getPlayer(),
                "durability"
        )) {
            event.setCancelled(true);
        }
    }

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onElytraBoost(PlayerElytraBoostEvent event) {
        if (ordinaryFeatureEnabled(
                event.getPlayer(),
                "fireworks"
        )) {
            event.setShouldConsume(false);
        }
    }

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onPlayerExperienceChange(
            PlayerExpChangeEvent event
    ) {
        Player player = event.getPlayer();

        if (!player.hasPermission("peaceout.use")) {
            return;
        }

        double multiplier = plugin.getSettings(player)
                .getMultiplier("experience-multiplier");

        event.setAmount(Math.max(
                0,
                (int) Math.round(event.getAmount() * multiplier)
        ));
    }

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (ordinaryFeatureEnabled(
                event.getEntity(),
                "keep-inventory"
        )) {
            event.setKeepInventory(true);
            event.setKeepLevel(true);
            event.getDrops().clear();
            event.setDroppedExp(0);
        }
    }

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onBlockDropItem(BlockDropItemEvent event) {
        Player player = event.getPlayer();

        if (!ordinaryFeatureEnabled(player, "drop-vacuum")) {
            return;
        }

        for (Item item : new ArrayList<>(event.getItems())) {
            ItemStack original = item.getItemStack().clone();
            InsertResult result = addToInventory(
                    player.getInventory(),
                    original
            );

            if (result.added() <= 0) {
                continue;
            }

            if (result.remaining() <= 0) {
                item.remove();
                event.getItems().remove(item);
            } else {
                original.setAmount(result.remaining());
                item.setItemStack(original);
            }
        }
    }

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (!ordinaryFeatureEnabled(
                player,
                "backpack-pickup"
        )) {
            return;
        }

        if (!player.hasPermission("peaceout.backpack")) {
            return;
        }

        int backpackCount = getBackpackCount(player);

        if (backpackCount <= 0) {
            return;
        }

        Item itemEntity = event.getItem();
        ItemStack item = itemEntity.getItemStack().clone();

        InsertResult inventoryResult = addToInventory(
                player.getInventory(),
                item
        );

        if (inventoryResult.remaining() <= 0) {
            event.setCancelled(true);
            itemEntity.remove();
            return;
        }

        ItemStack remaining = item.clone();
        remaining.setAmount(inventoryResult.remaining());

        InsertResult backpackResult = addToBackpacks(
                player,
                remaining,
                backpackCount
        );

        if (backpackResult.added() <= 0) {
            return;
        }

        int remainingAmount = backpackResult.remaining();

        if (remainingAmount <= 0) {
            event.setCancelled(true);
            itemEntity.remove();
        } else {
            ItemStack leftover = item.clone();
            leftover.setAmount(remainingAmount);
            itemEntity.setItemStack(leftover);
        }
    }

    public void applyBlockSpeedModifier(Player player) {
        AttributeInstance attribute =
                player.getAttribute(Attribute.BLOCK_BREAK_SPEED);

        if (attribute == null) {
            return;
        }

        removeBlockSpeedModifier(player);

        double multiplier = plugin.getSettings(player)
                .getMultiplier("block-break-speed");

        if (nearlyEqual(multiplier, 1.0)) {
            return;
        }

        attribute.addModifier(
                new AttributeModifier(
                        BLOCK_SPEED_MODIFIER_KEY,
                        multiplier - 1.0,
                        AttributeModifier.Operation
                                .MULTIPLY_SCALAR_1
                )
        );
    }

    public void removeBlockSpeedModifier(Player player) {
        AttributeInstance attribute =
                player.getAttribute(Attribute.BLOCK_BREAK_SPEED);

        if (attribute != null) {
            attribute.removeModifier(BLOCK_SPEED_MODIFIER_KEY);
        }
    }

    private InsertResult addToInventory(
            PlayerInventory inventory,
            ItemStack stack
    ) {
        int original = stack.getAmount();

        HashMap<Integer, ItemStack> leftovers =
                inventory.addItem(stack);

        int remaining = leftovers.values()
                .stream()
                .mapToInt(ItemStack::getAmount)
                .sum();

        return new InsertResult(
                original - remaining,
                remaining
        );
    }

    private InsertResult addToBackpacks(
            Player player,
            ItemStack stack,
            int count
    ) {
        int original = stack.getAmount();

        for (int number = 1;
                number <= Math.min(count, MAX_BACKPACKS);
                number++) {
            ItemStack remaining = addToBackpack(
                    player,
                    number,
                    stack
            );

            if (remaining == null
                    || remaining.getAmount() <= 0) {
                return new InsertResult(
                        original,
                        0
                );
            }

            stack = remaining;
        }

        return new InsertResult(
                original - stack.getAmount(),
                stack.getAmount()
        );
    }

    private ItemStack addToBackpack(
            Player player,
            int number,
            ItemStack incoming
    ) {
        List<ItemStack> contents =
                plugin.getMenu().getBackpackContents(
                        player.getUniqueId(),
                        number
                );

        ItemStack remaining = incoming.clone();

        for (int slot = 0; slot < contents.size(); slot++) {
            ItemStack stored = contents.get(slot);

            if (stored == null
                    || stored.getType().isAir()
                    || !stored.isSimilar(remaining)) {
                continue;
            }

            int max = Math.min(
                    stored.getMaxStackSize(),
                    remaining.getMaxStackSize()
            );

            int space = max - stored.getAmount();

            if (space <= 0) {
                continue;
            }

            int moved = Math.min(
                    space,
                    remaining.getAmount()
            );

            stored.setAmount(stored.getAmount() + moved);
            remaining.setAmount(remaining.getAmount() - moved);

            if (remaining.getAmount() <= 0) {
                break;
            }
        }

        for (int slot = 0; slot < contents.size(); slot++) {
            if (remaining.getAmount() <= 0) {
                break;
            }

            ItemStack stored = contents.get(slot);

            if (stored != null
                    && !stored.getType().isAir()) {
                continue;
            }

            int moved = Math.min(
                    remaining.getAmount(),
                    remaining.getMaxStackSize()
            );

            ItemStack placed = remaining.clone();
            placed.setAmount(moved);
            contents.set(slot, placed);
            remaining.setAmount(
                    remaining.getAmount() - moved
            );
        }

        plugin.getMenu().saveBackpackContents(
                player.getUniqueId(),
                number,
                contents
        );

        return remaining.getAmount() <= 0
                ? null
                : remaining;
    }

    private int getBackpackCount(Player player) {
        for (int value = 24; value >= 1; value--) {
            if (player.hasPermission(
                    "peaceout.backpacks." + value
            )) {
                return value;
            }
        }

        return 0;
    }

    private boolean ordinaryFeatureEnabled(
            Player player,
            String key
    ) {
        if (!player.hasPermission("peaceout.use")) {
            return false;
        }

        PlayerSettings settings = plugin.getSettings(player);

        return settings.isMasterEnabled()
                && settings.isEnabled(key);
    }

    private boolean nearlyEqual(
            double first,
            double second
    ) {
        return Math.abs(first - second) < 0.001;
    }

    private record InsertResult(
            int added,
            int remaining
    ) {
    }
}
