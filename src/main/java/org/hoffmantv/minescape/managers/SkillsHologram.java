package org.hoffmantv.minescape.managers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.Map;

public class SkillsHologram {

    private final SkillManager skillManager;
    private static final String SKILLS_OBJECTIVE = "skills";
    private static final String SKILLS_TITLE = ChatColor.GOLD + " =-=-=-=[ MineScape ]=-=-=-=";
    private static final ChatColor PRIMARY_COLOR = ChatColor.GOLD;
    private static final ChatColor SECONDARY_COLOR = ChatColor.GRAY;
    private static final ChatColor LEVEL_COLOR = ChatColor.GREEN;
    private static final String SPACER = ChatColor.DARK_GRAY + ChatColor.STRIKETHROUGH.toString() + "                      ";

    public SkillsHologram(SkillManager skillManager) {
        this.skillManager = skillManager;
    }

    public void toggleHologram(Player player) {
        if (playerHasSkillsDisplayed(player)) {
            clearSkillsHologram(player);
            player.sendMessage(ChatColor.RED + "Skills Hologram hidden.");
        } else {
            showSkillsHologram(player);
            player.sendMessage(ChatColor.GREEN + "Skills Hologram displayed.");
        }
    }

    private boolean playerHasSkillsDisplayed(Player player) {
        Scoreboard scoreboard = player.getScoreboard();
        return scoreboard.getObjective(DisplaySlot.SIDEBAR) != null;
    }

    public void showSkillsHologram(Player player) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) {
            return;
        }

        Scoreboard scoreboard = manager.getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective(SKILLS_OBJECTIVE, "dummy", SKILLS_TITLE);
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        addInformationSection(objective, player);
        addSkillsGrid(objective, player);
        addFooterText(objective);

        player.setScoreboard(scoreboard);

        // Schedule a repeating task to update the playtime and ping every second
        Bukkit.getScheduler().runTaskTimer(skillManager.getPlugin(), () -> {
            updateDynamicLine(objective, player, ChatColor.WHITE + "Play time: " + ChatColor.AQUA + skillManager.getFormattedPlaytime(player), 13);
            updateDynamicLine(objective, player, ChatColor.WHITE + "Online: " + LEVEL_COLOR + Bukkit.getOnlinePlayers().size() + ChatColor.GRAY + " | " + ChatColor.WHITE + " Ping: " + LEVEL_COLOR + skillManager.getPing(player), 12);
        }, 0L, 20L);
    }

    private void addInformationSection(Objective objective, Player player) {
        addLineToObjective(objective, ChatColor.WHITE + "HP: " + LEVEL_COLOR + skillManager.getHealth(player) + ChatColor.GRAY + " | " + ChatColor.WHITE + "CB: " + ChatColor.GOLD + skillManager.getCombatLevel(player), 14);
        addLineToObjective(objective, ChatColor.WHITE + "Play time: " + ChatColor.AQUA + skillManager.getFormattedPlaytime(player), 13);
        addLineToObjective(objective, SPACER, 11); // Spacer
    }

    private void addSkillsGrid(Objective objective, Player player) {
        Map<SkillManager.Skill, Integer> skills = skillManager.getAllSkillLevels(player);

        int[] rows = {10, 9, 8, 7, 6, 5, 4, 3, 2};
        int index = 0;

        SkillManager.Skill[][] skillLayout = {
                {SkillManager.Skill.ATTACK, SkillManager.Skill.DEFENCE, SkillManager.Skill.STRENGTH},
                {SkillManager.Skill.HITPOINTS, SkillManager.Skill.MAGIC, SkillManager.Skill.RANGE},
                {SkillManager.Skill.PRAYER, SkillManager.Skill.COOKING, SkillManager.Skill.WOODCUTTING},
                {SkillManager.Skill.FLETCHING, SkillManager.Skill.FIREMAKING, SkillManager.Skill.CRAFTING},
                {SkillManager.Skill.SMITHING, SkillManager.Skill.MINING, SkillManager.Skill.HERBLORE},
                {SkillManager.Skill.AGILITY, SkillManager.Skill.THEVING, SkillManager.Skill.SLAYER},
                {SkillManager.Skill.FARMING, SkillManager.Skill.RUNECRAFTING, SkillManager.Skill.HUNTER},
                {SkillManager.Skill.CONSTRUCTION, SkillManager.Skill.ALCHEMY, SkillManager.Skill.COMBAT}
        };

        for (SkillManager.Skill[] row : skillLayout) {
            StringBuilder rowText = new StringBuilder();
            for (SkillManager.Skill skill : row) {
                if (!skills.containsKey(skill)) continue;
                String skillIcon = getSkillIcon(skill);
                String skillLevel = formatSkillLevel(skills.get(skill));
                rowText.append(skillIcon).append(" ").append(skillLevel).append("  ");
            }
            if (rowText.length() > 0) {
                addLineToObjective(objective, rowText.toString().trim(), rows[index++]);
            }
        }
    }

    private void addFooterText(Objective objective) {
        String footerText = ChatColor.YELLOW + centerText("MINESCAPE (ALPHA)", 30);
        addLineToObjective(objective, footerText, 0);
    }

    private void updateDynamicLine(Objective objective, Player player, String line, int score) {
        // Reset the old entry first
        for (String entry : objective.getScoreboard().getEntries()) {
            if (entry.contains(line.split(":")[0])) {
                objective.getScoreboard().resetScores(entry);
            }
        }
        // Add the updated entry
        addLineToObjective(objective, line, score);
    }

    private void addLineToObjective(Objective objective, String line, int score) {
        Score scoreLine = objective.getScore(line);
        scoreLine.setScore(score);
    }

    private String getSkillIcon(SkillManager.Skill skill) {
        String white = ChatColor.WHITE.toString();
        String green = ChatColor.GREEN.toString();
        switch (skill) {
            case WOODCUTTING: return white + "⚒" + green;
            case MINING: return white + "⛏" + green;
            case SMITHING: return white + "⚒" + green;
            case FISHING: return white + "🎣" + green;
            case ATTACK: return white + "⚔" + green;
            case DEFENCE: return white + "🛡" + green;
            case STRENGTH: return white + "💪" + green;
            case RANGE: return white + "🏹" + green;
            case HITPOINTS: return white + "❤" + green;
            case PRAYER: return white + "🙏" + green;
            case MAGIC: return white + "✨" + green;
            case COOKING: return white + "🍳" + green;
            case FLETCHING: return white + "🏹" + green;
            case FIREMAKING: return white + "🔥" + green;
            case CRAFTING: return white + "🛠" + green;
            case HERBLORE: return white + "🌿" + green;
            case AGILITY: return white + "🏃" + green;
            case THEVING: return white + "🗝" + green;
            case SLAYER: return white + "💀" + green;
            case FARMING: return white + "🌾" + green;
            case RUNECRAFTING: return white + "📜" + green;
            case HUNTER: return white + "🐾" + green;
            case CONSTRUCTION: return white + "🏗" + green;
            case ALCHEMY: return white + "⚗" + green;
            default: return white + "❓" + green;
        }
    }

    private String formatSkillLevel(int level) {
        return String.format("%02d", level);
    }

    private String centerText(String text, int totalWidth) {
        int paddingSize = (totalWidth - ChatColor.stripColor(text).length()) / 2;
        StringBuilder paddedText = new StringBuilder();
        for (int i = 0; i < paddingSize; i++) {
            paddedText.append(" ");
        }
        paddedText.append(text);
        return paddedText.toString();
    }

    public void clearSkillsHologram(Player player) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager != null) {
            player.setScoreboard(manager.getNewScoreboard());
        }
    }
}