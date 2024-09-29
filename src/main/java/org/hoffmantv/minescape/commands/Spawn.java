package org.hoffmantv.minescape.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class Spawn implements CommandExecutor {

    private final JavaPlugin plugin;

    public Spawn(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        FileConfiguration config = plugin.getConfig();

        if (!config.contains("spawn.world")) {
            player.sendMessage(ChatColor.RED + "Spawn point has not been set yet.");
            return true;
        }

        Location spawnLocation = new Location(
                Bukkit.getWorld(config.getString("spawn.world")),
                config.getDouble("spawn.x"),
                config.getDouble("spawn.y"),
                config.getDouble("spawn.z"),
                (float) config.getDouble("spawn.yaw"),
                (float) config.getDouble("spawn.pitch")
        );

        player.teleport(spawnLocation);
        player.sendMessage(ChatColor.GREEN + "Teleported to spawn!");
        return true;
    }
}