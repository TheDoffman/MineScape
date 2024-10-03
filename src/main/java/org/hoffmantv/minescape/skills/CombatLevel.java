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

    // OSRS-like combat level calculation, reading from playerdata.yml
    public int calculateCombatLevel(Player player) {
        // Get skill levels from playerdata.yml through SkillManager
        int attack = skillManager.getSkillLevel(player, SkillManager.Skill.ATTACK);
        int strength = skillManager.getSkillLevel(player, SkillManager.Skill.STRENGTH);
        int defence = skillManager.getSkillLevel(player, SkillManager.Skill.DEFENCE);
        int hitpoints = skillManager.getSkillLevel(player, SkillManager.Skill.HITPOINTS);
        int prayer = skillManager.getSkillLevel(player, SkillManager.Skill.PRAYER);
        int ranged = skillManager.getSkillLevel(player, SkillManager.Skill.RANGE);
        int magic = skillManager.getSkillLevel(player, SkillManager.Skill.MAGIC);

        // Base combat level (Defence + Hitpoints + floor(Prayer / 2))
        double baseCombat = 0.25 * (defence + hitpoints + Math.floor(prayer / 2));

        // Melee combat (Attack + Strength)
        double meleeCombat = 0.325 * (attack + strength);

        // Ranged combat (floor(Ranged / 2) + Ranged)
        double rangedCombat = 0.325 * (Math.floor(ranged / 2) + ranged);

        // Magic combat (floor(Magic / 2) + Magic)
        double magicCombat = 0.325 * (Math.floor(magic / 2) + magic);

        // Return the highest value between melee, ranged, and magic combat
        return (int) Math.floor(baseCombat + Math.max(meleeCombat, Math.max(rangedCombat, magicCombat)));
    }

    // Update combat level and store it in playerdata.yml
    public void updateCombatLevel(Player player) {
        int playerCombatLevel = calculateCombatLevel(player);

        // Set player's combat skill level in SkillManager and save it to playerdata.yml
        skillManager.setSkillLevel(player, SkillManager.Skill.COMBAT, playerCombatLevel);

        // Update player name tag to show combat level
        updatePlayerNametag(player);
        updatePlayerHeadDisplay(player);  // For scoreboard display, if required
    }

    // Update the player's display name to show combat level and health
    public void updatePlayerNametag(Player player) {
        int combatLevel = skillManager.getSkillLevel(player, SkillManager.Skill.COMBAT);
        double health = player.getHealth(); // Get the player's current health

        String nameTag = ChatColor.GRAY + "[" + ChatColor.GREEN + combatLevel + ChatColor.GRAY + "] " +
                ChatColor.RESET + player.getName() + " " +
                ChatColor.RED + "[" + String.format("%.0f", health) + ChatColor.RED + "]"; // Health on the right

        player.setDisplayName(nameTag);
        player.setPlayerListName(nameTag);
    }

    // Update the player's head display with combat level and health (for scoreboard)
    public void updatePlayerHeadDisplay(Player player) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = scoreboard.getTeam(player.getName());

        if (team == null) {
            team = scoreboard.registerNewTeam(player.getName());
            team.addEntry(player.getName());
        }

        int combatLevel = skillManager.getSkillLevel(player, SkillManager.Skill.COMBAT);
        double health = player.getHealth(); // Get player's current health

        // Format the player's name to show combat level and health
        team.setPrefix(ChatColor.GRAY + "[" + combatLevel + "] " + ChatColor.RESET);
        team.setSuffix(" " + ChatColor.RED + "[" + String.format("%.0f", health) + "]");

        player.setScoreboard(scoreboard);
    }

}