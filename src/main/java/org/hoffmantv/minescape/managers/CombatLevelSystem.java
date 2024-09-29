package org.hoffmantv.minescape.managers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.hoffmantv.minescape.skills.CombatLevel;
import org.hoffmantv.minescape.skills.SkillManager;

import java.util.*;

public class CombatLevelSystem implements Listener {

    private final JavaPlugin plugin;
    private final Random random = new Random();
    private final CombatLevel combatLevel;
    private final SkillManager skillManager;
    private final Map<UUID, Long> lastAttackTime = new HashMap<>();
    private static final int MAX_DISTANCE = 15; // Maximum distance to cancel the fight
    private static final int INACTIVITY_TIMEOUT = 10 * 1000; // 10 seconds in milliseconds

    // Active combat sessions
    private final Map<UUID, CombatSession> activeCombatSessions = new HashMap<>();
    private final Set<UUID> engagedPlayers = new HashSet<>();
    private final Object sessionLock = new Object();
    private final Map<UUID, Long> attackCooldowns = new HashMap<>();

    public CombatLevelSystem(JavaPlugin plugin, CombatLevel combatLevel, SkillManager skillManager) {
        this.plugin = plugin;
        this.combatLevel = combatLevel;
        this.skillManager = skillManager;

        this.plugin.getServer().getPluginManager().registerEvents(this, plugin);

        // Assign levels to existing mobs on startup
        for (World world : plugin.getServer().getWorlds()) {
            for (LivingEntity entity : world.getLivingEntities()) {
                if (isMobEligibleForLeveling(entity)) {
                    assignMobLevel(entity);
                }
            }
        }

        // Start inactivity checker
        startInactivityChecker();
    }

    private void assignMobLevel(LivingEntity mob) {
        Player closestPlayer = findClosestPlayer(mob);
        int mobLevel;

        if (closestPlayer != null) {
            int playerCombatLevel = combatLevel.calculateCombatLevel(closestPlayer);
            mobLevel = playerCombatLevel + random.nextInt(5) - 2; // player level +/-2
        } else {
            mobLevel = random.nextInt(3) + 1; // Random level between 1 and 3
        }

        // Ensure mobLevel is at least 1
        mobLevel = Math.max(mobLevel, 1);

        // If the mob is a baby, reduce the level slightly
        if (mob instanceof Ageable && !((Ageable) mob).isAdult()) {
            mobLevel = Math.max(1, mobLevel - 1);
        }

        setMobNameAndAttributes(mob, mobLevel);
    }

    private void setMobNameAndAttributes(LivingEntity mob, int mobLevel) {
        String mobName = formatMobName(mob.getType().toString());
        if (mob instanceof Ageable && !((Ageable) mob).isAdult()) {
            mobName = "Baby " + mobName;
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
        // Set base health similar to player's max health
        double baseHealth = 20.0;

        // Adjust health slightly based on mob level
        double newHealth = baseHealth + ((mobLevel - 1) * 2); // Increase health by 2 per level above 1

        // Cap the mob's health to a reasonable maximum (e.g., 40)
        newHealth = Math.min(newHealth, 40.0);

        if (mob.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
            mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(newHealth);
            mob.setHealth(newHealth); // Set the new health to reflect immediately
        }

        // Adjust damage output
        if (mob.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) != null) {
            double baseDamage = 2.0; // Base damage similar to player without weapon
            double newDamage = baseDamage + ((mobLevel - 1) * 0.5); // Increase damage by 0.5 per level above 1
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
        if (mobLevel >= 10) return "&4";    // Red for high-level mobs
        if (mobLevel >= 7) return "&c";     // Dark red for mid-level mobs
        if (mobLevel >= 4) return "&e";     // Yellow for lower-level mobs
        return "&a";                        // Green for very low-level mobs
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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof LivingEntity)) {
            return;
        }

        Player player = (Player) event.getDamager();
        LivingEntity mob = (LivingEntity) event.getEntity();

