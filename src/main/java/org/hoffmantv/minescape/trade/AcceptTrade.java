package org.hoffmantv.minescape.trade;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.hoffmantv.minescape.trade.TradeMenu;

public class AcceptTrade implements CommandExecutor {

    private final TradeMenu tradeMenu;

    public AcceptTrade(TradeMenu tradeMenu) {
        this.tradeMenu = tradeMenu;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /accepttrade <player>");
            return true;
        }

        Player player = (Player) sender;
        Player target = Bukkit.getPlayer(args[0]);

        if (target == null || !target.isOnline()) {
            player.sendMessage(ChatColor.RED + "Player not found or not online.");
            return true;
        }

        if (tradeMenu.hasPendingRequest(target, player)) {
            tradeMenu.startTrade(target, player);
        } else {
            player.sendMessage(ChatColor.RED + "You have no trade request from this player.");
        }

        return true;
    }
}