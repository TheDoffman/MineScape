package org.hoffmantv.minescape.skills;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.hoffmantv.minescape.managers.CombatLevelSystem;
import org.bukkit.Material;

import java.util.Random;

public class Attack implements Listener {

    private final SkillManager skillManager;
    private final ConfigurationSection attackConfig;

    public Attack(SkillManager skillManager, ConfigurationSection attackConfig) {
        this.skillManager = skillManager;
        this.attackConfig = attackConfig;
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
        int playerStrengthLevel = skillManager.getSkillLevel(player, SkillManager.Skill.STRENGTH);

        // Check if player misses the attack (accuracy check)
        if (doesPlayerMissAttack(playerAttackLevel, mobLevel)) {
            event.setDamage(0); // No damage on a miss
            player.sendActionBar(ChatColor.RED + "Missed Attack");
            return;
        }

        // Calculate and apply damage based on player strength level
        double damage = calculateDamage(playerStrengthLevel, mobLevel, player);
        event.setDamage(damage);

        // Calculate XP based on the damage dealt
        int xpAmount = calculateXpReward(damage);

        // Add the XP reward to the player's ATTACK skill using the SkillManager
        skillManager.addXP(player, SkillManager.Skill.ATTACK, xpAmount);

        // Notify the player about the XP gained
        player.sendActionBar(ChatColor.GOLD + "Attack +" + xpAmount + " XP");
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
    }

    private double calculateDamage(int playerStrengthLevel, int mobLevel, Player player) {
        double baseDamage = playerStrengthLevel / 10.0; // Base damage scaled by player's strength
        double weaponMultiplier = getWeaponDamageMultiplier(player.getInventory().getItemInMainHand());
        double randomFactor = 0.5 + (new Random().nextDouble() * 1.0); // Random multiplier between 0.5 and 1.5

        // Factor in the mob's level (higher levels reduce damage)
        double mobDefenseMultiplier = Math.max(0.5, 1.0 - mobLevel * 0.01); // Reduce damage based on mob level, but never less than 50% damage

        return baseDamage * weaponMultiplier * randomFactor * mobDefenseMultiplier;
    }

    private double getWeaponDamageMultiplier(ItemStack weapon) {
        // Customize weapon damage multipliers for different weapons (like OSRS)
        if (weapon.getType() == Material.DIAMOND_SWORD) {
            return 1.5; // Strong weapon
        } else if (weapon.getType() == Material.IRON_SWORD) {
            return 1.3; // Medium weapon
        } else if (weapon.getType() == Material.WOODEN_SWORD) {
            return 0.8; // Weak weapon
        } else if (weapon.getType() == Material.BOW) {
            return 1.1; // For ranged attacks
        }
        return 1.0; // Default multiplier for unarmed or other weapons
    }

    private int calculateXpReward(double damage) {
        // In OSRS, XP is based on the damage dealt
        int xpPerDamage = attackConfig.getInt("xpPerDamage", 4); // 4 XP per point of damage as a default
        return (int) (damage * xpPerDamage);
    }

    public boolean doesPlayerMissAttack(int playerLevel, int mobLevel) {
        int baseMissChance = 20;  // 20% base chance to miss

        // Level difference adjustment: Each level the mob is higher increases miss chance by 1%
        int levelDifference = mobLevel - playerLevel;
        int levelDifferenceModifier = Math.max(0, levelDifference);

        // Player skill modifier: For every 10 levels in attack, reduce miss chance by 1%
        int playerSkillModifier = playerLevel / 10;

        // Total miss chance
        int totalMissChance = baseMissChance + levelDifferenceModifier - playerSkillModifier;

        // Ensure miss chance stays within reasonable bounds
        totalMissChance = Math.max(5, totalMissChance);  // No lower than 5%
        totalMissChance = Math.min(95, totalMissChance); // No higher than 95%

        Random random = new Random();
        int roll = random.nextInt(100);

        return roll < totalMissChance;
    }

    // Optional: Special attack logic for a specific weapon (like OSRS specials)
    private void performSpecialAttack(Player player, ItemStack weapon) {
        if (weapon.getType() == Material.DIAMOND_SWORD) {
            // Example of a diamond sword special attack
            player.sendMessage(ChatColor.GOLD + "You perform a special attack!");
            player.getWorld().strikeLightningEffect(player.getTargetBlockExact(10).getLocation());
            player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0F, 1.0F);
        }
    }
}