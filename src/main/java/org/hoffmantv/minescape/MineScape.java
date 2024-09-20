package org.hoffmantv.minescape;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.hoffmantv.minescape.commands.*;
import org.hoffmantv.minescape.listeners.*;
import org.hoffmantv.minescape.managers.*;
import org.hoffmantv.minescape.mobs.*;
import org.hoffmantv.minescape.skills.*;

import java.util.Objects;

public class MineScape extends JavaPlugin {
    private ConfigurationManager configManager;
    private SkillManager skillManager;

    @Override
    public void onEnable() {
        getLogger().info("MineScape Alpha Version 0.1 has been enabled!");

        // Initialize ConfigurationManager
        configManager = new ConfigurationManager(this);

        // Initialize Configuration Files
        setupConfiguration();

        // Initialize SkillManager
        skillManager = new SkillManager(this, configManager);

        // Initialize CombatLevel (if applicable)
        CombatLevel combatLevel = new CombatLevel(skillManager);

        // Register Commands
        registerCommands(skillManager);

        // Load or create the skills.yml configuration file
        FileConfiguration skillsConfig = configManager.getConfig("skills.yml");

        // Register Skills with proper configuration sections
        ConfigurationSection strengthConfig = skillsConfig.getConfigurationSection("skills.strength");
        ConfigurationSection defenseConfig = skillsConfig.getConfigurationSection("skills.defense");
        ConfigurationSection woodcuttingConfig = skillsConfig.getConfigurationSection("skills.woodcutting");
        ConfigurationSection prayerConfig = skillsConfig.getConfigurationSection("skills.prayer");
        ConfigurationSection miningConfig = skillsConfig.getConfigurationSection("skills.mining");
        ConfigurationSection smithingConfig = skillsConfig.getConfigurationSection("skills.smithing");

        if (strengthConfig != null) {
            registerListener(new StrengthSkill(skillManager, strengthConfig, configManager));
        } else {
            getLogger().warning("No 'strength' section found in skills.yml under 'skills'");
        }

        if (defenseConfig != null) {
            registerListener(new DefenseSkill(skillManager, defenseConfig, configManager));
        } else {
            getLogger().warning("No 'defense' section found in skills.yml under 'skills'");
        }

        if (woodcuttingConfig != null) {
            registerListener(new WoodcuttingSkill(skillManager, configManager, this));
        } else {
            getLogger().warning("No 'woodcutting' section found in skills.yml under 'skills'");
        }

        if (prayerConfig != null) {
            registerListener(new PrayerSkill(skillManager, prayerConfig, this));
        } else {
            getLogger().warning("No 'prayer' section found in skills.yml under 'skills'");
        }

        if (miningConfig != null) {
            registerListener(new MiningSkill(skillManager, miningConfig, this));
        } else {
            getLogger().warning("No 'mining' section found in skills.yml under 'skills'");
        }

        if (smithingConfig != null) {
            registerListener(new SmithingSkill(skillManager, configManager, this));
        } else {
            getLogger().warning("No 'smithing' section found in skills.yml under 'skills'");
        }

        // Register other listeners
        registerListener(new WaterListener(this));
        registerListener(skillManager); // Assuming SkillManager implements Listener
        registerListener(new CombatLevelSystem(this, combatLevel));
        registerListener(new FiremakingSkill(skillManager, this));
        registerListener(new HitpointsSkill(skillManager, this));
        registerListener(new RangeSkill(this, skillManager));
        registerListener(new AgilitySkill(skillManager));
        registerListener(new CookingSkill(skillManager));
        registerListener(new CraftingSkill(skillManager));
        registerListener(new MobListener());
        registerListener(new ChickenListener(this));
        registerListener(new ZombieListener());
        registerListener(new SpiderListener());
        registerListener(new SkeletonListener());
        registerListener(new CreeperListener());
        registerListener(new CowListener());
        registerListener(new SheepListener());
        registerListener(new HorseListener());
        registerListener(new PigListener());
        registerListener(new EndermenListener());
        registerListener(new VilligerListener()); // Corrected spelling
        registerListener(new AlwaysDay(this));

        // Register ResourcePack listener
        getServer().getPluginManager().registerEvents(new ResourcePack(this), this);

        // Log plugin enabled successfully
        getLogger().info("MineScape has been enabled successfully.");
    }

    @Override
    public void onDisable() {
        getLogger().info("MineScape has been disabled!");
    }

    /**
     * Sets up configuration files, ensuring defaults are copied if they don't exist.
     */
    private void setupConfiguration() {
        getConfig().options().copyDefaults(true);
        saveConfig();

        // Save default skills.yml if it doesn't exist
        configManager.saveDefaultConfig("skills.yml");

        // Save default playerdata.yml if it doesn't exist
        configManager.saveDefaultConfig("playerdata.yml"); // Assuming this method handles any config
    }

    /**
     * Registers command executors.
     *
     * @param skillManager The SkillManager instance.
     */
    private void registerCommands(SkillManager skillManager) {
        Objects.requireNonNull(this.getCommand("help")).setExecutor(new HelpCommand(this));
        Objects.requireNonNull(getCommand("skills")).setExecutor(new SkillsMenuCommand(skillManager));
        Objects.requireNonNull(getCommand("alwaysday")).setExecutor(new AlwaysDayCommand(this));
    }

    /**
     * Registers event listeners.
     *
     * @param skillManager The SkillManager instance.
     * @param combatLevel  The CombatLevel instance.
     */
    private void registerEventListeners(SkillManager skillManager, CombatLevel combatLevel) {
        // All listeners are registered during skill registration and above
        // This method can be used for additional listeners if necessary
    }

    /**
     * Helper method to register a listener.
     *
     * @param listener The listener to register.
     */
    private void registerListener(Listener listener) {
        getServer().getPluginManager().registerEvents(listener, this);
    }
}