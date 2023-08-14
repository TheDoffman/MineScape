package org.hoffmantv.minescape.skills;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.hoffmantv.minescape.managers.SkillManager;


import java.util.EnumMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.Random;

    public class MiningSkill implements Listener {

        private final SkillManager skillManager;
        private final EnumMap<Material, OreData> oreDataMap = new EnumMap<>(Material.class);
        private final BukkitScheduler scheduler = Bukkit.getScheduler();
        private final Plugin plugin;

        public MiningSkill(SkillManager skillManager, Plugin plugin) {
            this.skillManager = skillManager;
            this.plugin = plugin;
            initializeOreData();
        }

        @EventHandler
        public void onBlockBreak(BlockBreakEvent event) {
            // Check if the event is already cancelled
            if (event.isCancelled()) {
                return;
            }

            Material material = event.getBlock().getType();
            Player player = event.getPlayer();
            Material itemInHand = player.getInventory().getItemInMainHand().getType();

            OreData oreData = oreDataMap.get(material);

            // If the block being broken is an ore
            if (oreData != null) {
                if (!isPickaxe(itemInHand)) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "You need a pickaxe to mine this ore.");
                    return;
                }

                int requiredLevel = oreData.getRequiredLevel();
                int playerLevel = skillManager.getSkillLevel(player, SkillManager.Skill.MINING);

                if (playerLevel < requiredLevel) {
                    event.setCancelled(true);
                    return;
                }

                double xpEarned = oreData.getXpValue();
                skillManager.addXP(player, SkillManager.Skill.MINING, xpEarned);
                player.sendMessage(ChatColor.GREEN + "You earned " + xpEarned + " XP from mining this ore.");
                event.setDropItems(false);

// Save the updated XP value to the player's skill data
                skillManager.saveSkillsToConfig();

                // Create a new ItemStack with a custom attribute to make it non-stackable
                Material dropMaterial = getOreDrop(material);
                ItemStack item = new ItemStack(dropMaterial);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, new AttributeModifier(UUID.randomUUID(), "non-stackable", 0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND));
                    item.setItemMeta(meta);
                }
                player.getInventory().addItem(item);

                // Schedule a task to respawn the ore block after a set delay
                scheduler.runTaskLater(plugin, () -> {
                    event.getBlock().getLocation().getBlock().setType(material);
                }, toTicks(TimeUnit.SECONDS, oreData.getRespawnTime()));

                return;
            }

            // If the player is using a pickaxe on a block that's not an ore
            if (isPickaxe(itemInHand) && oreData == null) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "You can only use a pickaxe on ores.");
            }
            skillManager.saveSkillsToConfig();
        }

        // This will be added to your main class
        private int getRequiredLevelForPickaxe(Material pickaxe) {
            switch (pickaxe) {
                case WOODEN_PICKAXE:
                    return 1;  // Example: Level 1 required for wooden pickaxe
                case STONE_PICKAXE:
                    return 10;  // Example: Level 5 required for stone pickaxe
                case IRON_PICKAXE:
                    return 20; // and so on...
                case GOLDEN_PICKAXE:
                    return 15; // Gold is soft so maybe require less level?
                case DIAMOND_PICKAXE:
                    return 30;
                case NETHERITE_PICKAXE:
                    return 50;
                default:
                    return 0;  // No level required for other tools/items
            }
        }

        @EventHandler
        public void onPlayerInteract(PlayerInteractEvent event) {
            Action action = event.getAction();
            Block clickedBlock = event.getClickedBlock();

            if (action == Action.LEFT_CLICK_BLOCK && clickedBlock != null) {
                Material blockMaterial = clickedBlock.getType();
                Player player = event.getPlayer();
                ItemStack itemInHand = player.getInventory().getItemInMainHand();

                OreData oreData = oreDataMap.get(blockMaterial);

                if (oreData != null) {
                    int requiredLevel = oreData.getRequiredLevel();
                    int playerLevel = skillManager.getSkillLevel(player, SkillManager.Skill.MINING);

                    if (playerLevel < requiredLevel) {
                        player.sendMessage(ChatColor.RED + "You need level " + requiredLevel + " in mining to mine this ore.");
                        return;
                    }

                    if (isPickaxe(itemInHand.getType())) {
                        int requiredPickaxeLevel = getRequiredLevelForPickaxe(itemInHand.getType());
                        if (playerLevel < requiredPickaxeLevel) {
                            player.sendMessage(ChatColor.RED + "You need level " + requiredPickaxeLevel + " in mining to use this pickaxe.");
                            event.setCancelled(true);
                            return;
                        }

                        event.setCancelled(true);

                        // Play the block cracking animation
                        int maxStages = 8;
                        int randomStage = getRandomMiningStage(playerLevel, maxStages);

                        for (int stage = 0; stage < randomStage; stage++) {
                            final int currentStage = stage;
                            scheduler.runTaskLater(plugin, () -> clickedBlock.getWorld().playEffect(clickedBlock.getLocation(), Effect.STEP_SOUND, currentStage), stage * 4L);
                        }

                        // Break the block after the animation
                        scheduler.runTaskLater(plugin, () -> {
                            // Call BlockBreakEvent manually to handle XP gain, ore replacement, etc.
                            BlockBreakEvent blockBreakEvent = new BlockBreakEvent(clickedBlock, player);
                            Bukkit.getPluginManager().callEvent(blockBreakEvent);
                            if (!blockBreakEvent.isCancelled()) {
                                clickedBlock.setType(Material.AIR);  // 'Breaking' the block by setting it to air.
                            }
                        }, randomStage * 4L);  // 4 ticks per stage, for randomStage stages
                    } else {
                        player.sendMessage(ChatColor.RED + "You can only use a pickaxe on ores.");
                    }
                }
            }
        }

        private int getMaxStagesForSkill(int skillLevel) {
            // Example calculation
            if (skillLevel < 10) {
                return 8;
            } else if (skillLevel < 30) {
                return 7;
            } else if (skillLevel < 50) {
                return 6;
            } else if (skillLevel < 70) {
                return 5;
            } else {
                return 2;
            }
        }

        private int getRandomMiningStage(int playerLevel, int maxStages) {
            Random random = new Random();
            // The higher the player's level, the more weight we add to potentially reduce the stages required.
            double weight = 1 - (playerLevel / 99.0); // Assuming the max level is 100.
            int randomStage = (int) (weight * maxStages);
            return Math.max(1, randomStage); // Ensure the stage is at least 1.
        }

        @EventHandler
        public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
            if (event.getDamager() instanceof Player) {
                Player player = (Player) event.getDamager();
                Material itemInHand = player.getInventory().getItemInMainHand().getType();

                if (isPickaxe(itemInHand)) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "You cannot use a pickaxe to attack!");
                }
            }
        }

        private void initializeOreData() {
            oreDataMap.put(Material.COAL_ORE, new OreData(1, 10, 15));
            oreDataMap.put(Material.IRON_ORE, new OreData(10, 30, 30));
            oreDataMap.put(Material.GOLD_ORE, new OreData(20, 40, 30));
            oreDataMap.put(Material.DIAMOND_ORE, new OreData(30, 100, 30));
            oreDataMap.put(Material.EMERALD_ORE, new OreData(50, 80, 30));
            oreDataMap.put(Material.NETHER_QUARTZ_ORE, new OreData(25, 20, 30));
            oreDataMap.put(Material.REDSTONE_ORE, new OreData(15, 15, 30));
            oreDataMap.put(Material.LAPIS_ORE, new OreData(40, 20, 30));
            oreDataMap.put(Material.NETHER_GOLD_ORE, new OreData(35, 40, 30));
        }

        private boolean isPickaxe(Material material) {
            switch (material) {
                case WOODEN_PICKAXE:
                case STONE_PICKAXE:
                case IRON_PICKAXE:
                case GOLDEN_PICKAXE:
                case DIAMOND_PICKAXE:
                case NETHERITE_PICKAXE:
                    return true;
                default:
                    return false;
            }
        }

        private Material getOreDrop(Material ore) {
            switch (ore) {
                case COAL_ORE:
                    return Material.COAL;
                case IRON_ORE:
                    return Material.IRON_ORE; // Change this if you want the ore instead of ingot.
                case GOLD_ORE:
                    return Material.GOLD_ORE;
                case DIAMOND_ORE:
                    return Material.DIAMOND;
                case EMERALD_ORE:
                    return Material.EMERALD;
                case NETHER_QUARTZ_ORE:
                    return Material.NETHER_QUARTZ_ORE;
                case NETHER_GOLD_ORE:
                    return Material.NETHER_GOLD_ORE;
                case REDSTONE_ORE:
                    return Material.REDSTONE;
                case LAPIS_ORE:
                    return Material.LAPIS_LAZULI;
                // Add more ores here
                default:
                    return ore;
            }
        }

        private static class OreData {
            private final int requiredLevel;
            private final double xpValue;
            private final long respawnTime;

            OreData(int requiredLevel, double xpValue, long respawnTime) {
                this.requiredLevel = requiredLevel;
                this.xpValue = xpValue;
                this.respawnTime = respawnTime;
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

        public long toTicks(TimeUnit timeUnit, long duration) {
            return timeUnit.toSeconds(duration) * 20; // Converts the duration to seconds, then multiplies by 20 to get ticks.
        }
    }

