package org.hoffmantv.minescape.listeners;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WaterListener implements Listener {

    private final JavaPlugin plugin;
    private final Map<UUID, Long> lastMessageTime = new HashMap<>();

    public WaterListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Location toLocation = event.getTo();
        Location fromLocation = event.getFrom();

        if (toLocation == null || fromLocation == null) {
            return;
        }

        // Only proceed if the player has moved to a different block
        if (toLocation.getBlockX() == fromLocation.getBlockX()
                && toLocation.getBlockY() == fromLocation.getBlockY()
                && toLocation.getBlockZ() == fromLocation.getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        Block toBlock = toLocation.getBlock();
        Material toBlockType = toBlock.getType();

        // Check if the block the player is moving into is water or waterlogged
        if (isWaterBlock(toBlock)) {
            // Cancel the movement
            event.setCancelled(true);

            // Only send message every 3 seconds
            long lastMessageSent = lastMessageTime.getOrDefault(playerId, 0L);
            if (currentTime - lastMessageSent > 3000) {
                player.sendMessage(ChatColor.RED + "You can't go in the water!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                lastMessageTime.put(playerId, currentTime);
            }
        }
    }

    // Helper method to check if a block is water or waterlogged
    private boolean isWaterBlock(Block block) {
        Material type = block.getType();

        if (type == Material.WATER) {
            return true;
        }

        // Check if the block is waterlogged
        if (block.getBlockData() instanceof Waterlogged) {
            Waterlogged waterlogged = (Waterlogged) block.getBlockData();
            return waterlogged.isWaterlogged();
        }

        return false;
    }
}