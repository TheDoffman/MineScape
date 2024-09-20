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
import java.util.ArrayList;
import java.util.List;

public class SkillsMenu implements Listener {

    private static final String SKILLS_MENU_TITLE = ChatColor.RED + "Skills Menu";

    private final SkillManager skillManager;

    public SkillsMenu(SkillManager skillManager) {
        this.skillManager = skillManager;
    }

    /**
     * Opens the skills menu for the specified player.
     *
     * @param player The player for whom the menu is opened.
     */
    public void openFor(Player player) {
        SkillManager.Skill[] skills = SkillManager.Skill.values();
        Inventory skillsMenu = createSkillsMenu(skills.length);

        for (SkillManager.Skill skill : skills) {
            skillsMenu.addItem(createSkillItemFor(player, skill));
        }

        player.openInventory(skillsMenu);
    }

    /**
     * Creates the skills menu inventory with an appropriate size.
     *
     * @param numberOfSkills The total number of skills to display.
     * @return The created Inventory.
     */
    private Inventory createSkillsMenu(int numberOfSkills) {
        int size = ((numberOfSkills - 1) / 9 + 1) * 9; // Ensure inventory size is a multiple of 9
        return Bukkit.createInventory(new SkillsMenuHolder(), size, SKILLS_MENU_TITLE);
    }

    /**
     * Creates an ItemStack representing a specific skill for the player.
     *
     * @param player The player viewing the menu.
     * @param skill  The skill to represent.
     * @return The created ItemStack.
     */
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

    /**
     * Generates a user-friendly display name for a skill.
     *
     * @param skill The skill enum.
     * @return The formatted display name.
     */
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

    /**
     * Maps each skill to a specific material.
     *
     * @param skill The skill enum.
     * @return The corresponding Material.
     */
    private Material getMaterialForSkill(SkillManager.Skill skill) {
        switch (skill) {
            case WOODCUTTING:
                return Material.IRON_AXE;
            case MINING:
                return Material.IRON_PICKAXE;
            case SMITHING:
                return Material.ANVIL;
            case FISHING:
                return Material.FISHING_ROD;
            case ATTACK:
                return Material.DIAMOND_SWORD;
            case DEFENCE:
                return Material.SHIELD;
            case STRENGTH:
                return Material.STONE_AXE;
            case RANGE:
                return Material.BOW;
            case HITPOINTS:
                return Material.HEART_OF_THE_SEA; // Alternative: RED_DYE
            case PRAYER:
                return Material.BONE;
            case MAGIC:
                return Material.BLAZE_ROD;
            case COOKING:
                return Material.COOKED_BEEF;
            case FLETCHING:
                return Material.ARROW;
            case FIREMAKING:
                return Material.FLINT_AND_STEEL;
            case CRAFTING:
                return Material.CRAFTING_TABLE;
            case HERBLORE:
                return Material.WOODEN_HOE;
            case AGILITY:
                return Material.LEATHER_BOOTS;
            case THEVING:
                return Material.TRIPWIRE_HOOK; // Alternative: LEATHER_CHESTPLATE
            case SLAYER:
                return Material.SKELETON_SKULL;
            case FARMING:
                return Material.WHEAT;
            case RUNECRAFTING:
                return Material.PAPER;
            case HUNTER:
                return Material.TRIDENT;
            case CONSTRUCTION:
                return Material.BRICK;
            case ALCHEMY:
                return Material.BREWING_STAND;
            case COMBAT:
                return Material.IRON_SWORD;
            default:
                return Material.PAPER;
        }
    }

    /**
     * Handles clicks within the skills menu inventory.
     *
     * @param event The InventoryClickEvent.
     */
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
            event.setCancelled(true); // Prevent item movement

            // Handle skill interactions here if needed
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR) {
                return;
            }

            Player player = (Player) event.getWhoClicked();
            String skillName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());

            // Example: Notify the player about the clicked skill
            player.sendMessage(ChatColor.YELLOW + "You clicked on " + ChatColor.GOLD + skillName + ChatColor.YELLOW + " skill!");

            // Implement further interactions as needed
            // For example, open a detailed skill info menu or upgrade options
        }
    }

    /**
     * Custom InventoryHolder to identify the Skills Menu.
     */
    private static class SkillsMenuHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}