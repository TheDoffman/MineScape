package org.hoffmantv.minescape;

import org.bukkit.command.CommandExecutor;
import org.bukkit.plugin.java.JavaPlugin;
import org.hoffmantv.minescape.commands.HelpCommand;
import org.hoffmantv.minescape.commands.SaveSkillsCommand;
import org.hoffmantv.minescape.managers.SkillManager;
import org.hoffmantv.minescape.skills.FishingSkill;
import org.hoffmantv.minescape.skills.MiningSkill;
import org.hoffmantv.minescape.skills.SmithingSkill;
import org.hoffmantv.minescape.skills.WoodcuttingSkill;



public class MineScape extends JavaPlugin {
    // This method is called when the plugin is enabled (on server startup)
    @Override
    public void onEnable() {
        getLogger().info("MineScape" + getServer().getVersion() +  "has been enabled!");

        int pluginId = 19471; // <-- Replace with the id of your plugin!
        Metrics metrics = new Metrics(this, pluginId);

        this.saveDefaultConfig();

        SkillManager skillManager = new SkillManager(this);

        getCommand("help").setExecutor(new HelpCommand(this));

        getServer().getPluginManager().registerEvents(skillManager, this);

        WoodcuttingSkill woodcuttingSkill = new WoodcuttingSkill(skillManager, this);
        getServer().getPluginManager().registerEvents(woodcuttingSkill, this);

        MiningSkill miningSkill = new MiningSkill(skillManager, this);
        getServer().getPluginManager().registerEvents(miningSkill, this);

        SmithingSkill smithingSkill = new SmithingSkill(skillManager);
        getServer().getPluginManager().registerEvents(new SmithingSkill(skillManager), this);

        getServer().getPluginManager().registerEvents(new FishingSkill(this), this);
        FishingSkill fishingSkill = new FishingSkill(this);
        getCommand("setfishingspot").setExecutor((CommandExecutor) new FishingSkill(fishingSkill));


    }

    // This method is called when the plugin is disabled (on server shutdown)
    @Override
    public void onDisable() {
        getLogger().info("MineScape has been disabled!");
        // Add any cleanup code here
        // Save data, release resources, etc.

    }
    // Add any additional methods and functionalities for your plugin here
}
