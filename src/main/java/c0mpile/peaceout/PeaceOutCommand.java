package c0mpile.peaceout;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class PeaceOutCommand
        implements CommandExecutor, TabCompleter {

    private static final String PREFIX =
            ChatColor.AQUA + "[PeaceOut] " + ChatColor.RESET;

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
            "vein-miner",
            "tree-chopper"
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

        if (!player.hasPermission("peaceout.use")
                && !(args.length > 0 && args[0].equalsIgnoreCase("admin")
                && player.hasPermission("peaceout.admin"))) {
            player.sendMessage(
                    PREFIX + ChatColor.RED
                            + "You do not have permission to use PeaceOut."
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
            boolean enabled;

            if (action.equals("toggle")) {
                enabled = !settings.isMasterEnabled();
            } else {
                enabled = action.equals("on");
            }

            settings.setMasterEnabled(enabled);

            player.sendMessage(
                    PREFIX + ChatColor.GREEN
                            + "PeaceOut master switch: "
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
            String argument = args[1].toLowerCase(Locale.ROOT);

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
                                + "Use the setting by itself, or use on/off."
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
                            + settings.getMultiplier(key) + "x"
            );
            return;
        }

        double value;

        try {
            value = Double.parseDouble(args[1]);
        } catch (NumberFormatException exception) {
            player.sendMessage(
                    PREFIX + ChatColor.RED
                            + "That is not a valid multiplier."
            );
            return;
        }

        if (value < 0.25 || value > 10.0) {
            player.sendMessage(
                    PREFIX + ChatColor.RED
                            + "Multiplier must be between 0.25 and 10.0."
            );
            return;
        }

        settings.setMultiplier(key, value);

        if (key.equals("block-break-speed")) {
            plugin.getListener().applyBlockSpeedModifier(player);
        }

        player.sendMessage(
                PREFIX + ChatColor.GREEN
                        + displayName(key)
                        + " set to "
                        + value
                        + "x."
        );
    }

    private void sendStatus(
            Player player,
            PlayerSettings settings
    ) {
        player.sendMessage(
                PREFIX + ChatColor.GOLD + "PeaceOut status"
        );

        player.sendMessage(
                ChatColor.GRAY + "Master switch: "
                        + (settings.isMasterEnabled()
                        ? ChatColor.GREEN + "ON"
                        : ChatColor.RED + "OFF")
        );

        for (String key : TOGGLES) {
            player.sendMessage(
                    ChatColor.GRAY + displayName(key) + ": "
                            + (settings.isEnabled(key)
                            ? ChatColor.GREEN + "ON"
                            : ChatColor.RED + "OFF")
            );
        }

        player.sendMessage(
                ChatColor.GRAY + "Experience multiplier: "
                        + settings.getMultiplier("experience-multiplier")
                        + "x"
        );

        player.sendMessage(
                ChatColor.GRAY + "Block-break speed: "
                        + settings.getMultiplier("block-break-speed")
                        + "x"
        );
    }

    private void sendHelp(Player player, String label) {
        player.sendMessage(
                PREFIX + ChatColor.GOLD + "PeaceOut commands"
        );
        player.sendMessage(
                ChatColor.YELLOW + "/" + label
                        + ChatColor.GRAY + " - Open your settings menu."
        );
        player.sendMessage(
                ChatColor.YELLOW + "/" + label + " <setting>"
                        + ChatColor.GRAY + " - Toggle a setting."
        );
        player.sendMessage(
                ChatColor.YELLOW + "/" + label + " <setting> on|off"
                        + ChatColor.GRAY + " - Explicitly set it."
        );
        player.sendMessage(
                ChatColor.YELLOW + "/" + label + " xp <value>"
                        + ChatColor.GRAY + " - Set XP multiplier."
        );
        player.sendMessage(
                ChatColor.YELLOW + "/" + label + " block-speed <value>"
                        + ChatColor.GRAY + " - Set mining speed."
        );
        player.sendMessage(
                ChatColor.YELLOW + "/" + label + " status"
                        + ChatColor.GRAY + " - Show current settings."
        );
        player.sendMessage(
                ChatColor.YELLOW + "/" + label + " admin"
                        + ChatColor.GRAY + " - Open admin menu."
        );
    }

    private String displayName(String key) {
        return switch (key) {
            case "drowning" -> "Drowning protection";
            case "targeting" -> "Mob targeting protection";
            case "hunger" -> "No food drain";
            case "regeneration" -> "Increased regeneration";
            case "fall" -> "No fall damage";
            case "lava" -> "No lava damage";
            case "fire" -> "No fire damage";
            case "durability" -> "Infinite durability";
            case "fireworks" -> "Infinite fireworks";
            case "keep-inventory" -> "Keep inventory";
            case "drop-vacuum" -> "Drop vacuum";
            case "vein-miner" -> "Instant vein miner";
            case "tree-chopper" -> "Instant tree chopper";
            case "experience-multiplier" -> "Experience multiplier";
            case "block-break-speed" -> "Block-break speed";
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
        if (!(sender instanceof Player player)
                || !player.hasPermission("peaceout.use")) {
            return List.of();
        }

        if (args.length == 1) {
            List<String> values = new ArrayList<>(TOGGLES);
            values.addAll(Arrays.asList(
                    "on",
                    "off",
                    "toggle",
                    "status",
                    "admin",
                    "xp",
                    "experience",
                    "block-speed",
                    "break-speed"
            ));

            return values.stream()
                    .filter(value -> value.startsWith(
                            args[0].toLowerCase(Locale.ROOT)
                    ))
                    .toList();
        }

        if (args.length == 2
                && (TOGGLES.contains(args[0])
                || args[0].equalsIgnoreCase("all"))) {
            return List.of("on", "off");
        }

        return List.of();
    }
}
