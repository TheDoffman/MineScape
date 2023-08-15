package org.hoffmantv.minescape.skills;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import org.hoffmantv.minescape.managers.SkillManager;

public class SmithingSkill implements Listener {

    private SkillManager skillManager;

    public SmithingSkill(SkillManager skillManager) {
        this.skillManager = skillManager;
    }

    public int getSmithingLevel(Player player) {
        return skillManager.getSkillLevel(player, SkillManager.Skill.SMITHING);
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
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0F, 1.0F);
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
                xp = 5; // Example XP
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
