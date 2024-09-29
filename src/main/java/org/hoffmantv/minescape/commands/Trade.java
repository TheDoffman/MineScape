package org.hoffmantv.minescape.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.hoffmantv.minescape.managers.TradeMenu;

public class Trade implements CommandExecutor {
    private final TradeMenu tradeMenu;

    public Trade(TradeMenu tradeMenu) {
        this.tradeMenu = tradeMenu;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "Usage: /trade <player>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(ChatColor.RED + "Player not found or not online.");
            return true;
        }

        if (target.equals(player)) {
            player.sendMessage(ChatColor.RED + "You cannot trade with yourself.");
            return true;
        }

        // Check if the target player has already a pending trade request
        if (tradeMenu.hasPendingRequest(target, player)) {
            player.sendMessage(ChatColor.RED + "You have already sent a trade request to this player.");
            return true;
        }

        // Send a trade request
        tradeMenu.sendTradeRequest(player, target);
        return true;
    }
}