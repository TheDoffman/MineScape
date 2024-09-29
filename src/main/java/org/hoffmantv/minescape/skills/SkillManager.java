package org.hoffmantv.minescape.skills;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.hoffmantv.minescape.managers.ConfigurationManager;

import java.lang.reflect.Field;
import java.util.*;

public class SkillManager implements Listener {

    private final JavaPlugin plugin;
    private final ConfigurationManager configManager;
    private final FileConfiguration playerDataConfig;
    private final FileConfiguration skillsConfig;
    private final CombatLevel combatLevel;

    private static final int MAX_LEVEL = 99;

    // Initialize the maps to store player levels and XP
    private final Map<UUID, Map<Skill, Integer>> playerLevels = new HashMap<>();
    private final Map<UUID, Map<Skill, Double>> playerXP = new HashMap<>();

    // Map to store weapon requirements: <Material, Required Strength Level>
    private final Map<Material, Integer> weaponStrengthRequirements = new HashMap<>();

    public SkillManager(JavaPlugin plugin, ConfigurationManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;

        this.combatLevel = new CombatLevel(this);

        // Load the playerdata.yml configuration through ConfigurationManager
        this.playerDataConfig = configManager.getPlayerDataConfig();

        // Load the skills.yml configuration through ConfigurationManager
        this.skillsConfig = configManager.getSkillsConfig();

        // Load skills from the configuration
        loadSkillsFromConfig();

        // Load weapon requirements from skills.yml
        loadWeaponRequirements();
    }

    // Getter for the plugin instance
    public JavaPlugin getPlugin() {
        return plugin;
    }

    // Enum to represent different skills
    public enum Skill {
        WOODCUTTING, MINING, SMITHING, FISHING, ATTACK, DEFENCE,
        STRENGTH, RANGE, HITPOINTS, PRAYER, MAGIC, COOKING,
        FLETCHING, FIREMAKING, CRAFTING, HERBLORE, AGILITY,
        THEVING, SLAYER, FARMING, RUNECRAFTING, HUNTER, CONSTRUCTION, ALCHEMY,
        COMBAT
    }

    // Formula to calculate XP required for the next level
    public double xpRequiredForLevelUp(int currentLevel) {
        // Using a standard XP curve formula
        return Math.floor(0.04 * Math.pow(currentLevel, 3) + 0.8 * Math.pow(currentLevel, 2) + 2 * currentLevel);
    }

    public void loadSkillsFromConfig() {
        Set<String> uuidStrings = playerDataConfig.getKeys(false);
        for (String uuidString : uuidStrings) {
            try {
                UUID uuid = UUID.fromString(uuidString); // Check if it's a valid UUID

                Map<Skill, Integer> levels = new HashMap<>();
                Map<Skill, Double> xpMap = new HashMap<>();

                for (Skill skill : Skill.values()) {
                    int defaultLevel = (skill == Skill.COMBAT) ? 3 : 1; // Set default combat level to 3
                    int level = playerDataConfig.getInt(uuidString + "." + skill.name() + ".level", defaultLevel);
                    double xp = playerDataConfig.getDouble(uuidString + "." + skill.name() + ".xp", 0.0);

                    levels.put(skill, level);
                    xpMap.put(skill, xp);
                }

                playerLevels.put(uuid, levels);
                playerXP.put(uuid, xpMap);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid entry in playerdata.yml: " + uuidString + ". Skipping...");
            }
        }
    }

    // Load weapon requirements from skills.yml
    private void loadWeaponRequirements() {
        ConfigurationSection strengthConfig = skillsConfig.getConfigurationSection("strength");
        if (strengthConfig != null) {
            ConfigurationSection weaponReqSection = strengthConfig.getConfigurationSection("weaponRequirements");
            if (weaponReqSection != null) {
                Set<String> keys = weaponReqSection.getKeys(false);
                for (String key : keys) {
                    try {
                        Material material = Material.valueOf(key);
                        int requiredLevel = weaponReqSection.getInt(key);
                        weaponStrengthRequirements.put(material, requiredLevel);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid material in weapon requirements: " + key);
                    }
                }
            } else {
                plugin.getLogger().warning("No weapon requirements found in skills.yml under strength.weaponRequirements");
            }
        } else {
            plugin.getLogger().warning("No strength section found in skills.yml");
        }
    }

    // Get the required Strength level for a weapon
    public int getRequiredStrengthLevel(Material weaponType) {
        return weaponStrengthRequirements.getOrDefault(weaponType, 1); // Default required level is 1
    }

