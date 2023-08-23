package org.hoffmantv.minescape.skills;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.hoffmantv.minescape.managers.CombatLevelSystem;
import org.hoffmantv.minescape.managers.SkillManager;

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

        // Placeholder check to exclude certain mobs (you should define this properly)
        if (isExcludedMob(mob)) {
            return;
        }

        Integer mobLevel = CombatLevelSystem.extractMobLevelFromName(mob);
        if (mobLevel == null) {
            // Make sure to review how CombatLevelSystem works!
            return;
        }

        int playerAttackLevel = skillManager.getSkillLevel(player, SkillManager.Skill.ATTACK);

        // Check if player misses the attack
        if (attackSkill.doesPlayerMissAttack(playerAttackLevel, mobLevel)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You missed your attack!");
            return;
        }
    }

    @EventHandler
    public void onMobDeath(EntityDeathEvent event) {
        if (!(event.getEntity().getKiller() instanceof Player)) {
            return;
        }

        Player player = event.getEntity().getKiller();
        LivingEntity mob = event.getEntity();

        Integer mobLevel = CombatLevelSystem.extractMobLevelFromName(mob);
        if (mobLevel == null) {
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
        return (3 + mobLevel);
    }

    private boolean isExcludedMob(LivingEntity mob) {
        // Placeholder logic: You should implement your own criteria here.
        // For example, if you have friendly NPCs or mobs tagged as 'non-combat'.
        return false;
    }
}
