package org.hoffmantv.minescape.listeners;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
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
    private final Map<UUID, Long> lastTeleportTime = new HashMap<>();

    public WaterListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Location to = event.getTo();
        Player player = event.getPlayer();
        if (to == null) {
            return;
        }

        UUID playerId = event.getPlayer().getUniqueId();
        long currentTime = System.currentTimeMillis();
        long lastTeleportRequestTime = lastTeleportTime.getOrDefault(playerId, 0L);

        // Check if the player is moving into water
        if (to.getBlock().getType() == Material.WATER) {
            // Teleport the player 2 blocks back every 3 seconds
            if (currentTime - lastTeleportRequestTime > 10) {
                Location from = event.getFrom();
                Location newLocation = from.clone().subtract(from.getDirection().multiply(2)); // Move 2 blocks back
                event.getPlayer().teleport(newLocation);
                lastTeleportTime.put(playerId, currentTime);

                // Send them a message
                player.sendMessage(ChatColor.RED + "You can't go in the water!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
        }
    }
}
