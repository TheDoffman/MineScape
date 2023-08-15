package org.hoffmantv.minescape.managers;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SkillManager implements Listener {

    private final JavaPlugin plugin;
    private File configFile;
    private FileConfiguration config;

    private static final int MAX_LEVEL = 99;

    // Initialize the maps right here
    private Map<UUID, Map<Skill, Integer>> playerLevels = new HashMap<>();
    private Map<UUID, Map<Skill, Double>> playerXP = new HashMap<>();

    public SkillManager(JavaPlugin plugin) {
        this.plugin = plugin;

        // Create and load the config when SkillManager is instantiated
        createConfig();
        loadSkillsFromConfig();
    }

    // Enum to represent different skills
    public enum Skill {
        WOODCUTTING, MINING, SMITHING, FISHING, ATTACK, DEFENCE,
        STRENGTH, RANGE, HITPOINTS, PRAYER, MAGIC, COOKING,
        FLETCHING, FIREMAKING, CRAFTING, HERBLORE, AGILITY,
        THEVING, SLAYER, FARMING, RUNECRAFTING, HUNTER, CONSTRUCTION // Removed the trailing comma
    }

    private double xpRequiredForLevelUp(int currentLevel) {
        return currentLevel * 99;  // Just a placeholder formula
    }

    private void createConfig() {
        configFile = new File(plugin.getDataFolder(), "skills.yml");
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            plugin.saveResource("skills.yml", false);
        }

        config = new YamlConfiguration();
        try {
            config.load(configFile);
        } catch (IOException | org.bukkit.configuration.InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    public void loadSkillsFromConfig() {
        for (String uuidString : config.getKeys(false)) {
            UUID uuid = UUID.fromString(uuidString);

            for (Skill skill : Skill.values()) {
                int level = config.getInt(uuidString + "." + skill.name() + ".level", 1); // default to 1
                double xp = config.getDouble(uuidString + "." + skill.name() + ".xp", 0.0); // default to 0.0

                playerLevels.computeIfAbsent(uuid, k -> new HashMap<>()).put(skill, level);
                playerXP.computeIfAbsent(uuid, k -> new HashMap<>()).put(skill, xp);
            }
        }
    }

    // Set a player's skill level
    public void setSkillLevel(Player player, Skill skill, int level) {
        int cappedLevel = Math.min(level, MAX_LEVEL); // Ensure we don't go beyond MAX_LEVEL
        playerLevels.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>()).put(skill, cappedLevel);
    }

    // Get a player's skill level
    public int getSkillLevel(Player player, Skill skill) {
        return playerLevels.getOrDefault(player.getUniqueId(), Collections.emptyMap()).getOrDefault(skill, 1);
    }

    // Add experience to a skill
    public boolean addXP(Player player, Skill skill, double xp) {
        System.out.println("DEBUG: Adding XP. Player: " + player.getName() + ", Skill: " + skill.name() + ", XP: " + xp);  // DEBUG: XP addition

        double currentXP = getXP(player, skill);
        int currentLevel = getSkillLevel(player, skill);
        double newXP = currentXP + xp;

        // Check if we've gained a level or more
        while (newXP >= xpRequiredForLevelUp(currentLevel) && currentLevel < MAX_LEVEL) {
            newXP -= xpRequiredForLevelUp(currentLevel);
            currentLevel++;

            // Notify the player about the level up
            player.sendMessage(ChatColor.GOLD + "Congratulations! You've reached level " + currentLevel + " in " + skill.name() + "!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);  // Play a level-up sound

            playerLevels.get(player.getUniqueId()).put(skill, currentLevel);
            playerXP.get(player.getUniqueId()).put(skill, newXP);


            // Save the updated XP and skill levels to the configuration file
            saveSkillsToConfig();
        }


        // If we've hit MAX_LEVEL, any excess XP should be discarded
        if (currentLevel == MAX_LEVEL) {
            newXP = 0;
        }

        playerLevels.get(player.getUniqueId()).put(skill, currentLevel);
        playerXP.get(player.getUniqueId()).put(skill, newXP);
        return false;
    }
    public void saveSkillsToConfig() {
        System.out.println("DEBUG: Saving skills to config...");  // DEBUG: Starting save
        for (UUID uuid : playerLevels.keySet()) {
            for (Skill skill : playerLevels.get(uuid).keySet()) {
                config.set(uuid.toString() + "." + skill.name() + ".level", playerLevels.get(uuid).get(skill));
                config.set(uuid.toString() + "." + skill.name() + ".xp", playerXP.get(uuid).get(skill));
            }
        }

        try {
            config.save(configFile);
            System.out.println("DEBUG: Successfully saved to skills.yml!");  // DEBUG: Successful save
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("DEBUG: Error saving to skills.yml!");  // DEBUG: Error during save
        }
    }


    // Get the XP for a specific skill
    public double getXP(Player player, Skill skill) {
        return playerXP.getOrDefault(player.getUniqueId(), Collections.emptyMap()).getOrDefault(skill, 0.0);
    }
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        // Check if the player already has an entry in skills.yml
        if (!config.contains(playerUUID.toString())) {
            System.out.println("DEBUG: New player joined. Setting default skills for " + player.getName());  // DEBUG: New player

            // Create default skill entries for the player
            for (Skill skill : Skill.values()) {
                config.set(playerUUID.toString() + "." + skill.name() + ".level", 1);
                config.set(playerUUID.toString() + "." + skill.name() + ".xp", 0.0);
            }
            // Save the config after creating the entries
            saveSkillsToConfig();
        }
    }
}
