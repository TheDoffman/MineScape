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

    // Specific method to load player data configuration
    private void loadPlayerDataFile() {
        playerDataFile = new File(dataFolder, "playerdata.yml");
        if (!playerDataFile.exists()) {
            try {
                playerDataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create playerdata.yml");
                e.printStackTrace();
            }
        }
        playerDataConfig = YamlConfiguration.loadConfiguration(playerDataFile);
    }

    // Get the player data configuration
    public FileConfiguration getPlayerDataConfig() {
        if (playerDataConfig == null) {
            loadPlayerDataFile();
        }
        return playerDataConfig;
    }

    // Save the player data to the file
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

    // Method to get a specific configuration section (useful for skills)
    public ConfigurationSection getConfigSection(String fileName, String section) {
        FileConfiguration config = loadConfig(fileName);
        return config.getConfigurationSection(section);
    }

    // Get a section from playerdata.yml
    public ConfigurationSection getPlayerDataSection(UUID playerUUID, String section) {
        return playerDataConfig.getConfigurationSection(playerUUID.toString() + "." + section);
    }
}