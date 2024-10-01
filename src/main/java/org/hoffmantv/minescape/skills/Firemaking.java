package org.hoffmantv.minescape.skills;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Firemaking implements Listener {
    private final SkillManager skillManager;
    private final JavaPlugin plugin;
    private final ConfigurationSection firemakingConfig;
    private final Map<Material, LogProperties> logPropertiesMap = new HashMap<>();

    public Firemaking(SkillManager skillManager, JavaPlugin plugin) {
        this.skillManager = skillManager;
        this.plugin = plugin;
        this.firemakingConfig = skillManager.getSkillsConfig().getConfigurationSection("firemaking.logRequirements");
        loadLogProperties();
    }

    // Load log properties from the configuration file
    private void loadLogProperties() {
        if (firemakingConfig != null) {
            for (String key : firemakingConfig.getKeys(false)) {
                Material material;
                try {
                    material = Material.valueOf(key);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid material: " + key + " in skills.yml");
                    continue;
                }
                int xp = firemakingConfig.getInt(key + ".xp", 0);
                int despawnTime = firemakingConfig.getInt(key + ".despawnTime", 1200);
                int requiredLevel = firemakingConfig.getInt(key + ".requiredLevel", 1);
                logPropertiesMap.put(material, new LogProperties(material, xp, despawnTime, requiredLevel));
            }
        } else {
            plugin.getLogger().warning("Firemaking configuration section not found in skills.yml.");
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack handItem = player.getInventory().getItemInMainHand();
        Material logType = handItem.getType();
        LogProperties logProperties = logPropertiesMap.get(logType);

        if (logProperties == null) {
            return;
        }

        int requiredLevel = logProperties.getRequiredLevel();
        int playerLevel = skillManager.getSkillLevel(player, SkillManager.Skill.FIREMAKING);

        // If hand item isn't a log, exit immediately
        if (handItem == null || !isLogType(handItem.getType())) {
            return;
        }

        // Always cancel placing the log since we are using it for custom behavior
        event.setCancelled(true);

        Block clickedBlock = event.getClickedBlock();

        // If there's no clicked block (could be due to some other plugin interference), exit
        if (clickedBlock == null) {
            return;
        }

        // If clicked block isn't valid ground, exit
        if (!isValidGround(clickedBlock.getType())) {
            return;
        }

        Block blockAbove = clickedBlock.getRelative(event.getBlockFace());

        // If block above isn't air, exit
        if (blockAbove.getType() != Material.AIR) {
            return;
        }

        // If player doesn't have the required level, notify and exit
        if (playerLevel < requiredLevel) {
            player.sendMessage(ChatColor.RED + "You need a firemaking level of " + requiredLevel + " to burn this log.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // Start the log ignition animation
        startIgnitionAnimation(blockAbove, player, logProperties);

        // Remove the log from player's hand
        handItem.setAmount(handItem.getAmount() - 1);
    }

    // Check if the material is a log type
    private boolean isLogType(Material material) {
        return logPropertiesMap.containsKey(material);
    }

    // Log properties class to manage different log types
    private static class LogProperties {
        private final Material material;
        private final int xpValue;
        private final int despawnTime;
        private final int requiredLevel;

        public LogProperties(Material material, int xpValue, int despawnTime, int requiredLevel) {
            this.material = material;
            this.xpValue = xpValue;
            this.despawnTime = despawnTime;
            this.requiredLevel = requiredLevel;
        }

        public int getXpValue() {
            return xpValue;
        }

        public int getDespawnTime() {
            return despawnTime;
        }

        public int getRequiredLevel() {
            return requiredLevel;
        }
    }

    // Check if the ground is a valid type for firemaking
    private boolean isValidGround(Material material) {
        switch (material) {
            case GRASS_BLOCK:
            case DIRT:
            case STONE:
            case SAND:
            case GRAVEL:
            case COBBLESTONE:
            case COBBLESTONE_SLAB:
            case COARSE_DIRT:
            case ROOTED_DIRT:
            case PODZOL:
            case DIRT_PATH:
            case MYCELIUM:
                return true;
            default:
                return false;
        }
    }

    // Start the ignition animation before placing the bonfire
    private void startIgnitionAnimation(Block block, Player player, LogProperties logProperties) {
        new BukkitRunnable() {
            int step = 0;

            @Override
            public void run() {
                if (step > 20) { // 1 second duration (20 ticks = 1 second)
                    placeBonfire(block, player, logProperties);
                    cancel();
                    return;
                }

                // Ignition particle effect
                block.getWorld().spawnParticle(Particle.FLAME, block.getLocation().add(0.5, 0.5, 0.5), 5, 0.2, 0.2, 0.2, 0.02);
                block.getWorld().playSound(block.getLocation(), Sound.ITEM_FLINTANDSTEEL_USE, 1.0f, 1.0f);

                step++;
            }
        }.runTaskTimer(plugin, 0L, 1L); // Run every tick (0.05 seconds)
    }

    private void placeBonfire(Block block, Player player, LogProperties logProperties) {
        if (block.getType() != Material.CAMPFIRE) { // Prevent placing if it's already a campfire
            block.setType(Material.CAMPFIRE);
            block.getWorld().spawnParticle(Particle.SMOKE_LARGE, block.getLocation().add(0.5, 0.5, 0.5), 30, 0.5, 0.5, 0.5, 0.05);
            player.playSound(player.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 1.0f, 1.0f);

            // Give the player XP
            grantXp(player, logProperties.getXpValue());

            // Set a timer to remove the bonfire after a log-specific time
            new BukkitRunnable() {
                @Override
                public void run() {
                    block.setType(Material.AIR);
                    dropAsh(block.getLocation());
                }
            }.runTaskLater(plugin, logProperties.getDespawnTime());
        }
    }

    // Grant XP to the player
    private void grantXp(Player player, int xpAmount) {
        if (xpAmount <= 0) {
            return;
        }
        skillManager.addXP(player, SkillManager.Skill.FIREMAKING, xpAmount);
        player.sendActionBar(ChatColor.GOLD + "FireMaking +" + xpAmount);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }

    // Drop ash item after the bonfire is done burning
    private void dropAsh(Location location) {
        ItemStack ash = new ItemStack(Material.GRAY_DYE);
        ItemMeta meta = ash.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GRAY + "Ash");
            meta.setLore(Arrays.asList(ChatColor.DARK_GRAY + "Remains of a burned log."));
            ash.setItemMeta(meta);
        }
        location.getWorld().dropItemNaturally(location, ash);
    }
}