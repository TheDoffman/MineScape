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

import java.util.Random;

public class StrengthSkill implements Listener {

    private final SkillManager skillManager;
    private final AttackSkill attackSkill;

    public StrengthSkill(SkillManager skillManager, AttackSkill attackSkill) {
        this.skillManager = skillManager;
        this.attackSkill = attackSkill;
    }

    @EventHandler
    public void onPlayerDamageMob(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof LivingEntity)) {
            return;
        }

        Player player = (Player) event.getDamager();
        LivingEntity mob = (LivingEntity) event.getEntity();

        Integer mobLevel = CombatLevelSystem.extractMobLevelFromName(mob);
        if (mobLevel == null) {
            return;
        }
        int playerAttackLevel = skillManager.getSkillLevel(player, SkillManager.Skill.ATTACK);

        // Check if player misses the attack
        if (attackSkill.doesPlayerMissAttack(playerAttackLevel, mobLevel)) {
            event.setCancelled(true);
            return;
        }
        // Calculate XP reward based on the mob's level
        int xpAmount = calculateXpReward(mobLevel);

        // Add the XP reward to the player's ATTACK skill using the SkillManager
        skillManager.addXP(player, SkillManager.Skill.STRENGTH, xpAmount);

        // Notify the player about the XP gained
        player.sendActionBar(ChatColor.GOLD + "Strength +" + xpAmount);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
    }
    private int calculateXpReward(int mobLevel) {
        // This formula can be adjusted to your liking
        return (3 + mobLevel);
    }
}