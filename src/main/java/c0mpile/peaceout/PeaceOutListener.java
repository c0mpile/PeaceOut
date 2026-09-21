package c0mpile.peaceout;

import com.destroystokyo.paper.event.player.PlayerElytraBoostEvent;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public final class PeaceOutListener implements Listener {

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

        PlayerSettings settings = plugin.getSettings(player);
        settings.initialize(player);

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

        if (!ordinaryFeatureEnabled(player, "targeting")) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        EntityDamageEvent.DamageCause cause =
                event.getCause();

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

        if (!ordinaryFeatureEnabled(player, "hunger")) {
            return;
        }

        if (event.getFoodLevel() < player.getFoodLevel()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onPlayerItemDamage(PlayerItemDamageEvent event) {
        Player player = event.getPlayer();

        if (!ordinaryFeatureEnabled(player, "durability")) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onElytraBoost(PlayerElytraBoostEvent event) {
        Player player = event.getPlayer();

        if (!ordinaryFeatureEnabled(player, "fireworks")) {
            return;
        }

        event.setShouldConsume(false);
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

        PlayerSettings settings = plugin.getSettings(player);
        double multiplier = settings.getMultiplier(
                "experience-multiplier"
        );

        if (nearlyEqual(multiplier, 1.0)) {
            return;
        }

        int original = event.getAmount();
        int adjusted = (int) Math.round(
                original * multiplier
        );

        event.setAmount(Math.max(0, adjusted));
    }

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        if (!ordinaryFeatureEnabled(
                player,
                "keep-inventory"
        )) {
            return;
        }

        event.setKeepInventory(true);
        event.setKeepLevel(true);
        event.getDrops().clear();
        event.setDroppedExp(0);
    }

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onBlockDropItem(BlockDropItemEvent event) {
        Player player = event.getPlayer();

        if (!ordinaryFeatureEnabled(
                player,
                "drop-vacuum"
        )) {
            return;
        }

        PlayerInventory inventory = player.getInventory();

        for (Item itemEntity :
                new java.util.ArrayList<>(event.getItems())) {
            ItemStack stack =
                    itemEntity.getItemStack().clone();

            int originalAmount = stack.getAmount();

            HashMap<Integer, ItemStack> leftovers =
                    inventory.addItem(stack);

            int leftoverAmount = leftovers.values()
                    .stream()
                    .mapToInt(ItemStack::getAmount)
                    .sum();

            int addedAmount =
                    originalAmount - leftoverAmount;

            if (addedAmount <= 0) {
                continue;
            }

            if (addedAmount >= originalAmount) {
                itemEntity.remove();
                event.getItems().remove(itemEntity);
            } else {
                stack.setAmount(leftoverAmount);
                itemEntity.setItemStack(stack);
            }
        }
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

    public void applyBlockSpeedModifier(Player player) {
        AttributeInstance attribute =
                player.getAttribute(
                        Attribute.BLOCK_BREAK_SPEED
                );

        if (attribute == null) {
            return;
        }

        removeBlockSpeedModifier(player);

        PlayerSettings settings = plugin.getSettings(player);

        double multiplier = settings.getMultiplier(
                "block-break-speed"
        );

        if (nearlyEqual(multiplier, 1.0)) {
            return;
        }

        AttributeModifier modifier =
                new AttributeModifier(
                        BLOCK_SPEED_MODIFIER_KEY,
                        multiplier - 1.0,
                        AttributeModifier.Operation
                                .MULTIPLY_SCALAR_1
                );

        attribute.addModifier(modifier);
    }

    public void removeBlockSpeedModifier(Player player) {
        AttributeInstance attribute =
                player.getAttribute(
                        Attribute.BLOCK_BREAK_SPEED
                );

        if (attribute == null) {
            return;
        }

        attribute.removeModifier(
                BLOCK_SPEED_MODIFIER_KEY
        );
    }

    private boolean nearlyEqual(
            double first,
            double second
    ) {
        return Math.abs(first - second) < 0.001;
    }
}
