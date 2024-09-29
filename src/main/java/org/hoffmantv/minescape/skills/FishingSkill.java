package org.hoffmantv.minescape.skills;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.hoffmantv.minescape.managers.ConfigurationManager;

import java.util.*;

public class FishingSkill implements Listener {

    private final JavaPlugin plugin;
    private final SkillManager skillManager;
    private final ConfigurationManager configManager;
    private ConfigurationSection fishingConfig;

    // List of fishing spots
    private final List<FishingSpot> fishingSpots = new ArrayList<>();

    // Set to track players currently fishing
    private final Set<UUID> playersFishing = new HashSet<>();

    // BukkitTask for particle effects
    private BukkitTask particleTask = null;

    public FishingSkill(JavaPlugin plugin, SkillManager skillManager, ConfigurationManager configManager) {
        this.plugin = plugin;
        this.skillManager = skillManager;
        this.configManager = configManager;

        // Load fishing configurations from skills.yml
        this.fishingConfig = configManager.getSkillsConfig().getConfigurationSection("skills.fishing");
        if (fishingConfig == null) {
            plugin.getLogger().warning("No 'fishing' section found in skills.yml under 'skills'");
        } else {
            loadFishingSpots();
        }

        // Register events
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        // Start particle effects
        startParticleEffects();
    }

    private void loadFishingSpots() {
        fishingSpots.clear(); // Clear existing spots before loading
        ConfigurationSection spotsSection = fishingConfig.getConfigurationSection("fishingSpots");
        if (spotsSection != null) {
            for (String key : spotsSection.getKeys(false)) {
                ConfigurationSection spotSection = spotsSection.getConfigurationSection(key);
                if (spotSection == null) {
                    plugin.getLogger().warning("Invalid fishing spot configuration: " + key);
                    continue;
                }

                String worldName = spotSection.getString("world");
                double x = spotSection.getDouble("x");
                double y = spotSection.getDouble("y");
                double z = spotSection.getDouble("z");
                int requiredLevel = spotSection.getInt("requiredLevel", 1);
                List<String> fishTypesList = spotSection.getStringList("fishTypes");

                World world = plugin.getServer().getWorld(worldName);
                if (world == null) {
                    plugin.getLogger().warning("Invalid world in fishing spot: " + worldName);
                    continue;
                }

                List<FishType> spotFishTypes = new ArrayList<>();
                ConfigurationSection fishTypesSection = fishingConfig.getConfigurationSection("fishTypes");
                if (fishTypesSection == null) {
                    plugin.getLogger().warning("No 'fishTypes' section found in fishing configuration.");
                    continue;
                }

                for (String fishName : fishTypesList) {
                    ConfigurationSection fishSection = fishTypesSection.getConfigurationSection(fishName);
                    if (fishSection == null) {
                        plugin.getLogger().warning("Fish type '" + fishName + "' not defined in fishTypes.");
                        continue;
                    }

                    double xp = fishSection.getDouble("xp", 10.0);
                    String baitString = fishSection.getString("bait", null);
                    Material bait = baitString != null ? Material.matchMaterial(baitString) : null;

                    Material fishMaterial = Material.matchMaterial(fishName.toUpperCase());
                    if (fishMaterial == null) {
                        plugin.getLogger().warning("Invalid fish material: " + fishName);
                        continue;
                    }

                    spotFishTypes.add(new FishType(fishMaterial, xp, bait));
                }

                Location location = new Location(world, x, y, z);
                FishingSpot fishingSpot = new FishingSpot(location, requiredLevel, spotFishTypes);
                fishingSpots.add(fishingSpot);
            }
        } else {
            plugin.getLogger().warning("No 'fishingSpots' section found in fishing configuration.");
        }
    }

