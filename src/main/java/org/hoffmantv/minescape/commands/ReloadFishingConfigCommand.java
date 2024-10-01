package org.hoffmantv.minescape.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.hoffmantv.minescape.skills.FishingSkill;
import org.hoffmantv.minescape.skills.SkillManager;

public class ReloadFishingConfigCommand implements CommandExecutor {

    private final SkillManager skillManager;
    private final FishingSkill fishingSkill;

    public ReloadFishingConfigCommand(SkillManager skillManager, FishingSkill fishingSkill) {
        this.skillManager = skillManager;
        this.fishingSkill = fishingSkill;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // Check permissions
        if (!sender.hasPermission("minescape.reloadfishing")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        // Reload the skills configuration by directly accessing the skills config in SkillManager
        skillManager.getSkillsConfig(); // Assuming you have a method to load the config
        fishingSkill.reloadFishingSpots();

        sender.sendMessage(ChatColor.GREEN + "Fishing configuration reloaded.");

        return true;
    }
}