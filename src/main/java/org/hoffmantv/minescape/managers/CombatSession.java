package org.hoffmantv.minescape.managers;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

public class CombatSession {
    private final Player player;
    private final LivingEntity mob;
    private final JavaPlugin plugin;
    private final SkillManager skillManager;
    private final CombatLevelSystem combatLevelSystem;
    private final Location initialMobLocation;
    private boolean playerTurn = true; // Start with player's turn
    private boolean active = true;
    private double playerHealth;
    private double mobHealth;
    private final double playerMaxHealth;
    private final double mobMaxHealth;
    private final Random random = new Random();
    private long lastAttackTime; // Track the last attack time
    private final String mobBaseName; // Store the mob's original name

    public CombatSession(Player player, LivingEntity mob, JavaPlugin plugin, SkillManager skillManager, CombatLevelSystem combatLevelSystem) {
        this.player = player;
        this.mob = mob;
        this.plugin = plugin;
        this.skillManager = skillManager;
        this.combatLevelSystem = combatLevelSystem;
        this.playerMaxHealth = player.getHealth();
        this.mobMaxHealth = mob.getHealth();
        this.playerHealth = playerMaxHealth;
        this.mobHealth = mobMaxHealth;
        this.initialMobLocation = mob.getLocation().clone(); // Store initial location of the mob
        this.mobBaseName = mob.getCustomName() != null ? mob.getCustomName() : mob.getName(); // Handle null custom names

        freezeMob(true); // Freeze the mob in place
        updateMobName(); // Update mob's name to include health
        startCombat();
        startMobFacingTask(); // Start the task to make the mob always face the player
    }

    public void startCombat() {
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

        double damage = calculateDamage(player, mob);
        if (damage > 0) {
            mobHealth -= damage;
            mob.damage(damage); // Apply damage to the mob

            // Visual effect for player attack
            player.getWorld().playSound(mob.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.0f, 1.0f);
            player.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, mob.getLocation().add(0, 1, 0), 10, 0.2, 0.2, 0.2, 0.1);

            // Update mob's name with new health
            updateMobName();
        } else {
            // Player missed the attack
            player.sendMessage(ChatColor.GRAY + "You missed your attack!");
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 0.5f);
        }

        lastAttackTime = System.currentTimeMillis(); // Update last attack time
        checkCombatEnd();
    }

    private void mobAttack() {
        if (!active) return;

        double damage = calculateDamage(mob, player);
        if (damage > 0) {
            playerHealth -= damage;
            player.damage(damage); // Apply damage to the player

            // Visual and sound effects to simulate mob attack
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 1.0f);
            player.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, player.getLocation().add(0, 1, 0), 10, 0.2, 0.2, 0.2, 0.1);
        } else {
            // Mob missed the attack
            player.sendMessage(ChatColor.GRAY + "The mob missed its attack!");
            player.getWorld().playSound(mob.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 0.5f);
        }

        lastAttackTime = System.currentTimeMillis(); // Update last attack time
        checkCombatEnd();
    }

    private double calculateDamage(LivingEntity attacker, LivingEntity defender) {
        // Use skill levels or combat levels to influence damage calculations
        int attackLevel = getCombatLevel(attacker);
        int defenseLevel = getCombatLevel(defender);

        // Get base damage from attacker's attribute, with a default if null
        double baseDamage = getBaseDamage(attacker);

        // Introduce randomness in damage
        double randomFactor = 0.8 + (random.nextDouble() * 0.4); // Random factor between 0.8 and 1.2
        double damage = baseDamage * randomFactor;

        // Adjust damage based on attack and defense levels
        double levelDifference = attackLevel - defenseLevel;
        double levelAdjustment = 1 + (levelDifference * 0.05); // 5% damage increase/decrease per level difference
        damage *= levelAdjustment;

        // Ensure damage is at least 1
        damage = Math.max(damage, 1);

        // Calculate hit chance
        double hitChance = Math.min(0.95, 0.5 + ((double) (attackLevel - defenseLevel) / 100));
        if (random.nextDouble() > hitChance) {
            return 0; // Missed attack
        }

        return damage;
    }

    private double getBaseDamage(LivingEntity attacker) {
        if (attacker instanceof Player) {
            // Get weapon from player's main hand
            Player playerAttacker = (Player) attacker;
            ItemStack weapon = playerAttacker.getInventory().getItemInMainHand();
            return getWeaponDamage(weapon);
        } else {
            // For mobs, use their attack damage attribute or a default value
            AttributeInstance attackAttribute = attacker.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
            if (attackAttribute != null) {
                return attackAttribute.getBaseValue();
            } else {
                return 2.0; // Default base damage for mobs
            }
        }
    }

    private double getWeaponDamage(ItemStack weapon) {
        if (weapon == null || weapon.getType() == Material.AIR) {
            return 1.0; // Fist damage
        }

        switch (weapon.getType()) {
            case WOODEN_SWORD:
                return 4.0; // Base damage for wooden sword
            case STONE_SWORD:
                return 5.0;
            case IRON_SWORD:
                return 6.0;
            case GOLDEN_SWORD:
                return 4.0;
            case DIAMOND_SWORD:
                return 7.0;
            case NETHERITE_SWORD:
                return 8.0;
            case WOODEN_AXE:
                return 7.0;
            case STONE_AXE:
                return 9.0;
            case IRON_AXE:
                return 9.0;
            case GOLDEN_AXE:
                return 7.0;
            case DIAMOND_AXE:
                return 9.0;
            case NETHERITE_AXE:
                return 10.0;
            // Add other weapons as needed
            default:
                return 1.0; // Default base damage for unrecognized items
        }
    }

    private int getCombatLevel(LivingEntity entity) {
        // Utilize the CombatLevel system if available, or use default values
        if (entity instanceof Player) {
            return skillManager.getSkillLevel((Player) entity, SkillManager.Skill.COMBAT);
        }
        // For mobs, extract level from name
        Integer mobLevel = CombatLevelSystem.extractMobLevelFromName(mob);
        return mobLevel != null ? mobLevel : 1; // Default value for mobs
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

    public void endCombat(String message) {
        if (!active) return; // Prevent multiple endings
        active = false;
        player.sendMessage(ChatColor.GREEN + message);

        // End combat visual effects
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, player.getLocation(), 10, 0.5, 0.5, 0.5, 0.1);

        freezeMob(false); // Unfreeze the mob

        // Restore mob's original name
        mob.setCustomName(mobBaseName);

        // Notify CombatLevelSystem to end the session
        combatLevelSystem.endCombatSession(player);
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

    // Update the mob's name to include current health
    private void updateMobName() {
        String healthInfo = ChatColor.WHITE + " [" + ChatColor.GREEN + (int) mobHealth + ChatColor.WHITE + "/" + (int) mobMaxHealth + ChatColor.WHITE + "]";
        mob.setCustomName(mobBaseName + healthInfo);
    }

    // Task to make the mob always face the player
    private void startMobFacingTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!active || !mob.isValid()) {
                    this.cancel();
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