package org.hoffmantv.minescape.skills;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.hoffmantv.minescape.MineScape;
import org.hoffmantv.minescape.managers.SkillManager;

public class SmithingSkill implements Listener {

    private final SkillManager skillManager;

    public SmithingSkill(SkillManager skillManager) {
        this.skillManager = skillManager;
    }

    public int getSmithingLevel(Player player) {
        return skillManager.getSkillLevel(player, SkillManager.Skill.SMITHING);
    }
    private int getRequiredSmeltingLevel(Material ore) {
        switch (ore) {
            case IRON_ORE:
                return 15;
            case GOLD_ORE:
                return 20;
            case ANCIENT_DEBRIS: // For Netherite
                return 40;
            // ... Add any other ores and their requirements here ...
            default:
                return 0;
        }
    }

    private boolean hasRequiredLevel(Player player, Material material) {
        int playerLevel = skillManager.getSkillLevel(player, SkillManager.Skill.SMITHING);
        int requiredLevel = getRequiredSmeltingLevel(material); // Corrected here
        return playerLevel >= requiredLevel;
    }

    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getRecipe().getResult();

        if (isRestrictedItem(item)) {
            int requiredLevel = getRequiredSmithingLevel(item);

            if (getSmithingLevel(player) < requiredLevel) {
                // If they don't have the required level, cancel crafting and notify the player
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "You need a Smithing level of " + requiredLevel + " to craft this item.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            } else {
                // If they have the required level, award XP and play sound
                int earnedXP = giveSmithingXP(player, item);
                skillManager.addXP(player, SkillManager.Skill.MINING, earnedXP);
                skillManager.saveSkillsToConfig();
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
                player.sendMessage(ChatColor.GREEN + "You earned " + earnedXP + " Smithing XP!");
            }
        }
    }

    @EventHandler
    public void onFurnaceSmelt(FurnaceSmeltEvent event) {
        Material sourceOre = event.getSource().getType();
        if (isOreRestricted(sourceOre)) {
            Player player = (Player) event.getBlock().getWorld().getNearbyEntities(event.getBlock().getLocation(), 5, 5, 5).stream().filter(entity -> entity instanceof Player).findFirst().orElse(null);
            if (player != null) {
                int requiredLevel = getRequiredSmeltingLevel(sourceOre);
                if (getSmithingLevel(player) < requiredLevel) {
                    event.setCancelled(true);
                    player.sendMessage("You need a Smithing level of " + requiredLevel + " to smelt this ore.");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                }
            }
        }
    }
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();
        ItemStack handItem = player.getInventory().getItemInMainHand();

        if (handItem.getType() == Material.AIR) {
            return; // Check for null or air in hand
        }

        if (action == Action.RIGHT_CLICK_BLOCK) {
            Block clickedBlock = event.getClickedBlock();

            if (clickedBlock != null && clickedBlock.getType() == Material.FURNACE) {
                Material materialToSmelt = handItem.getType();

                // Check for smithing level requirements
                if (!hasRequiredLevel(player, materialToSmelt)) {
                    event.setCancelled(true);
                    return;
                }

                // Check for available inventory space
                if (player.getInventory().firstEmpty() == -1) {
                    player.sendMessage(ChatColor.RED + "You don't have enough space in your inventory!");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    event.setCancelled(true);
                    return;
                }

                Material smeltedResult = getSmeltedResult(materialToSmelt);
                if (smeltedResult != null) {
                    event.setCancelled(true); // Prevents the furnace UI from showing

                    player.playSound(player.getLocation(), Sound.BLOCK_FURNACE_FIRE_CRACKLE, 1.0f, 1.0f);
                    player.spawnParticle(Particle.SMOKE_LARGE, clickedBlock.getLocation().add(0.5, 1, 0.5), 10);

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            ItemStack resultItem = makeNonStackable(smeltedResult);
                            player.getInventory().removeItem(new ItemStack(materialToSmelt, 1));
                            player.getInventory().addItem(resultItem);

                            player.sendMessage(ChatColor.GREEN + "You've smelted the " + materialToSmelt.toString().toLowerCase() + "!");
                            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);

                        }
                    }.runTaskLater(JavaPlugin.getPlugin(MineScape.class), 60L);
                }
            }
        }
    }
    @EventHandler
    public void onPlayerRightClickFurnace(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        Player player = event.getPlayer();
        if (block == null) return;
        if (block.getType() != Material.FURNACE && block.getType() != Material.BLAST_FURNACE) return;

        ItemStack handItem = player.getInventory().getItemInMainHand();
        if (handItem.getType() == Material.IRON_ORE || handItem.getType() == Material.GOLD_ORE || handItem.getType() == Material.ANCIENT_DEBRIS) {

            int requiredLevel = 0;
            switch(handItem.getType()) {
                case IRON_ORE:
                    requiredLevel = 15;
                    break;
                case GOLD_ORE:
                    requiredLevel = 20;
                    break;
                case ANCIENT_DEBRIS:
                    requiredLevel = 40;
                    break;
            }

            int playerLevel = skillManager.getSkillLevel(player, SkillManager.Skill.SMITHING);
            if (playerLevel < requiredLevel) {
                player.sendMessage(ChatColor.RED + "You need Smithing level " + requiredLevel + " to smelt this ore!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }
            // Remove the item from the player's hand
            if (handItem.getAmount() > 1) {
                handItem.setAmount(handItem.getAmount() - 1);
            } else {
                player.getInventory().setItemInMainHand(null);
            }
        }
    }

    private Material getSmeltedResult(Material material) {
        switch (material) {
            case IRON_ORE:
                return Material.IRON_INGOT;
            case GOLD_ORE:
                return Material.GOLD_INGOT;
            case ANCIENT_DEBRIS:
                return Material.NETHERITE_SCRAP;
            default:
                return null;
        }
    }

    private ItemStack makeNonStackable(Material material) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();

        // Set some custom model data to make it unique
        meta.setCustomModelData((int) (Math.random() * Integer.MAX_VALUE));

        item.setItemMeta(meta);
        return item;
    }

    private boolean isOreRestricted(Material ore) {
        switch (ore) {
            case IRON_ORE:
            case GOLD_ORE:
            case ANCIENT_DEBRIS:
                // ... Add other restricted ores here ...
                return true;
            default:
                return false;
        }
    }

    private int giveSmithingXP(Player player, ItemStack item) {
        int xp = 0; // Default XP

        // Depending on the item type, you can assign different XP values
        switch (item.getType()) {
            // Wooden tools
            case WOODEN_PICKAXE:
            case WOODEN_AXE:
            case WOODEN_SHOVEL:
            case WOODEN_HOE:
            case WOODEN_SWORD:
                xp = 5;
                break;

            // Stone tools
            case STONE_PICKAXE:
            case STONE_AXE:
            case STONE_SHOVEL:
            case STONE_HOE:
            case STONE_SWORD:
                xp = 10;
                break;

            // Iron tools and armors
            case IRON_PICKAXE:
            case IRON_AXE:
            case IRON_SHOVEL:
            case IRON_HOE:
            case IRON_SWORD:
            case IRON_HELMET:
            case IRON_CHESTPLATE:
            case IRON_LEGGINGS:
            case IRON_BOOTS:
                xp = 15;
                break;

            // Gold tools and armors
            case GOLDEN_PICKAXE:
            case GOLDEN_AXE:
            case GOLDEN_SHOVEL:
            case GOLDEN_HOE:
            case GOLDEN_SWORD:
            case GOLDEN_HELMET:
            case GOLDEN_CHESTPLATE:
            case GOLDEN_LEGGINGS:
            case GOLDEN_BOOTS:
                xp = 20;
                break;

            // Diamond tools and armors
            case DIAMOND_PICKAXE:
            case DIAMOND_AXE:
            case DIAMOND_SHOVEL:
            case DIAMOND_HOE:
            case DIAMOND_SWORD:
            case DIAMOND_HELMET:
            case DIAMOND_CHESTPLATE:
            case DIAMOND_LEGGINGS:
            case DIAMOND_BOOTS:
                xp = 25;
                break;

            // Netherite tools and armors
            case NETHERITE_PICKAXE:
            case NETHERITE_AXE:
            case NETHERITE_SHOVEL:
            case NETHERITE_HOE:
            case NETHERITE_SWORD:
            case NETHERITE_HELMET:
            case NETHERITE_CHESTPLATE:
            case NETHERITE_LEGGINGS:
            case NETHERITE_BOOTS:
                xp = 30;
                break;

            // Leather and Chainmail armors
            case LEATHER_HELMET:
            case LEATHER_CHESTPLATE:
            case LEATHER_LEGGINGS:
            case LEATHER_BOOTS:
            case CHAINMAIL_HELMET:
            case CHAINMAIL_CHESTPLATE:
            case CHAINMAIL_LEGGINGS:
            case CHAINMAIL_BOOTS:
                xp = 8; // Example XP
                break;

            default:
                break;
        }

        // Use your SkillManager to increase the player's Smithing skill XP
        skillManager.addXP(player, SkillManager.Skill.SMITHING, xp);
        return xp;
    }

    private boolean isRestrictedItem(ItemStack item) {
        switch (item.getType()) {
            case LEATHER_HELMET:
            case LEATHER_CHESTPLATE:
            case LEATHER_LEGGINGS:
            case LEATHER_BOOTS:
            case IRON_HELMET:
            case IRON_CHESTPLATE:
            case IRON_LEGGINGS:
            case IRON_BOOTS:
            case DIAMOND_HELMET:
            case DIAMOND_CHESTPLATE:
            case DIAMOND_LEGGINGS:
            case DIAMOND_BOOTS:
            case NETHERITE_HELMET:
            case NETHERITE_CHESTPLATE:
            case NETHERITE_LEGGINGS:
            case NETHERITE_BOOTS:
            case GOLDEN_HELMET:
            case GOLDEN_CHESTPLATE:
            case GOLDEN_LEGGINGS:
            case GOLDEN_BOOTS:
            case CHAINMAIL_HELMET:
            case CHAINMAIL_CHESTPLATE:
            case CHAINMAIL_LEGGINGS:
            case CHAINMAIL_BOOTS:
            case WOODEN_PICKAXE:
            case WOODEN_AXE:
            case WOODEN_SHOVEL:
            case WOODEN_HOE:
            case WOODEN_SWORD:
            case STONE_PICKAXE:
            case STONE_AXE:
            case STONE_SHOVEL:
            case STONE_HOE:
            case STONE_SWORD:
            case IRON_PICKAXE:
            case IRON_AXE:
            case IRON_SHOVEL:
            case IRON_HOE:
            case IRON_SWORD:
            case GOLDEN_PICKAXE:
            case GOLDEN_AXE:
            case GOLDEN_SHOVEL:
            case GOLDEN_HOE:
            case GOLDEN_SWORD:
            case DIAMOND_PICKAXE:
            case DIAMOND_AXE:
            case DIAMOND_SHOVEL:
            case DIAMOND_HOE:
            case DIAMOND_SWORD:
            case NETHERITE_PICKAXE:
            case NETHERITE_AXE:
            case NETHERITE_SHOVEL:
            case NETHERITE_HOE:
            case NETHERITE_SWORD:
            case ARROW:
            case BOW:
                return true;
            default:
                return false;
        }
    }

    private int getRequiredSmithingLevel(ItemStack item) {
        switch (item.getType()) {

            case WOODEN_PICKAXE:
            case WOODEN_AXE:
            case WOODEN_SHOVEL:
            case WOODEN_HOE:
            case WOODEN_SWORD:
            case LEATHER_HELMET:
            case LEATHER_CHESTPLATE:
            case LEATHER_LEGGINGS:
            case LEATHER_BOOTS:
            case ARROW:
            case BOW:
                return 1;  // Example: Require level 3 for wooden tools

            case STONE_PICKAXE:
            case STONE_AXE:
            case STONE_SHOVEL:
            case STONE_HOE:
            case STONE_SWORD:
            case CHAINMAIL_HELMET:
            case CHAINMAIL_CHESTPLATE:
            case CHAINMAIL_LEGGINGS:
            case CHAINMAIL_BOOTS:
                return 10;

            case IRON_PICKAXE:
            case IRON_AXE:
            case IRON_SHOVEL:
            case IRON_HOE:
            case IRON_SWORD:
            case IRON_HELMET:
            case IRON_CHESTPLATE:
            case IRON_LEGGINGS:
            case IRON_BOOTS:
                return 15;

            case GOLDEN_PICKAXE:
            case GOLDEN_AXE:
            case GOLDEN_SHOVEL:
            case GOLDEN_HOE:
            case GOLDEN_SWORD:
            case GOLDEN_HELMET:
            case GOLDEN_CHESTPLATE:
            case GOLDEN_LEGGINGS:
            case GOLDEN_BOOTS:
                return 20;

            case DIAMOND_PICKAXE:
            case DIAMOND_AXE:
            case DIAMOND_SHOVEL:
            case DIAMOND_HOE:
            case DIAMOND_SWORD:
            case DIAMOND_HELMET:
            case DIAMOND_CHESTPLATE:
            case DIAMOND_LEGGINGS:
            case DIAMOND_BOOTS:
                return 30;

            case NETHERITE_PICKAXE:
            case NETHERITE_AXE:
            case NETHERITE_SHOVEL:
            case NETHERITE_HOE:
            case NETHERITE_SWORD:
            case NETHERITE_HELMET:
            case NETHERITE_CHESTPLATE:
            case NETHERITE_LEGGINGS:
            case NETHERITE_BOOTS:
                return 40;

            default:
                return 0;
        }
    }

}
