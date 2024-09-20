package org.hoffmantv.minescape.managers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class SkillsMenu implements Listener {

    private static final String SKILLS_MENU_TITLE = ChatColor.RED + "Skills Menu";

    private final SkillManager skillManager;

    public SkillsMenu(SkillManager skillManager) {
        this.skillManager = skillManager;
    }

    public void openFor(Player player) {
        SkillManager.Skill[] skills = SkillManager.Skill.values();
        Inventory skillsMenu = createSkillsMenu(skills.length);

        for (SkillManager.Skill skill : skills) {
            skillsMenu.addItem(createSkillItemFor(player, skill));
        }

        player.openInventory(skillsMenu);
    }

    private Inventory createSkillsMenu(int numberOfSkills) {
        int size = ((numberOfSkills - 1) / 9 + 1) * 9; // Ensure inventory size is a multiple of 9
        return Bukkit.createInventory(new SkillsMenuHolder(), size, SKILLS_MENU_TITLE);
    }

    private ItemStack createSkillItemFor(Player player, SkillManager.Skill skill) {
        Material material = getMaterialForSkill(skill);
        ItemStack skillItem = new ItemStack(material);
        ItemMeta skillMeta = skillItem.getItemMeta();

        if (skillMeta != null) {
            skillMeta.setDisplayName(ChatColor.GOLD + getDisplayNameForSkill(skill));

            int playerLevel = skillManager.getSkillLevel(player, skill);
            int xpNeeded = (int) skillManager.xpNeededForNextLevel(player, skill);
            skillMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Level: " + ChatColor.GREEN + playerLevel,
                    ChatColor.BLUE + "XP till next level: " + ChatColor.AQUA + xpNeeded
            ));

            skillItem.setItemMeta(skillMeta);
        }

        return skillItem;
    }

    private String getDisplayNameForSkill(SkillManager.Skill skill) {
        String name = skill.name().toLowerCase().replace('_', ' ');
        String[] words = name.split(" ");
        StringBuilder displayName = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                displayName.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
            }
        }
        return displayName.toString().trim();
    }

    private Material getMaterialForSkill(SkillManager.Skill skill) {
        switch (skill) {
            case WOODCUTTING:
                return Material.IRON_AXE;
            case MINING:
                return Material.IRON_PICKAXE;
            case FISHING:
                return Material.FISHING_ROD;
            case FARMING:
                return Material.WHEAT;
            case COMBAT:
                return Material.IRON_SWORD;
            case MAGIC:
                return Material.BLAZE_ROD;
            case COOKING:
                return Material.COOKED_BEEF;
            case CRAFTING:
                return Material.CRAFTING_TABLE;
            case SMITHING:
                return Material.ANVIL;
            case ALCHEMY:
                return Material.BREWING_STAND;
            // Add other skills and their corresponding materials
            default:
                return Material.PAPER;
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null) {
            return;
        }

        if (clickedInventory.getHolder() instanceof SkillsMenuHolder) {
            event.setCancelled(true);
            // Optionally handle item interactions here
        }
    }

    // Custom InventoryHolder to identify the Skills Menu
    private static class SkillsMenuHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}