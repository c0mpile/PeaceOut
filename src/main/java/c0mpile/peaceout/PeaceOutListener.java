package c0mpile.peaceout;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import com.destroystokyo.paper.event.player.PlayerElytraBoostEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

public final class PeaceOutListener implements Listener {

    private static final NamespacedKey BLOCK_SPEED_MODIFIER_KEY =
            new NamespacedKey("c0mpile", "peaceout-block-break-speed");

    private final PeaceOut plugin;
    private final Set<UUID> treeAndVeinTasks = new HashSet<>();

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
        treeAndVeinTasks.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onEntityTarget(EntityTargetLivingEntityEvent event) {
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
    public void onPlayerExperienceChange(PlayerExpChangeEvent event) {
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
        int adjusted = (int) Math.round(original * multiplier);

        event.setAmount(Math.max(0, adjusted));
    }

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        if (!ordinaryFeatureEnabled(player, "keep-inventory")) {
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

        if (!ordinaryFeatureEnabled(player, "drop-vacuum")) {
            return;
        }

        PlayerInventory inventory = player.getInventory();

        for (Item itemEntity : new ArrayList<>(event.getItems())) {
            ItemStack stack = itemEntity.getItemStack().clone();

            int originalAmount = stack.getAmount();
            MapInsertResult result = addToInventory(inventory, stack);

            if (result.addedAmount <= 0) {
                continue;
            }

            if (result.addedAmount >= originalAmount) {
                itemEntity.remove();
                event.getItems().remove(itemEntity);
            } else {
                stack.setAmount(originalAmount - result.addedAmount);
                itemEntity.setItemStack(stack);
            }
        }
    }

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        if (!player.isSneaking()) {
            return;
        }

        UUID uuid = player.getUniqueId();

        if (treeAndVeinTasks.contains(uuid)) {
            return;
        }

        PlayerSettings settings = plugin.getSettings(player);

        boolean veinMiner = settings.isEnabled("vein-miner");
        boolean treeChopper = settings.isEnabled("tree-chopper");

        Block block = event.getBlock();

        boolean isOre = isOre(block.getType());
        boolean isLog = isLog(block.getType());

        if ((!veinMiner || !isOre)
                && (!treeChopper || !isLog)) {
            return;
        }

        int limit = isOre
                ? plugin.getConfig().getInt(
                "limits.vein-miner",
                64
        )
                : plugin.getConfig().getInt(
                "limits.tree-chopper",
                128
        );

        treeAndVeinTasks.add(uuid);

        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    breakConnectedBlocks(
                            player,
                            block,
                            limit
                    );
                } finally {
                    treeAndVeinTasks.remove(uuid);
                }
            }
        }.runTask(plugin);
    }

    private void breakConnectedBlocks(
            Player player,
            Block origin,
            int limit
    ) {
        Material targetMaterial = origin.getType();

        Queue<Block> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();

        queue.add(origin);
        visited.add(blockKey(origin));

        int broken = 0;

        while (!queue.isEmpty() && broken < limit) {
            Block current = queue.poll();

            if (current.getType() != targetMaterial) {
                continue;
            }

            if (!current.equals(origin)) {
                current.breakNaturally(
                        player.getInventory().getItemInMainHand()
                );
                broken++;
            }

            for (Block nearby : adjacentBlocks(current)) {
                String key = blockKey(nearby);

                if (!visited.add(key)) {
                    continue;
                }

                if (nearby.getType() == targetMaterial) {
                    queue.add(nearby);
                }
            }
        }
    }
    
    public void applyBlockSpeedModifier(Player player) {
        AttributeInstance attribute =
                player.getAttribute(Attribute.BLOCK_BREAK_SPEED);

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
                        AttributeModifier.Operation.MULTIPLY_SCALAR_1
                );

        attribute.addModifier(modifier);
    }

    public void removeBlockSpeedModifier(Player player) {
        AttributeInstance attribute =
                player.getAttribute(Attribute.BLOCK_BREAK_SPEED);

        if (attribute == null) {
            return;
        }

        attribute.removeModifier(BLOCK_SPEED_MODIFIER_KEY);
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

    private boolean isOre(Material material) {
        String name = material.name();

        return name.endsWith("_ORE")
                || name.equals("ANCIENT_DEBRIS");
    }

    private boolean isLog(Material material) {
        String name = material.name();

        return name.endsWith("_LOG")
                || name.endsWith("_WOOD")
                || name.endsWith("_STEM")
                || name.endsWith("_HYPHAE");
    }

    private List<Block> adjacentBlocks(Block block) {
        List<Block> blocks = new ArrayList<>();

        blocks.add(block.getRelative(1, 0, 0));
        blocks.add(block.getRelative(-1, 0, 0));
        blocks.add(block.getRelative(0, 1, 0));
        blocks.add(block.getRelative(0, -1, 0));
        blocks.add(block.getRelative(0, 0, 1));
        blocks.add(block.getRelative(0, 0, -1));

        return blocks;
    }

    private String blockKey(Block block) {
        return block.getWorld().getUID()
                + ":"
                + block.getX()
                + ":"
                + block.getY()
                + ":"
                + block.getZ();
    }

    private MapInsertResult addToInventory(
            PlayerInventory inventory,
            ItemStack stack
    ) {
        int originalAmount = stack.getAmount();

        java.util.HashMap<Integer, ItemStack> leftovers =
                inventory.addItem(stack);

        int leftoverAmount = leftovers.values().stream()
                .mapToInt(ItemStack::getAmount)
                .sum();

        return new MapInsertResult(
                originalAmount - leftoverAmount
        );
    }

    private boolean nearlyEqual(double first, double second) {
        return Math.abs(first - second) < 0.001;
    }

    private record MapInsertResult(int addedAmount) {
    }
}
