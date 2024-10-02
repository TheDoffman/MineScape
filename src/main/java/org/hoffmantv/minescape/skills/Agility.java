package org.hoffmantv.minescape.skills;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Agility implements Listener {

    private final SkillManager skillManager;
    private final Map<UUID, Location> playerStartingPositions = new HashMap<>();

    // Define custom obstacle points (agility obstacles in a course)
    private final Map<Location, Integer> agilityObstacles = new HashMap<>(); // Obstacle location and corresponding XP reward

    public Agility(SkillManager skillManager,  JavaPlugin plugin) {
        this.skillManager = skillManager;
        setupAgilityObstacles();  // Define the obstacle locations and XP rewards
    }

    // Setup OSRS-like agility obstacles with specific XP rewards
    private void setupAgilityObstacles() {
        agilityObstacles.put(new Location(Bukkit.getWorld("world"), 100, 65, 200), 15); // Example obstacle 1
        agilityObstacles.put(new Location(Bukkit.getWorld("world"), 105, 65, 205), 25); // Example obstacle 2
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Check if the player is sprinting and track their movement for XP rewards
        if (player.isSprinting()) {
            if (!playerStartingPositions.containsKey(playerId)) {
                playerStartingPositions.put(playerId, player.getLocation());
            } else {
                Location startingPosition = playerStartingPositions.get(playerId);
                double distanceCovered = startingPosition.distance(player.getLocation());

                if (distanceCovered >= 50) {
                    int xpAmount = calculateDistanceXpReward();
                    skillManager.addXP(player, SkillManager.Skill.AGILITY, xpAmount);

                    player.sendMessage("You gained " + xpAmount + " agility XP for running!");
                    skillManager.savePlayerDataAsync();  // Save player data asynchronously

                    // Reset the starting position to track another 50 blocks
                    playerStartingPositions.put(playerId, player.getLocation());
                }
            }
        } else {
            playerStartingPositions.remove(playerId);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Location location = player.getLocation();

        // Check if the player is interacting with an agility obstacle
        if (agilityObstacles.containsKey(location)) {
            int xpReward = agilityObstacles.get(location);

            // Simulate player performing agility action (e.g., jumping, climbing)
            performAgilityAction(player);

            // Award XP for completing the obstacle
            skillManager.addXP(player, SkillManager.Skill.AGILITY, xpReward);
            player.sendMessage("You gained " + xpReward + " agility XP for completing the obstacle!");
            skillManager.savePlayerDataAsync();  // Save player data asynchronously
        }
    }

    private void performAgilityAction(Player player) {
        // Give the player a small velocity boost to simulate a jump/climb
        Vector jumpBoost = new Vector(0, 1, 0.5);
        player.setVelocity(jumpBoost);

        // Play a sound to simulate climbing or jumping action
        player.playSound(player.getLocation(), "minecraft:block.wooden_trapdoor.open", 1.0F, 1.0F);
    }

    private int calculateDistanceXpReward() {
        return 10;  // Example: reward 10 XP for every 50 blocks sprinted
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();

        if (!player.isSprinting()) return; // Only adjust if the player is sprinting

        int agilityLevel = skillManager.getSkillLevel(player, SkillManager.Skill.AGILITY);
        int currentFoodLevel = player.getFoodLevel();
        int newFoodLevel = event.getFoodLevel();

        if (newFoodLevel < currentFoodLevel) {
            double adjustment = 1 + (0.02 * agilityLevel); // 2% less hunger depletion per agility level
            int adjustedFoodLevel = (int) (currentFoodLevel - (currentFoodLevel - newFoodLevel) / adjustment);

            event.setFoodLevel(adjustedFoodLevel);
        }
    }

    public void regenerateStamina(Player player) {
        int agilityLevel = skillManager.getSkillLevel(player, SkillManager.Skill.AGILITY);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isSprinting()) {
                    int currentFoodLevel = player.getFoodLevel();
                    int newFoodLevel = Math.min(20, currentFoodLevel + (1 + agilityLevel / 20)); // Regenerate faster at higher agility levels

                    player.setFoodLevel(newFoodLevel);
                }
            }
        }.runTaskTimer(skillManager.getPlugin(), 0L, 100L); // Run every 5 seconds (100 ticks)
    }
}