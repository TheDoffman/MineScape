package org.hoffmantv.minescape.skills;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class Mining implements Listener {

    private final SkillManager skillManager;
    private final Plugin plugin;
    private final BukkitScheduler scheduler = Bukkit.getScheduler();
    private final Map<Material, OreData> oreDataMap = new EnumMap<>(Material.class);
    private final Map<Material, PickaxeData> pickaxeDataMap = new EnumMap<>(Material.class);

    /**
     * Constructor for Mining.
     *
     * @param skillManager The SkillManager instance.
     * @param miningConfig The ConfigurationSection for mining from skills.yml.
     * @param plugin       The main plugin instance.
     */
    public Mining(SkillManager skillManager, ConfigurationSection miningConfig, Plugin plugin) {
        this.skillManager = skillManager;
        this.plugin = plugin;
        loadMiningConfigs(miningConfig);
    }

    /**
     * Loads mining configurations for ores and pickaxes.
     *
     * @param miningConfig The ConfigurationSection for mining.
     */
    private void loadMiningConfigs(ConfigurationSection miningConfig) {
        if (miningConfig != null) {
            plugin.getLogger().info("Loading Mining Skill configurations...");

            // Load ores
            ConfigurationSection oresSection = miningConfig.getConfigurationSection("ores");
            if (oresSection != null) {
                for (String key : oresSection.getKeys(false)) {
                    ConfigurationSection oreConfig = oresSection.getConfigurationSection(key);
                    if (oreConfig != null) {
                        Material oreMaterial = Material.getMaterial(key.toUpperCase());
                        if (oreMaterial != null) {
                            int requiredLevel = oreConfig.getInt("requiredLevel", 1);
                            double xpValue = oreConfig.getDouble("xpValue", 0);
                            long respawnTime = oreConfig.getLong("respawnTime", 30);
                            String dropMaterialName = oreConfig.getString("dropMaterial", key);
                            Material dropMaterial = Material.getMaterial(dropMaterialName.toUpperCase());

                            if (dropMaterial == null) {
                                dropMaterial = oreMaterial; // Default to ore material if dropMaterial is invalid
                                plugin.getLogger().warning("Invalid dropMaterial for " + key + ". Defaulting to " + oreMaterial);
                            }

                            OreData oreData = new OreData(oreMaterial, dropMaterial, requiredLevel, xpValue, respawnTime);
                            oreDataMap.put(oreMaterial, oreData);
                            plugin.getLogger().info("Loaded Ore: " + oreMaterial + " | Required Level: " + requiredLevel + " | XP: " + xpValue + " | Respawn Time: " + respawnTime + "s | Drop: " + dropMaterial);
                        } else {
                            plugin.getLogger().warning("Invalid ore material in mining config: " + key);
                        }
                    }
                }
            } else {
                plugin.getLogger().warning("No 'ores' section found in skills.yml under 'skills.mining'");
            }

            // Load pickaxes
            ConfigurationSection pickaxesSection = miningConfig.getConfigurationSection("pickaxes");
            if (pickaxesSection != null) {
                for (String key : pickaxesSection.getKeys(false)) {
                    ConfigurationSection pickaxeConfig = pickaxesSection.getConfigurationSection(key);
                    if (pickaxeConfig != null) {
                        Material pickaxeMaterial = Material.getMaterial(key.toUpperCase());
                        if (pickaxeMaterial != null) {
                            int requiredLevel = pickaxeConfig.getInt("requiredLevel", 1);
                            PickaxeData pickaxeData = new PickaxeData(pickaxeMaterial, requiredLevel);
                            pickaxeDataMap.put(pickaxeMaterial, pickaxeData);
                            plugin.getLogger().info("Loaded Pickaxe: " + pickaxeMaterial + " | Required Level: " + requiredLevel);
                        } else {
                            plugin.getLogger().warning("Invalid pickaxe material in mining config: " + key);
                        }
                    }
                }
            } else {
                plugin.getLogger().warning("No 'pickaxes' section found in skills.yml under 'skills.mining'");
            }

            plugin.getLogger().info("Mining Skill configurations loaded successfully.");
        } else {
            plugin.getLogger().warning("Mining configuration section is null.");
        }
    }

    /**
     * Event handler for block breaking.
     *
     * @param event The BlockBreakEvent.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) {
            plugin.getLogger().fine("BlockBreakEvent is cancelled. Skipping...");
            return;
        }

        Block block = event.getBlock();
        Material blockMaterial = block.getType();
        Player player = event.getPlayer();
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        Material itemInHand = heldItem.getType();

        plugin.getLogger().fine("Player " + player.getName() + " is attempting to break block: " + blockMaterial + " with " + itemInHand);

        // Check if the held item is a pickaxe
        if (isPickaxe(itemInHand)) {
            OreData oreData = oreDataMap.get(blockMaterial);
            if (oreData != null) {
                // Player is using a pickaxe on a valid ore
                handleOreBreak(event, player, heldItem, oreData, block);
            } else {
                // Player is using a pickaxe on a non-ore block
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "You can only use a pickaxe on ores.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                plugin.getLogger().info("Player " + player.getName() + " attempted to use a pickaxe on non-ore block: " + blockMaterial);
            }
        }
        // If the player is not using a pickaxe, do nothing (allow other interactions)
    }

    /**
     * Handles the logic when a player breaks a valid ore.
     *
     * @param event     The BlockBreakEvent.
     * @param player    The player breaking the block.
     * @param heldItem  The pickaxe used.
     * @param oreData   The OreData for the block.
     * @param block     The block being broken.
     */
    private void handleOreBreak(BlockBreakEvent event, Player player, ItemStack heldItem, OreData oreData, Block block) {
        // Check player's Mining level for the ore
        int requiredOreLevel = oreData.getRequiredLevel();
        int playerMiningLevel = skillManager.getSkillLevel(player, SkillManager.Skill.MINING);

        plugin.getLogger().fine("Player " + player.getName() + " has Mining level: " + playerMiningLevel + " | Required for ore: " + requiredOreLevel);

        if (playerMiningLevel < requiredOreLevel) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You need Mining level " + requiredOreLevel + " to mine this ore.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            plugin.getLogger().info("Player " + player.getName() + " has Mining level " + playerMiningLevel + " which is insufficient for ore " + block.getType());
            return;
        }

        // Check pickaxe level requirement
        int requiredPickaxeLevel = getRequiredLevelForPickaxe(heldItem.getType());
        plugin.getLogger().fine("Pickaxe " + heldItem.getType() + " requires Mining level: " + requiredPickaxeLevel);

        if (playerMiningLevel < requiredPickaxeLevel) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You need Mining level " + requiredPickaxeLevel + " to use this pickaxe.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            plugin.getLogger().info("Player " + player.getName() + " has Mining level " + playerMiningLevel + " which is insufficient for pickaxe " + heldItem.getType());
            return;
        }

        // Award XP
        double xpEarned = oreData.getXpValue();
        skillManager.addXP(player, SkillManager.Skill.MINING, xpEarned);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        player.sendActionBar(ChatColor.GOLD + "Mining +" + xpEarned);
        plugin.getLogger().info("Player " + player.getName() + " mined " + block.getType() + " and earned " + xpEarned + " XP.");

        // Prevent normal drops
        event.setDropItems(false);

        // Create non-stackable item
        ItemStack dropItem = createNonStackableItem(oreData.getDropMaterial());
        Map<Integer, ItemStack> excess = player.getInventory().addItem(dropItem);
        if (!excess.isEmpty()) {
            // Handle excess items if inventory is full
            for (ItemStack excessStack : excess.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), excessStack);
                plugin.getLogger().info("Dropped excess item " + excessStack.getType() + " for player " + player.getName());
            }
        } else {
            plugin.getLogger().info("Dropped " + oreData.getDropMaterial() + " for player " + player.getName());
        }

        // Set block to air
        block.setType(Material.AIR);

        // Schedule ore respawn
        scheduler.runTaskLater(plugin, () -> {
            block.setType(oreData.getOreMaterial());
            plugin.getLogger().info("Respawned ore: " + oreData.getOreMaterial() + " at " + block.getLocation());
        }, toTicks(TimeUnit.SECONDS, oreData.getRespawnTime()));
    }

    /**
     * Event handler to prevent pickaxes from damaging entities.
     *
     * @param event The EntityDamageByEntityEvent.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();
            ItemStack heldItem = player.getInventory().getItemInMainHand();
            Material heldMaterial = heldItem.getType();

            if (isPickaxe(heldMaterial)) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "You cannot use a pickaxe to attack!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                plugin.getLogger().info("Player " + player.getName() + " attempted to attack with a pickaxe: " + heldMaterial);
            }
        }
    }

    /**
     * Checks if the given material is a registered pickaxe.
     *
     * @param material The material to check.
     * @return True if it's a pickaxe, false otherwise.
     */
    private boolean isPickaxe(Material material) {
        boolean isPickaxe = pickaxeDataMap.containsKey(material);
        plugin.getLogger().fine("Checking if material " + material + " is a pickaxe: " + isPickaxe);
        return isPickaxe;
    }

    /**
     * Retrieves the required Mining level for a given pickaxe.
     *
     * @param pickaxe The pickaxe material.
     * @return The required Mining level, or Integer.MAX_VALUE if not found.
     */
    private int getRequiredLevelForPickaxe(Material pickaxe) {
        PickaxeData data = pickaxeDataMap.get(pickaxe);
        int level = data != null ? data.getRequiredLevel() : Integer.MAX_VALUE;
        plugin.getLogger().fine("Required level for pickaxe " + pickaxe + ": " + level);
        return level;
    }

    /**
     * Creates a non-stackable item by setting a unique CustomModelData.
     *
     * @param material The material of the item.
     * @return The non-stackable ItemStack.
     */
    private ItemStack createNonStackableItem(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE));
            item.setItemMeta(meta);
        }
        plugin.getLogger().fine("Created non-stackable item: " + material);
        return item;
    }

    /**
     * Converts a duration in the specified TimeUnit to ticks.
     *
     * @param timeUnit The time unit of the duration.
     * @param duration The duration value.
     * @return The duration converted to ticks.
     */
    private long toTicks(TimeUnit timeUnit, long duration) {
        long ticks = timeUnit.toSeconds(duration) * 20;
        plugin.getLogger().fine("Converted " + duration + " " + timeUnit + " to " + ticks + " ticks.");
        return ticks;
    }

    /**
     * Inner class to store ore-related data.
     */
    private static class OreData {
        private final Material oreMaterial;
        private final Material dropMaterial;
        private final int requiredLevel;
        private final double xpValue;
        private final long respawnTime;

        public OreData(Material oreMaterial, Material dropMaterial, int requiredLevel, double xpValue, long respawnTime) {
            this.oreMaterial = oreMaterial;
            this.dropMaterial = dropMaterial;
            this.requiredLevel = requiredLevel;
            this.xpValue = xpValue;
            this.respawnTime = respawnTime;
        }

        public Material getOreMaterial() {
            return oreMaterial;
        }

        public Material getDropMaterial() {
            return dropMaterial;
        }

        public int getRequiredLevel() {
            return requiredLevel;
        }

        public double getXpValue() {
            return xpValue;
        }

        public long getRespawnTime() {
            return respawnTime;
        }
    }

    /**
     * Inner class to store pickaxe-related data.
     */
    private static class PickaxeData {
        private final Material pickaxeMaterial;
        private final int requiredLevel;

        public PickaxeData(Material pickaxeMaterial, int requiredLevel) {
            this.pickaxeMaterial = pickaxeMaterial;
            this.requiredLevel = requiredLevel;
        }

        public Material getPickaxeMaterial() {
            return pickaxeMaterial;
        }

        public int getRequiredLevel() {
            return requiredLevel;
        }
    }
}