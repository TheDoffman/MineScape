package org.hoffmantv.minescape.managers;

import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

public class CombatSession {
    private final Player player;
    private final LivingEntity mob;
    private final JavaPlugin plugin;
    private final SkillManager skillManager;
    private final BossBar bossBar;
    private final Location initialMobLocation;
    private boolean playerTurn = true; // Start with player's turn
    private boolean active = true;
    private int playerHealth;
    private int mobHealth;
    private final int playerMaxHealth;
    private final int mobMaxHealth;
    private final Random random = new Random();
    private long lastAttackTime; // Track the last attack time

    public CombatSession(Player player, LivingEntity mob, JavaPlugin plugin, SkillManager skillManager) {
        this.player = player;
        this.mob = mob;
        this.plugin = plugin;
        this.skillManager = skillManager;
        this.playerMaxHealth = (int) player.getHealth();
        this.mobMaxHealth = (int) mob.getHealth();
        this.playerHealth = playerMaxHealth;
        this.mobHealth = mobMaxHealth;
        this.initialMobLocation = mob.getLocation().clone(); // Store initial location of the mob
        this.bossBar = Bukkit.createBossBar(
                ChatColor.translateAlternateColorCodes('&', "&c" + mob.getName() + " - " + mobHealth + "/" + mobMaxHealth),
                BarColor.RED,
                BarStyle.SOLID
        );
        this.bossBar.addPlayer(player);

        freezeMob(true); // Freeze the mob in place
        startCombat();
        startMobFacingTask(); // Start the task to make the mob always face the player
    }

    private void startCombat() {
        lastAttackTime = System.currentTimeMillis(); // Set initial last attack time
        Bukkit.getScheduler().runTaskTimer(plugin, this::takeTurn, 0L, 40L); // 40 ticks = 2 seconds per turn
    }

    private void takeTurn() {
        if (!active) return;

        if (playerTurn) {
            playerAttack();
        } else {
            mobAttack();
        }

        playerTurn = !playerTurn; // Alternate turns
    }

    private void playerAttack() {
        if (!active) return;

        int damage = calculateDamage(player, mob);
        mobHealth -= damage;
        mob.damage(damage); // Apply damage to the mob

        // Visual effect for player attack
        player.getWorld().playSound(mob.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.CRIT, mob.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);

        updateBossBar();

        player.sendMessage("You dealt " + damage + " damage to the mob! (" + mobHealth + "/" + mobMaxHealth + " HP)");

        lastAttackTime = System.currentTimeMillis(); // Update last attack time
        checkCombatEnd();
    }

    private void mobAttack() {
        if (!active) return;

        // Visual and sound effects to simulate mob attack
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.CRIT_MAGIC, player.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1);

        int damage = calculateDamage(mob, player);
        playerHealth -= damage;
        player.damage(damage); // Apply damage to the player

        player.sendMessage("The mob dealt " + damage + " damage to you! (" + playerHealth + "/" + playerMaxHealth + " HP)");

        lastAttackTime = System.currentTimeMillis(); // Update last attack time
        checkCombatEnd();
    }

    private int calculateDamage(LivingEntity attacker, LivingEntity defender) {
        // Use skill levels or combat levels to influence damage calculations
        int attackLevel = getCombatLevel(attacker);
        int defenseLevel = getCombatLevel(defender);

        int baseDamage = random.nextInt(attackLevel + 1); // Random damage between 0 and attackLevel
        int reducedDamage = Math.max(baseDamage - (defenseLevel / 4), 0); // Reduce damage based on defense level

        return reducedDamage;
    }

    private int getCombatLevel(LivingEntity entity) {
        // Utilize the CombatLevel system if available, or use default values
        if (entity instanceof Player) {
            return skillManager.getSkillLevel((Player) entity, SkillManager.Skill.COMBAT);
        }
        return 10; // Default value for mobs
    }

    private void checkCombatEnd() {
        if (mobHealth <= 0) {
            endCombat("You have defeated the mob!");
        } else if (playerHealth <= 0) {
            endCombat("You have been defeated by the mob!");
        } else if (player.getLocation().distance(initialMobLocation) > 15) {
            endCombat("You have moved too far away and the combat has ended!");
        }
    }

    private void endCombat(String message) {
        active = false;
        player.sendMessage(message);

        // End combat visual effects
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, player.getLocation(), 5, 0.5, 0.5, 0.5, 0.1);
        player.getWorld().playEffect(player.getLocation(), Effect.ENDER_SIGNAL, null);

        bossBar.removeAll();
        freezeMob(false); // Unfreeze the mob
    }

    // Overloaded endCombat method without parameters
    public void endCombat() {
        endCombat("Combat ended.");
    }

    public boolean isActive() {
        return active;
    }

    public LivingEntity getMob() {
        return mob;
    }

    // Freeze or unfreeze the mob
    private void freezeMob(boolean freeze) {
        if (freeze) {
            mob.setAI(false); // Disable AI to lock in place
            mob.teleport(initialMobLocation); // Ensure mob doesn't move from initial location
        } else {
            mob.setAI(true); // Re-enable AI
        }
    }

    public long getLastAttackTime() {
        return lastAttackTime;
    }

    // Update the boss bar with the latest health values
    private void updateBossBar() {
        bossBar.setProgress(Math.max(0, (double) mobHealth / mobMaxHealth));
        bossBar.setTitle(ChatColor.translateAlternateColorCodes('&', "&c" + mob.getName() + " - " + mobHealth + "/" + mobMaxHealth));
    }

    // Task to make the mob always face the player
    private void startMobFacingTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!active || !mob.isValid()) {
                    this.cancel(); // Stop the task if the combat session is no longer active or the mob is not valid
                    return;
                }

                // Make the mob face the player
                Location mobLocation = mob.getLocation();
                Location playerLocation = player.getLocation();

                double deltaX = playerLocation.getX() - mobLocation.getX();
                double deltaZ = playerLocation.getZ() - mobLocation.getZ();
                float yaw = (float) Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90;

                mobLocation.setYaw(yaw);
                mob.teleport(mobLocation);
            }
        }.runTaskTimer(plugin, 0L, 2L); // Run every 2 ticks (0.1 seconds)
    }
}