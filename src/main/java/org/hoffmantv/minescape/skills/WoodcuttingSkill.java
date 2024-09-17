package org.hoffmantv.minescape.skills;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.hoffmantv.minescape.managers.SkillManager;

import java.util.*;

public class WoodcuttingSkill implements Listener {

    private final SkillManager skillManager;
    private final JavaPlugin plugin;

    public WoodcuttingSkill(SkillManager skillManager, JavaPlugin plugin) {
        this.skillManager = skillManager;
        this.plugin = plugin;
    }

    // Enum for Tree Types with corresponding Material and Required Level
    public enum TreeType {

        OAK(Material.OAK_LOG, 1, 10, Material.OAK_SAPLING),
        DARK_OAK(Material.DARK_OAK_LOG, 10, 20, Material.DARK_OAK_SAPLING),
        BIRCH(Material.BIRCH_LOG, 20, 12, Material.BIRCH_SAPLING),
        SPRUCE(Material.SPRUCE_LOG, 30, 14, Material.SPRUCE_SAPLING),
        JUNGLE(Material.JUNGLE_LOG, 40, 16, Material.JUNGLE_SAPLING),
        ACACIA(Material.ACACIA_LOG, 50, 18, Material.ACACIA_SAPLING);

        private final Material material;
        private final int requiredLevel;
        private final double xpValue;
        private final Material sapling;

        TreeType(Material material, int requiredLevel, double xpValue, Material sapling) {
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

        public static TreeType fromMaterial(Material material) {
            for (TreeType type : values()) {
                if (type.getMaterial() == material) {
                    return type;
                }
            }
            return null;
        }

        public static TreeType fromSapling(Material material) {
            for (TreeType type : values()) {
                if (type.getSapling() == material) {
                    return type;
                }
            }
            return null;
        }
    }

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
        return TreeType.fromMaterial(material) != null;
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

