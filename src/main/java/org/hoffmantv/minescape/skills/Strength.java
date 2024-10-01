package org.hoffmantv.minescape.skills;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.hoffmantv.minescape.managers.CombatLevelSystem;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Strength implements Listener {

    private final SkillManager skillManager;
    private final ConfigurationSection strengthConfig;
    private final Map<Material, Integer> weaponStrengthRequirements = new HashMap<>();
    private final Random random = new Random();

    public Strength(SkillManager skillManager) {
        this.skillManager = skillManager;
        this.strengthConfig = skillManager.getSkillsConfig().getConfigurationSection("skills.strength");
        loadWeaponRequirements();
    }

    // Load weapon requirements from the configuration
    private void loadWeaponRequirements() {
        ConfigurationSection weaponReqSection = strengthConfig.getConfigurationSection("weaponRequirements");
        if (weaponReqSection != null) {
            for (String key : weaponReqSection.getKeys(false)) {
                try {
                    Material material = Material.valueOf(key);
                    int requiredLevel = weaponReqSection.getInt(key);
                    weaponStrengthRequirements.put(material, requiredLevel);
                } catch (IllegalArgumentException e) {
                    skillManager.getPlugin().getLogger().warning("Invalid material in weapon requirements: " + key);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerDamageMob(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof LivingEntity)) {
            return;
        }

        Player player = (Player) event.getDamager();
        LivingEntity mob = (LivingEntity) event.getEntity();

        Material weaponType = player.getInventory().getItemInMainHand().getType();
        if (!weaponStrengthRequirements.containsKey(weaponType)) {
            return;
        }

        int requiredLevel = weaponStrengthRequirements.get(weaponType);
        int playerStrengthLevel = skillManager.getSkillLevel(player, SkillManager.Skill.STRENGTH);

        if (playerStrengthLevel < requiredLevel) {
            player.sendMessage(ChatColor.RED + "You need a Strength level of " + requiredLevel + " to wield this weapon.");
            event.setCancelled(true);
            return;
        }

        if (isExcludedMob(mob)) {
            return;
        }

        Integer mobLevel = CombatLevelSystem.extractMobLevelFromName(mob);
        if (mobLevel == null) {
            return;
        }

        // Calculate hit chance and damage
        if (doesPlayerHit(playerStrengthLevel, mobLevel)) {
            double maxHit = calculateMaxHit(playerStrengthLevel);
            double damageDealt = random.nextDouble() * maxHit;
            event.setDamage(damageDealt);

            // Grant XP based on damage dealt
            int xpAmount = calculateXpFromDamage(damageDealt);
            skillManager.addXP(player, SkillManager.Skill.STRENGTH, xpAmount);

            // Notify player
            player.sendActionBar(ChatColor.GOLD + "Strength +" + xpAmount);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
        } else {
            player.sendActionBar(ChatColor.RED + "Missed!");
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

        // XP on mob death
        int xpAmount = calculateXpReward(mobLevel);
        skillManager.addXP(player, SkillManager.Skill.STRENGTH, xpAmount);

        player.sendActionBar(ChatColor.GOLD + "Strength +" + xpAmount);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
    }

    private boolean doesPlayerHit(int playerLevel, int mobLevel) {
        int baseHitChance = 70; // Base hit chance is 70%
        int levelDifference = playerLevel - mobLevel;

        baseHitChance += levelDifference * 2; // Increase hit chance by 2% for each level higher than mob
        baseHitChance = Math.max(5, baseHitChance); // Minimum hit chance is 5%
        baseHitChance = Math.min(95, baseHitChance); // Maximum hit chance is 95%

        return random.nextInt(100) < baseHitChance;
    }

    private double calculateMaxHit(int strengthLevel) {
        return 1 + (strengthLevel / 10.0); // Example: Strength 50 would have max hit of 6.0
    }

    private int calculateXpFromDamage(double damageDealt) {
        return (int) (damageDealt * 4);
    }

    private int calculateXpReward(int mobLevel) {
        int baseXp = strengthConfig.getInt("baseXp", 10);
        return baseXp + mobLevel * 2; // Give more XP for higher level mobs
    }

    private boolean isExcludedMob(LivingEntity mob) {
        // Placeholder logic for excluding certain mobs (e.g., friendly mobs, pets)
        return false;
    }
}