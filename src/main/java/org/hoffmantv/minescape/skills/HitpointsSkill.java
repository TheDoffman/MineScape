package org.hoffmantv.minescape.skills;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.hoffmantv.minescape.managers.SkillManager;

public class HitpointsSkill implements Listener {
    private final SkillManager skillManager;
    private final JavaPlugin plugin;

    public HitpointsSkill(SkillManager skillManager, JavaPlugin plugin) {
        this.skillManager = skillManager;
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        double damage = event.getFinalDamage();

        grantXp(player, (int) damage); // Assuming 1 XP for each point of damage for simplicity.

        int playerLevel = skillManager.getSkillLevel(player, SkillManager.Skill.HITPOINTS);
        int baseHealth = 20;  // Default health is 20 (10 hearts)

        int heartsGained = playerLevel / 5; // For every 5 levels, they gain 1 heart.
        int extraHealth = heartsGained * 2; // Multiply by 2 because each heart is 2 health points.

        // Setting max health but ensuring it does not exceed 40 (20 hearts).
        int newMaxHealth = Math.min(baseHealth + extraHealth, 40);

        player.setMaxHealth(newMaxHealth);
    }
    private void grantXp(Player player, int xpAmount) {
        skillManager.addXP(player, SkillManager.Skill.HITPOINTS, xpAmount);
        skillManager.saveSkillsToConfig();
    }
}
