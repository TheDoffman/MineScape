package org.hoffmantv.minescape.mobs;

import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDeathEvent;

public class MobListener implements Listener {

    @EventHandler
    public void onEntityCombust(EntityCombustEvent event) {
        // Check if the entity is a Zombie or Skeleton
        if (event.getEntityType() == EntityType.ZOMBIE || event.getEntityType() == EntityType.SKELETON) {
            // Cancel the event to prevent burning
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        // Check if the entity is a Wandering Trader
        if (event.getEntityType() == EntityType.WANDERING_TRADER) {
            // Cancel the event to prevent spawning
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        // Clear drops
        event.getDrops().clear();
        // Set experience to 0 to prevent dropping experience orbs
        event.setDroppedExp(0);
    }
}
