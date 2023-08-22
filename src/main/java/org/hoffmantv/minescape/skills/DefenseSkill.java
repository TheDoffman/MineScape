package org.hoffmantv.minescape.skills;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.hoffmantv.minescape.managers.SkillManager;

public class DefenseSkill implements Listener {

    private final SkillManager skillManager;

    public DefenseSkill(SkillManager skillManager) {
        this.skillManager = skillManager;
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;  // Return if the damaged entity isn't a player
        }

        Player player = (Player) event.getEntity();

        // Ensure the damage is coming from a living entity (e.g., a mob)
        if (!(event.getDamager() instanceof LivingEntity)) {
            return;
        }

        // Calculate XP based on the damage taken (adjust this formula as you see fit)
        int xpAmount = calculateXpReward(event.getFinalDamage());

        // Add the XP to the player's defense skill using your SkillManager
        skillManager.addXP(player, SkillManager.Skill.DEFENCE, xpAmount);

        player.sendActionBar(ChatColor.GOLD + "Attack +" + xpAmount);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
    }

    private int calculateXpReward(double damageTaken) {
        // For example, you can give 10 XP per heart of damage taken.
        return (int) (damageTaken * 10);
    }
}
