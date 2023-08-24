package org.hoffmantv.minescape.skills;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.hoffmantv.minescape.managers.SkillManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class CookingSkill implements Listener {

    private final SkillManager skillManager;
    private final Random random = new Random();

    // Define level requirements for certain foods
    private final Map<Material, Integer> levelRequirements = new HashMap<Material, Integer>() {{
        put(Material.BEEF, 1);
        put(Material.CHICKEN, 5);
        put(Material.MUTTON, 10);
        put(Material.SALMON, 15);
    }};
    public CookingSkill(SkillManager skillManager) {
        this.skillManager = skillManager;
    }

    @EventHandler
    public void onPlayerCookFood(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (event.getClickedBlock() == null || event.getClickedBlock().getType() != Material.CAMPFIRE) {
            return;
        }

        ItemStack itemInHand = event.getItem();
        if (itemInHand == null || !isUncookedFood(itemInHand.getType())) {
            return;
        }

        int playerLevel = skillManager.getSkillLevel(event.getPlayer(), SkillManager.Skill.COOKING);
        Material cookedVersion = getCookedVersion(itemInHand.getType());

        if (shouldBurnFood(playerLevel)) {
            event.getPlayer().sendMessage("Oops! You burned the food.");
            itemInHand.setAmount(itemInHand.getAmount() - 1); // Decrease the uncooked food by 1
            event.getPlayer().getWorld().dropItemNaturally(event.getPlayer().getLocation(), new ItemStack(Material.CHARCOAL)); // Drop burned item (using charcoal as an example of burned food)
        } else {
            event.getPlayer().sendMessage("You successfully cooked the food.");
            itemInHand.setAmount(itemInHand.getAmount() - 1); // Decrease the uncooked food by 1
            event.getPlayer().getWorld().dropItemNaturally(event.getPlayer().getLocation(), new ItemStack(cookedVersion));
            // Optionally add XP here based on success
        }
        if (!meetsLevelRequirement(event.getPlayer(), itemInHand.getType())) {
            event.getPlayer().sendMessage("You do not have the required level to cook this food.");
            return;
        }

        if (shouldBurnFood(playerLevel)) {
            // ... (burning logic)
        } else {
            event.getPlayer().sendMessage("You successfully cooked the food.");
            itemInHand.setAmount(itemInHand.getAmount() - 1);
            event.getPlayer().getWorld().dropItemNaturally(event.getPlayer().getLocation(), new ItemStack(cookedVersion));

            // Add XP to the player's cooking skill
            int xpToAdd = calculateXpReward(itemInHand.getType());
            skillManager.addXP(event.getPlayer(), SkillManager.Skill.COOKING, xpToAdd);
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
            default:
                return Material.CHARCOAL; // This is just a default for error-handling, won't really be reached based on our isUncookedFood check.
        }
    }
}
