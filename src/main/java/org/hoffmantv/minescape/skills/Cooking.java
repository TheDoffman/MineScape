package org.hoffmantv.minescape.skills;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Cooking implements Listener {

    private final SkillManager skillManager;
    private final Random random = new Random();

    // Define level requirements, XP, and burn stop levels for all Minecraft cookable foods
    private final Map<Material, FoodData> foodData = new HashMap<>();

    public Cooking(SkillManager skillManager, ConfigurationSection cookingConfig) {
        this.skillManager = skillManager;
        loadFoodData(cookingConfig);
    }

    private void loadFoodData(ConfigurationSection cookingConfig) {
        ConfigurationSection foodSection = cookingConfig.getConfigurationSection("skills.cooking.foods");
        if (foodSection != null) {
            for (String key : foodSection.getKeys(false)) {
                ConfigurationSection foodConfig = foodSection.getConfigurationSection(key);
                if (foodConfig != null) {
                    int requiredLevel = foodConfig.getInt("requiredLevel", 1);
                    int xpReward = foodConfig.getInt("xpReward", 0);
                    int burnStopLevel = foodConfig.getInt("burnStopLevel", 0);
                    foodData.put(Material.valueOf(key), new FoodData(requiredLevel, xpReward, burnStopLevel));
                }
            }
        }
    }

    @EventHandler
    public void onPlayerCookFood(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        if (event.getClickedBlock() == null ||
                (event.getClickedBlock().getType() != Material.CAMPFIRE &&
                        event.getClickedBlock().getType() != Material.FURNACE)) return;

        ItemStack itemInHand = event.getItem();
        if (itemInHand == null || !isUncookedFood(itemInHand.getType())) return;

        // Cancel the default interaction to prevent adding the food to the campfire/furnace
        event.setCancelled(true);

        Player player = event.getPlayer();
        Material uncookedFood = itemInHand.getType();
        FoodData foodInfo = foodData.get(uncookedFood);

        // Check if player meets the level requirements for cooking
        if (!meetsLevelRequirement(player, uncookedFood)) {
            player.sendMessage(ChatColor.RED + "⚠ " + ChatColor.GRAY +
                    "You require level " + ChatColor.GOLD + foodInfo.requiredLevel +
                    ChatColor.GRAY + " in cooking to prepare this item.");
            return;
        }

        // Decrease the uncooked food by 1
        itemInHand.setAmount(itemInHand.getAmount() - 1);

        if (shouldBurnFood(player, foodInfo)) {
            player.sendMessage(ChatColor.RED + "✖ " + ChatColor.GRAY + "Sadly, you've burned the food.");
            player.getInventory().addItem(getCustomNamedItem(Material.CHARCOAL, "Burned Food"));
        } else {
            Material cookedVersion = getCookedVersion(uncookedFood);
            player.sendMessage(ChatColor.GREEN + "✓ " + ChatColor.GRAY + "Successfully cooked: " +
                    ChatColor.GOLD + cookedVersion.name().replace("_", " "));
            player.getInventory().addItem(getCustomNamedItem(cookedVersion, cookedVersion.name().replace("_", " ")));

            // Add XP based on success
            player.sendActionBar(ChatColor.GOLD + "Cooking +" + foodInfo.xpReward);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            skillManager.addXP(player, SkillManager.Skill.COOKING, foodInfo.xpReward);
        }
    }

    private boolean meetsLevelRequirement(Player player, Material foodType) {
        return foodData.getOrDefault(foodType, new FoodData(0, 0, 0)).requiredLevel <=
                skillManager.getSkillLevel(player, SkillManager.Skill.COOKING);
    }

    private boolean shouldBurnFood(Player player, FoodData foodInfo) {
        int playerLevel = skillManager.getSkillLevel(player, SkillManager.Skill.COOKING);
        int burnChance = 50 - (playerLevel * 2); // Default burn chance based on level

        // If the player has reached the burn stop level, food doesn't burn
        if (playerLevel >= foodInfo.burnStopLevel) {
            return false;
        }

        // If cooking on a furnace, reduce burn chance further (e.g., 10% less chance to burn)
        if (player.getLocation().getBlock().getType() == Material.FURNACE) {
            burnChance -= 10;
        }

        burnChance = Math.max(5, burnChance);  // Ensure a minimum burn chance for challenge
        return random.nextInt(100) < burnChance;
    }

    private boolean isUncookedFood(Material material) {
        return foodData.containsKey(material); // If the food is in our foodData map, it's considered uncooked
    }

    private Material getCookedVersion(Material uncooked) {
        switch (uncooked) {
            case BEEF:
                return Material.COOKED_BEEF;
            case CHICKEN:
                return Material.COOKED_CHICKEN;
            case MUTTON:
                return Material.COOKED_MUTTON;
            case PORKCHOP:
                return Material.COOKED_PORKCHOP;
            case SALMON:
                return Material.COOKED_SALMON;
            case COD:
                return Material.COOKED_COD;
            case RABBIT:
                return Material.COOKED_RABBIT;
            case POTATO:
                return Material.BAKED_POTATO;
            case KELP:
                return Material.DRIED_KELP;
            default:
                return Material.CHARCOAL; // Default for burned food
        }
    }

    private ItemStack getCustomNamedItem(Material material, String name) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }

        return item;
    }

    // Data class for food level requirements, XP rewards, and burn stop levels
    private static class FoodData {
        final int requiredLevel;
        final int xpReward;
        final int burnStopLevel;

        FoodData(int requiredLevel, int xpReward, int burnStopLevel) {
            this.requiredLevel = requiredLevel;
            this.xpReward = xpReward;
            this.burnStopLevel = burnStopLevel;
        }
    }
}