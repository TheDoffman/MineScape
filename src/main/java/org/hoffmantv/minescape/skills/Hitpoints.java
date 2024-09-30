package org.hoffmantv.minescape.skills;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class Hitpoints implements Listener {
    private final SkillManager skillManager;
    private final JavaPlugin plugin;

    public Hitpoints(SkillManager skillManager, JavaPlugin plugin) {
        this.skillManager = skillManager;
        this.plugin = plugin;
        startHealthRegeneration();  // Start the regeneration system
    }

    /**
     * Handles hitpoints XP gain based on damage dealt by the player (OSRS-like mechanic).
     */
    @EventHandler
    public void onPlayerDealDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) {
            return; // Return if the damager is not a player
        }

        Player player = (Player) event.getDamager();
        double damageDealt = event.getFinalDamage();

        // Grant hitpoints XP based on the damage dealt
        grantXp(player, (int) damageDealt);

        // Update the player's max health based on their hitpoints level
        updatePlayerHealth(player);
    }

    /**
     * Grants hitpoints XP to the player based on the damage they deal.
     *
     * @param player   the player gaining XP
     * @param xpAmount the amount of XP to be added
     */
    private void grantXp(Player player, int xpAmount) {
        skillManager.addXP(player, SkillManager.Skill.HITPOINTS, xpAmount);
    }

    /**
     * Updates the player's max health based on their hitpoints skill level.
     *
     * @param player the player whose max health will be updated
     */
    private void updatePlayerHealth(Player player) {
        int hitpointsLevel = skillManager.getSkillLevel(player, SkillManager.Skill.HITPOINTS);

        // OSRS-style hitpoints system: Max health corresponds directly to hitpoints level
        double newMaxHealth = hitpointsLevel;

        // Set the player's max health to the hitpoints level (capped at 99 for OSRS-style logic)
        player.setMaxHealth(Math.min(newMaxHealth, 99)); // Max health cap at 99
    }

    /**
     * Starts the health regeneration system that periodically regenerates health based on hitpoints level.
     */
    private void startHealthRegeneration() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    int hitpointsLevel = skillManager.getSkillLevel(player, SkillManager.Skill.HITPOINTS);
                    double currentHealth = player.getHealth();
                    double maxHealth = player.getMaxHealth();

                    // Regenerate health only if the player is not at full health
                    if (currentHealth < maxHealth) {
                        double regenAmount = calculateHealthRegen(hitpointsLevel);

                        // Ensure the player doesn't exceed max health
                        double newHealth = Math.min(currentHealth + regenAmount, maxHealth);
                        player.setHealth(newHealth);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 100L); // Run every 5 seconds (100 ticks = 5 seconds)
    }

    /**
     * Calculates the health regeneration amount based on the player's hitpoints level.
     *
     * @param hitpointsLevel the player's hitpoints level
     * @return the amount of health to regenerate
     */
    private double calculateHealthRegen(int hitpointsLevel) {
        // In OSRS, higher hitpoints levels increase the natural regeneration rate.
        // You can scale this however you'd like. Here we regenerate 1 HP every 10 seconds at level 10,
        // and at higher levels, we increase the rate by adding an extra HP every 20 levels.
        return Math.max(1, hitpointsLevel / 10.0); // 1 HP every 10 levels as a base regen
    }
}