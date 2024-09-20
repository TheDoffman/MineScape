package org.hoffmantv.minescape.managers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.hoffmantv.minescape.skills.CombatLevel;

import java.util.*;

public class CombatLevelSystem implements Listener {

    private final JavaPlugin plugin;
    private final Random random = new Random();
    private final CombatLevel combatLevel;
    private final Map<UUID, BossBar> mobBossBars = new WeakHashMap<>();

    public CombatLevelSystem(JavaPlugin plugin, CombatLevel combatLevel) {
        this.plugin = plugin;
        this.combatLevel = combatLevel;
        this.plugin.getServer().getPluginManager().registerEvents(this, plugin);

        // Assign levels to existing mobs (hostile, passive, and baby animals) in all worlds on startup
        for (World world : plugin.getServer().getWorlds()) {
            for (LivingEntity entity : world.getLivingEntities()) {
                if (isMobEligibleForLeveling(entity)) {
                    assignMobLevel(entity);
                }
            }
        }

        // Start BossBar updater
        startBossBarUpdater();
    }

    private void assignMobLevel(LivingEntity mob) {
        Player closestPlayer = findClosestPlayer(mob);
        int mobLevel;

        if (closestPlayer != null) {
            int playerCombatLevel = combatLevel.calculateCombatLevel(closestPlayer);
            mobLevel = playerCombatLevel + random.nextInt(11) - 5; // player level +/-5
        } else {
            mobLevel = random.nextInt(5) + 1; // Random level between 1 and 5
        }

        // Ensure mobLevel is at least 1
        mobLevel = Math.max(mobLevel, 1);

        // If the mob is a baby, reduce the level slightly
        if (mob instanceof Ageable && !((Ageable) mob).isAdult()) {
            mobLevel = Math.max(1, mobLevel - 2); // Reduce level by 2 for baby animals, with minimum of 1
        }

        setMobNameAndAttributes(mob, mobLevel);
    }

    private void setMobNameAndAttributes(LivingEntity mob, int mobLevel) {
        String mobName = formatMobName(mob.getType().toString());
        if (mob instanceof Ageable && !((Ageable) mob).isAdult()) {
            mobName = "Baby " + mobName; // Prefix with "Baby" if the mob is a baby
        }
        mob.setCustomNameVisible(true);
        mob.setCustomName(ChatColor.translateAlternateColorCodes('&',
                getColorBasedOnDifficulty(mobLevel) + mobName + ": LvL " + mobLevel
        ));
        adjustMobAttributes(mob, mobLevel);
    }

    private String formatMobName(String rawName) {
        String formattedName = rawName.replace("_", " ").toLowerCase();
        return Character.toUpperCase(formattedName.charAt(0)) + formattedName.substring(1);
    }

    // Adjust the attributes of the mob based on its level
    private void adjustMobAttributes(LivingEntity mob, int mobLevel) {
        // Adjust health
        if (mob.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
            double baseHealth = mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
            double newHealth = baseHealth + (mobLevel * 0.5);  // Increase health by 0.5 per level
            mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(newHealth);
            mob.setHealth(newHealth); // Set the new health to reflect immediately
        }

        // Adjust damage (only for entities that can deal damage)
        if (mob.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) != null) {
            double baseDamage = mob.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getBaseValue();
            double newDamage = baseDamage + (mobLevel * 0.1);  // Increase damage by 0.1 per level
            mob.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(newDamage);
        }
    }

    @EventHandler
    public void onMobSpawn(CreatureSpawnEvent event) {
        if (isMobEligibleForLeveling(event.getEntity())) {
            assignMobLevel(event.getEntity());
        }
    }

    private boolean isMobEligibleForLeveling(LivingEntity entity) {
        // Include both hostile and passive mobs, including baby animals
        return entity instanceof Monster || entity instanceof Animals || entity instanceof WaterMob || entity instanceof Ambient;
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

    private String getColorBasedOnDifficulty(int mobLevel) {
        if (mobLevel >= 50) return "&4";    // Red for very high-level mobs
        if (mobLevel >= 30) return "&c";    // Dark red for high-level mobs
        if (mobLevel >= 20) return "&e";    // Yellow for mid-level mobs
        if (mobLevel >= 10) return "&a";    // Green for low-level mobs
        return "&2";                        // Dark green for very low-level mobs
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

    @EventHandler
    public void onMobAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof LivingEntity)) return;

        LivingEntity mob = (LivingEntity) event.getEntity();
        Player player = (Player) event.getDamager();

        if (extractMobLevelFromName(mob) == null) return;

        mobBossBars.computeIfAbsent(mob.getUniqueId(), uuid -> {
            BossBar bossBar = plugin.getServer().createBossBar(mob.getCustomName(), BarColor.RED, BarStyle.SOLID);
            bossBar.setProgress(mob.getHealth() / mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
            return bossBar;
        });

        BossBar bossBar = mobBossBars.get(mob.getUniqueId());
        bossBar.addPlayer(player);
    }

    @EventHandler
    public void onMobDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) return;

        LivingEntity mob = (LivingEntity) event.getEntity();
        BossBar bossBar = mobBossBars.get(mob.getUniqueId());

        if (bossBar == null) return;

        double health = mob.getHealth() - event.getFinalDamage(); // health after damage is applied
        double maxHealth = mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();

        bossBar.setProgress(Math.max(0, health) / maxHealth);
    }

    @EventHandler
    public void onMobDeath(EntityDeathEvent event) {
        BossBar bossBar = mobBossBars.remove(event.getEntity().getUniqueId());
        if (bossBar != null) {
            bossBar.removeAll();
        }
    }

    // Replace onPlayerMove with a scheduled task
    public void startBossBarUpdater() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                updateBossBarsForPlayer(player);
            }
        }, 0L, 20L); // Update every second (20 ticks)
    }

    private void updateBossBarsForPlayer(Player player) {
        mobBossBars.forEach((uuid, bossBar) -> {
            Entity mob = player.getWorld().getEntity(uuid);
            if (mob == null || mob.getLocation().distance(player.getLocation()) > 25) {
                bossBar.removePlayer(player);
            } else {
                bossBar.addPlayer(player);
            }
        });
    }
}