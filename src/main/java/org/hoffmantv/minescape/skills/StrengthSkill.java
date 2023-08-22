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
    private final CombatLevel combatLevel;
    private final AttackSkill attackSkill;

    public StrengthSkill(SkillManager skillManager, CombatLevel combatLevel, AttackSkill attackSkill) {
        this.skillManager = skillManager;
        this.combatLevel = combatLevel;
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
        player.sendActionBar(ChatColor.GOLD + "Strenght +" + xpAmount);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
    }
    private int calculateXpReward(int mobLevel) {
        // This formula can be adjusted to your liking
        return (int) (10 + mobLevel * 1.1);
    }
    private boolean doesPlayerMissAttack(int playerLevel, int mobLevel) {
        int baseMissChance = 20;  // 20% base chance to miss

        // Level difference adjustment. For each level the mob is higher, increase miss chance by 1%.
        int levelDifference = mobLevel - playerLevel;
        int levelDifferenceModifier = Math.max(0, levelDifference);  // ensure this is not negative

        // Player skill modifier. For every 10 levels in attack, reduce miss chance by 1%.
        int playerSkillModifier = playerLevel / 10;

        // Total miss chance
        int totalMissChance = baseMissChance + levelDifferenceModifier - playerSkillModifier;

        // Ensure miss chance stays within reasonable bounds
        totalMissChance = Math.max(5, totalMissChance);  // No lower than 5%
        totalMissChance = Math.min(95, totalMissChance);  // No higher than 95%

        Random random = new Random();
        int roll = random.nextInt(100);

        return roll < totalMissChance;
    }

}