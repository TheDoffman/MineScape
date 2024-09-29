package org.hoffmantv.minescape;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.hoffmantv.minescape.commands.*;
import org.hoffmantv.minescape.commands.AlwaysDay;
import org.hoffmantv.minescape.listeners.*;
import org.hoffmantv.minescape.managers.*;
import org.hoffmantv.minescape.mobs.*;
import org.hoffmantv.minescape.skills.*;
import org.hoffmantv.minescape.trade.AcceptTrade;
import org.hoffmantv.minescape.trade.Trade;
import org.hoffmantv.minescape.trade.TradeMenu;

public class MineScape extends JavaPlugin {
    private ConfigurationManager configManager;
    private SkillManager skillManager;
    private SkillsHologram skillsHologram;
    private CombatLevelSystem combatLevelSystem;

    private TradeMenu tradeMenu;

    @Override
    public void onEnable() {
        getLogger().info("MineScape Alpha Version 0.2 has been enabled!");

        // Initialize ConfigurationManager
        configManager = new ConfigurationManager(this);

        // Initialize Configuration Files
        setupConfiguration();

        // Initialize SkillManager
        skillManager = new SkillManager(this, configManager);

        // Initialize CombatLevel (if applicable)
        CombatLevel combatLevel = new CombatLevel(skillManager);

        // Initialize SkillsHologram with SkillManager
        skillsHologram = new SkillsHologram(skillManager);

        tradeMenu = new TradeMenu(this);

        // Register Commands
        registerCommands(skillManager);

        // Load or create the skills.yml configuration file
        FileConfiguration skillsConfig = configManager.getConfig("skills.yml");

        // Initialize CombatLevelSystem
        combatLevelSystem = new CombatLevelSystem(this, combatLevel, skillManager);

        FishingSkill fishingSkill = new FishingSkill(this, skillManager, configManager);

        // Register Skills with proper configuration sections
        ConfigurationSection strengthConfig = skillsConfig.getConfigurationSection("skills.strength");
        ConfigurationSection defenseConfig = skillsConfig.getConfigurationSection("skills.defense");
        ConfigurationSection woodcuttingConfig = skillsConfig.getConfigurationSection("skills.woodcutting");
        ConfigurationSection prayerConfig = skillsConfig.getConfigurationSection("skills.prayer");
        ConfigurationSection miningConfig = skillsConfig.getConfigurationSection("skills.mining");
        ConfigurationSection smithingConfig = skillsConfig.getConfigurationSection("skills.smithing");

        if (strengthConfig != null) {
            registerListener(new Strength(skillManager, strengthConfig, configManager));
        } else {
            getLogger().warning("No 'strength' section found in skills.yml under 'skills'");
        }

        if (defenseConfig != null) {
            registerListener(new Defense(skillManager, defenseConfig, configManager));
        } else {
            getLogger().warning("No 'defense' section found in skills.yml under 'skills'");
        }

        if (woodcuttingConfig != null) {
            registerListener(new Woodcutting(skillManager, configManager, this));
        } else {
            getLogger().warning("No 'woodcutting' section found in skills.yml under 'skills'");
        }

        if (prayerConfig != null) {
            registerListener(new Prayer(skillManager, prayerConfig, this));
        } else {
            getLogger().warning("No 'prayer' section found in skills.yml under 'skills'");
        }

        if (miningConfig != null) {
            registerListener(new Mining(skillManager, miningConfig, this));
        } else {
            getLogger().warning("No 'mining' section found in skills.yml under 'skills'");
        }

        if (smithingConfig != null) {
            registerListener(new Smithing(skillManager, configManager, this));
        } else {
            getLogger().warning("No 'smithing' section found in skills.yml under 'skills'");
        }

        // Register other listeners
        registerListener(new Water(this));
        registerListener(skillManager); // Assuming SkillManager implements Listener
        registerListener(combatLevelSystem);
        registerListener(new Firemaking(skillManager, this, configManager));
        registerListener(new Hitpoints(skillManager, this));
        registerListener(new Range(this, skillManager));
        registerListener(new Agility(skillManager));
        registerListener(new Cooking(skillManager));
        registerListener(new Crafting(skillManager));
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
        registerListener(new org.hoffmantv.minescape.listeners.AlwaysDay(this));
        getServer().getPluginManager().registerEvents(new Login(this), this);

        // Register ResourcePack listener
        getServer().getPluginManager().registerEvents(new ResourcePack(this), this);

        // Log plugin enabled successfully
        getLogger().info("MineScape has been enabled successfully.");
    }

    @Override
    public void onDisable() {
        getLogger().info("MineScape has been disabled!");
    }

    private void setupConfiguration() {
        getConfig().options().copyDefaults(true);
        saveConfig();

        // Save default skills.yml if it doesn't exist
        configManager.saveDefaultConfig("skills.yml");

        // Save default playerdata.yml if it doesn't exist
        configManager.saveDefaultConfig("playerdata.yml"); // Assuming this method handles any config
    }

    // Getter for CombatLevelSystem
    public CombatLevelSystem getCombatLevelSystem() {
        return combatLevelSystem;
    }

    // Getter for SkillsHologram
    public SkillsHologram getSkillsHologram() {
        return skillsHologram;
    }

    private void registerCommands(SkillManager skillManager) {
        this.getCommand("help").setExecutor(new Help(this));
        getCommand("alwaysday").setExecutor(new AlwaysDay(this));
        getCommand("togglehologram").setExecutor(new ToggleSkillsMenu(skillsHologram));
        getCommand("serverreload").setExecutor(new Reload(this));
        getCommand("accepttrade").setExecutor(new AcceptTrade(tradeMenu));
        this.getCommand("trade").setExecutor(new Trade(tradeMenu));
        this.getCommand("setspawn").setExecutor(new SetSpawn(this));
        this.getCommand("spawn").setExecutor(new Spawn(this));
    }

    private void registerListener(Listener listener) {
        getServer().getPluginManager().registerEvents(listener, this);
    }
}