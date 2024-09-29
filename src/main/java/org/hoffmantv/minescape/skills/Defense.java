package org.hoffmantv.minescape.skills;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.hoffmantv.minescape.managers.CombatLevelSystem;
import org.hoffmantv.minescape.managers.ConfigurationManager;

public class Defense implements Listener {

    private final SkillManager skillManager;
    private final ConfigurationSection defenseConfig;
    private final ConfigurationManager configManager;

    public Defense(SkillManager skillManager, ConfigurationSection defenseConfig, ConfigurationManager configManager) {
        this.skillManager = skillManager;
        this.defenseConfig = defenseConfig;
        this.configManager = configManager;
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;  // Return if the damaged entity isn't a player
        }

        Player player = (Player) event.getEntity();
        LivingEntity damager = (LivingEntity) event.getDamager();

        // Ensure the damage is coming from a living entity (e.g., a mob)
        if (!(damager instanceof LivingEntity)) {
            return;
        }

        Integer damagerLevel = CombatLevelSystem.extractMobLevelFromName(damager);
        if (damagerLevel == null) {
            return;
        }

        // Process defense skill logic
        double damageTaken = event.getFinalDamage();
        processDefensiveAction(player, damageTaken);
    }

    /**
     * Process the defensive action of the player.
     * Calculate XP based on the damage taken and notify the player.
     */
    private void processDefensiveAction(Player player, double damageTaken) {
        // Calculate XP based on the damage taken
        int xpAmount = calculateXpReward(damageTaken);

        // Add the XP to the player's defense skill
        skillManager.addXP(player, SkillManager.Skill.DEFENCE, xpAmount);

        // Notify the player
        player.sendActionBar(ChatColor.GOLD + "Defense +" + xpAmount);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);

        // Log XP gain to playerdata.yml using ConfigurationManager
        configManager.logXpGain(player, "Defense", xpAmount);
    }

    /**
     * Calculate the XP reward based on the damage taken.
     *
     * @param damageTaken the damage taken by the player
     * @return the XP amount to be rewarded
     */
    private int calculateXpReward(double damageTaken) {
        // Fetch XP per damage value from skills.yml
        int xpPerDamage = defenseConfig.getInt("xpPerDamage", 5); // Default XP per heart of damage taken is 5
        return (int) (damageTaken * xpPerDamage);
    }
}