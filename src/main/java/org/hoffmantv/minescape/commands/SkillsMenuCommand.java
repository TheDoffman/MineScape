package org.hoffmantv.minescape.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.hoffmantv.minescape.managers.SkillsMenu;
import org.hoffmantv.minescape.managers.SkillManager;

public class SkillsMenuCommand implements CommandExecutor {

    private final SkillsMenu skillsMenu;

    public SkillsMenuCommand(SkillManager skillManager) {
        this.skillsMenu = new SkillsMenu(skillManager);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;
        skillsMenu.openFor(player);
        return true;
    }
}
