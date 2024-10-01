package org.hoffmantv.minescape.skills;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class Smithing implements Listener {

    private final SkillManager skillManager;
    private final JavaPlugin plugin;

    private final Map<Material, SmeltingRecipe> smeltingRecipes = new HashMap<>();
    private final Map<Material, CraftingRecipe> craftingRecipes = new HashMap<>();

    public Smithing(SkillManager skillManager, JavaPlugin plugin) {
        this.skillManager = skillManager;
        this.plugin = plugin;

        loadSmithingConfigs();
    }

    private void loadSmithingConfigs() {
        ConfigurationSection smithingSection = skillManager.getSkillsConfig().getConfigurationSection("skills.smithing");

        if (smithingSection != null) {
            plugin.getLogger().info("Loading Smithing Skill configurations...");

            // Load smelting configurations
            ConfigurationSection smeltingSection = smithingSection.getConfigurationSection("smelting");
            if (smeltingSection != null) {
                for (String key : smeltingSection.getKeys(false)) {
                    ConfigurationSection oreConfig = smeltingSection.getConfigurationSection(key);
                    if (oreConfig != null) {
                        Material oreMaterial = Material.getMaterial(key.toUpperCase());
                        int requiredLevel = oreConfig.getInt("requiredLevel", 1);
                        double xpValue = oreConfig.getDouble("xpValue", 0);
                        Material resultMaterial = getSmeltedResult(oreMaterial);

                        if (oreMaterial != null && resultMaterial != null) {
                            SmeltingRecipe recipe = new SmeltingRecipe(oreMaterial, resultMaterial, requiredLevel, xpValue);
                            smeltingRecipes.put(oreMaterial, recipe);
                        } else {
                            plugin.getLogger().warning("Invalid material in smelting config: " + key);
                        }
                    }
                }
            } else {
                plugin.getLogger().warning("No 'smelting' section found in skills.yml under 'skills.smithing'");
            }

            // Load crafting configurations
            ConfigurationSection craftingSection = smithingSection.getConfigurationSection("crafting.items");
            if (craftingSection != null) {
                for (String key : craftingSection.getKeys(false)) {
                    ConfigurationSection itemConfig = craftingSection.getConfigurationSection(key);
                    if (itemConfig != null) {
                        Material itemMaterial = Material.getMaterial(key.toUpperCase());
                        int requiredLevel = itemConfig.getInt("requiredLevel", 1);
                        double xpValue = itemConfig.getDouble("xpValue", 0);

                        if (itemMaterial != null) {
                            CraftingRecipe recipe = new CraftingRecipe(itemMaterial, requiredLevel, xpValue);
                            craftingRecipes.put(itemMaterial, recipe);
                        } else {
                            plugin.getLogger().warning("Invalid material in crafting config: " + key);
                        }
                    }
                }
            } else {
                plugin.getLogger().warning("No 'crafting' section found in skills.yml under 'skills.smithing'");
            }

            plugin.getLogger().info("Smithing Skill configurations loaded successfully.");
        } else {
            plugin.getLogger().warning("Smithing configuration section is null.");
        }
    }

    // SmeltingRecipe class
    private static class SmeltingRecipe {
        private final Material oreMaterial;
        private final Material resultMaterial;
        private final int requiredLevel;
        private final double xpValue;

        public SmeltingRecipe(Material oreMaterial, Material resultMaterial, int requiredLevel, double xpValue) {
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

        public double getXpValue() {
            return xpValue;
        }
    }

    // CraftingRecipe class
    private static class CraftingRecipe {
        private final Material itemMaterial;
        private final int requiredLevel;
        private final double xpValue;

        public CraftingRecipe(Material itemMaterial, int requiredLevel, double xpValue) {
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

        public double getXpValue() {
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

            if (skillManager.getSkillLevel(player, SkillManager.Skill.SMITHING) < requiredLevel) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "You need a Smithing level of " + requiredLevel + " to craft this item.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            } else {
                int earnedXP = (int) recipe.getXpValue();
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
                if (skillManager.getSkillLevel(player, SkillManager.Skill.SMITHING) < recipe.getRequiredLevel()) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "You need a Smithing level of " + recipe.getRequiredLevel() + " to smelt this ore.");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                } else {
                    // Award XP for smelting
                    int earnedXP = (int) recipe.getXpValue();
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