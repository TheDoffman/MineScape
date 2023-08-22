package org.hoffmantv.minescape.skills;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.hoffmantv.minescape.managers.SkillManager;

public class CombatLevel {

    private SkillManager skillManager;  // Assuming SkillManager is the class that contains the enums and the methods related to player skills

    public CombatLevel(SkillManager skillManager) {
        this.skillManager = skillManager;
    }

    public enum Skill {
        // ... your other skills ...
        COMBAT
    }

    public int calculateCombatLevel(Player player) {
        int strength = skillManager.getSkillLevel(player, SkillManager.Skill.STRENGTH);
        int attack = skillManager.getSkillLevel(player, SkillManager.Skill.ATTACK);
        int defence = skillManager.getSkillLevel(player, SkillManager.Skill.DEFENCE);

        return (strength + attack + defence) / 3;
    }

    public void updateCombatLevel(Player player, Player opponent) {
        String coloredCombatLevel = getColoredCombatLevel(player, opponent);
        String nameTag = player.getName() + " " + coloredCombatLevel;
        player.setCustomName(nameTag);
        player.setCustomNameVisible(true);
    }

    public void updatePlayerNametag(Player player) {
        int combatLevel = skillManager.getSkillLevel(player, SkillManager.Skill.COMBAT);
        String nameTag = ChatColor.GRAY + "[" + combatLevel + "] " + ChatColor.RESET + player.getName();
        player.setDisplayName(nameTag);
        player.setPlayerListName(nameTag);  // This is for the player list in the tab menu
    }

    public void updatePlayerHeadDisplay(Player player) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = scoreboard.getTeam(player.getName());

        if (team == null) {
            team = scoreboard.registerNewTeam(player.getName());
            team.addEntry(player.getName());
        }

        int combatLevel = skillManager.getSkillLevel(player, SkillManager.Skill.COMBAT);
        team.setPrefix(ChatColor.GRAY + "[" + combatLevel + "] " + ChatColor.RESET);

        player.setScoreboard(scoreboard);
    }
    public String getColoredCombatLevel(Player player, Player opponent) {
        int playerCombatLevel = calculateCombatLevel(player);
        int opponentCombatLevel = calculateCombatLevel(opponent);
        int difference = playerCombatLevel - opponentCombatLevel;

        // Close to each other
        if (Math.abs(difference) <= 5) {
            return ChatColor.GREEN + String.valueOf(playerCombatLevel);
        }
        // Player's combat level is higher
        else if (difference > 5) {
            return ChatColor.RED + String.valueOf(playerCombatLevel);
        }
        // Player's combat level is lower
        else {
            return ChatColor.BLUE + String.valueOf(playerCombatLevel);
        }
    }
}
