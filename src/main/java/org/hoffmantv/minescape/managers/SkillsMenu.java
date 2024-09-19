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
import org.hoffmantv.minescape.managers.SkillManager;

import java.util.Arrays;
import java.util.Collections;

public class SkillsMenu implements Listener {

    private static final String SKILLS_MENU_TITLE =  ChatColor.RED + "Skills Menu";
    private static final int SKILLS_MENU_SIZE = 27;

    private final SkillManager skillManager;

    public SkillsMenu(SkillManager skillManager) {
        this.skillManager = skillManager;
    }

    public void openFor(Player player) {
        Inventory skillsMenu = createSkillsMenu();

        for (SkillManager.Skill skill : SkillManager.Skill.values()) {
            skillsMenu.addItem(createSkillItemFor(player, skill));
        }

        player.openInventory(skillsMenu);
    }

    private Inventory createSkillsMenu() {
        return Bukkit.createInventory(null, SKILLS_MENU_SIZE, SKILLS_MENU_TITLE);
    }

    private ItemStack createSkillItemFor(Player player, SkillManager.Skill skill) {
        ItemStack skillItem = new ItemStack(Material.PAPER);
        ItemMeta skillMeta = skillItem.getItemMeta();

        skillMeta.setDisplayName(ChatColor.GOLD + skill.name());

        int playerLevel = skillManager.getSkillLevel(player, skill);
        int xpNeeded = (int) skillManager.xpNeededForNextLevel(player, skill); // Assuming you have such a method
        skillMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Level: " + playerLevel,
                ChatColor.BLUE + "XP till next: " + xpNeeded
        ));

        skillItem.setItemMeta(skillMeta);

        return skillItem;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        String title = event.getView().getTitle();
        Inventory clickedInventory = event.getClickedInventory();

        if (clickedInventory == null || !title.equals(SKILLS_MENU_TITLE)) {
            return;
        }

        event.setCancelled(true);
        // Handle item interactions, if needed
    }

}