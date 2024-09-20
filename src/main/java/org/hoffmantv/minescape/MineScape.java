package org.hoffmantv.minescape;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.hoffmantv.minescape.commands.*;
import org.hoffmantv.minescape.listeners.*;
import org.hoffmantv.minescape.managers.*;
import org.hoffmantv.minescape.mobs.*;
import org.hoffmantv.minescape.skills.*;
import java.util.Objects;

public class  MineScape extends JavaPlugin {

    private ConfigurationManager configManager;

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

        // Initialize the configuration manager
        configManager = new ConfigurationManager(this);

        // Load your custom configuration files
        FileConfiguration attackConfig = configManager.loadConfig("skills/attack.yml");
        FileConfiguration strengthConfig = configManager.loadConfig("skills/strength.yml");
        FileConfiguration defenceConfig = configManager.loadConfig("skills/defence.yml");

// Initialize AttackSkill with the attackConfig
        AttackSkill attackSkill = new AttackSkill(skillManager, attackConfig, configManager);

        registerListener(skillManager);
        registerListener(attackSkill);
        registerListener(new WaterListener(this));
        registerListener(new CombatLevelSystem(this, combatLevel));
        registerListener(new WoodcuttingSkill(skillManager, this));
        registerListener(new MiningSkill(skillManager, this));
        registerListener(new SmithingSkill(skillManager));
        registerListener(new FiremakingSkill(skillManager, this));
        registerListener(new HitpointsSkill(skillManager, this));
        registerListener(new PrayerSkill(skillManager, this));
        registerListener(new StrengthSkill(skillManager));
        registerListener(new DefenseSkill(skillManager));
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
        registerListener(new VilligerListener());
        registerListener(new AlwaysDay(this));
        getServer().getPluginManager().registerEvents(new ResourcePack(this), this);

    }

    private void registerListener(Listener listener) {
        getServer().getPluginManager().registerEvents(listener, this);

    }
}
