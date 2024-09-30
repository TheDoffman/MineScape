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

    // Maps to store player levels and XP
    private final Map<UUID, Map<Skill, Integer>> playerLevels = new HashMap<>();
    private final Map<UUID, Map<Skill, Double>> playerXP = new HashMap<>();

    // Weapon strength requirements
    private final Map<Material, Integer> weaponStrengthRequirements = new HashMap<>();

    public SkillManager(JavaPlugin plugin, ConfigurationManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;

        this.combatLevel = new CombatLevel(this);

        // Load player data and skills configuration
        this.playerDataConfig = configManager.getPlayerDataConfig();
        this.skillsConfig = configManager.getSkillsConfig();

        loadSkillsFromConfig();
        loadWeaponRequirements();
    }

    // Enum to represent different skills
    public enum Skill {
        WOODCUTTING, MINING, SMITHING, FISHING, ATTACK, DEFENCE,
        STRENGTH, RANGE, HITPOINTS, PRAYER, MAGIC, COOKING,
        FLETCHING, FIREMAKING, CRAFTING, HERBLORE, AGILITY,
        THEVING, SLAYER, FARMING, RUNECRAFTING, HUNTER, CONSTRUCTION, ALCHEMY,
        COMBAT
    }

    // XP formula
    public double xpRequiredForLevelUp(int currentLevel) {
        double totalXp = 0;
        for (int i = 1; i <= currentLevel; i++) {
            totalXp += Math.floor(i + 300 * Math.pow(2, i / 7.0));
        }
        return Math.floor(totalXp / 4);
    }

    // Load player skills from config
    public void loadSkillsFromConfig() {
        Set<String> uuidStrings = playerDataConfig.getKeys(false);
        for (String uuidString : uuidStrings) {
            try {
                UUID uuid = UUID.fromString(uuidString);
                Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
                Map<Skill, Double> xpMap = new EnumMap<>(Skill.class);

                for (Skill skill : Skill.values()) {
                    int defaultLevel = (skill == Skill.COMBAT) ? 3 : 1;
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

    // Load weapon requirements from config
    private void loadWeaponRequirements() {
        ConfigurationSection strengthConfig = skillsConfig.getConfigurationSection("strength");
        if (strengthConfig != null) {
            ConfigurationSection weaponReqSection = strengthConfig.getConfigurationSection("weaponRequirements");
            if (weaponReqSection != null) {
                weaponReqSection.getKeys(false).forEach(key -> {
                    try {
                        Material material = Material.valueOf(key.toUpperCase());
                        int requiredLevel = weaponReqSection.getInt(key);
                        weaponStrengthRequirements.put(material, requiredLevel);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid material in weapon requirements: " + key);
                    }
                });
            }
        }
    }

    // Retrieve required Strength level for a weapon
    public int getRequiredStrengthLevel(Material weaponType) {
        return weaponStrengthRequirements.getOrDefault(weaponType, 1);
    }

    // Set player skill level
    public void setSkillLevel(Player player, Skill skill, int level) {
        UUID playerUUID = player.getUniqueId();
        initializePlayerData(playerUUID);
        int cappedLevel = Math.min(level, MAX_LEVEL);

        playerLevels.get(playerUUID).put(skill, cappedLevel);
        playerDataConfig.set(playerUUID.toString() + "." + skill.name() + ".level", cappedLevel);
        savePlayerDataAsync();
    }

    // Get player skill level
    public int getSkillLevel(Player player, Skill skill) {
        UUID playerUUID = player.getUniqueId();
        initializePlayerData(playerUUID);
        return playerLevels.get(playerUUID).getOrDefault(skill, skill == Skill.COMBAT ? 3 : 1);
    }

    // Add XP to player skill
    public boolean addXP(Player player, Skill skill, double xp) {
        UUID playerUUID = player.getUniqueId();
        initializePlayerData(playerUUID);

        Map<Skill, Integer> playerSkillLevels = playerLevels.get(playerUUID);
        Map<Skill, Double> playerSkillXP = playerXP.get(playerUUID);

        double currentXP = playerSkillXP.get(skill);
        int currentLevel = playerSkillLevels.get(skill);
        double newXP = currentXP + xp;

        boolean leveledUp = false;
        while (newXP >= xpRequiredForLevelUp(currentLevel) && currentLevel < MAX_LEVEL) {
            newXP -= xpRequiredForLevelUp(currentLevel);
            currentLevel++;
            leveledUp = true;

            notifyPlayerLevelUp(player, skill, currentLevel);
        }

        playerSkillLevels.put(skill, currentLevel);
        playerSkillXP.put(skill, newXP);

        playerDataConfig.set(playerUUID.toString() + "." + skill.name() + ".level", currentLevel);
        playerDataConfig.set(playerUUID.toString() + "." + skill.name() + ".xp", newXP);
        savePlayerDataAsync();

        return leveledUp;
    }

    private void notifyPlayerLevelUp(Player player, Skill skill, int level) {
        String title = ChatColor.GOLD + "Level Up!";
        String subtitle = ChatColor.YELLOW + "You've reached level " + level + " in " + skill.name() + "!";
        player.sendTitle(title, subtitle, 10, 70, 20);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
        launchFirework(player.getLocation());
    }

    // Get XP for specific skill
    public double getXP(Player player, Skill skill) {
        UUID playerUUID = player.getUniqueId();
        initializePlayerData(playerUUID);
        return playerXP.get(playerUUID).getOrDefault(skill, 0.0);
    }

    // Calculate remaining XP for next level
    public double xpNeededForNextLevel(Player player, Skill skill) {
        int currentLevel = getSkillLevel(player, skill);
        if (currentLevel >= MAX_LEVEL) return 0.0;
        double currentXP = getXP(player, skill);
        return xpRequiredForLevelUp(currentLevel) - currentXP;
    }

    // Player join event
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        initializePlayer(player);
    }

    // Player quit event
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        long playtime = playerDataConfig.getLong(playerUUID.toString() + ".playtime");
        playerDataConfig.set(playerUUID.toString() + ".playtime", playtime);
        savePlayerDataAsync();
    }

    // Initialize player data
    private void initializePlayer(Player player) {
        UUID playerUUID = player.getUniqueId();
        initializePlayerData(playerUUID);

        if (!playerDataConfig.contains(playerUUID.toString() + ".playtime")) {
            playerDataConfig.set(playerUUID.toString() + ".playtime", 0L);
        }

        savePlayerDataAsync();
        startPlaytimeTracking(player);
        combatLevel.updateCombatLevel(player, player);
        combatLevel.updatePlayerNametag(player);
        combatLevel.updatePlayerHeadDisplay(player);
    }

    // Initialize player skill and XP data
    private void initializePlayerData(UUID playerUUID) {
        playerLevels.computeIfAbsent(playerUUID, k -> new EnumMap<>(Skill.class));
        playerXP.computeIfAbsent(playerUUID, k -> new EnumMap<>(Skill.class));
    }

    // Start playtime tracking for a player
    private void startPlaytimeTracking(Player player) {
        UUID playerUUID = player.getUniqueId();
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    this.cancel();
                    return;
                }

                long playtime = playerDataConfig.getLong(playerUUID.toString() + ".playtime");
                playtime += 60;
                playerDataConfig.set(playerUUID.toString() + ".playtime", playtime);
                savePlayerDataAsync();
            }
        }.runTaskTimer(plugin, 0L, 1200L); // 60 seconds = 1200 ticks
    }

    // Launch a firework at a location
    public void launchFirework(Location location) {
        Firework firework = (Firework) location.getWorld().spawnEntity(location, EntityType.FIREWORK);
        FireworkMeta fireworkMeta = firework.getFireworkMeta();
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

    // Save player data asynchronously
    public void savePlayerDataAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, configManager::savePlayerData);
    }

    // Get player playtime in ticks
    public long getPlaytime(Player player) {
        UUID playerUUID = player.getUniqueId();
        return playerDataConfig.getLong(playerUUID.toString() + ".playtime", 0L);
    }

    // Update player playtime
    public void updatePlaytime(Player player, long ticksPlayed) {
        UUID playerUUID = player.getUniqueId();
        long currentPlaytime = getPlaytime(player);
        playerDataConfig.set(playerUUID.toString() + ".playtime", currentPlaytime + ticksPlayed);
        savePlayerDataAsync();
    }

    // Get formatted playtime
    public String getFormattedPlaytime(Player player) {
        long playtimeTicks = getPlaytime(player);
        long playtimeSeconds = playtimeTicks / 20;
        long hours = playtimeSeconds / 3600;
        long minutes = (playtimeSeconds % 3600) / 60;
        long seconds = playtimeSeconds % 60;

        return String.format("%dh %dm %ds", hours, minutes, seconds);
    }

    // Get player ping
    public int getPing(Player player) {
        try {
            return player.getPing(); // Paper server method
        } catch (NoSuchMethodError e) {
            try {
                Object craftPlayer = player.getClass().getMethod("getHandle").invoke(player);
                Field pingField = craftPlayer.getClass().getDeclaredField("ping");
                return pingField.getInt(craftPlayer);
            } catch (Exception ex) {
                ex.printStackTrace();
                return -1;
            }
        }
    }
    public Map<Skill, Integer> getAllSkillLevels(Player player) {
        UUID playerUUID = player.getUniqueId();
        initializePlayerData(playerUUID); // Ensures player data is properly initialized
        return playerLevels.getOrDefault(playerUUID, Collections.emptyMap());
    }
    public JavaPlugin getPlugin() {
        return plugin;
    }
    public int getHealth(Player player) {
        return (int) Math.ceil(player.getHealth()); // Return player's health as an integer
    }
    public int getCombatLevel(Player player) {
        return combatLevel.calculateCombatLevel(player); // Assuming CombatLevel has a method to calculate the level
    }
}