package org.hoffmantv.minescape.skills;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.hoffmantv.minescape.managers.CombatLevelSystem;
import org.hoffmantv.minescape.managers.SkillManager;

public class AttackSkill implements Listener {

    private final SkillManager skillManager;

    public AttackSkill(SkillManager skillManager) {
        this.skillManager = skillManager;
    }

    @EventHandler
    public void onPlayerDamageMob(EntityDamageByEntityEvent event) {
        System.out.println("EntityDamageByEntityEvent triggered!");
        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof LivingEntity)) {
            System.out.println("Exit Point 1: Not a player or not a living entity.");
            return;
        }

        Player player = (Player) event.getDamager();
        LivingEntity mob = (LivingEntity) event.getEntity();

        Integer mobLevel = CombatLevelSystem.extractMobLevelFromName(mob);
        if (mobLevel == null) {
            return;
        }

        // Calculate XP reward based on the mob's level
        int xpReward = calculateXpReward(mobLevel);

        // Add the XP reward to the player's ATTACK skill using the SkillManager
        skillManager.addXP(player, SkillManager.Skill.ATTACK, xpReward);

        // Optional: Notify the player about the XP gained
        player.sendMessage(ChatColor.GREEN + "You earned " + xpReward + " ATTACK XP!");
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
    }
    private int calculateXpReward(int mobLevel) {
        // This formula can be adjusted to your liking
        return 10 + mobLevel * 5;
    }

}