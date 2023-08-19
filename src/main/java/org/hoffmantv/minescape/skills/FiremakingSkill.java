package org.hoffmantv.minescape.skills;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
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

        Material logType = handItem.getType();
        int requiredLevel = getRequiredLevel(logType);
        int playerLevel = skillManager.getSkillLevel(player, SkillManager.Skill.FIREMAKING);

        // 4. If player doesn't have the required level, notify and exit
        if (playerLevel < requiredLevel) {
            player.sendMessage(ChatColor.RED + "You need a firemaking level of " + requiredLevel + " to burn this log.");
            return;
        }

        // Give the player XP
        int xpValue = getFiremakingXp(logType);
        if (xpValue > 0) {
            grantXp(player, xpValue);
        }

        // Place the bonfire
        placeBonfire(blockAbove, logType);

        // Remove the log from player's hand
        handItem.setAmount(handItem.getAmount() - 1);

        // Set a timer to remove the bonfire after 20 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                blockAbove.setType(Material.AIR);
            }
        }.runTaskLater(plugin, 20 * 20);
    }
    private boolean isLogType(Material material) {
        switch (material) {
            case OAK_LOG:
            case BIRCH_LOG:
            case SPRUCE_LOG:
            case JUNGLE_LOG:
            case ACACIA_LOG:
            case DARK_OAK_LOG:
                return true;
            default:
                return false;
        }
    }

    private int getFiremakingXp(Material log) {
        switch (log) {
            case OAK_LOG:
                return 10;  // Example XP for oak log
            case SPRUCE_LOG:
                return 12;  // Example XP for spruce log
            case BIRCH_LOG:
                return 14;  // And so on...
            case JUNGLE_LOG:
                return 16;
            case ACACIA_LOG:
                return 18;
            case DARK_OAK_LOG:
                return 20;
            default:
                return 0;
        }
    }

    private int getRequiredLevel(Material log) {
        switch (log) {
            case OAK_LOG:
                return 1;   // Level 1 required for oak log
            case SPRUCE_LOG:
                return 5;   // Level 5 required for spruce log
            case BIRCH_LOG:
                return 10;  // And so on...
            case JUNGLE_LOG:
                return 15;
            case ACACIA_LOG:
                return 20;
            case DARK_OAK_LOG:
                return 25;
            default:
                return 0;
        }
    }

    private boolean isValidGround(Material material) {
        switch (material) {
            case GRASS_BLOCK:
            case DIRT:
            case STONE:
            case SAND:
            case GRAVEL:
                return true;
            default:
                return false;
        }
    }

    private void placeBonfire(Block block, Material logType) {
        block.setType(Material.CAMPFIRE);
        block.getWorld().spawnParticle(Particle.SMOKE_LARGE, block.getLocation().add(0.5, 0.5, 0.5), 30, 0.5, 0.5, 0.5, 0.05);
    }

    private void grantXp(Player player, int xpAmount) {
        if (xpAmount <= 0) {
            return; // if the xpAmount is 0 or less, simply return without doing anything.
        }
        skillManager.addXP(player, SkillManager.Skill.FIREMAKING, xpAmount);
        skillManager.saveSkillsToConfig();
        player.sendMessage(ChatColor.GREEN + "You gained " + xpAmount + " firemaking XP!");
    }
}
