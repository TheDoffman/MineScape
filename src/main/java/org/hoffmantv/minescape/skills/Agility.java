package org.hoffmantv.minescape.skills;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.UUID;

public class Agility implements Listener {

    private final SkillManager skillManager;
    private final HashMap<UUID, Location> playerStartingPositions = new HashMap<>();


    public Agility(SkillManager skillManager) {
        this.skillManager = skillManager;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (player.isSprinting()) {
            // If player is sprinting but not yet tracked, start tracking
            if (!playerStartingPositions.containsKey(playerId)) {
                playerStartingPositions.put(playerId, player.getLocation());
            } else {
                Location startingPosition = playerStartingPositions.get(playerId);
                double distanceCovered = startingPosition.distance(player.getLocation());

                if (distanceCovered >= 50) {
                    // Reward XP here
                    int xpAmount = calculateXpReward();  // Implement this method based on how you want to reward XP
                    skillManager.addXP(player, SkillManager.Skill.AGILITY, xpAmount);

                    // Reset the starting position so the player can earn XP again after another 30 blocks
                    playerStartingPositions.put(playerId, player.getLocation());
                }
            }
        } else {
            // If player stops sprinting, remove them from the tracking map
            playerStartingPositions.remove(playerId);
        }
    }

    private int calculateXpReward() {
        // Define your XP reward calculation here
        return 1;  // Example: reward 10 XP for every 30 blocks sprinted
    }
    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();

        if (!player.isSprinting()) return; // Only adjust if the player is sprinting

        int agilityLevel = skillManager.getSkillLevel(player, SkillManager.Skill.AGILITY);
        int currentFoodLevel = player.getFoodLevel();
        int newFoodLevel = event.getFoodLevel();

        // Check if the food level is decreasing (due to sprinting, for instance)
        if (newFoodLevel < currentFoodLevel) {
            // Adjust the decrease based on agility level
            // This is just an example formula, you can adjust as needed
            double adjustment = 1 + (0.02 * agilityLevel); // 2% less hunger depletion per agility level
            int adjustedFoodLevel = (int) (currentFoodLevel - (currentFoodLevel - newFoodLevel) / adjustment);

            event.setFoodLevel(adjustedFoodLevel);
        }
    }
}
