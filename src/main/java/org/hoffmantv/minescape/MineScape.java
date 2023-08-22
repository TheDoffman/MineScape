package org.hoffmantv.minescape;

import org.bukkit.plugin.java.JavaPlugin;
import org.hoffmantv.minescape.commands.HelpCommand;
import org.hoffmantv.minescape.commands.SaveSkillsCommand;
import org.hoffmantv.minescape.commands.SkillsMenuCommand;
import org.hoffmantv.minescape.listeners.MobListener;
import org.hoffmantv.minescape.listeners.WaterListener;
import org.hoffmantv.minescape.managers.CombatLevelSystem;
import org.hoffmantv.minescape.managers.SkillManager;
import org.hoffmantv.minescape.skills.*;

import java.util.Objects;


public class MineScape extends JavaPlugin {

    private final SkillManager skillManager;

    public MineScape(SkillManager skillManager) {
        this.skillManager = skillManager;
    }

    @Override
    public void onEnable() {
        getLogger().info("MineScape has been enabled!");

        int pluginId = 19471;

        new Metrics(this, pluginId);

        this.saveDefaultConfig();

        SkillManager skillManager = new SkillManager(this);

        Objects.requireNonNull(this.getCommand("help")).setExecutor(new HelpCommand(this));
        Objects.requireNonNull(this.getCommand("saveskills")).setExecutor(new SaveSkillsCommand(skillManager));
        getCommand("skills").setExecutor(new SkillsMenuCommand(skillManager));

        WaterListener waterListener = new WaterListener(this);
        getServer().getPluginManager().registerEvents(waterListener, this);

        getServer().getPluginManager().registerEvents(skillManager, this);

        CombatLevelSystem combatLevelSystem = new CombatLevelSystem(this);
        getServer().getPluginManager().registerEvents(combatLevelSystem, this);

        WoodcuttingSkill woodcuttingSkill = new WoodcuttingSkill(skillManager, this);
        getServer().getPluginManager().registerEvents(woodcuttingSkill, this);

        MiningSkill miningSkill = new MiningSkill(skillManager, this);
        getServer().getPluginManager().registerEvents(miningSkill, this);

        getServer().getPluginManager().registerEvents(new SmithingSkill(skillManager), this);

        FiremakingSkill firemakingSkill = new FiremakingSkill(skillManager, this);
        getServer().getPluginManager().registerEvents(firemakingSkill, this);

        HitpointsSkill hitpointsSkill = new HitpointsSkill(skillManager, this);
        getServer().getPluginManager().registerEvents(hitpointsSkill, this);

        PrayerSkill prayerSkill = new PrayerSkill(skillManager, this);
        getServer().getPluginManager().registerEvents(prayerSkill, this);

        AttackSkill attackSkill = new AttackSkill(skillManager);
        getServer().getPluginManager().registerEvents (attackSkill, this);

        getServer().getPluginManager().registerEvents(new MobListener(), this);

    }

    @Override
    public void onDisable() {
        getLogger().info("MineScape has been disabled!");
        saveDefaultConfig();
        skillManager.saveSkillsToConfig();
        getLogger().info("Saved all Configs!");

        // Add any cleanup code here
        // Save data, release resources, etc.

    }
}
