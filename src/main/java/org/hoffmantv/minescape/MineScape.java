package org.hoffmantv.minescape;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.hoffmantv.minescape.commands.*;
import org.hoffmantv.minescape.listeners.*;
import org.hoffmantv.minescape.managers.CombatLevelSystem;
import org.hoffmantv.minescape.mobs.*;
import org.hoffmantv.minescape.skills.*;
import org.hoffmantv.minescape.trade.AcceptTrade;
import org.hoffmantv.minescape.trade.Trade;
import org.hoffmantv.minescape.trade.TradeMenu;

public class MineScape extends JavaPlugin {
    private SkillManager skillManager;
    private SkillsHologram skillsHologram;
    private CombatLevelSystem combatLevelSystem;
    private FishingSkill fishingSkill;
    private TradeMenu tradeMenu;

    @Override
    public void onEnable() {
        getLogger().info("MineScape Alpha Version 0.2 has been enabled!");

        // Initialize SkillManager
        skillManager = new SkillManager(this);

        // Initialize CombatLevelSystem
        combatLevelSystem = new CombatLevelSystem(this, new CombatLevel(skillManager), skillManager);

        // Initialize SkillsHologram
        skillsHologram = new SkillsHologram(skillManager);

        // Initialize FishingSkill
        fishingSkill = new FishingSkill(this, skillManager);

        // Register Commands
        registerCommands();

        // Load or create the skills.yml configuration file
        // Access skillsConfig directly from skillManager
        ConfigurationSection skillsConfig = skillManager.getSkillsConfig();

        // Register Skills with their corresponding configuration sections
        registerSkills(skillsConfig);

        // Register all other listeners
        registerAllListeners();

        // Log plugin enabled successfully
        getLogger().info("MineScape has been enabled successfully.");
    }

    @Override
    public void onDisable() {
        skillManager.savePlayerData(); // Ensure data is saved when the plugin is disabled
        getLogger().info("MineScape has been disabled!");
    }

    private void registerSkills(ConfigurationSection skillsConfig) {
        // Register each skill by checking if the configuration section exists
        for (SkillManager.Skill skill : SkillManager.Skill.values()) {
            ConfigurationSection skillConfig = skillsConfig.getConfigurationSection("skills." + skill.name().toLowerCase());
            if (skillConfig != null) {
                switch (skill) {
                    case PRAYER:
                        registerListener(new Prayer(skillManager, skillConfig, this)); // Pass config and plugin
                        break;
                    case AGILITY:
                        registerListener(new Agility(skillManager,this)); // Pass config
                        break;
                    case MINING:
                        registerListener(new Mining(skillManager, skillConfig, this)); // Pass config
                        break;
                    case COOKING:
                        registerListener(new Cooking(skillManager, skillConfig)); // Pass config
                        break;
                    case FIREMAKING:
                        registerListener(new Firemaking(skillManager, this)); // Pass config
                        break;
                    // Add other skills as needed
                    default:
                        getLogger().warning("No configuration found for skill: " + skill);
                }
            } else {
                getLogger().warning("No '" + skill.name().toLowerCase() + "' section found in skills.yml under 'skills'");
            }
        }
    }

    private void registerAllListeners() {
        registerListener(new Water(this));
        registerListener(skillManager); // Register SkillManager as a listener
        registerListener(combatLevelSystem);
        registerListener(new Hitpoints(skillManager, this)); // Pass plugin
        registerListener(new Range(this, skillManager)); // Pass plugin
        registerListener(new Crafting(skillManager)); // No config needed
        getServer().getPluginManager().registerEvents(new Cooking(skillManager, skillManager.getSkillsConfig()), this);
        registerListener(new MobListener());
        registerListener(new ChickenListener(this)); // Pass plugin
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
        getServer().getPluginManager().registerEvents(new Login(skillsHologram, this, skillManager), this);

        // Register ResourcePack listener
        getServer().getPluginManager().registerEvents(new ResourcePack(this), this);
    }

    private void registerCommands() {
        this.getCommand("help").setExecutor(new Help(this));
        getCommand("togglehologram").setExecutor(new ToggleSkillsMenu(skillsHologram));
        getCommand("serverreload").setExecutor(new Reload(this));
        getCommand("accepttrade").setExecutor(new AcceptTrade(tradeMenu));
        this.getCommand("trade").setExecutor(new Trade(tradeMenu));
        getCommand("setspawn").setExecutor(new SetSpawn(this));
        getCommand("spawn").setExecutor(new Spawn(this));

        // Adjusting the command registration for ReloadFishingConfigCommand
        getCommand("reloadfishing").setExecutor(new ReloadFishingConfigCommand(skillManager, fishingSkill)); // Pass skillManager and fishingSkill
        getCommand("addfishingspot").setExecutor(new AddFishingSpotCommand(skillManager, fishingSkill)); // Pass skillManager and fishingSkill
    }

    private void registerListener(Listener listener) {
        getServer().getPluginManager().registerEvents(listener, this);
    }
}