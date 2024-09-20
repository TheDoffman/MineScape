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
    private final Map<UUID, Location> lastSafeLocation = new HashMap<>();
    private final Map<UUID, Long> lastMessageTime = new HashMap<>();
    private final long messageCooldown = 3000; // 3 seconds cooldown for messages

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

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Only process if the player moves to a new block
        if (toLocation.getBlockX() == fromLocation.getBlockX()
                && toLocation.getBlockY() == fromLocation.getBlockY()
                && toLocation.getBlockZ() == fromLocation.getBlockZ()) {
            return;
        }

        Block toBlock = toLocation.getBlock();

        // If the player is in water or waterlogged block
        if (isWaterBlock(toBlock)) {
            // Check for last safe location, if none, set current location as last safe
            Location safeLocation = lastSafeLocation.getOrDefault(playerId, fromLocation);

            // Teleport the player back to the last safe location
            player.teleport(safeLocation);

            // Check if we should send the message (use cooldown to avoid spam)
            long currentTime = System.currentTimeMillis();
            long lastMessageSent = lastMessageTime.getOrDefault(playerId, 0L);

            if (currentTime - lastMessageSent > messageCooldown) {
                player.sendMessage(ChatColor.RED + "You can't go in the water!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                lastMessageTime.put(playerId, currentTime);
            }

        } else {
            // If not in water, update the last safe location
            lastSafeLocation.put(playerId, toLocation);
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