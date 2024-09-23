package org.hoffmantv.minescape.skills;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
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

        // Log the XP gain to playerdata.yml or any other logging mechanism
        configManager.logXpGain(player, "strength", xpAmount);

        // Notify the player about the XP gained
        player.sendActionBar(ChatColor.GOLD + "Strength +" + xpAmount);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
    }

    private int calculateXpReward(int mobLevel) {
        // Fetch base XP from the configuration section, default to 10 if not found
        int baseXp = strengthConfig.getInt("baseXp", 10);

        // Optionally use mobLevel in the calculation
        int xp = baseXp + (mobLevel * 2); // Example: Increase XP reward per mob level

        return xp;
    }
}