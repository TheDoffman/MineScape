package org.hoffmantv.minescape.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.hoffmantv.minescape.managers.SkillsHologram;

public class ToggleSkillsCommand implements CommandExecutor {
    private final SkillsHologram skillsHologram;

    public ToggleSkillsCommand(SkillsHologram skillsHologram) {
        this.skillsHologram = skillsHologram;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players!");
            return false;
        }

        Player player = (Player) sender;

        // Toggle the hologram display for the player
        skillsHologram.toggleHologram(player);
        return true;
    }
}