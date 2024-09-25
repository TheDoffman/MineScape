package org.hoffmantv.minescape.managers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.Map;

public class SkillsHologram {

    private final SkillManager skillManager;
    private static final String SKILLS_OBJECTIVE = "skills";
    private static final String SKILLS_TITLE = ChatColor.DARK_RED + "" + ChatColor.BOLD + "» " + ChatColor.GOLD + "Your Skills" + ChatColor.DARK_RED + " «";
    private static final DisplaySlot DISPLAY_SLOT = DisplaySlot.SIDEBAR;
    private static final ChatColor PRIMARY_COLOR = ChatColor.GOLD;
    private static final ChatColor LEVEL_COLOR = ChatColor.GREEN;
    private static final ChatColor SEPARATOR_COLOR = ChatColor.DARK_GRAY;

    public SkillsHologram(SkillManager skillManager) {
        if (skillManager == null) {
            throw new IllegalArgumentException("SkillManager cannot be null");
        }
        this.skillManager = skillManager;
    }

    /**
     * Toggles the skill hologram display for a player.
     *
     * @param player The player to toggle the hologram for.
     */
    public void toggleHologram(Player player) {
        if (playerHasSkillsDisplayed(player)) {
            clearSkillsHologram(player);
            player.sendMessage(ChatColor.RED + "Skills Hologram hidden.");
        } else {
            showSkillsHologram(player);
            player.sendMessage(ChatColor.GREEN + "Skills Hologram displayed.");
        }
    }

    /**
     * Checks if the player currently has the skills hologram displayed.
     *
     * @param player The player to check.
     * @return True if the skills hologram is displayed, false otherwise.
     */
    private boolean playerHasSkillsDisplayed(Player player) {
        Scoreboard scoreboard = player.getScoreboard();
        return scoreboard.getObjective(DISPLAY_SLOT) != null;
    }

    /**
     * Displays the skills hologram for the player.
     *
     * @param player The player to display the skills hologram to.
     */
    public void showSkillsHologram(Player player) {
        Scoreboard scoreboard = createSkillScoreboard(player);
        player.setScoreboard(scoreboard);
    }

    /**
     * Creates a scoreboard displaying all skills and their levels for the player.
     *
     * @param player The player whose skills are being displayed.
     * @return A Scoreboard object populated with the player's skills and levels.
     */
    private Scoreboard createSkillScoreboard(Player player) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) {
            Bukkit.getLogger().warning("Scoreboard manager is null! Returning a blank scoreboard.");
            return Bukkit.getScoreboardManager().getNewScoreboard(); // Return a blank scoreboard if manager is null
        }

        Scoreboard scoreboard = manager.getNewScoreboard();
        Objective objective = createSkillsObjective(scoreboard);

        Map<SkillManager.Skill, Integer> skills = skillManager.getAllSkillLevels(player);

        if (skills.isEmpty()) {
            player.sendMessage(ChatColor.RED + "No skills data found for the player.");
            return scoreboard;
        }

        int index = skills.size(); // Line index for unique entry names, descending order

        // Add each skill and its rank to the scoreboard
        for (Map.Entry<SkillManager.Skill, Integer> entry : skills.entrySet()) {
            String skillDisplay = getFormattedSkillDisplay(entry.getKey(), entry.getValue());

            // Create a unique invisible entry name for each line
            String entryName = getUniqueEntryName(index);

            // Use teams to set the prefix and suffix for the display
            Team team = scoreboard.registerNewTeam("skill_" + index);
            team.addEntry(entryName);
            team.setPrefix(skillDisplay); // Skill name and symbol
            team.setSuffix(" " + LEVEL_COLOR + getSkillRank(entry.getValue())); // Skill rank

            objective.getScore(entryName).setScore(index); // Set score to the index
            index--;
        }

        return scoreboard;
    }

    /**
     * Creates and configures the skills objective on the provided scoreboard.
     *
     * @param scoreboard The scoreboard to configure.
     * @return The created Objective.
     */
    private Objective createSkillsObjective(Scoreboard scoreboard) {
        Objective objective = scoreboard.registerNewObjective(SKILLS_OBJECTIVE, "dummy", SKILLS_TITLE);
        objective.setDisplaySlot(DISPLAY_SLOT);
        return objective;
    }

    /**
     * Formats the skill display with appropriate colors and symbols.
     *
     * @param skill The skill to display.
     * @param level The level of the skill.
     * @return The formatted skill display string.
     */
    private String getFormattedSkillDisplay(SkillManager.Skill skill, int level) {
        String symbol = getSkillSymbol(skill);
        String skillName = formatSkillName(skill);
        return symbol + " " + PRIMARY_COLOR + skillName + SEPARATOR_COLOR + " »";
    }

    /**
     * Returns a themed symbol for the skill.
     *
     * @param skill The skill enum.
     * @return A Unicode symbol representing the skill.
     */
    private String getSkillSymbol(SkillManager.Skill skill) {
        switch (skill) {
            case WOODCUTTING:
                return "🌲";
            case MINING:
                return "⛏";
            case SMITHING:
                return "⚒";
            case FISHING:
                return "🎣";
            case ATTACK:
                return "⚔";
            case DEFENCE:
                return "🛡";
            case STRENGTH:
                return "💪";
            case RANGE:
                return "🏹";
            case HITPOINTS:
                return "❤️";
            case PRAYER:
                return "🙏";
            case MAGIC:
                return "✨";
            case COOKING:
                return "🍳";
            case FLETCHING:
                return "🏹";
            case FIREMAKING:
                return "🔥";
            case CRAFTING:
                return "🛠";
            case HERBLORE:
                return "🌿";
            case AGILITY:
                return "🏃";
            case THEVING:
                return "🗝";
            case SLAYER:
                return "💀";
            case FARMING:
                return "🌾";
            case RUNECRAFTING:
                return "📜";
            case HUNTER:
                return "🐾";
            case CONSTRUCTION:
                return "🏗";
            case ALCHEMY:
                return "⚗";
            case COMBAT:
                return "🗡";
            default:
                return "❓";
        }
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
     * Generates a unique entry name for each line.
     *
     * @param index The index of the line.
     * @return A unique string for the entry.
     */
    private String getUniqueEntryName(int index) {
        return ChatColor.values()[index % ChatColor.values().length].toString() + ChatColor.RESET;
    }

    /**
     * Gets the skill rank based on the skill level.
     *
     * @param level The level of the skill.
     * @return The corresponding rank as a string.
     */
    private String getSkillRank(int level) {
        if (level >= 90) {
            return "Master";
        } else if (level >= 70) {
            return "Expert";
        } else if (level >= 50) {
            return "Adept";
        } else if (level >= 30) {
            return "Apprentice";
        } else if (level >= 10) {
            return "Novice";
        } else {
            return "Beginner";
        }
    }

    /**
     * Clears the player's scoreboard.
     *
     * @param player The player whose scoreboard will be cleared.
     */
    public void clearSkillsHologram(Player player) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager != null) {
            player.setScoreboard(manager.getNewScoreboard()); // Sets a blank scoreboard
        }
    }
}