package org.hoffmantv.minescape.skills;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.hoffmantv.minescape.MineScape;
import org.hoffmantv.minescape.managers.SkillManager;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class FishingSkill implements CommandExecutor {

    private JavaPlugin plugin;

    private final SkillManager skillManager;

    // Structure to hold fishing locations and their type of fish
    private Map<Location, FishingSpot> fishingSpots = new HashMap<>();


    // Structure to hold cooldown data for players
    private Map<Player, Long> lastFishingTime = new HashMap<>();

    // A flat chance to catch a fish. Adjust as needed.
    private static final double CATCH_CHANCE = 0.7;

    // The amount of XP granted per catch. Adjust based on your needs.
    private static final int XP_PER_CATCH = 10;
    private static final long COOLDOWN_TIME = 60000L;  // 1 minute in milliseconds
    private FileConfiguration config;
    private final File configFile;


    public FishingSkill(SkillManager skillManager, JavaPlugin plugin) {
        this.plugin = plugin;
        this.skillManager = skillManager;
        this.configFile = new File(plugin.getDataFolder(), "fishingloc.yml");
        loadFishingSpots();
        this.plugin.getCommand("setfishingspot").setExecutor(this);  // Registering the command here
    }

    private class FishingSpot {
        private Location location;
        private FishType fishType;
        private BukkitRunnable effectTask;

        public FishingSpot(Location location, FishType fishType) {
            this.location = location;
            this.fishType = fishType;
        }

        public Location getLocation() {
            return location;
        }

        public FishType getFishType() {
            return fishType;
        }

        public BukkitRunnable getEffectTask() {
            return effectTask;
        }

        public void setEffectTask(BukkitRunnable effectTask) {
            this.effectTask = effectTask;
        }
    }


    public enum FishType {
        SALMON,
        TROUT,
        CATFISH,
        LOBSTER
        // Add more fish types as needed
    }

    public void setFishingSpot(Location location, FishType fishType) {
        FishingSpot spot = new FishingSpot(location, fishType);
        FishingSpot fishingSpot = fishingSpots.get(spot);
        if (fishingSpot != null) {
            if (fishingSpot.effectTask != null) {
                fishingSpot.effectTask.cancel();
            }

        // If there's an existing effect for this location, stop it
        if(spot.getEffectTask() != null) {
            spot.getEffectTask().cancel();
        }

        // Re-enable the splash effect after 5 seconds (100 ticks).
        new BukkitRunnable() {
            @Override
            public void run() {
                fishingSpot.effectTask = new BukkitRunnable() {
                    @Override
                    public void run() {
                        fishingSpot.location.getWorld().spawnParticle(Particle.WATER_BUBBLE, fishingSpot.location.add(0, .2, 0), 10, 0.5, 0.5, 0.5, 0);

                    }
                };
                fishingSpot.effectTask.runTaskTimer(plugin, 0, 20L); // Restart the effect to run every second
            }
        }.runTaskLater(plugin, 100L);  // Delay of 5 seconds
    }
    }



    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Usage: /setfishingspot <FishType>");
            return true;
        }

        try {
            FishType type = FishType.valueOf(args[0].toUpperCase());
            Location loc = player.getLocation();
            setFishingSpot(loc, type);
            saveFishingSpotToConfig(loc, type);
            player.sendMessage(ChatColor.GREEN + "Fishing spot set for " + type.name().toLowerCase() + " at your location.");
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.RED + "Invalid FishType. Available types are: SALMON, TROUT, CATFISH");
        }

        return true;
    }
    private void saveFishingSpotToConfig(Location loc, FishType type) {
        String key = loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
        getConfig().set(key, type.name());
        saveConfig();
    }
    public void fishAtSpot(Player player, Location spot) {
        if (lastFishingTime.containsKey(player)) {
            long lastTime = lastFishingTime.get(player);
            if (System.currentTimeMillis() - lastTime < COOLDOWN_TIME) {
                player.sendMessage(ChatColor.RED + "You need to wait a bit before fishing again.");
                return;
            }
        }

        FishingSpot fishingSpot = fishingSpots.get(spot);
        FishType type = (fishingSpot != null) ? fishingSpot.getFishType() : null;

        if (type != null) {
            if (Math.random() <= CATCH_CHANCE) {
                // Grant XP
                grantXp(player, XP_PER_CATCH);

                // Give non-stackable fish
                ItemStack fish = new ItemStack(Material.COD);  // Placeholder, replace with actual fish type
                ItemMeta meta = fish.getItemMeta();
                meta.setDisplayName(type.name().toLowerCase());
                fish.setItemMeta(meta);
                fish.setAmount(1);
                player.getInventory().addItem(fish);

                // Set the cooldown for this player
                lastFishingTime.put(player, System.currentTimeMillis());

                // Particles or effects can be added here

                player.sendMessage(ChatColor.GREEN + "You caught a " + type.name().toLowerCase() + "!");
            } else {
                player.sendMessage(ChatColor.RED + "You didn't catch anything this time.");
            }
        } else {
            player.sendMessage(ChatColor.RED + "This is not a valid fishing spot.");
        }
    }

    private void grantXp(Player player, int xpAmount) {
        double xpEarned = XP_PER_CATCH;
        skillManager.addXP(player, SkillManager.Skill.FISHING, xpEarned);

        // Fetch current XP from `skills.yml`
        int currentXP = getXPFromSkillsFile(player, "fishing");
        int newXP = currentXP + xpAmount;

        // Save new XP to `skills.yml`
        saveXPToSkillsFile(player, "fishing", newXP);

        player.sendMessage(ChatColor.GREEN + "You gained " + xpAmount + " fishing XP!");
    }

    private int getXPFromSkillsFile(Player player, String skill) {
        skillManager.getSkillLevel(player, SkillManager.Skill.FISHING);
        return 0;
    }

    private void saveXPToSkillsFile(Player player, String skill, int xp) {
        skillManager.saveSkillsToConfig();
    }
    public FileConfiguration getConfig() {
        if (config == null) {
            reloadConfig();
        }
        return config;
    }

    public void reloadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public void saveConfig() {
        if (config != null) {
            try {
                config.save(configFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Could not save config to " + configFile);
            }
        }
    }
    private void loadFishingSpots() {
        for (String key : getConfig().getKeys(false)) {
            String[] parts = key.split(",");
            World world = Bukkit.getWorld(parts[0]);
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);
            Location location = new Location(world, x, y, z);

            FishType fishType = FishType.valueOf(getConfig().getString(key));
            FishingSpot fishingSpot = new FishingSpot(location, fishType);
            fishingSpots.put(location, fishingSpot);



            fishingSpots.put(location, fishingSpot);

        }
    }
}
