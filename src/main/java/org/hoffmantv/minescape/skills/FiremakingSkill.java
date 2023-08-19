package org.hoffmantv.minescape.skills;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.hoffmantv.minescape.managers.SkillManager;

public class FiremakingSkill implements Listener {
    private final SkillManager skillManager;
    private final JavaPlugin plugin;

    public FiremakingSkill(SkillManager skillManager, JavaPlugin plugin) {
        this.skillManager = skillManager;
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack handItem = player.getInventory().getItemInMainHand();
        Material logType = handItem.getType();
        LogProperties logProperties = LogProperties.fromMaterial(logType);

        if (logProperties == null) {
            return;  // Not a recognized log type
        }

        int requiredLevel = logProperties.getRequiredLevel();
        int playerLevel = skillManager.getSkillLevel(player, SkillManager.Skill.FIREMAKING);

        // 1. If hand item isn't a log, exit immediately
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

        // 2. If clicked block isn't valid ground, exit
        if (!isValidGround(clickedBlock.getType())) {
            return;
        }

        Block blockAbove = clickedBlock.getRelative(event.getBlockFace());

        // 3. If block above isn't air, exit
        if (blockAbove.getType() != Material.AIR) {
            return;
        }

        // 4. If player doesn't have the required level, notify and exit
        if (playerLevel < requiredLevel) {
            player.sendMessage(ChatColor.RED + "You need a firemaking level of " + requiredLevel + " to burn this log.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // Give the player XP
        grantXp(player, logProperties.getXpValue());

        // Place the bonfire
        placeBonfire(blockAbove, logType, player);

        // Remove the log from player's hand
        handItem.setAmount(handItem.getAmount() - 1);

        // Set a timer to remove the bonfire after a log-specific time
        new BukkitRunnable() {
            @Override
            public void run() {
                blockAbove.setType(Material.AIR);
            }
        }.runTaskLater(plugin, logProperties.getDespawnTime());
    }
    private boolean isLogType(Material material) {
        return LogProperties.fromMaterial(material) != null;
    }
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

    public static LogProperties fromMaterial(Material material) {
        switch (material) {
            case OAK_LOG:
                return new LogProperties(material, 10, 20 * 60, 1);
            case SPRUCE_LOG:
                return new LogProperties(material, 12, 20 * 60, 30);
            case BIRCH_LOG:
                return new LogProperties(material, 14, 20 * 60, 20);
            case JUNGLE_LOG:
                return new LogProperties(material, 16, 20 * 70, 40);
            case ACACIA_LOG:
                return new LogProperties(material, 18, 20 * 80, 50);
            case DARK_OAK_LOG:
                return new LogProperties(material, 20, 20 * 60, 10);
            default:
                return null;
        }
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

    private boolean isValidGround(Material material) {
        switch (material) {
            case GRASS_BLOCK:
            case DIRT:
            case STONE:
            case SAND:
            case GRAVEL:
            case COBBLESTONE:
            case COBBLESTONE_SLAB:
                return true;
            default:
                return false;
        }
    }

    private void placeBonfire(Block block, Material logType, Player player) {
        block.setType(Material.CAMPFIRE);
        block.getWorld().spawnParticle(Particle.SMOKE_LARGE, block.getLocation().add(0.5, 0.5, 0.5), 30, 0.5, 0.5, 0.5, 0.05);
        player.playSound(player.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 1.0f, 1.0f);
    }

    private void grantXp(Player player, int xpAmount) {
        if (xpAmount <= 0) {
            return; // if the xpAmount is 0 or less, simply return without doing anything.
        }
        skillManager.addXP(player, SkillManager.Skill.FIREMAKING, xpAmount);
        skillManager.saveSkillsToConfig();
        player.sendMessage(ChatColor.GREEN + "You gained " + xpAmount + " firemaking XP!");
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }
}
