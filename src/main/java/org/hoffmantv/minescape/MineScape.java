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
import org.hoffmantv.minescape.npc.CreateNPCCommand;
import org.hoffmantv.minescape.skills.*;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class MineScape extends JavaPlugin {

    private File npcFile;
    private FileConfiguration npcConfig;

    @Override
    public void onEnable() {
        getLogger().info("MineScape has been enabled!");

        // Metrics
        int pluginId = 19471;
        new Metrics(this, pluginId);
        // Configuration setup
        setupConfiguration();
        loadNPCFile();
        loadNPCs();

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
        this.getCommand("createnpc").setExecutor(new CreateNPCCommand());
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
        registerListener(new AttackSkill(skillManager, combatLevel));
        registerListener(new StrengthSkill(skillManager, new AttackSkill(skillManager, combatLevel)));
        registerListener(new DefenseSkill(skillManager, combatLevel, new AttackSkill(skillManager, combatLevel)));
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
    }

    private void registerListener(Listener listener) {
        getServer().getPluginManager().registerEvents(listener, this);

    }
    private void loadNPCFile() {
        npcFile = new File(getDataFolder(), "npcs.yml");
        if (!npcFile.exists()) {
            npcFile.getParentFile().mkdirs();
            saveResource("npcs.yml", false);
        }
        npcConfig = new YamlConfiguration();
        try {
            npcConfig.load(npcFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    private void loadNPCs() {
        // Implement NPC loading code here
    }

    public FileConfiguration getNpcConfig() {
        return npcConfig;
    }

    public File getNpcFile() {
        return npcFile;
    }
}
