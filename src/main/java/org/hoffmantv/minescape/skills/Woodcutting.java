package org.hoffmantv.minescape.skills;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class Woodcutting implements Listener {

    private final SkillManager skillManager;
    private final Plugin plugin;
    private final Map<Material, TreeData> treeDataMap = new HashMap<>();
    private final Map<Material, AxeData> axeDataMap = new HashMap<>();
    private final Set<Material> leafMaterials = EnumSet.of(
            Material.OAK_LEAVES, Material.BIRCH_LEAVES, Material.SPRUCE_LEAVES,
            Material.JUNGLE_LEAVES, Material.ACACIA_LEAVES, Material.DARK_OAK_LEAVES,
            Material.MANGROVE_LEAVES, Material.CHERRY_LEAVES
    );

    private final Map<Block, List<ChoppedTreeData>> choppedTrees = new HashMap<>();

    public Woodcutting(SkillManager skillManager, ConfigurationSection woodcuttingConfig, Plugin plugin) {
        this.skillManager = skillManager;
        this.plugin = plugin;
        loadWoodcuttingConfigs(woodcuttingConfig);
    }

    private void loadWoodcuttingConfigs(ConfigurationSection woodcuttingConfig) {
        if (woodcuttingConfig != null) {
            plugin.getLogger().info("Loading Woodcutting Skill configurations...");

            ConfigurationSection treesSection = woodcuttingConfig.getConfigurationSection("trees");
            if (treesSection != null) {
                for (String key : treesSection.getKeys(false)) {
                    Material treeMaterial = Material.getMaterial(key.toUpperCase());
                    if (treeMaterial != null) {
                        int requiredLevel = treesSection.getInt(key + ".requiredLevel");
                        double xp = treesSection.getDouble(key + ".xp");
                        long respawnTime = treesSection.getLong(key + ".respawnTime", 30);
                        treeDataMap.put(treeMaterial, new TreeData(treeMaterial, requiredLevel, xp, respawnTime));
                    }
                }
            }

            ConfigurationSection axesSection = woodcuttingConfig.getConfigurationSection("axes");
            if (axesSection != null) {
                for (String key : axesSection.getKeys(false)) {
                    Material axeMaterial = Material.getMaterial(key.toUpperCase());
                    if (axeMaterial != null) {
                        int requiredLevel = axesSection.getInt(key + ".requiredLevel");
                        axeDataMap.put(axeMaterial, new AxeData(axeMaterial, requiredLevel));
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;  // Only proceed for left-click actions
        }

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        Material axeMaterial = itemInHand.getType();
        Material blockMaterial = block.getType();

        TreeData treeData = treeDataMap.get(blockMaterial);
        AxeData axeData = axeDataMap.get(axeMaterial);

        if (treeData != null && axeData != null) {
            int playerLevel = skillManager.getSkillLevel(player, SkillManager.Skill.WOODCUTTING);

            if (playerLevel < treeData.getRequiredLevel()) {
                player.sendMessage(ChatColor.RED + "You need Woodcutting level " + treeData.getRequiredLevel() + " to chop this tree.");
                event.setCancelled(true);
                return;
            }

            if (playerLevel < axeData.getRequiredLevel()) {
                player.sendMessage(ChatColor.RED + "You need Woodcutting level " + axeData.getRequiredLevel() + " to use this axe.");
                event.setCancelled(true);
                return;
            }

            event.setCancelled(true);
            simulateChopping(player, block, itemInHand, treeData);
        }
    }

    private void simulateChopping(Player player, Block block, ItemStack axe, TreeData treeData) {
        int choppingDelay = 20 * (5 + new Random().nextInt(6)); // Random delay between 5 and 10 seconds

        player.sendMessage(ChatColor.YELLOW + "You begin chopping the " + block.getType().name().toLowerCase().replace("_", " ") + "...");
        playChoppingEffects(player, block);

        new BukkitRunnable() {
            int counter = 0;

            @Override
            public void run() {
                if (counter >= choppingDelay / 20) {
                    chopTreeAndLeaves(player, block, treeData);
                    cancel();
                } else {
                    playChoppingEffects(player, block);
                    counter++;
                }
            }
        }.runTaskTimer(plugin, 0, 20); // Runs every second
    }

    private void playChoppingEffects(Player player, Block block) {
        player.playSound(player.getLocation(), Sound.BLOCK_WOOD_HIT, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.BLOCK_CRACK, block.getLocation().add(0.5, 0.5, 0.5), 10, block.getBlockData());
    }

    private void chopTreeAndLeaves(Player player, Block block, TreeData treeData) {
        List<ChoppedTreeData> choppedTreeData = getTreeAndLeaves(block);

        // Store tree data for respawning later
        choppedTrees.put(block, choppedTreeData);

        // Remove logs and leaves
        for (ChoppedTreeData treeDataBlock : choppedTreeData) {
            treeDataBlock.getBlock().setType(Material.AIR);  // Remove block
        }

        // Grant XP and show in action bar
        skillManager.addXP(player, SkillManager.Skill.WOODCUTTING, treeData.getXp());
        player.sendActionBar(ChatColor.GOLD + "Woodcutting +" + treeData.getXp());

        // Play sound and particle effects
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, block.getLocation(), 10);

        // Get the dirt block directly below the tree base
        final Block dirtBlock = block.getRelative(BlockFace.DOWN);
        if (dirtBlock.getType() != Material.DIRT && dirtBlock.getType() != Material.GRASS_BLOCK) {
            // If the block below isn't dirt or grass, skip placing the sapling
            return;
        }

        // Schedule sapling placement 1 second after tree removal
        new BukkitRunnable() {
            @Override
            public void run() {
                Material saplingType = getSaplingTypeForLog(block.getType());
                if (saplingType != null) {
                    dirtBlock.setType(saplingType);  // Place sapling on the dirt block
                }
            }
        }.runTaskLater(plugin, 20L);  // 20 ticks = 1 second delay

        // Respawn the tree after the specified respawn time
        scheduleTreeRespawn(block, treeData, dirtBlock);
    }

    private void scheduleTreeRespawn(Block block, TreeData treeData, Block dirtBlock) {
        long respawnTimeTicks = treeData.getRespawnTime() * 20; // Convert seconds to ticks

        new BukkitRunnable() {
            @Override
            public void run() {
                List<ChoppedTreeData> originalTree = choppedTrees.get(block);
                if (originalTree != null) {
                    for (ChoppedTreeData treeDataBlock : originalTree) {
                        Block treeBlock = treeDataBlock.getBlock();
                        treeBlock.setType(treeDataBlock.getMaterial());
                        treeBlock.setBlockData(treeDataBlock.getBlockData());
                    }
                    choppedTrees.remove(block); // Clean up after respawn
                }
            }
        }.runTaskLater(plugin, respawnTimeTicks);
    }

    private ItemStack createNonStackableLog(Material logType) {
        // Create the log item
        ItemStack logItem = new ItemStack(logType);
        ItemMeta meta = logItem.getItemMeta();

        if (meta != null) {
            // Set the custom model data to make it non-stackable
            meta.setCustomModelData(ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE));

            // Set the display name based on the log type
            String logName = getLogName(logType);
            meta.setDisplayName(ChatColor.GOLD + logName);

            // Optionally, add lore to the item
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "A freshly chopped " + logName);
            meta.setLore(lore);

            // Apply the custom meta to the log item
            logItem.setItemMeta(meta);
        }

        return logItem;
    }
    private String getLogName(Material logType) {
        // Map the log type to a friendly name
        return switch (logType) {
            case OAK_LOG -> "Oak Log";
            case BIRCH_LOG -> "Birch Log";
            case SPRUCE_LOG -> "Spruce Log";
            case JUNGLE_LOG -> "Jungle Log";
            case ACACIA_LOG -> "Acacia Log";
            case DARK_OAK_LOG -> "Dark Oak Log";
            case MANGROVE_LOG -> "Mangrove Log";
            case CHERRY_LOG -> "Cherry Log";
            default -> "Log";
        };
    }
    private List<ChoppedTreeData> getTreeAndLeaves(Block block) {
        List<ChoppedTreeData> treeBlocks = new ArrayList<>();
        Material logType = block.getType();
        Set<Block> checkedBlocks = new HashSet<>();

        traverseTree(block, logType, treeBlocks, checkedBlocks);

        return treeBlocks;
    }

    private void traverseTree(Block block, Material logType, List<ChoppedTreeData> treeBlocks, Set<Block> checkedBlocks) {
        Queue<Block> blocksToCheck = new LinkedList<>();
        blocksToCheck.add(block);

        while (!blocksToCheck.isEmpty()) {
            Block currentBlock = blocksToCheck.poll();
            if (checkedBlocks.contains(currentBlock)) {
                continue;
            }

            checkedBlocks.add(currentBlock);
            Material currentMaterial = currentBlock.getType();

            if (currentMaterial == logType || leafMaterials.contains(currentMaterial)) {
                treeBlocks.add(new ChoppedTreeData(currentBlock, currentBlock.getBlockData()));

                for (int x = -1; x <= 1; x++) {
                    for (int y = -1; y <= 1; y++) {
                        for (int z = -1; z <= 1; z++) {
                            Block relative = currentBlock.getRelative(x, y, z);
                            if (!checkedBlocks.contains(relative)) {
                                blocksToCheck.add(relative);
                            }
                        }
                    }
                }
            }
        }
    }

    private void scheduleTreeRespawn(Block block, TreeData treeData) {
        long respawnTimeTicks = treeData.getRespawnTime() * 20; // Convert seconds to ticks

        new BukkitRunnable() {
            @Override
            public void run() {
                List<ChoppedTreeData> originalTree = choppedTrees.get(block);
                if (originalTree != null) {
                    for (ChoppedTreeData treeDataBlock : originalTree) {
                        Block treeBlock = treeDataBlock.getBlock();
                        treeBlock.setType(treeDataBlock.getMaterial());
                        treeBlock.setBlockData(treeDataBlock.getBlockData());
                    }
                    choppedTrees.remove(block);
                }
            }
        }.runTaskLater(plugin, respawnTimeTicks);
    }

    private Material getSaplingTypeForLog(Material log) {
        return switch (log) {
            case OAK_LOG -> Material.OAK_SAPLING;
            case BIRCH_LOG -> Material.BIRCH_SAPLING;
            case SPRUCE_LOG -> Material.SPRUCE_SAPLING;
            case JUNGLE_LOG -> Material.JUNGLE_SAPLING;
            case ACACIA_LOG -> Material.ACACIA_SAPLING;
            case DARK_OAK_LOG -> Material.DARK_OAK_SAPLING;
            case MANGROVE_LOG -> Material.MANGROVE_PROPAGULE;
            case CHERRY_LOG -> Material.CHERRY_SAPLING;
            default -> null;
        };
    }

    private static class TreeData {
        private final Material treeMaterial;
        private final int requiredLevel;
        private final double xp;
        private final long respawnTime;

        public TreeData(Material treeMaterial, int requiredLevel, double xp, long respawnTime) {
            this.treeMaterial = treeMaterial;
            this.requiredLevel = requiredLevel;
            this.xp = xp;
            this.respawnTime = respawnTime;
        }

        public Material getTreeMaterial() {
            return treeMaterial;
        }

        public int getRequiredLevel() {
            return requiredLevel;
        }

        public double getXp() {
            return xp;
        }

        public long getRespawnTime() {
            return respawnTime;
        }
    }

    private static class AxeData {
        private final Material axeMaterial;
        private final int requiredLevel;

        public AxeData(Material axeMaterial, int requiredLevel) {
            this.axeMaterial = axeMaterial;
            this.requiredLevel = requiredLevel;
        }

        public Material getAxeMaterial() {
            return axeMaterial;
        }

        public int getRequiredLevel() {
            return requiredLevel;
        }
    }

    private static class ChoppedTreeData {
        private final Block block;
        private final BlockData blockData;

        public ChoppedTreeData(Block block, BlockData blockData) {
            this.block = block;
            this.blockData = blockData;
        }

        public Block getBlock() {
            return block;
        }

        public BlockData getBlockData() {
            return blockData;
        }

        public Material getMaterial() {
            return block.getType();
        }
    }
}