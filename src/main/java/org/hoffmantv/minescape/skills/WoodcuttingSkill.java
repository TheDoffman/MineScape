package org.hoffmantv.minescape.skills;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.hoffmantv.minescape.managers.SkillManager;


import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

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
        DARK_OAK(Material.DARK_OAK_LOG, 10, 20, Material.DARK_OAK_SAPLING, 300L), //1 5 Seconds
        BIRCH(Material.BIRCH_LOG, 20, 12, Material.BIRCH_SAPLING,500L), // 25 Seconds
        SPRUCE(Material.SPRUCE_LOG, 30, 14, Material.SPRUCE_SAPLING,1000L), // 50 Seconds
        JUNGLE(Material.JUNGLE_LOG, 40, 16, Material.JUNGLE_SAPLING,1200L), // 1 Min
        ACACIA(Material.ACACIA_LOG, 50, 18, Material.ACACIA_SAPLING,2400L); // 2 Min

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
        public org.bukkit.TreeType getBukkitTreeType() {
            switch(this) {
                case OAK:
                    return org.bukkit.TreeType.TREE;
                case DARK_OAK:
                    return org.bukkit.TreeType.DARK_OAK;
                case BIRCH:
                    return org.bukkit.TreeType.BIRCH;
                case SPRUCE:
                    return org.bukkit.TreeType.TALL_REDWOOD;
                case JUNGLE:
                    return org.bukkit.TreeType.SMALL_JUNGLE;
                case ACACIA:
                    return org.bukkit.TreeType.ACACIA;
                default:
                    return null;
            }
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
    }
    private boolean isPlantableBase(Material material) {
        return material == Material.DIRT || material == Material.GRASS_BLOCK;
    }
    private void handleTreeRemoval(Player player, Block block, TreeType treeType) {
        // Capture the stump location
        Location stump = removeTree(block);

        // Logging for debugging
        plugin.getLogger().info("Stump Location: " + stump.toString());

        // Check if the block below is dirt or grass
        Material blockBelowType = stump.getBlock().getRelative(BlockFace.DOWN).getType();
        plugin.getLogger().info("Block below stump: " + blockBelowType.name());

        if (isPlantableBase(blockBelowType)) {
            // Logging for debugging
            plugin.getLogger().info("Trying to plant sapling for tree type: " + treeType.name());

            // Plant the sapling at the stump location
            if (treeType != null) {
                stump.getBlock().setType(treeType.getSapling());

                // Logging for debugging
                plugin.getLogger().info("Sapling should be planted at: " + stump.toString());
                plugin.getLogger().info("Actual block type after attempt: " + stump.getBlock().getType().name());

                scheduleTreeGrowth(stump, treeType);
            }
        } else {
            plugin.getLogger().info("The block below is not suitable for planting a sapling.");
        }

        // Provide XP to the player
        if (treeType != null) {
            double xpValue = treeType.getXpValue();
            skillManager.addXP(player, SkillManager.Skill.WOODCUTTING, xpValue);

            // Send a message to the player about the tree they cut and the XP they received
            player.sendMessage(ChatColor.GREEN + "You cut down a " + treeType.name().toLowerCase() + " tree and received " + xpValue + " XP!");
        }
    }


    private Location removeTree(Block startBlock) {
        Set<Block> visited = new HashSet<>();
        Queue<Block> queue = new LinkedList<>();
        Location stumpLocation = startBlock.getLocation();

        queue.add(startBlock);

        while (!queue.isEmpty()) {
            Block current = queue.poll();
            visited.add(current);

            if(isLog(current.getType())) {
                if (current.getLocation().getY() < stumpLocation.getY()) {
                    stumpLocation = current.getLocation();
                }
                for (BlockFace face : BlockFace.values()) {
                    Block neighbor = current.getRelative(face);
                    if (!visited.contains(neighbor)) {
                        if (isLeaf(neighbor.getType())) {
                            neighbor.breakNaturally();
                        } else if (isLog(neighbor.getType())) {
                            queue.add(neighbor);
                        }
                    }
                }
                current.breakNaturally();
            }
        }
        return stumpLocation; // Return the stump's location
    }

    private void scheduleTreeGrowth(Location location, TreeType treeType) {
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            @Override
            public void run() {
                // Ensure that the block at the location is still the expected sapling
                if (location.getBlock().getType() == treeType.getSapling()) {

                    // Directly use the bukkit tree type defined in the TreeType enum
                    org.bukkit.TreeType bukkitTreeType = treeType.getBukkitTreeType();

                    if (bukkitTreeType != null) {
                        boolean success = location.getWorld().generateTree(location, bukkitTreeType);
                        if (!success) {
                            plugin.getLogger().info("Tree growth was unsuccessful for " + treeType.name() + " at " + location);
                        }
                    }
                } else {
                    plugin.getLogger().info("Scheduled tree growth failed: Expected sapling not found. Current block type: " + location.getBlock().getType());
                }
            }
        }, treeType.getGrowthDelayTicks());
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

    private boolean isAxe(Material material) {
        return material == Material.WOODEN_AXE ||
                material == Material.STONE_AXE ||
                material == Material.IRON_AXE ||
                material == Material.GOLDEN_AXE ||
                material == Material.DIAMOND_AXE ||
                material == Material.NETHERITE_AXE;
    }

    private void removeLeavesBFS(Block startBlock) {
        Set<Block> visited = new HashSet<>();
        Queue<Block> queue = new LinkedList<>();

        queue.add(startBlock);

        while (!queue.isEmpty()) {
            Block current = queue.poll();
            visited.add(current);

            for (BlockFace face : BlockFace.values()) {
                Block neighbor = current.getRelative(face);
                if (!visited.contains(neighbor) && isLeaf(neighbor.getType())) {
                    neighbor.breakNaturally();
                    queue.add(neighbor);
                }
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        ItemStack heldItem = player.getInventory().getItemInMainHand();

        if (isLog(block.getType())) {
            // Ensure the player is using an axe
            if (!isAxe(heldItem.getType())) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Use an axe to chop down trees!");
                return;
            }

            TreeType treeType = TreeType.fromMaterial(block.getType());

            // If it's a type of tree we're tracking and the player's level isn't high enough, cancel the event
            if (treeType != null && skillManager.getSkillLevel(player, SkillManager.Skill.WOODCUTTING) < treeType.getRequiredLevel()) {
                player.sendMessage(ChatColor.RED + "You need a higher woodcutting level to chop this tree!");
                event.setCancelled(true);
                return;
            }

            // Logic to remove the entire tree but keep the stump and remove leaves
            handleTreeRemoval(player, block, treeType);
        }
    }
}
