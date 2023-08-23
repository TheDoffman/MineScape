package org.hoffmantv.minescape.mobs;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Sheep;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.UUID;

public class SheepListener implements Listener {

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        // Check if the dead entity is a Chicken
        if (event.getEntity() instanceof Sheep) {
            Sheep sheep = (Sheep) event.getEntity();

            // Clear existing drops
            event.getDrops().clear();

            ItemStack bone = new ItemStack(Material.BONE);
            ItemMeta meta = bone.getItemMeta();

            // Setting the bone's name to "Chicken Bone"
            meta.setDisplayName(sheep.getType().toString() + " Bone");

            // Make the bone non-stackable
            AttributeModifier modifier = new AttributeModifier(UUID.randomUUID(), "nonStackable", 0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND);
            meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, modifier);

            bone.setItemMeta(meta);

            // Adding the custom bone to the chicken's drops
            event.getDrops().add(bone);
        }
    }
}
