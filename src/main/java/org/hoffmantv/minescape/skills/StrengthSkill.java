package org.hoffmantv.minescape.skills;

import org.bukkit.ChatColor;
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
import org.hoffmantv.minescape.managers.SkillManager;

public class StrengthSkill implements Listener {

    private final SkillManager skillManager;
    private final ConfigurationSection strengthConfig;
    private final ConfigurationManager configManager;

    public StrengthSkill(SkillManager skillManager, ConfigurationSection strengthConfig, ConfigurationManager configManager) {
        this.skillManager = skillManager;
        this.strengthConfig = strengthConfig;
        this.configManager = configManager;
    }

    @EventHandler
    public void onPlayerDamageMob(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof LivingEntity)) {
            return;
        }

        Player player = (Player) event.getDamager();
        LivingEntity mob = (LivingEntity) event.getEntity();

        // Check if the player is holding a sword
        switch (player.getInventory().getItemInMainHand().getType()) {
            case DIAMOND_SWORD:
            case GOLDEN_SWORD:
            case IRON_SWORD:
            case STONE_SWORD:
            case WOODEN_SWORD:
            case NETHERITE_SWORD:
                break; // If they are holding a sword, continue to the logic below.
            default:
                return; // If not, exit out of the event.
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