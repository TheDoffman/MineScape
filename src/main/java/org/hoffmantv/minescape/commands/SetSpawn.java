package org.hoffmantv.minescape.commands;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class SetSpawn implements CommandExecutor {

    private final JavaPlugin plugin;

    public SetSpawn(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can set the spawn point.");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("minescape.setspawn")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to set the spawn point.");
            return true;
        }

        Location location = player.getLocation();
        FileConfiguration config = plugin.getConfig();

        config.set("spawn.world", location.getWorld().getName());
        config.set("spawn.x", location.getX());
        config.set("spawn.y", location.getY());
        config.set("spawn.z", location.getZ());
        config.set("spawn.yaw", location.getYaw());
        config.set("spawn.pitch", location.getPitch());
        plugin.saveConfig();

        player.sendMessage(ChatColor.GREEN + "Spawn point set to your current location!");
        return true;
    }
}