    // Set a player's skill level
    public void setSkillLevel(Player player, Skill skill, int level) {
        int cappedLevel = Math.min(level, MAX_LEVEL); // Ensure we don't go beyond MAX_LEVEL
        UUID playerUUID = player.getUniqueId();

        // Ensure player's data is initialized
        playerLevels.computeIfAbsent(playerUUID, k -> new HashMap<>());
        playerLevels.get(playerUUID).put(skill, cappedLevel);

        // Update the configuration
        playerDataConfig.set(playerUUID.toString() + "." + skill.name() + ".level", cappedLevel);
        configManager.savePlayerData();
    }

    // Get a player's skill level
    public int getSkillLevel(Player player, Skill skill) {
        UUID playerUUID = player.getUniqueId();
        return playerLevels.getOrDefault(playerUUID, Collections.emptyMap())
                .getOrDefault(skill, skill == Skill.COMBAT ? 3 : 1);
    }

    // Add experience to a skill
    public boolean addXP(Player player, Skill skill, double xp) {
        System.out.println("DEBUG: Adding XP. Player: " + player.getName() + ", Skill: " + skill.name() + ", XP: " + xp);  // DEBUG: XP addition

        UUID playerUUID = player.getUniqueId();

        // Ensure player's data is initialized
        playerLevels.computeIfAbsent(playerUUID, k -> new HashMap<>());
        playerXP.computeIfAbsent(playerUUID, k -> new HashMap<>());

        Map<Skill, Integer> playerSkillLevels = playerLevels.get(playerUUID);
        Map<Skill, Double> playerSkillXP = playerXP.get(playerUUID);

        // Ensure the specific skill is initialized
        playerSkillLevels.putIfAbsent(skill, skill == Skill.COMBAT ? 3 : 1);
        playerSkillXP.putIfAbsent(skill, 0.0);

        double currentXP = playerSkillXP.get(skill);
        int currentLevel = playerSkillLevels.get(skill);
        double newXP = currentXP + xp;

        boolean leveledUp = false;

        while (newXP >= xpRequiredForLevelUp(currentLevel) && currentLevel < MAX_LEVEL) {
            newXP -= xpRequiredForLevelUp(currentLevel);
            currentLevel++;
            leveledUp = true;

            // Notify the player about the level up
            String title = ChatColor.GOLD + "Level Up!";
            String subtitle = ChatColor.YELLOW + "You've reached level " + currentLevel + " in " + skill.name() + "!";
            int fadeIn = 10; // time in ticks (20 ticks = 1 second)
            int stay = 70;
            int fadeOut = 20;
            player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
            launchFirework(player.getLocation());

            // Update combat level if necessary
            if (skill == Skill.ATTACK || skill == Skill.DEFENCE || skill == Skill.STRENGTH ||
                    skill == Skill.HITPOINTS || skill == Skill.MAGIC || skill == Skill.RANGE || skill == Skill.PRAYER) {
                combatLevel.updateCombatLevel(player, player);
            }

            playerSkillLevels.put(skill, currentLevel);
            playerSkillXP.put(skill, newXP);

            // Update the configuration
            playerDataConfig.set(playerUUID.toString() + "." + skill.name() + ".level", currentLevel);
            playerDataConfig.set(playerUUID.toString() + "." + skill.name() + ".xp", newXP);
            configManager.savePlayerData();
        }

        // If we've hit MAX_LEVEL, any excess XP should be discarded
        if (currentLevel == MAX_LEVEL) {
            newXP = 0;
        }

        playerSkillLevels.put(skill, currentLevel);
        playerSkillXP.put(skill, newXP);

        // Update the configuration
        playerDataConfig.set(playerUUID.toString() + "." + skill.name() + ".level", currentLevel);
        playerDataConfig.set(playerUUID.toString() + "." + skill.name() + ".xp", newXP);
        configManager.savePlayerData();

        return leveledUp;
    }

    // Get the XP for a specific skill
    public double getXP(Player player, Skill skill) {
        UUID playerUUID = player.getUniqueId();
        return playerXP.getOrDefault(playerUUID, Collections.emptyMap())
                .getOrDefault(skill, 0.0);
    }

