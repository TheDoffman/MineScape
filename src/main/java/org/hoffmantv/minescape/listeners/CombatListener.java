package org.hoffmantv.minescape.listeners;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.hoffmantv.minescape.MineScape;
import org.hoffmantv.minescape.managers.CombatSession;
import org.hoffmantv.minescape.managers.SkillManager;

public class CombatListener implements Listener {
    private final MineScape plugin;
    private final SkillManager skillManager;

    public CombatListener(MineScape plugin, SkillManager skillManager) {
        this.plugin = plugin;
        this.skillManager = skillManager;
    }

    @EventHandler
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player && event.getEntity() instanceof LivingEntity) {
            Player player = (Player) event.getDamager();
            LivingEntity mob = (LivingEntity) event.getEntity();

            // Create a new combat session with all required parameters
            CombatSession combatSession = new CombatSession(player, mob, plugin, skillManager);

            // Additional logic to manage the combat session, e.g., storing it in a map
            // sessions.put(player.getUniqueId(), combatSession);
        }
    }
}