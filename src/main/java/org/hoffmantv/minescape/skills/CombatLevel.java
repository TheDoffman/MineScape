package org.hoffmantv.minescape.skills;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.hoffmantv.minescape.managers.SkillManager;

public class CombatLevel {

    private final SkillManager skillManager;

    public CombatLevel(SkillManager skillManager) {
        if(skillManager == null) {
            throw new IllegalArgumentException("SkillManager cannot be null");
        }
        this.skillManager = skillManager;
    }

    public enum Skill {
        COMBAT
    }

    public int calculateCombatLevel(Player player) {
        int strength = skillManager.getSkillLevel(player, SkillManager.Skill.STRENGTH);
        int attack = skillManager.getSkillLevel(player, SkillManager.Skill.ATTACK);
        int defence = skillManager.getSkillLevel(player, SkillManager.Skill.DEFENCE);

        return (strength + attack + defence);
    }

    public void updateCombatLevel(Player player, Player opponent) {
        int playerCombatLevel = calculateCombatLevel(player);
        String coloredCombatLevel = getColoredCombatLevel(playerCombatLevel, calculateCombatLevel(opponent));
        String nameTag = player.getName() + " " + coloredCombatLevel;
        skillManager.setSkillLevel(player, SkillManager.Skill.COMBAT, playerCombatLevel);
        player.setCustomName(nameTag);
        player.setCustomNameVisible(true);
    }

    public void updatePlayerNametag(Player player) {
        int combatLevel = skillManager.getSkillLevel(player, SkillManager.Skill.COMBAT);
        String nameTag = ChatColor.GRAY + "[" + combatLevel + "] " + ChatColor.RESET + player.getName();
        player.setDisplayName(nameTag);
        player.setPlayerListName(nameTag);
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
