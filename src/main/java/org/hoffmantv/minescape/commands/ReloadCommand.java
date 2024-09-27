package org.hoffmantv.minescape.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class ReloadCommand implements CommandExecutor {

    private final Plugin plugin;

    public ReloadCommand(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if the sender has permission
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (!player.hasPermission("minescape.reload")) {
                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "✘ " + ChatColor.GRAY + "You don't have permission to execute this command.");
                return true;
            }
        }

        // Send a themed reload message to the sender
        String reloadStartMessage = ChatColor.DARK_RED + "" + ChatColor.BOLD + "⚠ " + ChatColor.GOLD + "MineScape" + ChatColor.GRAY + " is reloading... Please wait!";
        sender.sendMessage(reloadStartMessage);

        // Reload the server
        Bukkit.reload();

        // Notify that the reload is complete with a themed message
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            String reloadCompleteMessage = ChatColor.GREEN + "" + ChatColor.BOLD + "✔ " + ChatColor.GOLD + "MineScape" + ChatColor.GRAY + " has been reloaded successfully!";
            sender.sendMessage(reloadCompleteMessage);
        }, 20L); // 1-second delay to allow reload to finish

        return true;
    }
}