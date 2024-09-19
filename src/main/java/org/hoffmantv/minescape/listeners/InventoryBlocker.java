package org.hoffmantv.minescape.listeners;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.plugin.java.JavaPlugin;

public class InventoryBlocker implements Listener {

    private final JavaPlugin plugin;

    public InventoryBlocker(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player) {
            if (event.getInventory().getType() == InventoryType.CRAFTING) {
                event.setCancelled(true);
                Player player = (Player) event.getPlayer();
                player.sendMessage(ChatColor.RED + "You cannot open your inventory!");
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            event.setCancelled(true);
            Player player = (Player) event.getWhoClicked();
            player.sendMessage(ChatColor.RED + "You cannot interact with your inventory!");
        }
    }
}