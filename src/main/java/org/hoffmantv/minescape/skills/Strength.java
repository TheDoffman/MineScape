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
import org.hoffmantv.minescape.managers.ConfigurationManager;

import java.util.HashMap;
import java.util.Map;

public class Strength implements Listener {

    private final SkillManager skillManager;
    private final ConfigurationSection strengthConfig;
    private final ConfigurationManager configManager;
    private final Map<Material, Integer> weaponStrengthRequirements = new HashMap<>();

    public Strength(SkillManager skillManager, ConfigurationSection strengthConfig, ConfigurationManager configManager) {
        this.skillManager = skillManager;
        this.strengthConfig = strengthConfig;
        this.configManager = configManager;
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

        // Check if the player is holding a sword
        Material weaponType = player.getInventory().getItemInMainHand().getType();
        if (!weaponStrengthRequirements.containsKey(weaponType)) {
            return; // If the weapon isn't in the requirements list, exit out of the event.
        }

        int requiredLevel = weaponStrengthRequirements.get(weaponType);
        int playerStrengthLevel = skillManager.getSkillLevel(player, SkillManager.Skill.STRENGTH);

        if (playerStrengthLevel < requiredLevel) {
            player.sendMessage(ChatColor.RED + "You need a Strength level of " + requiredLevel + " to wield this weapon.");
            event.setCancelled(true);
            return;
        }

        // Placeholder check to exclude certain mobs (you should define this properly)
        if (isExcludedMob(mob)) {
            return;
        }

        Integer mobLevel = CombatLevelSystem.extractMobLevelFromName(mob);
        if (mobLevel == null) {
            // Make sure to review how CombatLevelSystem works!
            return;
        }

        // Additional logic for when the player damages a mob can be added here
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

        // Add the XP reward to the player's STRENGTH skill using the SkillManager
        skillManager.addXP(player, SkillManager.Skill.STRENGTH, xpAmount);

        // Log the XP gain to playerdata.yml
        configManager.logXpGain(player, "strength", xpAmount);

        // Notify the player about the XP gained
        player.sendActionBar(ChatColor.GOLD + "Strength +" + xpAmount);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
    }

    private int calculateXpReward(int mobLevel) {
        // Fetch base XP from the configuration section, default to 10 if not found
        int baseXp = strengthConfig.getInt("baseXp", 10);

        // Optionally use mobLevel in the calculation if needed, for now, return just the base XP
        return baseXp;
    }

    private boolean isExcludedMob(LivingEntity mob) {
        // Placeholder logic: You should implement your own criteria here.
        // For example, if you have friendly NPCs or mobs tagged as 'non-combat'.
        return false;
    }
}