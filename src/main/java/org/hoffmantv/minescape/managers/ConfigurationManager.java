package org.hoffmantv.minescape.managers;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class ConfigurationManager {
    private final JavaPlugin plugin;
    private final File dataFolder;
    private FileConfiguration playerDataConfig;
    private File playerDataFile;

    public ConfigurationManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        loadPlayerDataFile(); // Load player data file during initialization
    }

    // General method to load any configuration file
    public FileConfiguration loadConfig(String fileName) {
        File configFile = new File(dataFolder, fileName);

        if (!configFile.exists()) {
            plugin.saveResource(fileName, false); // Create from resources
        }

        return YamlConfiguration.loadConfiguration(configFile);
    }

    // Save method for any configuration file
    public void saveConfig(String fileName, FileConfiguration config) {
        File configFile = new File(dataFolder, fileName);

        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Error saving " + fileName);
        }
    }

    // Specific method to get player data configuration
    private void loadPlayerDataFile() {
        playerDataFile = new File(dataFolder, "playerdata.yml");
        if (!playerDataFile.exists()) {
            try {
                playerDataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        playerDataConfig = YamlConfiguration.loadConfiguration(playerDataFile);
    }

    public FileConfiguration getPlayerDataConfig() {
        return playerDataConfig;
    }

    public void savePlayerData() {
        try {
            playerDataConfig.save(playerDataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save playerdata.yml");
        }
    }

    // Log XP gain into playerdata.yml
    public void logXpGain(Player player, String skill, int xp) {
        UUID playerUUID = player.getUniqueId();
        String path = playerUUID.toString() + ".xpGains." + skill;
        int totalXp = playerDataConfig.getInt(path, 0);
        playerDataConfig.set(path, totalXp + xp);
        savePlayerData();
    }

    // Method to get a specific configuration section from a config file
    public ConfigurationSection getConfigSection(String fileName, String section) {
        FileConfiguration config = loadConfig(fileName);
        return config.getConfigurationSection(section);
    }

    // Method to get the full configuration file
    public FileConfiguration getConfig(String fileName) {
        return loadConfig(fileName);
    }

    public void saveDefaultConfig(String fileName) {
        File configFile = new File(dataFolder, fileName);
        if (!configFile.exists()) {
            plugin.saveResource(fileName, false);
        }
    }}