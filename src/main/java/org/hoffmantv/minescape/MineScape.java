package org.hoffmantv.minescape;

import org.bukkit.plugin.java.JavaPlugin;
import org.hoffmantv.minescape.commands.HelpCommand;
import org.hoffmantv.minescape.commands.SaveSkillsCommand;
import org.hoffmantv.minescape.managers.SkillManager;
import org.hoffmantv.minescape.skills.*;


public class MineScape extends JavaPlugin {
    private SkillManager skillManager; // Make sure you have this
    private FiremakingSkill firemakingSkill;

    @Override
    public void onEnable() {
        getLogger().info("MineScape has been enabled!");

        int pluginId = 19471;
        Metrics metrics = new Metrics(this, pluginId);

        this.saveDefaultConfig();

        skillManager = new SkillManager(this);

        this.getCommand("help").setExecutor(new HelpCommand(this));
        this.getCommand("saveskills").setExecutor(new SaveSkillsCommand(skillManager));

        getServer().getPluginManager().registerEvents(skillManager, this);

        WoodcuttingSkill woodcuttingSkill = new WoodcuttingSkill(skillManager, this);
        getServer().getPluginManager().registerEvents(woodcuttingSkill, this);

        MiningSkill miningSkill = new MiningSkill(skillManager, this);
        getServer().getPluginManager().registerEvents(miningSkill, this);

        SmithingSkill smithingSkill = new SmithingSkill(skillManager);
        getServer().getPluginManager().registerEvents(new SmithingSkill(skillManager), this);

        firemakingSkill = new FiremakingSkill(skillManager, this);
        getServer().getPluginManager().registerEvents(firemakingSkill, this);

        getServer().getPluginManager().registerEvents(new HitpointsSkill(skillManager, this), this);

        getServer().getPluginManager().registerEvents(new PrayerSkill(skillManager, this), this);
    }

    // This method is called when the plugin is disabled (on server shutdown)
    @Override
    public void onDisable() {
        getLogger().info("MineScape has been disabled!");
        skillManager.saveSkillsToConfig();

    }

}
