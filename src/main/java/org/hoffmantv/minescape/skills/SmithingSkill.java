package org.hoffmantv.minescape.skills;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.hoffmantv.minescape.managers.ConfigurationManager;
import org.hoffmantv.minescape.managers.SkillManager;

import java.util.HashMap;
import java.util.Map;

public class SmithingSkill implements Listener {

    private final SkillManager skillManager;
    private final ConfigurationManager configManager;
    private final JavaPlugin plugin;

    private final Map<Material, SmeltingRecipe> smeltingRecipes = new HashMap<>();
    private final Map<Material, CraftingRecipe> craftingRecipes = new HashMap<>();

    public SmithingSkill(SkillManager skillManager, ConfigurationManager configManager, JavaPlugin plugin) {
        this.skillManager = skillManager;
        this.configManager = configManager;
        this.plugin = plugin;

        loadSmithingConfigs();
    }

    private void loadSmithingConfigs() {
        ConfigurationSection smithingSection = configManager.getConfig("skills.yml").getConfigurationSection("skills.smithing");

        if (smithingSection != null) {
            // Load smelting configurations
            ConfigurationSection smeltingSection = smithingSection.getConfigurationSection("smelting");
            if (smeltingSection != null) {
                for (String key : smeltingSection.getKeys(false)) {
                    ConfigurationSection oreConfig = smeltingSection.getConfigurationSection(key);
                    if (oreConfig != null) {
                        Material oreMaterial = Material.getMaterial(key.toUpperCase());
                        int requiredLevel = oreConfig.getInt("requiredLevel", 1);
                        int xpValue = oreConfig.getInt("xpValue", 0);
                        Material resultMaterial = getSmeltedResult(oreMaterial);

                        if (oreMaterial != null && resultMaterial != null) {
                            SmeltingRecipe recipe = new SmeltingRecipe(oreMaterial, resultMaterial, requiredLevel, xpValue);
                            smeltingRecipes.put(oreMaterial, recipe);
                        } else {
                            plugin.getLogger().warning("Invalid material in smelting config: " + key);
                        }
                    }
                }
            }

            // Load crafting configurations
            ConfigurationSection craftingSection = smithingSection.getConfigurationSection("crafting.items");
            if (craftingSection != null) {
                for (String key : craftingSection.getKeys(false)) {
                    ConfigurationSection itemConfig = craftingSection.getConfigurationSection(key);
                    if (itemConfig != null) {
                        Material itemMaterial = Material.getMaterial(key.toUpperCase());
                        int requiredLevel = itemConfig.getInt("requiredLevel", 1);
                        int xpValue = itemConfig.getInt("xpValue", 0);

                        if (itemMaterial != null) {
                            CraftingRecipe recipe = new CraftingRecipe(itemMaterial, requiredLevel, xpValue);
                            craftingRecipes.put(itemMaterial, recipe);
                        } else {
                            plugin.getLogger().warning("Invalid material in crafting config: " + key);
                        }
                    }
                }
            }
        }
    }

    // SmeltingRecipe class
    private static class SmeltingRecipe {
        private final Material oreMaterial;
        private final Material resultMaterial;
        private final int requiredLevel;
        private final int xpValue;

        public SmeltingRecipe(Material oreMaterial, Material resultMaterial, int requiredLevel, int xpValue) {
            this.oreMaterial = oreMaterial;
            this.resultMaterial = resultMaterial;
            this.requiredLevel = requiredLevel;
            this.xpValue = xpValue;
        }

        public Material getOreMaterial() {
            return oreMaterial;
        }

        public Material getResultMaterial() {
            return resultMaterial;
        }

        public int getRequiredLevel() {
            return requiredLevel;
        }

        public int getXpValue() {
            return xpValue;
        }
    }

    // CraftingRecipe class
    private static class CraftingRecipe {
        private final Material itemMaterial;
        private final int requiredLevel;
        private final int xpValue;

        public CraftingRecipe(Material itemMaterial, int requiredLevel, int xpValue) {
            this.itemMaterial = itemMaterial;
            this.requiredLevel = requiredLevel;
            this.xpValue = xpValue;
        }

        public Material getItemMaterial() {
            return itemMaterial;
        }

        public int getRequiredLevel() {
            return requiredLevel;
        }

        public int getXpValue() {
            return xpValue;
        }
    }

    // Event Handlers

    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getRecipe().getResult();
        Material itemType = item.getType();

        if (craftingRecipes.containsKey(itemType)) {
            CraftingRecipe recipe = craftingRecipes.get(itemType);
            int requiredLevel = recipe.getRequiredLevel();

            if (getSmithingLevel(player) < requiredLevel) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "You need a Smithing level of " + requiredLevel + " to craft this item.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            } else {
                int earnedXP = recipe.getXpValue();
                skillManager.addXP(player, SkillManager.Skill.SMITHING, earnedXP);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
                player.sendActionBar(ChatColor.GOLD + "Smithing +" + earnedXP);
            }
        }
    }

    @EventHandler
    public void onFurnaceSmelt(FurnaceSmeltEvent event) {
        Material sourceOre = event.getSource().getType();

        if (smeltingRecipes.containsKey(sourceOre)) {
            SmeltingRecipe recipe = smeltingRecipes.get(sourceOre);

            // Find the player who placed the ore (this is a simplified approach)
            Player player = getNearestPlayer(event.getBlock().getLocation());
            if (player != null) {
                if (getSmithingLevel(player) < recipe.getRequiredLevel()) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "You need a Smithing level of " + recipe.getRequiredLevel() + " to smelt this ore.");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                } else {
                    // Award XP for smelting
                    int earnedXP = recipe.getXpValue();
                    skillManager.addXP(player, SkillManager.Skill.SMITHING, earnedXP);
                    player.sendActionBar(ChatColor.GOLD + "Smithing +" + earnedXP);
                }
            }
        }
    }

    private Player getNearestPlayer(Location location) {
        return location.getWorld().getNearbyEntities(location, 5, 5, 5).stream()
                .filter(entity -> entity instanceof Player)
                .map(entity -> (Player) entity)
                .findFirst()
                .orElse(null);
    }

    // Helper Methods

    public int getSmithingLevel(Player player) {
        return skillManager.getSkillLevel(player, SkillManager.Skill.SMITHING);
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
}