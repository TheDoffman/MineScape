package org.hoffmantv.minescape.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.ChatColor;

public class AlwaysDay implements CommandExecutor {

    private final JavaPlugin plugin;
    private final String PREFIX = ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "MineScape" + ChatColor.DARK_GRAY + "] " + ChatColor.RESET;

    public AlwaysDay(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(PREFIX + ChatColor.RED + "Only players can execute this command.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length != 1) {
            player.sendMessage(PREFIX + ChatColor.YELLOW + "Usage: " + ChatColor.AQUA + "/alwaysday <enable|disable>");
            return true;
        }

        if (args[0].equalsIgnoreCase("enable")) {
            plugin.getConfig().set("always-day", true);
            plugin.saveConfig();
            player.getWorld().setTime(1000);
            player.getWorld().setGameRuleValue("doDaylightCycle", "false");
            player.sendMessage(PREFIX + ChatColor.GREEN + "Always Day feature " + ChatColor.BOLD + "ENABLED" + ChatColor.RESET + ChatColor.GREEN + ". Enjoy the sunshine!");
            return true;
        }

        if (args[0].equalsIgnoreCase("disable")) {
            plugin.getConfig().set("always-day", false);
            plugin.saveConfig();
            player.getWorld().setGameRuleValue("doDaylightCycle", "true");
            player.sendMessage(PREFIX + ChatColor.RED + "Always Day feature " + ChatColor.BOLD + "DISABLED" + ChatColor.RESET + ChatColor.RED + ". The natural cycle returns.");
            return true;
        }

        player.sendMessage(PREFIX + ChatColor.YELLOW + "Usage: " + ChatColor.AQUA + "/alwaysday <enable|disable>");
        return true;
    }
}