        plugin.getLogger().info("Scheduling growth for " + treeType.name() + " in " + (randomDelay / 20) + " seconds at " + location);

        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            plugin.getLogger().info("Attempting to grow " + treeType.name() + " at " + location);
            if (location.getBlock().getType() == treeType.getSapling()) {
                growTree(location, treeType);
            } else {
                plugin.getLogger().info("Sapling was not found at the location.");
            }
        }, randomDelay);
    }

    private void growTree(Location location, TreeType treeType) {
        // Clear space around the sapling location
        clearSpaceForTree(location);

        // Remove the sapling before generating the tree
        location.getBlock().setType(Material.AIR);

        // Use custom tree generation methods
        switch (treeType) {
            case OAK:
                growCustomOakTree(location);
                break;
            case BIRCH:
                growCustomBirchTree(location);
                break;
            case SPRUCE:
                growCustomSpruceTree(location);
                break;
            case JUNGLE:
                growCustomJungleTree(location);
                break;
            case ACACIA:
                growCustomAcaciaTree(location);
                break;
            case DARK_OAK:
                growCustomDarkOakTree(location);
                break;
            default:
                // Default to built-in generation if custom method is not available
                boolean success = location.getWorld().generateTree(location, convertToBukkitTreeType(treeType));
                if (success) {
                    plugin.getLogger().info(treeType.name() + " tree successfully grown at " + location);
                } else {
                    plugin.getLogger().info("Failed to grow " + treeType.name() + " tree at " + location);
                }
                break;
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

    // Custom tree generation methods
    private void growCustomOakTree(Location location) {
        World world = location.getWorld();

        int height = 5; // Example fixed height

        // Place log blocks
        for (int y = 0; y < height; y++) {
            world.getBlockAt(location.getBlockX(), location.getBlockY() + y, location.getBlockZ()).setType(Material.OAK_LOG);
        }

        // Place leaves around the top
        int leafRadius = 2;
        for (int x = -leafRadius; x <= leafRadius; x++) {
            for (int y = height - 2; y <= height; y++) {
                for (int z = -leafRadius; z <= leafRadius; z++) {
                    if (Math.abs(x) + Math.abs(y - (height - 1)) + Math.abs(z) <= leafRadius) {
                        Block leafBlock = world.getBlockAt(location.getBlockX() + x, location.getBlockY() + y, location.getBlockZ() + z);
                        if (leafBlock.getType() == Material.AIR) {
                            leafBlock.setType(Material.OAK_LEAVES);
                        }
                    }
                }
            }
        }

        plugin.getLogger().info("Custom oak tree grown at " + location);
    }

    // Implement similar methods for other tree types
    private void growCustomBirchTree(Location location) {
        World world = location.getWorld();

        int height = 6; // Example height for birch

        // Place log blocks
        for (int y = 0; y < height; y++) {
            world.getBlockAt(location.getBlockX(), location.getBlockY() + y, location.getBlockZ()).setType(Material.BIRCH_LOG);
        }

        // Place leaves around the top
        int leafRadius = 2;
        for (int x = -leafRadius; x <= leafRadius; x++) {
            for (int y = height - 2; y <= height; y++) {
                for (int z = -leafRadius; z <= leafRadius; z++) {
                    if (Math.abs(x) + Math.abs(y - (height - 1)) + Math.abs(z) <= leafRadius) {
                        Block leafBlock = world.getBlockAt(location.getBlockX() + x, location.getBlockY() + y, location.getBlockZ() + z);
                        if (leafBlock.getType() == Material.AIR) {
                            leafBlock.setType(Material.BIRCH_LEAVES);
                        }
                    }
                }
            }
        }

        plugin.getLogger().info("Custom birch tree grown at " + location);
    }

    private void growCustomSpruceTree(Location location) {
        World world = location.getWorld();

        int height = 8; // Example height for spruce

        // Place log blocks
        for (int y = 0; y < height; y++) {
            world.getBlockAt(location.getBlockX(), location.getBlockY() + y, location.getBlockZ()).setType(Material.SPRUCE_LOG);
        }

        // Place leaves in a cone shape
        int maxRadius = 3;
        for (int y = 0; y < height; y++) {
            int radius = maxRadius - (y * maxRadius) / height;
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + z * z <= radius * radius) {
                        Block leafBlock = world.getBlockAt(location.getBlockX() + x, location.getBlockY() + y, location.getBlockZ() + z);
                        if (leafBlock.getType() == Material.AIR) {
                            leafBlock.setType(Material.SPRUCE_LEAVES);
                        }
                    }
                }
            }
        }

        plugin.getLogger().info("Custom spruce tree grown at " + location);
    }

    private void growCustomJungleTree(Location location) {
        World world = location.getWorld();

        int height = 10; // Example height for jungle tree

        // Place log blocks
        for (int y = 0; y < height; y++) {
            world.getBlockAt(location.getBlockX(), location.getBlockY() + y, location.getBlockZ()).setType(Material.JUNGLE_LOG);
        }

        // Place leaves around the top
        int leafRadius = 3;
        for (int x = -leafRadius; x <= leafRadius; x++) {
            for (int y = height - 3; y <= height; y++) {
                for (int z = -leafRadius; z <= leafRadius; z++) {
                    if (Math.abs(x) + Math.abs(y - (height - 1)) + Math.abs(z) <= leafRadius + 1) {
                        Block leafBlock = world.getBlockAt(location.getBlockX() + x, location.getBlockY() + y, location.getBlockZ() + z);
                        if (leafBlock.getType() == Material.AIR) {
                            leafBlock.setType(Material.JUNGLE_LEAVES);
                        }
                    }
                }
            }
        }

        plugin.getLogger().info("Custom jungle tree grown at " + location);
    }

    private void growCustomAcaciaTree(Location location) {
        World world = location.getWorld();

        int height = 6; // Example height for acacia

        // Place log blocks in a diagonal
        for (int y = 0; y < height; y++) {
            int offsetX = y % 2 == 0 ? 0 : 1;
            world.getBlockAt(location.getBlockX() + offsetX, location.getBlockY() + y, location.getBlockZ()).setType(Material.ACACIA_LOG);
        }

        // Place leaves at the top
        int leafRadius = 3;
        int topY = location.getBlockY() + height - 1;
        for (int x = -leafRadius; x <= leafRadius; x++) {
            for (int z = -leafRadius; z <= leafRadius; z++) {
                if (Math.abs(x) + Math.abs(z) <= leafRadius) {
                    Block leafBlock = world.getBlockAt(location.getBlockX() + x, topY, location.getBlockZ() + z);
                    if (leafBlock.getType() == Material.AIR) {
                        leafBlock.setType(Material.ACACIA_LEAVES);
                    }
                }
            }
        }

        plugin.getLogger().info("Custom acacia tree grown at " + location);
    }

    private void growCustomDarkOakTree(Location location) {
        World world = location.getWorld();

        int height = 7; // Example height for dark oak

        // Place log blocks in a 2x2 pattern
        for (int y = 0; y < height; y++) {
            for (int dx = 0; dx <= 1; dx++) {
                for (int dz = 0; dz <= 1; dz++) {
                    world.getBlockAt(location.getBlockX() + dx, location.getBlockY() + y, location.getBlockZ() + dz).setType(Material.DARK_OAK_LOG);
                }
            }
        }

        // Place leaves around the top
        int leafRadius = 3;
        for (int x = -leafRadius; x <= leafRadius + 1; x++) {
            for (int y = height - 2; y <= height + 1; y++) {
                for (int z = -leafRadius; z <= leafRadius + 1; z++) {
                    if (Math.abs(x) + Math.abs(y - (height - 1)) + Math.abs(z) <= leafRadius + 1) {
                        Block leafBlock = world.getBlockAt(location.getBlockX() + x, location.getBlockY() + y, location.getBlockZ() + z);
                        if (leafBlock.getType() == Material.AIR) {
                            leafBlock.setType(Material.DARK_OAK_LEAVES);
                        }
                    }
                }
            }
        }

        plugin.getLogger().info("Custom dark oak tree grown at " + location);
    }

    private org.bukkit.TreeType convertToBukkitTreeType(TreeType treeType) {
        switch (treeType) {
            case OAK:
                return org.bukkit.TreeType.TREE;
            case DARK_OAK:
                return org.bukkit.TreeType.DARK_OAK;
            case BIRCH:
                return org.bukkit.TreeType.BIRCH;
            case SPRUCE:
                return org.bukkit.TreeType.REDWOOD;
            case JUNGLE:
                return org.bukkit.TreeType.SMALL_JUNGLE;
            case ACACIA:
                return org.bukkit.TreeType.ACACIA;
            default:
                return org.bukkit.TreeType.TREE;
        }
    }

    private void grantXpForTree(Player player, TreeType treeType) {
        double xpValue = treeType.getXpValue();
        skillManager.addXP(player, SkillManager.Skill.WOODCUTTING, xpValue);
        player.sendActionBar(ChatColor.GOLD + "Woodcutting +" + xpValue);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }

    private boolean isAxe(Material material) {
        return material == Material.WOODEN_AXE ||
                material == Material.STONE_AXE ||
                material == Material.IRON_AXE ||
                material == Material.GOLDEN_AXE ||
                material == Material.DIAMOND_AXE ||
                material == Material.NETHERITE_AXE;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        ItemStack heldItem = player.getInventory().getItemInMainHand();

        // Prevent saplings from being broken
        if (isSapling(block.getType())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Saplings cannot be broken!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        if (isLog(block.getType())) {
            // Ensure the player is using an axe
            if (!isAxe(heldItem.getType())) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "You must use an axe to chop a tree down!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }

            TreeType treeType = TreeType.fromMaterial(block.getType());

            // If it's a type of tree we're tracking and the player's level isn't high enough, cancel the event
            if (treeType != null && skillManager.getSkillLevel(player, SkillManager.Skill.WOODCUTTING) < treeType.getRequiredLevel()) {
                player.sendMessage(ChatColor.RED + "You need to be level " + treeType.getRequiredLevel() + " to chop " + treeType.name().toLowerCase() + "!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                event.setCancelled(true);
                return;
            }

            // If the item is an axe, check the player's level
            AxeType axeType = AxeType.fromMaterial(heldItem.getType());
            if (axeType != null && skillManager.getSkillLevel(player, SkillManager.Skill.WOODCUTTING) < axeType.getRequiredLevel()) {
                player.sendMessage(ChatColor.RED + "You need a woodcutting level of " + axeType.getRequiredLevel() + " to use this " + axeType.name().toLowerCase().replace("_", " ") + " axe.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                event.setCancelled(true);
                return;
            }

            // Handle tree cutting and regrowth
            handleTreeCut(player, block, treeType);
            skillManager.saveSkillsToConfig();
            skillManager.loadSkillsFromConfig();

            // Cancel the default block break event to prevent default drops
            event.setCancelled(true);
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