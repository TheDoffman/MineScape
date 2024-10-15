package org.hoffmantv.minescape.skills;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class CombatLevel {

    private final SkillManager skillManager;

    public CombatLevel(SkillManager skillManager) {
        if (skillManager == null) {
            throw new IllegalArgumentException("SkillManager cannot be null");
        }
        this.skillManager = skillManager;
    }

    // OSRS-like combat level calculation
    public int calculateCombatLevel(Player player) {
        int attack = skillManager.getSkillLevel(player, SkillManager.Skill.ATTACK);
        int strength = skillManager.getSkillLevel(player, SkillManager.Skill.STRENGTH);
        int defence = skillManager.getSkillLevel(player, SkillManager.Skill.DEFENCE);
        int hitpoints = skillManager.getSkillLevel(player, SkillManager.Skill.HITPOINTS);
        int prayer = skillManager.getSkillLevel(player, SkillManager.Skill.PRAYER);
        int ranged = skillManager.getSkillLevel(player, SkillManager.Skill.RANGE);
        int magic = skillManager.getSkillLevel(player, SkillManager.Skill.MAGIC);

        // Calculate OSRS combat level
        double baseCombat = 0.25 * (defence + hitpoints + Math.floor(prayer / 2));
        double meleeCombat = 0.325 * (attack + strength);
        double rangedCombat = 0.325 * (Math.floor(ranged / 2) + ranged);
        double magicCombat = 0.325 * (Math.floor(magic / 2) + magic);

        // Choose the highest combat level (melee, ranged, or magic)
        return (int) Math.floor(baseCombat + Math.max(meleeCombat, Math.max(rangedCombat, magicCombat)));
    }

    // Update combat level with OSRS mechanics
    public void updateCombatLevel(Player player) {
        int playerCombatLevel = calculateCombatLevel(player);
        updatePlayerNametag(player);
    }

    // Update the player's display name to show combat level and health
    public void updatePlayerNametag(Player player) {
        int combatLevel = calculateCombatLevel(player); // Calculate the combat level inside this method
        double health = player.getHealth(); // Get the player's current health

        String nameTag = ChatColor.GRAY + "[" + ChatColor.GREEN + combatLevel + ChatColor.GRAY + "] " +
                ChatColor.RESET + player.getName() + " " +
                ChatColor.RED + "[" + String.format("%.0f", health) + ChatColor.RED + "]"; // Health on the right

        player.setDisplayName(nameTag);
        player.setPlayerListName(nameTag);
    }

    // Update the player's head display with combat level and health
    public void updatePlayerHeadDisplay(Player player) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = scoreboard.getTeam(player.getName());

        if (team == null) {
            team = scoreboard.registerNewTeam(player.getName());
            team.addEntry(player.getName());
        }

        int combatLevel = calculateCombatLevel(player);
        double health = player.getHealth(); // Get player's current health

        // Format the player's name to show combat level and health
        team.setPrefix(ChatColor.GRAY + "[" + combatLevel + "] " + ChatColor.RESET);
        team.setSuffix(" " + ChatColor.RED + "[" + String.format("%.0f", health) + "]");

        player.setScoreboard(scoreboard);
    }

    // Get the colored combat level based on the difference between the player and another opponent
    public String getColoredCombatLevel(int playerCombatLevel, int opponentCombatLevel) {
        int difference = playerCombatLevel - opponentCombatLevel;

        if (Math.abs(difference) <= 5) {
            return ChatColor.GREEN + String.valueOf(playerCombatLevel);
        } else if (difference > 5) {
            return ChatColor.RED + String.valueOf(playerCombatLevel);
        } else {
            return ChatColor.BLUE + String.valueOf(playerCombatLevel);
        }
    }
}