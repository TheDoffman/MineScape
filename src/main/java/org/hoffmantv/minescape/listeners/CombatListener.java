package org.hoffmantv.minescape.listeners;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.hoffmantv.minescape.MineScape;
import org.hoffmantv.minescape.managers.CombatSession;
import org.hoffmantv.minescape.managers.SkillManager;
import org.hoffmantv.minescape.managers.CombatLevelSystem;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CombatListener implements Listener {
    private final MineScape plugin;
    private final SkillManager skillManager;
    private final CombatLevelSystem combatLevelSystem; // Add reference to CombatLevelSystem
    private final Map<UUID, CombatSession> activeSessions; // Tracks active sessions

    public CombatListener(MineScape plugin, SkillManager skillManager) {
        this.plugin = plugin;
        this.skillManager = skillManager;
        this.combatLevelSystem = plugin.getCombatLevelSystem(); // Initialize CombatLevelSystem
        this.activeSessions = new HashMap<>(); // Initialize active sessions map
    }

    @EventHandler
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player && event.getEntity() instanceof LivingEntity) {
            Player player = (Player) event.getDamager();
            LivingEntity mob = (LivingEntity) event.getEntity();

            // Check if the player is already in a combat session
            if (activeSessions.containsKey(player.getUniqueId())) {
                player.sendMessage("You are already in a combat session!");
                event.setCancelled(true);
                return;
            }

            // Prevent the player from dealing direct damage while in combat session
            event.setCancelled(true);

            // Create a new combat session and store it in the map
            CombatSession session = new CombatSession(player, mob, plugin, skillManager, combatLevelSystem);
            activeSessions.put(player.getUniqueId(), session);

            // Start the combat session
            session.startCombat();
        }
    }

    // Helper method to remove combat sessions when they end
    public void removeCombatSession(UUID playerUUID) {
        activeSessions.remove(playerUUID);
    }
}