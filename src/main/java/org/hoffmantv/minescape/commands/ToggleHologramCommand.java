package org.hoffmantv.minescape.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.hoffmantv.minescape.managers.SkillsHologram;

public class ToggleHologramCommand implements CommandExecutor {

    private final SkillsHologram skillsHologram;

    public ToggleHologramCommand(SkillsHologram skillsHologram) {
        this.skillsHologram = skillsHologram;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        skillsHologram.toggleHologram(player);
        return true;
    }
}