package org.hoffmantv.minescape.managers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class SkillsHologram {

    private final SkillManager skillManager;
    private final Set<UUID> hologramEnabledPlayers = new HashSet<>(); // Stores players with hologram enabled
    private BukkitTask hologramUpdateTask; // Task for updating the hologram
    private static final long UPDATE_INTERVAL = 20L * 5; // Update every 5 seconds (20 ticks = 1 second)

    public SkillsHologram(SkillManager skillManager) {
        this.skillManager = skillManager;
    }

    /**
     * Toggles the hologram display for the player.
     *
     * @param player The player to toggle the hologram for.
     */
    public void toggleHologram(Player player) {
        UUID playerUUID = player.getUniqueId();
        if (hologramEnabledPlayers.contains(playerUUID)) {
            hologramEnabledPlayers.remove(playerUUID);
            clearSkillsHologram(player);
            player.sendMessage(ChatColor.RED + "Skills hologram disabled.");
        } else {
            hologramEnabledPlayers.add(playerUUID);
            showSkillsHologram(player);
            player.sendMessage(ChatColor.GREEN + "Skills hologram enabled.");
        }
    }

    /**
     * Starts a repeating task to update the skills hologram for players who have it enabled.
     */
    public void startHologramUpdater() {
        if (hologramUpdateTask == null || hologramUpdateTask.isCancelled()) {
            hologramUpdateTask = new BukkitRunnable() {
                @Override
                public void run() {
                    for (UUID playerUUID : hologramEnabledPlayers) {
                        Player player = Bukkit.getPlayer(playerUUID);
                        if (player != null && player.isOnline()) {
                            showSkillsHologram(player); // Update the hologram for the player
                        }
                    }
                }
            }.runTaskTimer(skillManager.getPlugin(), 0L, UPDATE_INTERVAL);
        }
    }

    /**
     * Stops the hologram update task.
     */
    public void stopHologramUpdater() {
        if (hologramUpdateTask != null) {
            hologramUpdateTask.cancel();
        }
    }

    /**
     * Displays the skills scoreboard for the specified player.
     *
     * @param player The player to display the scoreboard to.
     */
    public void showSkillsHologram(Player player) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) {
            return; // Safety check
        }

        Scoreboard scoreboard = manager.getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("skills", "dummy", ChatColor.RED + "" + ChatColor.BOLD + "Skills");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        Map<SkillManager.Skill, Integer> skills = skillManager.getAllSkillLevels(player);
        int scoreValue = skills.size();

        for (Map.Entry<SkillManager.Skill, Integer> entry : skills.entrySet()) {
            String skillName = formatSkillName(entry.getKey());
            int level = entry.getValue();

            Score score = objective.getScore(ChatColor.YELLOW + skillName + ": " + ChatColor.GREEN + level);
            score.setScore(scoreValue--);
        }

        player.setScoreboard(scoreboard);
    }

    /**
     * Formats the skill name for display.
     *
     * @param skill The skill enum.
     * @return A user-friendly skill name.
     */
    private String formatSkillName(SkillManager.Skill skill) {
        String name = skill.name().toLowerCase().replace('_', ' ');
        String[] words = name.split(" ");
        StringBuilder formattedName = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                formattedName.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
            }
        }
        return formattedName.toString().trim();
    }

    /**
     * Clears the player's scoreboard.
     *
     * @param player The player whose scoreboard will be cleared.
     */
    public void clearSkillsHologram(Player player) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager != null) {
            player.setScoreboard(manager.getNewScoreboard());
        }
    }
}