package org.hoffmantv.minescape.skills;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.hoffmantv.minescape.managers.ConfigurationManager;

import java.util.*;

public class Woodcutting implements Listener {

    private final SkillManager skillManager;
    private final ConfigurationManager configManager;
    private final JavaPlugin plugin;
    private final Random random = new Random();

    // Configurable tree settings
    private final Map<Material, TreeType> treeTypes = new HashMap<>();

    public Woodcutting(SkillManager skillManager, ConfigurationManager configManager, JavaPlugin plugin) {
        this.skillManager = skillManager;
        this.configManager = configManager;
        this.plugin = plugin;

        loadTreeTypesFromConfig();
    }

    private void loadTreeTypesFromConfig() {
        // Load the tree types configuration from skills.yml
        ConfigurationSection treeSection = configManager.getConfig("skills.yml").getConfigurationSection("skills.woodcutting.trees");

        if (treeSection != null) {
            for (String key : treeSection.getKeys(false)) {
                ConfigurationSection treeConfig = treeSection.getConfigurationSection(key);

                if (treeConfig != null) {
                    Material logMaterial = Material.getMaterial(treeConfig.getString("log", "OAK_LOG"));
                    int requiredLevel = treeConfig.getInt("requiredLevel", 1);
                    double xpValue = treeConfig.getDouble("xpValue", 10);
                    Material sapling = Material.getMaterial(treeConfig.getString("sapling", "OAK_SAPLING"));

                    if (logMaterial != null && sapling != null) {
                        TreeType treeType = new TreeType(logMaterial, requiredLevel, xpValue, sapling);
                        treeTypes.put(logMaterial, treeType);
                    } else {
                        plugin.getLogger().warning("Invalid material specified in skills.yml for tree: " + key);
                    }
                }
            }
        }
    }

    // TreeType class for storing tree properties
    public static class TreeType {
        private final Material material;
        private final int requiredLevel;
        private final double xpValue;
        private final Material sapling;

        public TreeType(Material material, int requiredLevel, double xpValue, Material sapling) {
            this.material = material;
            this.requiredLevel = requiredLevel;
            this.xpValue = xpValue;
            this.sapling = sapling;
        }

        public Material getMaterial() {
            return material;
        }

        public int getRequiredLevel() {
            return requiredLevel;
        }

        public double getXpValue() {
            return xpValue;
        }

        public Material getSapling() {
            return sapling;
        }
    }

    // AxeType enum for storing axe properties
    public enum AxeType {
        WOODEN(Material.WOODEN_AXE, 1),
        STONE(Material.STONE_AXE, 10),
        IRON(Material.IRON_AXE, 15),
        GOLDEN(Material.GOLDEN_AXE, 25),
        DIAMOND(Material.DIAMOND_AXE, 35),
        NETHERITE(Material.NETHERITE_AXE, 50);

        private final Material material;
        private final int requiredLevel;

        AxeType(Material material, int requiredLevel) {
            this.material = material;
            this.requiredLevel = requiredLevel;
        }

        public Material getMaterial() {
            return material;
        }

        public int getRequiredLevel() {
            return requiredLevel;
        }

        public static AxeType fromMaterial(Material material) {
            for (AxeType type : values()) {
                if (type.getMaterial() == material) {
                    return type;
                }
            }
            return null;
        }
    }

    private boolean isPlantableBase(Material material) {
        return material == Material.DIRT || material == Material.GRASS_BLOCK;
    }

