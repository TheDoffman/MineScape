package org.hoffmantv.minescape.commands;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.hoffmantv.minescape.skills.FishingSkill;
import org.hoffmantv.minescape.skills.SkillManager;

import java.util.ArrayList;
import java.util.List;

public class AddFishingSpotCommand implements CommandExecutor {

    private final FishingSkill fishingSkill;
    private final SkillManager skillManager;

    public AddFishingSpotCommand(SkillManager skillManager, FishingSkill fishingSkill) {
        this.skillManager = skillManager;
        this.fishingSkill = fishingSkill;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Ensure the sender is a player
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can execute this command.");
            return true;
        }

        Player player = (Player) sender;

        // Check permissions
        if (!player.hasPermission("minescape.addfishingspot")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        // Command usage: /addfishingspot <requiredLevel> <fishType1> <fishType2> ...
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /addfishingspot <requiredLevel> <fishType1> <fishType2> ...");
            return true;
        }

        // Parse requiredLevel
        int requiredLevel;
        try {
            requiredLevel = Integer.parseInt(args[0]);
            if (requiredLevel < 1) {
                throw new NumberFormatException("Level must be at least 1.");
            }
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid required level: " + args[0]);
            return true;
        }

        // Parse fish types
        List<String> fishTypesInput = new ArrayList<>();
        for (int i = 1; i < args.length; i++) {
            fishTypesInput.add(args[i].toUpperCase());
        }

        // Validate fish types against defined fishTypes in skills.yml
        ConfigurationSection fishTypesSection = skillManager.getSkillsConfig().getConfigurationSection("skills.fishing.fishTypes");
        if (fishTypesSection == null) {
            player.sendMessage(ChatColor.RED + "No 'fishTypes' defined in skills.yml.");
            return true;
        }

        List<String> validFishTypes = new ArrayList<>();
        List<String> invalidFishTypes = new ArrayList<>();

        for (String fishName : fishTypesInput) {
            if (fishTypesSection.isConfigurationSection(fishName)) {
                validFishTypes.add(fishName);
            } else {
                invalidFishTypes.add(fishName);
            }
        }

        if (!invalidFishTypes.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Invalid fish types: " + String.join(", ", invalidFishTypes));
            player.sendMessage(ChatColor.RED + "Please define these fish types in skills.yml under 'fishTypes'.");
            return true;
        }

        // Save the fishing spot to skills.yml under fishingSpots
        ConfigurationSection skillsConfig = skillManager.getSkillsConfig();
        ConfigurationSection fishingConfig = skillsConfig.getConfigurationSection("skills.fishing");
        if (fishingConfig == null) {
            fishingConfig = skillsConfig.createSection("skills.fishing");
        }

        // Get or create the fishingSpots section
        ConfigurationSection spotsSection = fishingConfig.getConfigurationSection("fishingSpots");
        if (spotsSection == null) {
            spotsSection = fishingConfig.createSection("fishingSpots");
        }

        // Determine a unique spot key
        int spotIndex = spotsSection.getKeys(false).size();
        String spotKey = "spot" + spotIndex;

        // Get player's current location
        Location location = player.getLocation();

        // Set the fishing spot details
        spotsSection.set(spotKey + ".world", location.getWorld().getName());
        spotsSection.set(spotKey + ".x", location.getX());
        spotsSection.set(spotKey + ".y", location.getY());
        spotsSection.set(spotKey + ".z", location.getZ());
        spotsSection.set(spotKey + ".requiredLevel", requiredLevel);
        spotsSection.set(spotKey + ".fishTypes", validFishTypes);

        // Log XP gain in player data
        skillManager.addXP(player, SkillManager.Skill.FISHING, (int) validFishTypes.size()); // Adjust this if you have a specific XP gain method.

        // Reload fishing spots in FishingSkill
        fishingSkill.reloadFishingSpots();

        player.sendMessage(ChatColor.GREEN + "Fishing spot added at your current location with required level " + requiredLevel + " and fish types: " + String.join(", ", validFishTypes));

        return true;
    }
}