package org.hoffmantv.minescape.managers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;


import java.util.Arrays;

public class SkillsMenu implements Listener {

    private final SkillManager skillManager;

    public SkillsMenu(SkillManager skillManager) {
        this.skillManager = skillManager;
    }

    public void openFor(Player player) {
        Inventory skillsMenu = Bukkit.createInventory(null, 27, ChatColor.RED +"Skills Menu");

        for (SkillManager.Skill skill : SkillManager.Skill.values()) {
            ItemStack skillItem = new ItemStack(Material.PAPER);
            ItemMeta skillMeta = skillItem.getItemMeta();

            skillMeta.setDisplayName(ChatColor.GOLD + skill.name());

            int playerLevel = skillManager.getSkillLevel(player, skill);
            skillMeta.setLore(Arrays.asList(ChatColor.GRAY + "Level: " + playerLevel));

            skillItem.setItemMeta(skillMeta);

            skillsMenu.addItem(skillItem);
        }

        player.openInventory(skillsMenu);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        // Using the InventoryView class to retrieve the inventory title
        String title = event.getView().getTitle();
        Inventory clickedInventory = event.getClickedInventory();

        if (clickedInventory == null || !title.equals("Skills Menu")) {
            return;
        }

        event.setCancelled(true);

        // Handle interactions, if needed
    }
}
