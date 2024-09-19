package org.hoffmantv.minescape.managers;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.hoffmantv.minescape.skills.CombatLevel;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class CombatLevelSystem implements Listener {

    private final JavaPlugin plugin;
    private final Random random = new Random();
    private final CombatLevel combatLevel;
    private final Map<UUID, BossBar> mobBossBars = new HashMap<>();
    public CombatLevelSystem(JavaPlugin plugin, CombatLevel combatLevel) {
        this.plugin = plugin;
        this.combatLevel = combatLevel;
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
            int playerCombatLevel = combatLevel.calculateCombatLevel(
                    closestPlayer
            ); // use CombatLevel class here

            int mobLevel = playerCombatLevel + random.nextInt(11) - 5; // Range: player combat level +/- 5

            String mobName = mob.getType().toString().replace("_", " ").toLowerCase();
            // Capitalize the first letter
            mobName = Character.toUpperCase(mobName.charAt(0)) + mobName.substring(1);

            mob.setCustomNameVisible(true);
            mob.setCustomName(ChatColor.translateAlternateColorCodes('&',
                    getColorBasedOnDifficulty(playerCombatLevel, mobLevel) + mobName + ": LvL " + mobLevel
            ));

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

        if (!mobBossBars.containsKey(mob.getUniqueId())) {
            BossBar bossBar = plugin.getServer().createBossBar(mob.getCustomName(), BarColor.RED, BarStyle.SOLID);
            bossBar.addPlayer(player);
            mobBossBars.put(mob.getUniqueId(), bossBar);
        }

        BossBar bossBar = mobBossBars.get(mob.getUniqueId());
        bossBar.setProgress(mob.getHealth() / mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
    }

    @EventHandler
    public void onMobDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) return;

        LivingEntity mob = (LivingEntity) event.getEntity();
        BossBar bossBar = mobBossBars.get(mob.getUniqueId());

        if (bossBar == null) return;

        double health = mob.getHealth() - event.getFinalDamage(); // health after damage is applied
        bossBar.setProgress(Math.max(0, health) / mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
    }
    @EventHandler
    public void onMobDeath(EntityDeathEvent event) {
        BossBar bossBar = mobBossBars.remove(event.getEntity().getUniqueId());
        if (bossBar != null) {
            bossBar.removeAll();
        }
    }
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
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