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
import org.bukkit.inventory.meta.ItemMeta;
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
    private void handleTreeRemoval(Player player, Block block, TreeType treeType) {
        // Capture the stump location
        Location stump = removeTree(block);

        // Check if the block below is dirt or grass
        Material blockBelowType = stump.getBlock().getRelative(BlockFace.DOWN).getType();

        if (isPlantableBase(blockBelowType)) {
            // Plant the sapling at the stump location
            if (treeType != null) {
                stump.getBlock().setType(treeType.getSapling());
                scheduleTreeGrowth(stump, treeType);
            }
        }

        // Provide XP to the player
        if (treeType != null) {
            double xpValue = treeType.getXpValue();
            skillManager.addXP(player, SkillManager.Skill.WOODCUTTING, xpValue);

            // Send a message to the player about the tree they cut and the XP they received
            player.sendActionBar(ChatColor.GOLD + "WoodCutting +" + xpValue);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
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
            }

            // We're considering all six faces (up, down, north, south, east, west) for both logs and leaves.
            for (BlockFace face : BlockFace.values()) {
                Block neighbor = current.getRelative(face);

                if (!visited.contains(neighbor)) {
                    if (isLeaf(neighbor.getType())) {
                        neighbor.setType(Material.AIR);
                        queue.add(neighbor); // Add the leaf to the queue to ensure all attached leaves are removed.
                    } else if (isLog(neighbor.getType())) {
                        queue.add(neighbor);
                    }
                }
            }

            // We'll change the block type to AIR after processing its neighbors.
            // This ensures both logs and leaves are removed without any drops.
            current.setType(Material.AIR);
        }

        return stumpLocation; // Return the stump's location
    }
    private void removeTreeAndGiveLog(Player player, Block brokenBlock) {
        // Remove other parts of the tree...

        // Give player a non-stackable log
        ItemStack log = new ItemStack(brokenBlock.getType(), 1); // Only one log
        ItemMeta meta = log.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(UUID.randomUUID().toString()); // A unique name to prevent stacking
            log.setItemMeta(meta);
        }

        player.getInventory().addItem(log);
    }
    public void growNiceTree(Location location, Material logMaterial, Material leavesMaterial) {
        World world = location.getWorld();

        // Random height between 5 to 7 for some variety
        int height = new Random().nextInt(3) + 5;

        // Base log of the tree
        for (int y = 0; y < height; y++) {
            world.getBlockAt(location.getBlockX(), location.getBlockY() + y, location.getBlockZ()).setType(logMaterial);
        }

        // Leaves, a combination of loops and conditions for a more natural shape
        for (int x = -3; x <= 3; x++) {
            for (int y = height - 3; y <= height; y++) {
                for (int z = -3; z <= 3; z++) {
                    // Condition for the general shape of the leaves
                    if (Math.abs(x) + Math.abs(y - (height - 1.5)) * 2 + Math.abs(z) < 3.5) {
                        Block block = world.getBlockAt(location.getBlockX() + x, location.getBlockY() + y, location.getBlockZ() + z);
                        if(block.getType() == Material.AIR) {
                            block.setType(leavesMaterial);
                        }
                    }
                    // Adding some randomness for leaves, so they don't form a perfect shape
                    if (new Random().nextInt(3) == 0 && Math.abs(x) < 2 && Math.abs(z) < 2 && y == height - 3) {
                        Block block = world.getBlockAt(location.getBlockX() + x, location.getBlockY() + y, location.getBlockZ() + z);
                        if(block.getType() == Material.AIR) {
                            block.setType(leavesMaterial);
                        }
                    }
                }
            }
        }
    }
    public void growNiceOakTree(Location location) {
        growNiceTree(location, Material.OAK_LOG, Material.OAK_LEAVES);
    }
    public void growNiceDarkOakTree(Location location) {
        growNiceTree(location, Material.DARK_OAK_LOG, Material.DARK_OAK_LEAVES);
    }
    public void growNiceBirchTree(Location location) {
        growNiceTree(location, Material.BIRCH_LOG, Material.BIRCH_LEAVES);
    }
    public void growNiceSpruceTree(Location location) {
        growNiceTree(location, Material.SPRUCE_LOG, Material.SPRUCE_LEAVES);
    }
    public void growNiceJungleTree(Location location) {
        growNiceTree(location, Material.JUNGLE_LOG, Material.JUNGLE_LEAVES);
    }
    public void growNiceAcaciaTree(Location location) {
        growNiceTree(location, Material.ACACIA_LOG, Material.ACACIA_LEAVES);
    }
    private void scheduleTreeGrowth(Location location, TreeType treeType) {
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            if (location.getBlock().getType() == treeType.getSapling()) {
                if (treeType == TreeType.OAK) {
                    growNiceOakTree(location);
                }
                else if (treeType == TreeType.DARK_OAK) {
                    growNiceDarkOakTree(location);
                }
                else if (treeType == TreeType.BIRCH) {
                    growNiceBirchTree(location);
                }
                else if (treeType == TreeType.SPRUCE) {
                    growNiceSpruceTree(location);
                }
                else if (treeType == TreeType.JUNGLE) {
                    growNiceJungleTree(location);
                } else if (treeType == TreeType.ACACIA) {
                    growNiceAcaciaTree(location);
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
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        ItemStack heldItem = player.getInventory().getItemInMainHand();

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
            // Logic to remove the entire tree but keep the stump and remove leaves
            removeTreeAndGiveLog(player, block);
            handleTreeRemoval(player, block, treeType);
            skillManager.saveSkillsToConfig();
            skillManager.loadSkillsFromConfig();
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
}