        // Only proceed if the damage cause is ENTITY_ATTACK (melee attack)
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            return;
        }

        Integer mobLevel = extractMobLevelFromName(mob);
        if (mobLevel == null) return;

        UUID playerUUID = player.getUniqueId();

        long currentTime = System.currentTimeMillis();

        synchronized (sessionLock) {
            // Cooldown to prevent rapid multiple sessions
            Long lastAttack = attackCooldowns.get(playerUUID);
            if (lastAttack != null && currentTime - lastAttack < 500) { // 500ms cooldown
                event.setCancelled(true);
                return;
            }
            attackCooldowns.put(playerUUID, currentTime);

            // Check if player is already in a combat session
            if (activeCombatSessions.containsKey(playerUUID) || engagedPlayers.contains(playerUUID)) {
                player.sendMessage(ChatColor.RED + "You are already in a combat session. Finish it before starting a new one!");
                event.setCancelled(true);
                return;
            }

            // Check if this mob is already in a combat session
            if (activeCombatSessions.values().stream().anyMatch(session -> session.getMob().equals(mob))) {
                player.sendMessage(ChatColor.RED + "This mob is already engaged in combat with another player!");
                event.setCancelled(true);
                return;
            }

            // Start the combat session
            startCombatSession(player, mob);
            lastAttackTime.put(mob.getUniqueId(), currentTime);
        }

        event.setCancelled(true); // Cancel regular damage
    }

    private void startCombatSession(Player player, LivingEntity mob) {
        UUID playerUUID = player.getUniqueId();

        synchronized (sessionLock) {
            if (activeCombatSessions.containsKey(playerUUID) || engagedPlayers.contains(playerUUID)) {
                return;
            }

            // Mark the player as engaged
            engagedPlayers.add(playerUUID);

            // Create a new combat session
            CombatSession session = new CombatSession(player, mob, plugin, skillManager, this);
            activeCombatSessions.put(playerUUID, session);
        }
    }

    @EventHandler
    public void onMobDeath(EntityDeathEvent event) {
        LivingEntity mob = event.getEntity();
        UUID mobUUID = mob.getUniqueId();

        lastAttackTime.remove(mobUUID);

        // End the combat session if it exists
        synchronized (sessionLock) {
            activeCombatSessions.entrySet().removeIf(entry -> {
                CombatSession session = entry.getValue();
                if (session.getMob().equals(mob)) {
                    UUID playerUUID = entry.getKey();
                    engagedPlayers.remove(playerUUID);
                    session.endCombat("Mob died."); // End the combat session
                    return true;
                }
                return false;
            });
        }
    }

    // Check for inactivity to cancel combat sessions
    private void startInactivityChecker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();

                synchronized (sessionLock) {
                    Iterator<Map.Entry<UUID, CombatSession>> iterator = activeCombatSessions.entrySet().iterator();
                    while (iterator.hasNext()) {
                        Map.Entry<UUID, CombatSession> entry = iterator.next();
                        UUID playerUUID = entry.getKey();
                        CombatSession session = entry.getValue();
                        if (session == null) {
                            iterator.remove();
                            continue;
                        }
                        Player player = Bukkit.getPlayer(playerUUID);
                        if (player == null || player.getLocation().distance(session.getMob().getLocation()) > MAX_DISTANCE ||
                                currentTime - session.getLastAttackTime() > INACTIVITY_TIMEOUT) {
                            session.endCombat("Combat ended due to inactivity.");
                            engagedPlayers.remove(playerUUID);
                            iterator.remove(); // Safely remove using iterator
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // Check every second
    }

    // Method to end the combat session and clean up
    public void endCombatSession(Player player) {
        UUID playerUUID = player.getUniqueId();
        synchronized (sessionLock) {
            // Check if the session is still present
            if (activeCombatSessions.containsKey(playerUUID)) {
                // The session will be removed by the iterator in startInactivityChecker()
                engagedPlayers.remove(playerUUID);
            }
        }
    }
}