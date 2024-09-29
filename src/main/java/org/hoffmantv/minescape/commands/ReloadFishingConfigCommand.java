package org.hoffmantv.minescape.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.hoffmantv.minescape.managers.ConfigurationManager;
import org.hoffmantv.minescape.skills.FishingSkill;

public class ReloadFishingConfigCommand implements CommandExecutor {

    private final ConfigurationManager configManager;
    private final FishingSkill fishingSkill;

    public ReloadFishingConfigCommand(ConfigurationManager configManager, FishingSkill fishingSkill) {
        this.configManager = configManager;
        this.fishingSkill = fishingSkill;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // Check permissions
        if (!sender.hasPermission("minescape.reloadfishing")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        // Reload the skills configuration
        configManager.reloadSkillsConfig();
        fishingSkill.reloadFishingSpots();

        sender.sendMessage(ChatColor.GREEN + "Fishing configuration reloaded.");

        return true;
    }
}