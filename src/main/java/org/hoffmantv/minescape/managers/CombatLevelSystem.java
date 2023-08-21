package org.hoffmantv.minescape.managers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Random;

public class CombatLevelSystem implements Listener {

    private final JavaPlugin plugin;
    private final Random random = new Random();
    private final int HEALTH_BAR_LENGTH = 10;

    public CombatLevelSystem(JavaPlugin plugin) {
        this.plugin = plugin;
        this.plugin.getServer().getPluginManager().registerEvents(this, plugin);

        // Assign levels to existing mobs
        for (World world : plugin.getServer().getWorlds()) {
            for (LivingEntity entity : world.getLivingEntities()) {
                if (entity instanceof Monster) {
                    assignMobLevel(entity);
                }
            }
        }
    }

    private void assignMobLevel(LivingEntity mob) {
        Player closestPlayer = findClosestPlayer(mob);


        if (closestPlayer != null) {
            int playerLevel = closestPlayer.getLevel();
            int mobLevel = playerLevel + random.nextInt(11) - 5;  // Range: player level +/- 5

            String mobName = mob.getType().toString().replace("_", " ").toLowerCase();
            // Capitalize the first letter
            mobName = Character.toUpperCase(mobName.charAt(0)) + mobName.substring(1);

            mob.setCustomNameVisible(true);
            mob.setCustomName(ChatColor.translateAlternateColorCodes('&', getColorBasedOnDifficulty(playerLevel, mobLevel) + mobName + ": LvL " + mobLevel));

            adjustMobAttributes(mob, mobLevel);
        }
    }

    // Adjust the attributes of the mob based on its level
    private void adjustMobAttributes(LivingEntity mob, int mobLevel) {
        // Adjust health
        double baseHealth = mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
        double newHealth = baseHealth + (mobLevel * 0.5);  // Example: Increase health by 0.5 per level
        mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(newHealth);
        mob.setHealth(newHealth);

        // Adjust damage (only if the mob has a melee damage attribute)
        if (mob.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) != null) {
            double baseDamage = mob.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getBaseValue();
            double newDamage = baseDamage + (mobLevel * 0.1);  // Example: Increase damage by 0.1 per level
            mob.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(newDamage);
        }
    }
    @EventHandler
    public void onEntitySpawn(CreatureSpawnEvent event) {
        LivingEntity entity = event.getEntity();
        assignMobLevel(entity);
    }

    @EventHandler
    public void onMobSpawn(CreatureSpawnEvent event) {
        if (!(event.getEntity() instanceof Monster)) return;
        LivingEntity mob = (LivingEntity) event.getEntity();
        assignMobLevel(mob);
    }
    private Player findClosestPlayer(LivingEntity mob) {
        double closestDistance = Double.MAX_VALUE;
        Player closestPlayer = null;
        for (Player player : mob.getWorld().getPlayers()) {
            double distance = player.getLocation().distance(mob.getLocation());
            if (distance < closestDistance) {
                closestDistance = distance;
                closestPlayer = player;
            }
        }
        return closestPlayer;
    }

    private String getColorBasedOnDifficulty(int playerLevel, int mobLevel) {
        int levelDifference = mobLevel - playerLevel;
        if (levelDifference > 4) return "&4";      // Red for much stronger mobs
        if (levelDifference > 2) return "&c";      // Dark red for stronger mobs
        if (levelDifference >= -2) return "&e";    // Yellow for mobs at similar level
        if (levelDifference >= -4) return "&a";    // Green for weaker mobs
        return "&2";                               // Dark green for much weaker mobs
    }
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof LivingEntity && event.getDamager() instanceof Player) {
            LivingEntity mob = (LivingEntity) event.getEntity();

            // Cancel any existing removal tasks for this entity
            if (mob.hasMetadata("healthBarRemovalTask")) {
                int taskId = mob.getMetadata("healthBarRemovalTask").get(0).asInt();
                Bukkit.getScheduler().cancelTask(taskId);
                mob.removeMetadata("healthBarRemovalTask", plugin);
            }

            // Calculate the percentage of health after damage is taken
            double healthPercentage = (mob.getHealth() - event.getFinalDamage()) / mob.getMaxHealth();
            updateHealthDisplay(mob, healthPercentage);

            // Schedule the health bar to be removed after 10 seconds and revert to level display
            int taskId = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                assignMobLevel(mob);
            }, 200L);  // 200 ticks = 10 seconds

            mob.setMetadata("healthBarRemovalTask", new FixedMetadataValue(plugin, taskId));
        }
    }

    private void updateHealthDisplay(LivingEntity entity, double healthPercentage) {
        int barLength = (int) (HEALTH_BAR_LENGTH * healthPercentage);

        String healthBar = ChatColor.GREEN.toString();
        for (int i = 0; i < barLength; i++) {
            healthBar += "|";
        }

        if (barLength < HEALTH_BAR_LENGTH) {
            healthBar += ChatColor.RED.toString();
            for (int i = barLength; i < HEALTH_BAR_LENGTH; i++) {
                healthBar += "|";
            }
        }

        entity.setCustomNameVisible(true);
        entity.setCustomName(healthBar);
    }
    public static Integer extractMobLevelFromName(LivingEntity mob) {
        String customName = mob.getCustomName();
        if (customName == null || !customName.contains(": LvL")) {
            return null;
        }

        String[] nameParts = customName.split(": LvL");
        if (nameParts.length != 2) {
            return null;
        }

        try {
            return Integer.parseInt(nameParts[1].trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

}
