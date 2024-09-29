package org.hoffmantv.minescape.skills;

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

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Cooking implements Listener {

    private final SkillManager skillManager;
    private final Random random = new Random();

    // Define level requirements for certain foods
    private final Map<Material, Integer> levelRequirements = new HashMap<Material, Integer>() {{
        put(Material.BEEF, 1);
        put(Material.CHICKEN, 5);
        put(Material.MUTTON, 10);
        put(Material.SALMON, 15);
        put(Material.RABBIT, 20);
    }};
    public Cooking(SkillManager skillManager) {
        this.skillManager = skillManager;
    }

    @EventHandler
    public void onPlayerCookFood(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        if (event.getClickedBlock() == null || event.getClickedBlock().getType() != Material.CAMPFIRE) return;

        ItemStack itemInHand = event.getItem();
        if (itemInHand == null || !isUncookedFood(itemInHand.getType())) return;

        // Cancel the default interaction to prevent adding the food to the campfire
        event.setCancelled(true);

        Player player = event.getPlayer();
        int playerLevel = skillManager.getSkillLevel(player, SkillManager.Skill.COOKING);
        Material cookedVersion = getCookedVersion(itemInHand.getType());

            // Check player level requirements
        int requiredLevel = getRequiredLevelForFood(itemInHand.getType());
        if (!meetsLevelRequirement(player, itemInHand.getType())) {
            player.sendMessage(ChatColor.RED + "⚠ " + ChatColor.GRAY + "You require level " + ChatColor.GOLD + requiredLevel
                    + ChatColor.GRAY + " in cooking to prepare this item.");
            return;
        }

        itemInHand.setAmount(itemInHand.getAmount() - 1); // Decrease the uncooked food by 1

        if (shouldBurnFood(playerLevel)) {
            player.sendMessage(ChatColor.RED + "✖ " + ChatColor.GRAY + "Sadly, you've burned the food.");
            player.getInventory().addItem(getCustomNamedItem(Material.CHARCOAL, "Burned Food"));
        } else {
            player.sendMessage(ChatColor.GREEN + "✓ " + ChatColor.GRAY + "Successfully cooked: " + ChatColor.GOLD + cookedVersion.name().replace("_", " "));
            player.getInventory().addItem(getCustomNamedItem(cookedVersion, cookedVersion.name().replace("_", " ")));

            // Add XP based on success
            int xpToAdd = calculateXpReward(itemInHand.getType());
            player.sendActionBar(ChatColor.GOLD + "Cooking +" + xpToAdd);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            skillManager.addXP(player, SkillManager.Skill.COOKING, xpToAdd);
        }
    }
    private boolean meetsLevelRequirement(Player player, Material foodType) {
        return levelRequirements.getOrDefault(foodType, 0) <= skillManager.getSkillLevel(player, SkillManager.Skill.COOKING);
    }
    private int calculateXpReward(Material foodType) {
        // You can adjust this method to return different XP amounts based on the food type or other criteria.
        return 10; // Example: a flat 10 XP per cooked item.
    }
    private boolean shouldBurnFood(int playerLevel) {
        int burnChance = 50 - (playerLevel * 2); // e.g., at level 10, there's a 30% chance to burn, at level 25, 5% chance.
        burnChance = Math.max(5, burnChance);  // Ensure there's always at least a 5% chance to burn for challenge
        return random.nextInt(100) < burnChance;
    }
    private boolean isUncookedFood(Material material) {
        // Modify this list to include all uncooked food items you wish to check.
        return material == Material.BEEF || material == Material.CHICKEN || material == Material.MUTTON || material == Material.SALMON || material == Material.PORKCHOP;
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
            case RABBIT:
                return Material.COOKED_RABBIT;
            default:
                return Material.CHARCOAL; // This is just a default for error-handling, won't really be reached based on our isUncookedFood check.
        }
    }
    private int getRequiredLevelForFood(Material foodItem) {
        // Example logic - adjust to your needs
        switch(foodItem) {
            case BEEF: return 1;
            case CHICKEN: return 5;
            case MUTTON: return 10;
            case SALMON: return 15;
            case RABBIT: return 20;
            // ... other foods
            default: return 0;
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
}
