package org.hoffmantv.minescape.skills;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class Mining implements Listener {

    private final SkillManager skillManager;
    private final Plugin plugin;
    private final BukkitScheduler scheduler = Bukkit.getScheduler();
    private final Map<Material, OreData> oreDataMap = new EnumMap<>(Material.class);
    private final Map<Material, PickaxeData> pickaxeDataMap = new EnumMap<>(Material.class);

    public Mining(SkillManager skillManager, ConfigurationSection miningConfig, Plugin plugin) {
        this.skillManager = skillManager;
        this.plugin = plugin;
        loadMiningConfigs(miningConfig);
    }

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

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Material blockMaterial = block.getType();
        Player player = event.getPlayer();
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        Material itemInHand = heldItem.getType();

        Set<Material> oreMaterials = EnumSet.of(
                Material.COAL_ORE, Material.IRON_ORE, Material.GOLD_ORE,
                Material.DIAMOND_ORE, Material.EMERALD_ORE, Material.LAPIS_ORE,
                Material.REDSTONE_ORE, Material.DEEPSLATE_COAL_ORE, Material.DEEPSLATE_IRON_ORE,
                Material.DEEPSLATE_GOLD_ORE, Material.DEEPSLATE_DIAMOND_ORE,
                Material.DEEPSLATE_EMERALD_ORE, Material.DEEPSLATE_LAPIS_ORE,
                Material.DEEPSLATE_REDSTONE_ORE, Material.NETHER_QUARTZ_ORE,
                Material.NETHER_GOLD_ORE, Material.ANCIENT_DEBRIS
        );

        if (!oreMaterials.contains(blockMaterial)) {
            return;
        }

        if (isPickaxe(itemInHand)) {
            OreData oreData = oreDataMap.get(blockMaterial);
            if (oreData != null) {
                handleOreBreak(event, player, heldItem, oreData, block);
            } else {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "You can only use a pickaxe on ores.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
        } else {
            if (oreMaterials.contains(blockMaterial)) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "You must use a pickaxe to mine ores!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
        }
    }

    private void handleOreBreak(BlockBreakEvent event, Player player, ItemStack heldItem, OreData oreData, Block block) {
        int requiredOreLevel = oreData.getRequiredLevel();
        int playerMiningLevel = skillManager.getSkillLevel(player, SkillManager.Skill.MINING);

        if (playerMiningLevel < requiredOreLevel) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You need Mining level " + requiredOreLevel + " to mine this ore.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        int requiredPickaxeLevel = getRequiredLevelForPickaxe(heldItem.getType());

        if (playerMiningLevel < requiredPickaxeLevel) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You need Mining level " + requiredPickaxeLevel + " to use this pickaxe.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        int miningDelay = ThreadLocalRandom.current().nextInt(3, 11) * 20; // Convert seconds to ticks (20 ticks = 1 second)

        player.sendMessage(ChatColor.YELLOW + "You begin mining the " + oreData.getOreMaterial().name().toLowerCase().replace("_", " ") + "...");

        startMiningAnimation(player, block, miningDelay / 20);

        scheduler.runTaskLater(plugin, () -> completeMining(player, heldItem, oreData, block), miningDelay);
    }

    private void completeMining(Player player, ItemStack heldItem, OreData oreData, Block block) {
        double miningSuccessChance = calculateMiningSuccessChance(skillManager.getSkillLevel(player, SkillManager.Skill.MINING), oreData.getRequiredLevel(), heldItem.getType());

        if (!(ThreadLocalRandom.current().nextDouble(100) < miningSuccessChance)) {
            player.sendMessage(ChatColor.GRAY + "You failed to mine the ore.");
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
            return;  // Block remains intact, mining fails
        }

        double xpEarned = oreData.getXpValue();
        skillManager.addXP(player, SkillManager.Skill.MINING, xpEarned);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        player.sendActionBar(ChatColor.GOLD + "Mining +" + xpEarned);

        block.getWorld().dropItemNaturally(block.getLocation(), createNonStackableItem(oreData.getDropMaterial()));
        block.setType(Material.AIR);
        scheduleOreRespawn(block, oreData);
        player.playSound(player.getLocation(), Sound.BLOCK_STONE_HIT, 1.0f, 1.0f);
        block.getWorld().spawnParticle(Particle.BLOCK_CRACK, block.getLocation(), 10, block.getBlockData());
    }

    private void startMiningAnimation(Player player, Block block, int miningTimeSeconds) {
        for (int i = 0; i < miningTimeSeconds; i++) {
            final int delay = i;
            scheduler.runTaskLater(plugin, () -> {
                player.getWorld().spawnParticle(Particle.BLOCK_CRACK, block.getLocation().add(0.5, 0.5, 0.5), 10, block.getBlockData());
                player.playSound(player.getLocation(), Sound.BLOCK_STONE_HIT, 1.0f, 1.0f);
            }, delay * 20); // Schedule every second (20 ticks = 1 second)
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;  // Only proceed for left-click actions (simulating mining)
        }

        Block block = event.getClickedBlock();
        if (block == null) {
            return;  // Block must exist
        }

        Material blockMaterial = block.getType();
        Player player = event.getPlayer();
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        Material itemInHand = heldItem.getType();

        event.setCancelled(true);

        if (isPickaxe(itemInHand)) {
            OreData oreData = oreDataMap.get(blockMaterial);
            if (oreData != null) {
                handleOreMining(player, heldItem, oreData, block);
            } else {
                player.sendMessage(ChatColor.RED + "You can only use a pickaxe on ores.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
        }
    }

    private void handleOreMining(Player player, ItemStack heldItem, OreData oreData, Block block) {
        int requiredOreLevel = oreData.getRequiredLevel();
        int playerMiningLevel = skillManager.getSkillLevel(player, SkillManager.Skill.MINING);

        if (playerMiningLevel < requiredOreLevel) {
            player.sendMessage(ChatColor.RED + "You need Mining level " + requiredOreLevel + " to mine this ore.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        int requiredPickaxeLevel = getRequiredLevelForPickaxe(heldItem.getType());

        if (playerMiningLevel < requiredPickaxeLevel) {
            player.sendMessage(ChatColor.RED + "You need Mining level " + requiredPickaxeLevel + " to use this pickaxe.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        int miningDelay = ThreadLocalRandom.current().nextInt(3, 11) * 20; // Convert seconds to ticks (20 ticks = 1 second)

        player.sendMessage(ChatColor.YELLOW + "You begin mining the " + oreData.getOreMaterial().name().toLowerCase().replace("_", " ") + "...");

        startMiningAnimation(player, block, miningDelay / 20);

        scheduler.runTaskLater(plugin, () -> completeMining(player, heldItem, oreData, block), miningDelay);
    }

    private ItemStack createNonStackableItem(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setCustomModelData(ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE));
            String displayName = getCustomDisplayName(material);
            meta.setDisplayName(ChatColor.GOLD + displayName);
            meta.setLore(getCustomLore(material));
            item.setItemMeta(meta);
        }

        return item;
    }

    private String getCustomDisplayName(Material material) {
        switch (material) {
            case COAL:
                return "Coal";
            case IRON_ORE:
                return "Iron Ore";
            case GOLD_ORE:
                return "Gold Ore";
            case DIAMOND:
                return "Uncut Diamond";
            case EMERALD:
                return "Uncut Emerald";
            case NETHERITE_SCRAP:
                return "Ancient Debris";
            case QUARTZ:
                return "Quartz Crystal";
            case COPPER_ORE:
                return "Copper Ore";
            default:
                return "Mined Resource";
        }
    }

    private List<String> getCustomLore(Material material) {
        List<String> lore = new ArrayList<>();

        switch (material) {
            case COAL:
                lore.add(ChatColor.GRAY + "A piece of coal, used to fuel");
                lore.add(ChatColor.GRAY + "forges and smelt ores.");
                break;
            case IRON_ORE:
                lore.add(ChatColor.GRAY + "A lump of iron ore.");
                lore.add(ChatColor.GRAY + "It can be smelted into iron bars.");
                break;
            case GOLD_ORE:
                lore.add(ChatColor.YELLOW + "A chunk of gold ore.");
                lore.add(ChatColor.GRAY + "Used in crafting valuable items.");
                break;
            case DIAMOND:
                lore.add(ChatColor.AQUA + "An uncut diamond.");
                lore.add(ChatColor.GRAY + "Can be cut into a gem for jewelry.");
                break;
            case EMERALD:
                lore.add(ChatColor.GREEN + "An uncut emerald.");
                lore.add(ChatColor.GRAY + "Valuable to traders.");
                break;
            case NETHERITE_SCRAP:
                lore.add(ChatColor.DARK_PURPLE + "A fragment of ancient debris.");
                lore.add(ChatColor.GRAY + "Used to forge netherite.");
                break;
            case QUARTZ:
                lore.add(ChatColor.WHITE + "A quartz crystal from the Nether.");
                lore.add(ChatColor.GRAY + "Often used in decorations.");
                break;
            case COPPER_ORE:
                lore.add(ChatColor.GOLD + "A chunk of copper ore.");
                lore.add(ChatColor.GRAY + "Used to create bronze items.");
                break;
            default:
                lore.add(ChatColor.GRAY + "A resource mined from the earth.");
                break;
        }

        lore.add("");
        lore.add(ChatColor.DARK_GRAY + "Mined on: " + ChatColor.GOLD + java.time.LocalDate.now());

        return lore;
    }

    private double calculateMiningSuccessChance(int playerMiningLevel, int oreLevel, Material pickaxe) {
        double baseChance = 50.0;
        int pickaxeBonus = getPickaxeBonus(pickaxe);

        if (playerMiningLevel > oreLevel) {
            baseChance += (playerMiningLevel - oreLevel) * 2.0;
        } else if (playerMiningLevel < oreLevel) {
            baseChance -= (oreLevel - playerMiningLevel) * 2.0;
        }

        return Math.min(100, baseChance + pickaxeBonus);
    }

    private int getPickaxeBonus(Material pickaxe) {
        switch (pickaxe) {
            case NETHERITE_PICKAXE:
                return 15;
            case DIAMOND_PICKAXE:
                return 10;
            case IRON_PICKAXE:
                return 5;
            case STONE_PICKAXE:
                return 2;
            case GOLDEN_PICKAXE:
                return 0;
            case WOODEN_PICKAXE:
            default:
                return 0;
        }
    }

    private void scheduleOreRespawn(Block block, OreData oreData) {
        long baseRespawnTime = oreData.getRespawnTime();
        long randomVariation = ThreadLocalRandom.current().nextLong(0, baseRespawnTime / 2);

        long finalRespawnTime = baseRespawnTime + randomVariation;
        scheduler.runTaskLater(plugin, () -> {
            block.setType(oreData.getOreMaterial());
            plugin.getLogger().info("Respawned ore: " + oreData.getOreMaterial() + " at " + block.getLocation());
        }, finalRespawnTime * 20); // Convert seconds to ticks
    }

    private boolean isPickaxe(Material material) {
        return pickaxeDataMap.containsKey(material);
    }

    private int getRequiredLevelForPickaxe(Material pickaxe) {
        PickaxeData data = pickaxeDataMap.get(pickaxe);
        return data != null ? data.getRequiredLevel() : Integer.MAX_VALUE;
    }

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