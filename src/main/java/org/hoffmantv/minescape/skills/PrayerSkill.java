package org.hoffmantv.minescape.skills;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Animals;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.hoffmantv.minescape.managers.SkillManager;

import java.util.UUID;

public class PrayerSkill implements Listener {
    private final SkillManager skillManager;
    private final JavaPlugin plugin;

    public PrayerSkill(SkillManager skillManager, JavaPlugin plugin) {
        this.skillManager = skillManager;
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack handItem = player.getInventory().getItemInMainHand();

        if (handItem == null || handItem.getType() != Material.BONE || !handItem.hasItemMeta() || !handItem.getItemMeta().hasDisplayName()) {
            return;
        }

        // Check if the player right-clicked on the ground
        if (event.getClickedBlock() == null || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        // Cancel the default action
        event.setCancelled(true);

        // Get the location of the clicked block
        Location clickedLocation = event.getClickedBlock().getLocation().add(0.5, 0, 0.5); // Center of the block

        // Simulate digging effect
        clickedLocation.getWorld().playEffect(clickedLocation, Effect.STEP_SOUND, Material.DIRT);

        // Simulate burying with smoke/dust particles
        player.getWorld().spawnParticle(Particle.BLOCK_DUST, clickedLocation, 30, Material.DIRT.createBlockData());

        // Play burying sound
        player.getWorld().playSound(clickedLocation, Sound.BLOCK_GRAVEL_PLACE, 1.0f, 1.0f);

        // Reduce the number of bones in the player's hand by 1
        handItem.setAmount(handItem.getAmount() - 1);

        // Grant XP for burying
        int xpValue = 10;  // Adjust based on the type of bone or other factors
        grantXp(player, xpValue);
    }



    private void grantXp(Player player, int xpAmount) {
        if (xpAmount <= 0) {
            return;
        }

        skillManager.addXP(player, SkillManager.Skill.PRAYER, xpAmount);
        skillManager.saveSkillsToConfig();
        player.sendActionBar(ChatColor.GOLD + "Prayer +" + xpAmount);

    }
}
