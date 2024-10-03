package org.hoffmantv.minescape;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
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
    private CombatLevel combatLevel;

    @Override
    public void onEnable() {
        // Initialize SkillManager
        skillManager = new SkillManager(this);

        ConfigurationSection woodcuttingConfig = skillManager.getSkillsConfig().getConfigurationSection("woodcutting");
        ConfigurationSection agilityConfig = skillManager.getSkillsConfig().getConfigurationSection("agility");
        ConfigurationSection miningConfig = skillManager.getSkillsConfig().getConfigurationSection("mining");

        // Initialize SkillsHologram
        skillsHologram = new SkillsHologram(skillManager);

        // Initialize CombatLevel
        combatLevel = new CombatLevel(skillManager);

        // Initialize CombatLevelSystem
        combatLevelSystem = new CombatLevelSystem(this, combatLevel, skillManager);

        // Register Commands
        registerCommands();

        // Register all other listeners
        registerAllListeners();

        // Log plugin enabled successfully
        getLogger().info("MineScape has been enabled successfully.");

        getServer().getPluginManager().registerEvents(combatLevelSystem, this);

        // Register the Agility listener
        if (agilityConfig != null) {
            getServer().getPluginManager().registerEvents(new Agility(skillManager, this), this);
        } else {
            getLogger().warning("Agility configuration section is missing in skills.yml!");
        }
        // Register the Woodcutting listener
        Woodcutting woodcutting = new Woodcutting(skillManager, woodcuttingConfig, this);
        if(woodcuttingConfig != null)
        getServer().getPluginManager().registerEvents(woodcutting, this);
        else {
            getLogger().warning("Woodcutting configuration section is missing in skills.yml!");
        }
        // Register the Mining listener
        Mining mining = new Mining(skillManager, miningConfig, this);
        if(miningConfig != null)
            getServer().getPluginManager().registerEvents(mining, this);
        else {
            getLogger().warning("Mining configuration section is missing in skills.yml!");
        }


        getLogger().info("MineScape Alpha Version 0.2 has been enabled!");
    }

    private void registerCombatLevel() {
        // Register any combat-related event listeners (if any)
        // If you have events related to combat that need to trigger CombatLevel updates,
        // register those listeners here.

        // Example: Update all player combat levels when the server starts
        Bukkit.getScheduler().runTaskLater(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                // Update the player's combat level when they join the server
                combatLevel.updateCombatLevel(player);  // Self-update for combat level
            }
        }, 20L);  // Delay of 1 second after startup (20 ticks = 1 second)
    }

    @Override
    public void onDisable() {
        skillManager.savePlayerData(); // Ensure data is saved when the plugin is disabled

        getLogger().info("MineScape has been disabled!");

    }

    private void registerAllListeners() {
        registerListener(new Water(this));
        registerListener(skillManager); // Register SkillManager as a listener
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