    public double xpNeededForNextLevel(Player player, Skill skill) {
        int currentLevel = getSkillLevel(player, skill);

        // Check if the player has reached level 99
        if (currentLevel >= MAX_LEVEL) {
            return 0.0;
        }

        double currentXP = getXP(player, skill);
        double nextLevelXP = xpRequiredForLevelUp(currentLevel);  // XP needed to reach the next level from 0

        return nextLevelXP - currentXP;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        // Initialize player's data if not present
        if (!playerLevels.containsKey(playerUUID)) {
            System.out.println("DEBUG: Initializing skill data for new player: " + player.getName());

            Map<Skill, Integer> levels = new HashMap<>();
            Map<Skill, Double> xpMap = new HashMap<>();

            for (Skill skill : Skill.values()) {
                int level = (skill == Skill.COMBAT) ? 3 : 1;
                levels.put(skill, level);
                xpMap.put(skill, 0.0);

                // Update the configuration
                playerDataConfig.set(playerUUID.toString() + "." + skill.name() + ".level", level);
                playerDataConfig.set(playerUUID.toString() + "." + skill.name() + ".xp", 0.0);
            }

            playerLevels.put(playerUUID, levels);
            playerXP.put(playerUUID, xpMap);
        }

        // Initialize or update playtime
        if (!playerDataConfig.contains(playerUUID.toString() + ".playtime")) {
            playerDataConfig.set(playerUUID.toString() + ".playtime", 0); // Set initial playtime to 0 seconds
        }

        // Save the player data
        configManager.savePlayerData();

        // Start tracking playtime
        startPlaytimeTracking(player);

        // Update combat level displays
        combatLevel.updateCombatLevel(player, player);
        combatLevel.updatePlayerNametag(player);
        combatLevel.updatePlayerHeadDisplay(player);
    }

    private void startPlaytimeTracking(Player player) {
        UUID playerUUID = player.getUniqueId();
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    this.cancel();
                    return;
                }

                // Increase playtime by 60 seconds every minute
                long playtime = playerDataConfig.getLong(playerUUID.toString() + ".playtime");
                playtime += 60;
                playerDataConfig.set(playerUUID.toString() + ".playtime", playtime);

                // Save the updated playtime
                configManager.savePlayerData();
            }
        }.runTaskTimer(plugin, 0L, 20L); // 1200 ticks = 1 minute
    }

    // Method to get player health
    public int getHealth(Player player) {
        return (int) player.getHealth(); // Return player health as an integer value
    }

    // Method to get player combat level
    public int getCombatLevel(Player player) {
        return combatLevel.calculateCombatLevel(player); // Use combatLevel object to get combat level
    }

    // Method to get player ping
    public int getPing(Player player) {
        // Using the Paper method if available, else use reflection
        try {
            return player.getPing(); // For Paper servers
        } catch (NoSuchMethodError e) {
            // Fallback for servers without getPing method (Reflection)
            try {
                Object craftPlayer = player.getClass().getMethod("getHandle").invoke(player);
                Field pingField = craftPlayer.getClass().getDeclaredField("ping");
                return pingField.getInt(craftPlayer);
            } catch (Exception ex) {
                ex.printStackTrace();
                return -1; // Return -1 if there is an issue retrieving the ping
            }
        }
    }
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        // Save the current playtime to playerdata.yml
        long playtime = playerDataConfig.getLong(playerUUID.toString() + ".playtime");
        playerDataConfig.set(playerUUID.toString() + ".playtime", playtime);
        configManager.savePlayerData();
    }
    // Utility method to get all skill levels for a player
    public Map<Skill, Integer> getAllSkillLevels(Player player) {
        UUID playerUUID = player.getUniqueId();
        return playerLevels.getOrDefault(playerUUID, Collections.emptyMap());
    }

    // Method to show the skills hologram
    public void showSkillsHologram(Player player) {
        SkillsHologram hologram = new SkillsHologram(this);
        hologram.showSkillsHologram(player);
    }

    // Utility method to launch a firework at a location
    public void launchFirework(Location location) {
        Firework firework = (Firework) location.getWorld().spawnEntity(location, EntityType.FIREWORK);
        FireworkMeta fireworkMeta = firework.getFireworkMeta();

        // Customize the firework
        FireworkEffect effect = FireworkEffect.builder()
                .withColor(Color.RED)
                .withFlicker()
                .withTrail()
                .withFade(Color.ORANGE)
                .with(FireworkEffect.Type.BALL_LARGE)
                .build();

        fireworkMeta.addEffect(effect);
        fireworkMeta.setPower(1);

        firework.setFireworkMeta(fireworkMeta);
    }

    // Method to get formatted playtime
    public String getFormattedPlaytime(Player player) {
        long playtimeTicks = getPlaytime(player);
        long playtimeSeconds = playtimeTicks / 20;
        long hours = playtimeSeconds / 3600;
        long minutes = (playtimeSeconds % 3600) / 60;
        long seconds = playtimeSeconds % 60;

        return String.format("%dh %dm %ds", hours, minutes, seconds);
    }

    // Method to get player playtime in ticks
    public long getPlaytime(Player player) {
        UUID playerUUID = player.getUniqueId();
        return playerDataConfig.getLong(playerUUID.toString() + ".playtime", 0L);
    }

    // Method to update player playtime
    public void updatePlaytime(Player player, long ticksPlayed) {
        UUID playerUUID = player.getUniqueId();
        long currentPlaytime = getPlaytime(player);
        playerDataConfig.set(playerUUID.toString() + ".playtime", currentPlaytime + ticksPlayed);
        configManager.savePlayerData();
    }
}