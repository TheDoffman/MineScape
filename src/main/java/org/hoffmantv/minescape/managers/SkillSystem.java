package org.hoffmantv.minescape.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.hoffmantv.minescape.skills.MiningSkill;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SkillSystem {
    private final JavaPlugin plugin;
    private final FileConfiguration config;
    private final Map<UUID, PlayerSkills> playerSkillsData = new HashMap<>();

    public SkillSystem(JavaPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        loadPlayerData();
    }

    // Loading player data from the config
    public void loadPlayerData() {
        if (config.contains("players")) {
            for (String uuidString : config.getConfigurationSection("players").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidString);

                    // Load Mining Skill
                    int miningXp = config.getInt("players." + uuid + ".mining.xp", 0);
                    int miningLevel = config.getInt("players." + uuid + ".mining.level", 1);
                    MiningSkill miningSkill = new MiningSkill(miningXp, miningLevel);

                    PlayerSkills skills = new PlayerSkills(miningSkill);
                    playerSkillsData.put(uuid, skills);

                } catch (IllegalArgumentException e) {
                    // Handle invalid UUID
                    plugin.getLogger().warning("Invalid UUID in config: " + uuidString);
                }
            }
        }
    }

    // Saving player data to the config
    public void savePlayerData() {
        for (Map.Entry<UUID, PlayerSkills> entry : playerSkillsData.entrySet()) {
            UUID uuid = entry.getKey();
            PlayerSkills skills = entry.getValue();

            // Save Mining Skill
            config.set("players." + uuid + ".mining.xp", skills.getMiningSkill().getXP());
            config.set("players." + uuid + ".mining.level", skills.getMiningSkill().getLevel());

            // Save Woodcutting Skill
            config.set("players." + uuid + ".woodcutting.xp", skills.getWoodcuttingSkill().getXP());
            config.set("players." + uuid + ".woodcutting.level", skills.getWoodcuttingSkill().getLevel());

            // ... [save additional skills as needed]
        }
        plugin.saveConfig();
    }

    // Getter for PlayerSkills
    public PlayerSkills getPlayerSkills(UUID uuid) {
        return playerSkillsData.getOrDefault(uuid, null);
    }

    // Setter for PlayerSkills (use this if you need to update the PlayerSkills object itself)
    public void setPlayerSkills(UUID uuid, PlayerSkills skills) {
        playerSkillsData.put(uuid, skills);
    }

    // ... [additional methods to handle skill-specific operations, leveling up, etc.]
}
