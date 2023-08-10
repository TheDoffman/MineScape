package org.hoffmantv.minescape.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.hoffmantv.minescape.managers.SkillManager;

public class SaveSkillsCommand implements CommandExecutor {

    private final SkillManager skillManager;

    public SaveSkillsCommand(SkillManager skillManager) {
        this.skillManager = skillManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender.hasPermission("minescape.saveskills")) {
            skillManager.saveSkillsToConfig();
            sender.sendMessage("§aSkills have been saved to the config!");
            return true;
        } else {
            sender.sendMessage("§cYou do not have permission to execute this command!");
            return true;
        }
    }
}
