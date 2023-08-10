package org.hoffmantv.minescape;

import org.bukkit.plugin.java.JavaPlugin;
import org.hoffmantv.minescape.commands.SaveSkillsCommand;
import org.hoffmantv.minescape.managers.SkillManager;
import org.hoffmantv.minescape.skills.WoodcuttingSkill;


public class MineScape extends JavaPlugin {


    // This method is called when the plugin is enabled (on server startup)
    @Override
    public void onEnable() {
        getLogger().info("MineScape has been enabled!");

        this.saveDefaultConfig();

        SkillManager skillManager = new SkillManager(this);
        getServer().getPluginManager().registerEvents(skillManager, this);
        getCommand("saveskills").setExecutor(new SaveSkillsCommand(skillManager));

        WoodcuttingSkill woodcuttingSkill = new WoodcuttingSkill(skillManager, this);
        getServer().getPluginManager().registerEvents(woodcuttingSkill, this);


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
