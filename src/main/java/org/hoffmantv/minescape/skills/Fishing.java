package org.hoffmantv.minescape.skills;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.hoffmantv.minescape.managers.ConfigurationManager;
import org.hoffmantv.minescape.skills.SkillManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FishingSkill implements Listener {

    private final JavaPlugin plugin;
    private final SkillManager skillManager;
    private final ConfigurationManager configManager;

    // Mapping fishing spots and their requirements
    private final Map<Material, FishSpot> fishingSpots = new HashMap<>();

    // Mapping player fishing states (for preventing spam-clicking)
    private final Map<UUID, Boolean> playerFishingState = new HashMap<>();

    public FishingSkill(JavaPlugin plugin, SkillManager skillManager, ConfigurationManager configManager) {
        this.plugin = plugin;
        this.skillManager = skillManager;
        this.configManager = configManager;

        initializeFishingSpots();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private void initializeFishingSpots() {
        // Add fishing spots with requirements and rewards
        fishingSpots.put(Material.WATER, new FishSpot("Shrimp", 1, 10, Material.FISHING_ROD));
        fishingSpots.put(Material.STONE, new FishSpot("Lobster", 40, 90, Material.FISHING_ROD));
        // Add more spots as needed following the OSRS mechanics
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        // Check if player is fishing
        if (playerFishingState.getOrDefault(playerUUID, false)) {
            player.sendMessage(ChatColor.RED + "You are already fishing!");
            return;
        }

        // Check if player is clicking on a valid fishing spot
        Material clickedBlockType = event.getClickedBlock() != null ? event.getClickedBlock().getType() : null;
        FishSpot fishSpot = fishingSpots.get(clickedBlockType);
        if (fishSpot == null) return; // Not a fishing spot

        // Check if player meets the requirements
        if (!hasRequiredLevel(player, fishSpot)) {
            player.sendMessage(ChatColor.RED + "You need a fishing level of " + fishSpot.getRequiredLevel() + " to fish here.");
            return;
        }

        // Check if player has the required tool
        if (!player.getInventory().contains(fishSpot.getRequiredTool())) {
            player.sendMessage(ChatColor.RED + "You need a " + fishSpot.getRequiredTool().name() + " to fish here.");
            return;
        }

        // Start fishing process
        startFishing(player, fishSpot);
    }

    private boolean hasRequiredLevel(Player player, FishSpot fishSpot) {
        int playerFishingLevel = skillManager.getSkillLevel(player, SkillManager.Skill.FISHING);
        return playerFishingLevel >= fishSpot.getRequiredLevel();
    }

    private void startFishing(Player player, FishSpot fishSpot) {
        UUID playerUUID = player.getUniqueId();
        playerFishingState.put(playerUUID, true);

        // Simulate fishing with a delay
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    playerFishingState.remove(playerUUID);
                    cancel();
                    return;
                }

                player.sendMessage(ChatColor.GREEN + "You catch a " + fishSpot.getFishName() + "!");
                player.getInventory().addItem(new ItemStack(Material.COD, 1)); // Use appropriate fish item
                skillManager.addXP(player, SkillManager.Skill.FISHING, fishSpot.getXpReward());
                player.playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_SPLASH, 1.0f, 1.0f);

                playerFishingState.remove(playerUUID);
            }
        }.runTaskLater(plugin, 100L); // 5 seconds delay (20 ticks per second)
    }

    /**
     * Inner class representing a fishing spot.
     */
    private static class FishSpot {
        private final String fishName;
        private final int requiredLevel;
        private final double xpReward;
        private final Material requiredTool;

        public FishSpot(String fishName, int requiredLevel, double xpReward, Material requiredTool) {
            this.fishName = fishName;
            this.requiredLevel = requiredLevel;
            this.xpReward = xpReward;
            this.requiredTool = requiredTool;
        }

        public String getFishName() {
            return fishName;
        }

        public int getRequiredLevel() {
            return requiredLevel;
        }

        public double getXpReward() {
            return xpReward;
        }

        public Material getRequiredTool() {
            return requiredTool;
        }
    }
}