    // Method to reload fishing spots
    public void reloadFishingSpots() {
        this.fishingConfig = configManager.getSkillsConfig().getConfigurationSection("skills.fishing");
        if (this.fishingConfig == null) {
            plugin.getLogger().warning("No 'fishing' section found in skills.yml under 'skills'");
            return;
        }
        loadFishingSpots();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // Check if the player is holding the correct equipment
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        Material equipment = heldItem != null ? heldItem.getType() : Material.AIR;

        if (equipment != Material.FISHING_ROD) {
            return; // Not holding a fishing rod
        }

        Location playerLocation = player.getLocation();

        // Check if the player is near a fishing spot
        for (FishingSpot spot : fishingSpots) {
            if (playerLocation.getWorld().equals(spot.getLocation().getWorld()) &&
                    playerLocation.distanceSquared(spot.getLocation()) <= 4) { // Within 2 blocks
                initiateFishing(player, spot);
                event.setCancelled(true); // Prevent default interaction
                break;
            }
        }
    }

    private void initiateFishing(Player player, FishingSpot spot) {
        UUID playerUUID = player.getUniqueId();

        if (playersFishing.contains(playerUUID)) {
            player.sendMessage(ChatColor.RED + "You are already fishing!");
            return;
        }

        // Check if player's fishing level meets the spot's required level
        int playerFishingLevel = skillManager.getSkillLevel(player, SkillManager.Skill.FISHING);
        if (playerFishingLevel < spot.getRequiredLevel()) {
            player.sendMessage(ChatColor.RED + "You need Fishing level " + spot.getRequiredLevel() + " to fish here.");
            return;
        }

        // Determine available fish based on the spot's specific fish types
        List<FishType> availableFish = spot.getFishTypes();

        if (availableFish.isEmpty()) {
            player.sendMessage(ChatColor.RED + "No fish types are defined for this fishing spot.");
            return;
        }

        playersFishing.add(playerUUID);

        // Send action bar message
        player.sendActionBar(ChatColor.YELLOW + "You begin fishing...");

        int actionDelay = fishingConfig.getInt("actionDelay", 5); // Default to 5 seconds

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    playersFishing.remove(playerUUID);
                    this.cancel();
                    return;
                }

                // Check if player is still near the fishing spot
                if (player.getLocation().distanceSquared(spot.getLocation()) > 4) {
                    playersFishing.remove(playerUUID);
                    player.sendMessage(ChatColor.RED + "You move away from the fishing spot.");
                    this.cancel();
                    return;
                }

                // Randomly select a fish from the available fish
                FishType caughtFish = getRandomFish(availableFish);

                // Remove bait if required
                if (caughtFish.getBait() != null) {
                    if (!player.getInventory().containsAtLeast(new ItemStack(caughtFish.getBait()), 1)) {
                        player.sendMessage(ChatColor.RED + "You don't have any " + formatMaterialName(caughtFish.getBait()) + " left.");
                        playersFishing.remove(playerUUID);
                        this.cancel();
                        return;
                    }
                    player.getInventory().removeItem(new ItemStack(caughtFish.getBait(), 1));
                }

                // Give the player the fish
                player.getInventory().addItem(new ItemStack(caughtFish.getMaterial(), 1));

                // Add XP
                boolean leveledUp = skillManager.addXP(player, SkillManager.Skill.FISHING, caughtFish.getXp());

                player.sendMessage(ChatColor.GREEN + "You catch a " + formatMaterialName(caughtFish.getMaterial()) + ".");

                if (leveledUp) {
                    int newLevel = skillManager.getSkillLevel(player, SkillManager.Skill.FISHING);

                    // Send level-up message
                    String message = ChatColor.GOLD + "Congratulations! You've reached level " + newLevel + " in Fishing!";
                    player.sendMessage(message);

                    // Play level-up sound
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);

