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

    private FileConfiguration skillsConfig;
    private File skillsFile;

    public ConfigurationManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        loadPlayerDataFile(); // Load player data file during initialization
        loadSkillsFile();     // Load skills file during initialization
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
            plugin.saveResource("playerdata.yml", false);
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

    // Specific method to load skills.yml
    private void loadSkillsFile() {
        skillsFile = new File(dataFolder, "skills.yml");
        if (!skillsFile.exists()) {
            plugin.saveResource("skills.yml", false); // Create from resources
        }
        skillsConfig = YamlConfiguration.loadConfiguration(skillsFile);
    }

    // Getter for skills.yml configuration
    public FileConfiguration getSkillsConfig() {
        return skillsConfig;
    }

    // Save method for skills.yml
    public void saveSkillsConfig() {
        try {
            skillsConfig.save(skillsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save skills.yml");
        }
    }

    // Reload method for skills.yml
    public void reloadSkillsConfig() {
        skillsConfig = YamlConfiguration.loadConfiguration(skillsFile);
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
    }
}