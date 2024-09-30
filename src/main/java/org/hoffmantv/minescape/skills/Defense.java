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

        // Get player's defense level and armor rating
        int playerDefenseLevel = skillManager.getSkillLevel(player, SkillManager.Skill.DEFENCE);
        double armorRating = calculateArmorBonus(player);

        // Reduce the damage based on defense level and armor
        double originalDamage = event.getFinalDamage();
        double reducedDamage = mitigateDamage(originalDamage, playerDefenseLevel, armorRating);
        event.setDamage(reducedDamage);

        // Process defense XP reward only if the player is using a defensive combat stance
        if (isInDefensiveStance(player)) {
            processDefensiveAction(player, reducedDamage);
        }
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

    /**
     * Calculate damage mitigation based on player's defense level and armor rating.
     *
     * @param originalDamage the original damage dealt
     * @param defenseLevel   the player's defense level
     * @param armorRating    the armor's mitigation percentage
     * @return the reduced damage value
     */
    private double mitigateDamage(double originalDamage, int defenseLevel, double armorRating) {
        // Use a formula similar to OSRS to mitigate damage based on defense level and armor
        double defenseFactor = 1 - (defenseLevel / 100.0); // Defense reduces damage by up to 100%
        return originalDamage * defenseFactor * (1 - armorRating); // Combine defense and armor mitigation
    }

    /**
     * Calculate the armor bonus for damage mitigation.
     *
     * @param player the player whose armor is being checked
     * @return the total armor bonus as a percentage (0 to 1)
     */
    private double calculateArmorBonus(Player player) {
        double totalArmorBonus = 0.0;

        // Example of checking armor slots (you can expand this to other slots and items)
        if (player.getInventory().getHelmet() != null) {
            totalArmorBonus += getArmorPieceBonus(player.getInventory().getHelmet().getType().toString());
        }
        if (player.getInventory().getChestplate() != null) {
            totalArmorBonus += getArmorPieceBonus(player.getInventory().getChestplate().getType().toString());
        }
        if (player.getInventory().getLeggings() != null) {
            totalArmorBonus += getArmorPieceBonus(player.getInventory().getLeggings().getType().toString());
        }
        if (player.getInventory().getBoots() != null) {
            totalArmorBonus += getArmorPieceBonus(player.getInventory().getBoots().getType().toString());
        }

        return Math.min(1.0, totalArmorBonus); // Cap at 100% armor mitigation
    }

    /**
     * Return the armor piece bonus based on the armor type.
     *
     * @param armorType the type of armor (e.g., DIAMOND_HELMET)
     * @return the mitigation percentage for that armor piece
     */
    private double getArmorPieceBonus(String armorType) {
        switch (armorType) {
            case "DIAMOND_HELMET":
            case "DIAMOND_CHESTPLATE":
            case "DIAMOND_LEGGINGS":
            case "DIAMOND_BOOTS":
                return 0.2; // 20% mitigation per diamond piece
            case "IRON_HELMET":
            case "IRON_CHESTPLATE":
            case "IRON_LEGGINGS":
            case "IRON_BOOTS":
                return 0.15; // 15% mitigation per iron piece
            case "LEATHER_HELMET":
            case "LEATHER_CHESTPLATE":
            case "LEATHER_LEGGINGS":
            case "LEATHER_BOOTS":
                return 0.05; // 5% mitigation per leather piece
            default:
                return 0.0; // No armor bonus for other items
        }
    }

    /**
     * Check if the player is in a defensive stance.
     * In OSRS, the player must select a combat style that trains defense.
     *
     * @param player the player whose stance is being checked
     * @return true if the player is in a defensive stance, false otherwise
     */
    private boolean isInDefensiveStance(Player player) {
        // You can add logic to check the player's current combat style
        // For example, you could check for custom item meta or a specific attack mode
        // This is a placeholder and should be implemented based on your combat system
        return true; // Assuming the player is in a defensive stance for now
    }
}