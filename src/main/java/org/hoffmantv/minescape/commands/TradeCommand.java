package org.hoffmantv.minescape.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.hoffmantv.minescape.managers.TradeMenu;

public class TradeCommand implements CommandExecutor {

    private final TradeMenu tradeMenu;

    public TradeCommand(TradeMenu tradeMenu) {
        this.tradeMenu = tradeMenu;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /trade <player>");
            return true;
        }

        Player player = (Player) sender;
        Player target = Bukkit.getPlayer(args[0]);

        if (target == null || !target.isOnline()) {
            player.sendMessage(ChatColor.RED + "Player not found or not online.");
            return true;
        }

        if (player.equals(target)) {
            player.sendMessage(ChatColor.RED + "You cannot trade with yourself.");
            return true;
        }

        tradeMenu.startTrade(player, target);
        return true;
    }
}