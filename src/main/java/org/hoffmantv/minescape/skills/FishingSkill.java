package org.hoffmantv.minescape.skills;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class FishingSkill implements Listener {
    private JavaPlugin plugin;
    private Map<Location, FishType> fishingSpots;
    private FileConfiguration config;

    public FishingSkill(JavaPlugin plugin) {
        this.plugin = plugin;
        this.fishingSpots = new HashMap<>();
        this.config = plugin.getConfig();

        loadFishingSpots();
    }

    private FishingSkill fishingSkill;

    public FishingSkill(FishingSkill fishingSkill) {
        this.fishingSkill = fishingSkill;
    }

    public enum FishType {
        SALMON, TROUT, CATFISH // Add more fish types as needed
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Location location = player.getLocation();

        FishType fish = getNearestFishingSpot(location);
        if (fish != null) {
            player.sendMessage("You've caught a " + fish.name().toLowerCase() + "!");
            player.getWorld().spawnParticle(Particle.WATER_SPLASH, location, 10);
            // Handle XP logic, adding fish to inventory, etc.
        }
    }

    public FishType getNearestFishingSpot(Location location) {
        for (Location spot : fishingSpots.keySet()) {
            if (spot.distanceSquared(location) <= 9) { // 3 blocks radius
                return fishingSpots.get(spot);
            }
        }
        return null;
    }

    public void addFishingSpot(Location location, FishType type) {
        fishingSpots.put(location, type);
        config.set("fishing_spots." + locationToString(location), type.name());
        plugin.saveConfig();
    }

    private void loadFishingSpots() {
        if (config.getConfigurationSection("fishing_spots") != null) {
            for (String locStr : config.getConfigurationSection("fishing_spots").getKeys(false)) {
                Location location = stringToLocation(locStr);
                FishType type = FishType.valueOf(config.getString("fishing_spots." + locStr));
                fishingSpots.put(location, type);
            }
        }
    }

    private String locationToString(Location location) {
        return location.getWorld().getName() + "," + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }

    private Location stringToLocation(String str) {
        String[] parts = str.split(",");
        return new Location(Bukkit.getWorld(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
    }
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        // Check for permission (optional)
        if (!player.hasPermission("minescape.setfishingspot")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to set a fishing spot.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Usage: /setfishingspot <FishType>");
            return true;
        }

        try {
            FishType type = FishType.valueOf(args[0].toUpperCase());
            Location loc = player.getLocation();
            fishingSkill.addFishingSpot(loc, type);
            player.sendMessage(ChatColor.GREEN + "Fishing spot set for " + type.name().toLowerCase() + " at your location.");
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.RED + "Invalid FishType. Available types are: SALMON, TROUT, CATFISH");
        }

        return true;
    }

}
