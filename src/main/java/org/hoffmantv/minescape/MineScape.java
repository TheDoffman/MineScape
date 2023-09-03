package org.hoffmantv.minescape;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.hoffmantv.minescape.commands.*;
import org.hoffmantv.minescape.listeners.*;
import org.hoffmantv.minescape.managers.*;
import org.hoffmantv.minescape.mobs.*;
import org.hoffmantv.minescape.skills.*;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class MineScape extends JavaPlugin {

    private File npcFile;
    private FileConfiguration npcConfig;

    private File attackFile;
    private FileConfiguration attackConfig;

    private File strengthFile;
    private FileConfiguration strengthConfig;

    @Override
    public void onEnable() {

        getLogger().info("MineScape Alpha Version 0.1 has been enabled!");

        // Metrics
        int pluginId = 19471;
        new Metrics(this, pluginId);
        // Configuration setup
        setupConfiguration();

        // Initialize managers and skills
        SkillManager skillManager = new SkillManager(this);
        CombatLevel combatLevel = new CombatLevel(skillManager);

        // Register commands
        registerCommands(skillManager);

        // Register event listeners
        registerEventListeners(skillManager, combatLevel);

        // Load or create the attack.yml configuration file
        attackFile = new File(getDataFolder(), "skills/attack.yml");
        attackConfig = YamlConfiguration.loadConfiguration(attackFile);

        if (!attackFile.exists()) {
            // Create the file if it doesn't exist
            saveResource("skills/attack.yml", false);
        }
        // Load or create the strength.yml configuration file
        strengthFile = new File(getDataFolder(), "skills/strength.yml");
        strengthConfig = YamlConfiguration.loadConfiguration(strengthFile);

        if (!strengthFile.exists()) {
            // Create the file if it doesn't exist
            saveResource("skills/strength.yml", false);
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("MineScape has been disabled!");

    }

    private void setupConfiguration() {
        getConfig().options().copyDefaults(true);
        saveConfig();
    }

    private void registerCommands(SkillManager skillManager) {
        Objects.requireNonNull(this.getCommand("help")).setExecutor(new HelpCommand(this));
        Objects.requireNonNull(this.getCommand("saveskills")).setExecutor(new SaveSkillsCommand(skillManager));
        Objects.requireNonNull(getCommand("skills")).setExecutor(new SkillsMenuCommand(skillManager));
        Objects.requireNonNull(getCommand("alwaysday")).setExecutor(new AlwaysDayCommand(this));
    }

    private void registerEventListeners(SkillManager skillManager, CombatLevel combatLevel) {
        registerListener(new WaterListener(this));
        registerListener(skillManager);
        registerListener(new CombatLevelSystem(this, combatLevel));
        registerListener(new WoodcuttingSkill(skillManager, this));
        registerListener(new MiningSkill(skillManager, this));
        registerListener(new SmithingSkill(skillManager));
        registerListener(new FiremakingSkill(skillManager, this));
        registerListener(new HitpointsSkill(skillManager, this));
        registerListener(new PrayerSkill(skillManager, this));
        registerListener(new AttackSkill(skillManager, attackConfig));
        registerListener(new StrengthSkill(skillManager, strengthConfig));
        registerListener(new DefenseSkill(skillManager, attackConfig));
        registerListener(new RangeSkill(this, skillManager));
        registerListener(new AgilitySkill(skillManager));
        registerListener(new CookingSkill(skillManager));
        registerListener(new CraftingSkill(skillManager));
        registerListener(new MobListener());
        registerListener(new ChickenListener());
        registerListener(new ZombieListener());
        registerListener(new SpiderListener());
        registerListener(new SkeletonListener());
        registerListener(new CreeperListener());
        registerListener(new CowListener());
        registerListener(new SheepListener());
        registerListener(new HorseListener());
        registerListener(new PigListener());
        registerListener(new EndermenListener());
        registerListener(new VilligerListener());
        registerListener(new AlwaysDayListener(this));
        getServer().getPluginManager().registerEvents(new ResourcePackListener(this), this);
    }

    private void registerListener(Listener listener) {
        getServer().getPluginManager().registerEvents(listener, this);

    }
    // Getter method to access the attack.yml configuration
    public FileConfiguration getAttackConfig() {
        return attackConfig;
    }
    public FileConfiguration getStrengthConfig() {
        return strengthConfig;
    }
}
