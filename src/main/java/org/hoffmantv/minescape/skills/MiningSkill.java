package org.hoffmantv.minescape.skills;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


    public class MiningSkill implements Listener {

        private final JavaPlugin plugin;
        private final HashMap<UUID, MiningData> playerSkills = new HashMap<>();
        private final HashMap<Material, Integer> blockXpValues = new HashMap<>();
        private final HashMap<Material, Integer> blockLevelRestrictions = new HashMap<>();
        private final FileConfiguration config;

        public MiningSkill(JavaPlugin plugin) {
            this.plugin = plugin;
            this.config = plugin.getConfig();

            initBlockValues();

            loadPlayerData();
        }

    private void initBlockValues() {
        // Initialize block XP values
        blockXpValues.put(Material.LAPIS_ORE, 5);
        blockXpValues.put(Material.COAL_ORE, 10);
        blockXpValues.put(Material.IRON_ORE, 15);
        blockXpValues.put(Material.GOLD_ORE, 20);
        blockXpValues.put(Material.DIAMOND_ORE, 25);
        blockXpValues.put(Material.OBSIDIAN,35);
        // ... add other blocks ...

        blockLevelRestrictions.put(Material.COAL_ORE, 1);
        blockLevelRestrictions.put(Material.LAPIS_ORE, 5);
        blockLevelRestrictions.put(Material.IRON_ORE, 10);
        blockLevelRestrictions.put(Material.GOLD_ORE, 20);
        blockLevelRestrictions.put(Material.DIAMOND_ORE, 50);
        blockLevelRestrictions.put(Material.OBSIDIAN, 60);
    }

    private void savePlayerData() {
        for (Map.Entry<UUID, MiningData> entry : playerSkills.entrySet()) {
            UUID uuid = entry.getKey();
            MiningData skill = entry.getValue();
            config.set("players." + uuid + ".mining.xp", skill.getXP());
            config.set("players." + uuid + ".mining.level", skill.getLevel());
        }
        plugin.saveConfig();
    }

    private void loadPlayerData() {
        if (config.contains("players")) {
            for (String uuidString : config.getConfigurationSection("miningSkills").getKeys(false)) {
                UUID uuid = UUID.fromString(uuidString);
                int miningxp = config.getInt("players." + uuid + ".mining.xp");
                int mininglevel = config.getInt("players." + uuid + ".mining.level");
                playerSkills.put(uuid, new MiningData(miningxp, mininglevel));
            }
        }
    }
    // Inner class to manage individual player's mining skill
    private class MiningData {
        private int xp;
        private int level;
        private static final int MAX_LEVEL = 99;  // Define the maximum level

        MiningData() {
            this.xp = 0;
            this.level = 1;
        }
        MiningData(int xp, int level) {
            this.xp = xp;
            this.level = level;
        }

        void addXP(int xpToAdd, Player player) {
            this.xp += xpToAdd;
            if (checkForLevelUp() && this.level <= MAX_LEVEL) {
                player.sendMessage("Congratulations! You've leveled up in Mining to level " + this.level + "!");

                // Play sound
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            }
        }

        private boolean checkForLevelUp() {
            if (this.xp >= this.level * 100 && this.level < MAX_LEVEL) { // Check if below the maximum level
                this.level++;
                this.xp = 0;
                return true;
            }
            return false;
        }

        int getXP() {
            return this.xp;
        }

        int getLevel() {
            return this.level;
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Material blockType = event.getBlock().getType();

        int xpToAdd = blockXpValues.getOrDefault(blockType, 0);

        if (xpToAdd > 0) {
            player.sendMessage(ChatColor.GREEN + "You've earned " + xpToAdd + " Mining XP!");

            // Play sound
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }

        UUID playerUUID = player.getUniqueId();
        MiningData miningSkill = playerSkills.computeIfAbsent(playerUUID, k -> new MiningData());
        miningSkill.addXP(xpToAdd, player);
    }

    public int getPlayerXP(Player player) {
        return playerSkills.getOrDefault(player.getUniqueId(), new MiningData()).getXP();
    }

    public int getPlayerLevel(Player player) {
        return playerSkills.getOrDefault(player.getUniqueId(), new MiningData()).getLevel();
    }
}
