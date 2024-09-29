package org.hoffmantv.minescape.skills;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;

public class Crafting implements Listener {

    private final SkillManager skillManager;

    public Crafting(SkillManager skillManager){
        this.skillManager = skillManager;
    }

    @EventHandler
    public void onItemCraft(CraftItemEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack craftedItem = event.getCurrentItem();
        if (craftedItem == null) {
            return;
        }

        // Calculate XP reward based on the crafted item
        int xpAmount = calculateXpReward();

        // Add the XP reward to the player's CRAFTING skill using the SkillManager
        skillManager.addXP(player, SkillManager.Skill.CRAFTING, xpAmount);

        // Notify the player about the XP gained
        player.sendActionBar(ChatColor.GOLD + "Crafting +" + xpAmount);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }

    private int calculateXpReward() {
        return 15; // Base XP for all items
    }

}
