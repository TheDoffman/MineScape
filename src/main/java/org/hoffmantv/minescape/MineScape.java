package org.hoffmantv.minescape;

import org.bukkit.plugin.java.JavaPlugin;
import org.hoffmantv.minescape.managers.SkillSystem;


public class MineScape extends JavaPlugin {

    private SkillSystem skillSystem;

    // This method is called when the plugin is enabled (on server startup)
    @Override
    public void onEnable() {
        getLogger().info("MineScape has been enabled!");

        skillSystem = new SkillSystem(this);

        getServer().getPluginManager().registerEvents(new WoodcuttingSkill(this), this);

        this.saveDefaultConfig();


    }

    // This method is called when the plugin is disabled (on server shutdown)
    @Override
    public void onDisable() {
        getLogger().info("MineScape has been disabled!");
        // Add any cleanup code here
        // Save data, release resources, etc.

        skillSystem.savePlayerData();

    }

    // Add any additional methods and functionalities for your plugin here
}