    // Handles tree removal and planting a sapling for regrowth
    private void handleTreeCut(Player player, Block block, TreeType treeType) {
        // Random delay simulation based on OSRS mechanics
        int delay = random.nextInt(20) + 30; // Random delay between 30-50 ticks (1.5 to 2.5 seconds)

        // Play woodcutting animation
        player.playSound(player.getLocation(), Sound.BLOCK_WOOD_BREAK, 1.0f, 1.0f);
        player.spawnParticle(Particle.BLOCK_CRACK, block.getLocation(), 10, block.getType().createBlockData());

        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            // Remove the tree and keep the stump
            int logsRemoved = removeTree(block);

            // Give logs to the player
            if (logsRemoved > 0) {
                ItemStack logs = new ItemStack(treeType.getMaterial(), logsRemoved);
                player.getInventory().addItem(logs);
            }

            // Check if the block below is dirt or grass to plant a sapling
            Location stumpLocation = block.getLocation();
            if (isPlantableBase(stumpLocation.getBlock().getRelative(BlockFace.DOWN).getType())) {
                plantSapling(stumpLocation, treeType);
            }

            // Provide XP to the player
            grantXpForTree(player, treeType);

        }, delay);
    }

    private int removeTree(Block startBlock) {
        Queue<Block> queue = new LinkedList<>();
        Set<Block> visited = new HashSet<>();
        queue.add(startBlock);
        visited.add(startBlock);
        int logCount = 0;

        while (!queue.isEmpty()) {
            Block currentBlock = queue.poll();
            if (isLog(currentBlock.getType())) {
                currentBlock.setType(Material.AIR);
                logCount++;
            } else if (isLeaf(currentBlock.getType())) {
                currentBlock.setType(Material.AIR);
            }
            for (BlockFace face : BlockFace.values()) {
                Block relative = currentBlock.getRelative(face);
                if (!visited.contains(relative) && (isLog(relative.getType()) || isLeaf(relative.getType()))) {
                    queue.add(relative);
                    visited.add(relative);
                }
            }
        }

        return logCount;
    }

    private boolean isLog(Material material) {
        return treeTypes.containsKey(material);
    }

    private boolean isLeaf(Material material) {
        return material == Material.OAK_LEAVES ||
                material == Material.SPRUCE_LEAVES ||
                material == Material.BIRCH_LEAVES ||
                material == Material.JUNGLE_LEAVES ||
                material == Material.ACACIA_LEAVES ||
                material == Material.DARK_OAK_LEAVES;
    }

    private void plantSapling(Location stumpLocation, TreeType treeType) {
        stumpLocation.getBlock().setType(treeType.getSapling());
        scheduleTreeGrowth(stumpLocation, treeType);
    }

    private void scheduleTreeGrowth(Location location, TreeType treeType) {
        long minDelay = 100L; // 5 seconds in ticks
        long maxDelay = 300L; // 15 seconds in ticks
        long randomDelay = minDelay + (long) (Math.random() * (maxDelay - minDelay + 1));

        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            if (location.getBlock().getType() == treeType.getSapling()) {
                growTree(location, treeType);
            }
        }, randomDelay);
    }

    private void growTree(Location location, TreeType treeType) {
        // Clear space around the sapling location
        clearSpaceForTree(location);

        // Remove the sapling before generating the tree
        location.getBlock().setType(Material.AIR);

        // Use built-in tree generation for simplicity
        boolean success = location.getWorld().generateTree(location, convertToBukkitTreeType(treeType));
        if (!success) {
            plugin.getLogger().info("Failed to grow " + treeType.getMaterial() + " tree at " + location);
        }
    }

    private void clearSpaceForTree(Location location) {
        int radius = 5; // Adjust as needed
        int height = 10; // Adjust as needed

        World world = location.getWorld();
        int baseX = location.getBlockX();
        int baseY = location.getBlockY();
        int baseZ = location.getBlockZ();

        for (int x = baseX - radius; x <= baseX + radius; x++) {
            for (int y = baseY; y <= baseY + height; y++) {
                for (int z = baseZ - radius; z <= baseZ + radius; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (!block.isEmpty() && block.getType() != Material.DIRT && block.getType() != Material.GRASS_BLOCK) {
                        block.setType(Material.AIR);
                    }
                }
            }
        }
    }

    private org.bukkit.TreeType convertToBukkitTreeType(TreeType treeType) {
        switch (treeType.getMaterial()) {
            case OAK_LOG:
                return org.bukkit.TreeType.TREE;
            case BIRCH_LOG:
                return org.bukkit.TreeType.BIRCH;
            case SPRUCE_LOG:
                return org.bukkit.TreeType.REDWOOD;
            case JUNGLE_LOG:
                return org.bukkit.TreeType.SMALL_JUNGLE;
            case ACACIA_LOG:
                return org.bukkit.TreeType.ACACIA;
            case DARK_OAK_LOG:
                return org.bukkit.TreeType.DARK_OAK;
            default:
                return org.bukkit.TreeType.TREE;
        }
    }

    private void grantXpForTree(Player player, TreeType treeType) {
        double xpValue = treeType.getXpValue();
        skillManager.addXP(player, SkillManager.Skill.WOODCUTTING, xpValue);
        player.sendActionBar(ChatColor.GOLD + "Woodcutting +" + xpValue);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);

        // Log XP gain in player data
        configManager.logXpGain(player, "woodcutting", (int) xpValue);
    }

    private boolean isAxe(Material material) {
        return AxeType.fromMaterial(material) != null;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        Material heldMaterial = heldItem.getType();

        // Check if the player is using an axe
        if (isAxe(heldMaterial)) {
            // Prevent axes from breaking non-log blocks
            if (!isLog(block.getType())) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "You can only use axes to chop down trees!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }

            // Ensure the player meets the axe's level requirement
            AxeType axeType = AxeType.fromMaterial(heldMaterial);
            if (axeType != null) {
                int playerLevel = skillManager.getSkillLevel(player, SkillManager.Skill.WOODCUTTING);
                if (playerLevel < axeType.getRequiredLevel()) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "You need a woodcutting level of " + axeType.getRequiredLevel() + " to use this " + axeType.name().toLowerCase() + " axe.");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return;
                }
            }

            // Handle saplings (prevent breaking)
            if (isSapling(block.getType())) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Saplings cannot be broken!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }

            // Handle tree cutting
            TreeType treeType = treeTypes.get(block.getType());

            // If the tree type isn't recognized, cancel the event
            if (treeType == null) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "You cannot chop down this type of tree.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }

            // Ensure the player meets the tree's level requirement
            int playerLevel = skillManager.getSkillLevel(player, SkillManager.Skill.WOODCUTTING);
            if (playerLevel < treeType.getRequiredLevel()) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "You need a woodcutting level of " + treeType.getRequiredLevel() + " to chop " + treeType.getMaterial().name().toLowerCase() + "!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }

            // Handle tree cutting and regrowth
            handleTreeCut(player, block, treeType);

            // Cancel the default block break event to prevent default drops
            event.setCancelled(true);
        } else {
            // If the player is not using an axe but is trying to break a log, cancel the event
            if (isLog(block.getType())) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "You must use an axe to chop down a tree!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        // Check if the damager is a player
        if (event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();
            ItemStack heldItem = player.getInventory().getItemInMainHand();

            // If the player is using an axe, cancel the damage event and inform them
            if (isAxe(heldItem.getType())) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Axes can only be used for woodcutting!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
        }
    }

    // Helper method to check if a material is a sapling
    private boolean isSapling(Material material) {
        return material == Material.OAK_SAPLING ||
                material == Material.SPRUCE_SAPLING ||
                material == Material.BIRCH_SAPLING ||
                material == Material.JUNGLE_SAPLING ||
                material == Material.ACACIA_SAPLING ||
                material == Material.DARK_OAK_SAPLING;
    }
}