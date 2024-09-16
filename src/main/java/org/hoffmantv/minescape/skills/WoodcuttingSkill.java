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

        OAK(Material.OAK_LOG, 1, 10, Material.OAK_SAPLING, 200L), // 10 Seconds
        DARK_OAK(Material.DARK_OAK_LOG, 10, 20, Material.DARK_OAK_SAPLING, 300L), // 15 Seconds
        BIRCH(Material.BIRCH_LOG, 20, 12, Material.BIRCH_SAPLING, 500L), // 25 Seconds
        SPRUCE(Material.SPRUCE_LOG, 30, 14, Material.SPRUCE_SAPLING, 1000L), // 50 Seconds
        JUNGLE(Material.JUNGLE_LOG, 40, 16, Material.JUNGLE_SAPLING, 1200L), // 1 Min
        ACACIA(Material.ACACIA_LOG, 50, 18, Material.ACACIA_SAPLING, 2400L); // 2 Min

        private final Material material;
        private final int requiredLevel;
        private final double xpValue;
        private final Material sapling;
        private final long growthDelayTicks;

        TreeType(Material material, int requiredLevel, double xpValue, Material sapling, long growthDelayTicks) {
            this.material = material;
            this.requiredLevel = requiredLevel;
            this.xpValue = xpValue;
            this.sapling = sapling;
            this.growthDelayTicks = growthDelayTicks;
        }

        public Material getMaterial() {
            return material;
        }

        public long getGrowthDelayTicks() {
            return growthDelayTicks;
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
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            if (location.getBlock().getType() == treeType.getSapling()) {
                growTree(location, treeType);
            }
        }, treeType.getGrowthDelayTicks());
    }

    private void growTree(Location location, TreeType treeType) {
        // Use Bukkit's built-in tree generation to simulate regrowth
        location.getWorld().generateTree(location, convertToBukkitTreeType(treeType));
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