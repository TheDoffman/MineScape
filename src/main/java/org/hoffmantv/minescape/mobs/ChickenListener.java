package org.hoffmantv.minescape.mobs;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Chicken;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ChickenListener implements Listener {

    private final Plugin plugin;

    public ChickenListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        // Check if the dead entity is a Chicken
        if (event.getEntity() instanceof Chicken) {
            // Clear existing drops
            event.getDrops().clear();

            // Create the custom bone item
            ItemStack chickenBone = new ItemStack(Material.BONE);
            ItemMeta meta = chickenBone.getItemMeta();

            if (meta != null) {
                // Set the display name to "Chicken Bone"
                meta.setDisplayName(ChatColor.WHITE + "Chicken Bone");

                // Make the bone non-stackable by adding a unique NBT tag
                NamespacedKey key = new NamespacedKey(plugin, "uniqueID");
                meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, UUID.randomUUID().toString());

                // Optionally, add lore to the item
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "A bone from a chicken.");
                meta.setLore(lore);

                chickenBone.setItemMeta(meta);

                // Add the custom bone to the drops
                event.getDrops().add(chickenBone);
            }
        }
    }
}