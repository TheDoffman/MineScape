package org.hoffmantv.minescape.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class ConfigurationManager {
    private final JavaPlugin plugin;
    private final File dataFolder;

    public ConfigurationManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    public FileConfiguration loadConfig(String fileName) {
        File configFile = new File(dataFolder, fileName);

        if (!configFile.exists()) {
            plugin.saveResource(fileName, false); // Create from resources
        }

        return YamlConfiguration.loadConfiguration(configFile);
    }

    public void saveConfig(String fileName, FileConfiguration config) {
        File configFile = new File(dataFolder, fileName);

        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Error saving " + fileName);
        }
    }
}
