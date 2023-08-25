package org.hoffmantv.minescape.npc;

import org.bukkit.Location;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.net.URL;
import java.util.UUID;

public class CustomNPC {

    private String name;
    private UUID uuid;
    private Location location;

    public CustomNPC(String name, Location location) {
        this.name = name;
        this.uuid = UUID.randomUUID(); // Generate a unique ID for the NPC.
        this.location = location;
    }

    // Getters and Setters
    public String getName() { return name; }
    public UUID getUUID() { return uuid; }
    public Location getLocation() { return location; }

    public void setSkin(URL skinURL) {
        // Apply the skin. Requires packet manipulation using libraries like ProtocolLib.
    }

    public void equip(ItemStack item, EquipmentSlot slot) {
        // Equip the item. Again, might require packets or using libraries.
    }
}

