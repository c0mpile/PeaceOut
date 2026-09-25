package c0mpile.peaceout;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PeaceOutCommand
        implements CommandExecutor, TabCompleter {

    private static final String PREFIX =
            ChatColor.AQUA + "[PeaceOut] "
                    + ChatColor.RESET;

    private static final List<String> TOGGLES = List.of(
            "drowning",
            "targeting",
            "hunger",
            "regeneration",
            "fall",
            "lava",
            "fire",
            "durability",
            "fireworks",
            "keep-inventory",
            "drop-vacuum",
            "trash",
            "backpack",
            "backpack-pickup",
            "backpack-sticky"
    );

    private final PeaceOut plugin;

    public PeaceOutCommand(PeaceOut plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(
                    PREFIX + ChatColor.RED
                            + "Only players can use this command."
            );
            return true;
        }

        String name = command.getName()
                .toLowerCase(Locale.ROOT);

        if (name.equals("trash")) {
            if (!player.hasPermission("peaceout.trash")) {
                player.sendMessage(
                        PREFIX + ChatColor.RED
                                + "You do not have trash permission."
                );
                return true;
            }

            if (!plugin.getSettings(player)
                    .isEnabled("trash")) {
                player.sendMessage(
                        PREFIX + ChatColor.RED
                                + "Trash is disabled in your menu."
                );
                return true;
            }

            plugin.getMenu().openTrash(player);
            return true;
        }

        if (name.equals("bp")) {
            if (!player.hasPermission("peaceout.backpack")) {
                player.sendMessage(
                        PREFIX + ChatColor.RED
                                + "You do not have backpack permission."
                );
                return true;
            }

            if (!plugin.getSettings(player)
                    .isEnabled("backpack")) {
                player.sendMessage(
                        PREFIX + ChatColor.RED
                                + "Backpacks are disabled in your menu."
                );
                return true;
            }

            int count = getBackpackCount(player);

            if (count <= 0) {
                player.sendMessage(
                        PREFIX + ChatColor.RED
                                + "You do not have any backpacks."
                );
                return true;
            }

            if (args.length == 0) {
                plugin.getMenu().openBackpackSelector(player);
                return true;
            }

            try {
                int number = Integer.parseInt(args[0]);

                if (number < 1 || number > count) {
                    throw new NumberFormatException();
                }

                plugin.getMenu().openBackpack(
                        player,
                        number
                );
            } catch (NumberFormatException exception) {
                player.sendMessage(
                        PREFIX + ChatColor.RED
                                + "Use /bp or /bp <number>, where "
                                + "number is between 1 and "
                                + count
                                + "."
                );
            }

            return true;
        }

        if (!player.hasPermission("peaceout.use")
                && !(args.length > 0
                && args[0].equalsIgnoreCase("admin")
                && player.hasPermission("peaceout.admin"))) {
            player.sendMessage(
                    PREFIX + ChatColor.RED
                            + "You do not have permission."
            );
            return true;
        }

        if (args.length == 0) {
            plugin.getMenu().openPersonalMenu(player);
            return true;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        PlayerSettings settings = plugin.getSettings(player);

        if (action.equals("admin")) {
            if (!player.hasPermission("peaceout.admin")) {
                player.sendMessage(
                        PREFIX + ChatColor.RED
                                + "You do not have admin permission."
                );
                return true;
            }

            plugin.getMenu().openAdminMenu(player);
            return true;
        }

        if (action.equals("status")) {
            sendStatus(player, settings);
            return true;
        }

        if (action.equals("on")
                || action.equals("off")
                || action.equals("toggle")) {
            boolean enabled = action.equals("toggle")
                    ? !settings.isMasterEnabled()
                    : action.equals("on");

            settings.setMasterEnabled(enabled);

            player.sendMessage(
                    PREFIX + ChatColor.GREEN
                            + "Master switch "
                            + (enabled ? "enabled." : "disabled.")
            );
            return true;
        }

        if (TOGGLES.contains(action)) {
            handleToggle(player, settings, action, args);
            return true;
        }

        if (action.equals("xp")
                || action.equals("experience")
                || action.equals("experience-multiplier")) {
            handleMultiplier(
                    player,
                    settings,
                    "experience-multiplier",
                    args
            );
            return true;
        }

        if (action.equals("block-speed")
                || action.equals("break-speed")
                || action.equals("block-break-speed")) {
            handleMultiplier(
                    player,
                    settings,
                    "block-break-speed",
                    args
            );
            return true;
        }

        sendHelp(player, label);
        return true;
    }

    private void handleToggle(
            Player player,
            PlayerSettings settings,
            String key,
            String[] args
    ) {
        boolean value;

        if (args.length == 1) {
            value = settings.toggle(key);
        } else {
            String argument = args[1]
                    .toLowerCase(Locale.ROOT);

            if (argument.equals("on")
                    || argument.equals("true")
                    || argument.equals("enable")
                    || argument.equals("enabled")) {
                value = true;
            } else if (argument.equals("off")
                    || argument.equals("false")
                    || argument.equals("disable")
                    || argument.equals("disabled")) {
                value = false;
            } else {
                player.sendMessage(
                        PREFIX + ChatColor.RED
                                + "Use on or off."
                );
                return;
            }

            settings.setEnabled(key, value);
        }

        player.sendMessage(
                PREFIX + ChatColor.WHITE
                        + displayName(key)
                        + ChatColor.GRAY
                        + ": "
                        + (value
                        ? ChatColor.GREEN + "enabled."
                        : ChatColor.RED + "disabled.")
        );
    }

    private void handleMultiplier(
            Player player,
            PlayerSettings settings,
            String key,
            String[] args
    ) {
        if (args.length < 2) {
            player.sendMessage(
                    PREFIX + ChatColor.YELLOW
                            + displayName(key)
                            + ": "
                            + settings.getMultiplier(key)
                            + "x"
            );
            return;
        }

        try {
            double value = Double.parseDouble(args[1]);

            if (value < 0.25 || value > 10.0) {
                throw new NumberFormatException();
            }

            settings.setMultiplier(key, value);

            if (key.equals("block-break-speed")) {
                plugin.getListener()
                        .applyBlockSpeedModifier(player);
            }

            player.sendMessage(
                    PREFIX + ChatColor.GREEN
                            + displayName(key)
                            + " set to "
                            + value
                            + "x."
            );
        } catch (NumberFormatException exception) {
            player.sendMessage(
                    PREFIX + ChatColor.RED
                            + "Multiplier must be between "
                            + "0.25 and 10.0."
            );
        }
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

    private void sendStatus(
            Player player,
            PlayerSettings settings
    ) {
        player.sendMessage(
                PREFIX + ChatColor.GOLD
                        + "PeaceOut status"
        );

        player.sendMessage(
                ChatColor.GRAY + "Master switch: "
                        + (settings.isMasterEnabled()
                        ? ChatColor.GREEN + "ON"
                        : ChatColor.RED + "OFF")
        );

        for (String key : TOGGLES) {
            player.sendMessage(
                    ChatColor.GRAY + displayName(key)
                            + ": "
                            + (settings.isEnabled(key)
                            ? ChatColor.GREEN + "ON"
                            : ChatColor.RED + "OFF")
            );
        }

        player.sendMessage(
                ChatColor.GRAY
                        + "Experience multiplier: "
                        + settings.getMultiplier(
                        "experience-multiplier"
                )
                        + "x"
        );

        player.sendMessage(
                ChatColor.GRAY
                        + "Block-break speed: "
                        + settings.getMultiplier(
                        "block-break-speed"
                )
                        + "x"
        );

        player.sendMessage(
                ChatColor.GRAY + "Backpacks available: "
                        + getBackpackCount(player)
        );
    }

    private void sendHelp(Player player, String label) {
        player.sendMessage(
                PREFIX + ChatColor.GOLD
                        + "PeaceOut commands"
        );
        player.sendMessage(
                ChatColor.YELLOW + "/" + label
                        + ChatColor.GRAY
                        + " - Open the settings menu."
        );
        player.sendMessage(
                ChatColor.YELLOW + "/" + label
                        + " <setting> [on|off]"
        );
        player.sendMessage(
                ChatColor.YELLOW + "/trash"
                        + ChatColor.GRAY
                        + " - Open the trash can."
        );
        player.sendMessage(
                ChatColor.YELLOW + "/bp [number]"
                        + ChatColor.GRAY
                        + " - Open a backpack."
        );
        player.sendMessage(
                ChatColor.YELLOW + "/" + label
                        + " admin"
                        + ChatColor.GRAY
                        + " - Open the admin menu."
        );
    }

    private String displayName(String key) {
        return switch (key) {
            case "drowning" -> "Drowning protection";
            case "targeting" -> "Mob targeting protection";
            case "hunger" -> "No food drain";
            case "regeneration" ->
                    "Increased health regeneration";
            case "fall" -> "No fall damage";
            case "lava" -> "No lava damage";
            case "fire" -> "No fire damage";
            case "durability" -> "Infinite durability";
            case "fireworks" -> "Infinite fireworks";
            case "keep-inventory" -> "Keep inventory";
            case "drop-vacuum" -> "Drop vacuum";
            case "trash" -> "Trash can";
            case "backpack" -> "Backpacks";
            case "backpack-pickup" ->
                    "Automatic backpack pickup";
            case "backpack-sticky" ->
                    "Smart Backpack Sorting";
            case "experience-multiplier" ->
                    "Experience multiplier";
            case "block-break-speed" ->
                    "Block-break speed";
            default -> key;
        };
    }

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args
    ) {
        if (!(sender instanceof Player player)) {
            return List.of();
        }

        if (command.getName().equalsIgnoreCase("bp")) {
            List<String> result = new ArrayList<>();
            int count = getBackpackCount(player);

            for (int number = 1; number <= count; number++) {
                result.add(String.valueOf(number));
            }

            return result;
        }

        if (args.length == 1
                && player.hasPermission("peaceout.use")) {
            return TOGGLES.stream()
                    .filter(value -> value.startsWith(
                            args[0].toLowerCase(Locale.ROOT)
                    ))
                    .toList();
        }

        if (args.length == 2
                && TOGGLES.contains(
                args[0].toLowerCase(Locale.ROOT)
        )) {
            return List.of("on", "off");
        }

        return List.of();
    }
}