                    // Optionally launch a firework
                    skillManager.launchFirework(player.getLocation());
                }

                // Continue fishing is handled by the repeating task
            }
        }.runTaskTimer(plugin, actionDelay * 20L, actionDelay * 20L); // Convert seconds to ticks
    }

    private List<FishType> getAvailableFish(int fishingLevel, Material equipment, Player player) {
        // This method is no longer necessary since fish types are specific to fishing spots
        return Collections.emptyList();
    }

    private FishType getRandomFish(List<FishType> availableFish) {
        Random random = new Random();
        return availableFish.get(random.nextInt(availableFish.size()));
    }

    private String formatMaterialName(Material material) {
        return material.name().toLowerCase().replace('_', ' ');
    }

    // Inner class to represent a fishing spot
    private static class FishingSpot {
        private final Location location;
        private final int requiredLevel;
        private final List<FishType> fishTypes;

        public FishingSpot(Location location, int requiredLevel, List<FishType> fishTypes) {
            this.location = location;
            this.requiredLevel = requiredLevel;
            this.fishTypes = fishTypes;
        }

        public Location getLocation() {
            return location;
        }

        public int getRequiredLevel() {
            return requiredLevel;
        }

        public List<FishType> getFishTypes() {
            return fishTypes;
        }
    }

    // Inner class to represent a fish type
    public static class FishType {
        private final Material material;
        private final double xp;
        private final Material bait;

        public FishType(Material material, double xp, Material bait) {
            this.material = material;
            this.xp = xp;
            this.bait = bait;
        }

        public Material getMaterial() {
            return material;
        }

        public double getXp() {
            return xp;
        }

        public Material getBait() {
            return bait;
        }
    }

    // Method to start particle effects around all fishing spots
    private void startParticleEffects() {
        // Ensure only one task is scheduled
        if (particleTask != null) {
            return;
        }

        particleTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (FishingSpot spot : fishingSpots) {
                    Location loc = spot.getLocation();
                    World world = loc.getWorld();

                    if (world == null) {
                        continue;
                    }

                    // Check if any player is near the fishing spot (within 10 blocks)
                    boolean isPlayerNearby = false;
                    for (Player player : world.getPlayers()) {
                        if (player.getLocation().distanceSquared(loc) <= 100) { // 10 blocks radius
                            isPlayerNearby = true;
                            break;
                        }
                    }

                    if (!isPlayerNearby) {
                        continue; // Skip particle effects for this spot if no players are nearby
                    }

                    // Retrieve effect configurations
                    ConfigurationSection spotSection = fishingConfig.getConfigurationSection("fishingSpots").getConfigurationSection(getSpotKey(spot));
                    if (spotSection == null) {
                        continue;
                    }

                    ConfigurationSection effectsSection = spotSection.getConfigurationSection("effects");
                    if (effectsSection == null) {
                        // Use default effect if none specified
                        world.spawnParticle(Particle.WATER_SPLASH, loc.clone().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.02);
                        continue;
                    }

                    String particleTypeName = effectsSection.getString("particleType", "WATER_SPLASH");
                    Particle particleType;
                    try {
                        particleType = Particle.valueOf(particleTypeName.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid particle type '" + particleTypeName + "' for fishing spot. Using WATER_SPLASH.");
                        particleType = Particle.WATER_SPLASH;
                    }

                    int particleCount = effectsSection.getInt("particleCount", 10);
                    double offsetX = effectsSection.getDouble("particleOffsetX", 0.5);
                    double offsetY = effectsSection.getDouble("particleOffsetY", 0.5);
                    double offsetZ = effectsSection.getDouble("particleOffsetZ", 0.5);
                    double particleSpeed = effectsSection.getDouble("particleSpeed", 0.02);

                    // Spawn the configured particle
                    world.spawnParticle(particleType, loc.clone().add(0, 1, 0), particleCount, offsetX, offsetY, offsetZ, particleSpeed);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // Runs every second (20 ticks)
    }

    // Helper method to get the spot key based on location
    private String getSpotKey(FishingSpot spot) {
        ConfigurationSection spotsSection = fishingConfig.getConfigurationSection("fishingSpots");
        for (String key : spotsSection.getKeys(false)) {
            ConfigurationSection spotSection = spotsSection.getConfigurationSection(key);
            if (spotSection != null) {
                String world = spotSection.getString("world");
                double x = spotSection.getDouble("x");
                double y = spotSection.getDouble("y");
                double z = spotSection.getDouble("z");
                Location loc = new Location(plugin.getServer().getWorld(world), x, y, z);
                if (loc.equals(spot.getLocation())) {
                    return key;
                }
            }
        }
        return null;
    }

    // Method to stop particle effects when plugin is disabled
    public void stopParticleEffects() {
        if (particleTask != null) {
            particleTask.cancel();
            particleTask = null;
        }
    }
}