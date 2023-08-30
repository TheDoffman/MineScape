package org.hoffmantv.minescape.npc;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.hoffmantv.minescape.MineScape;

import java.io.IOException;

public class CreateNPCCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 1) {
            player.sendMessage("Usage: /createnpc <name>");
            return true;
        }

        String name = args[0];
        Location location = player.getLocation();

        // Create NPC
        spawnNPC(name, location);

        // Save NPC data to config
        MineScape plugin = JavaPlugin.getPlugin(MineScape.class);
        FileConfiguration npcConfig = plugin.getNpcConfig();
        npcConfig.set(name + ".world", location.getWorld().getName());
        npcConfig.set(name + ".x", location.getX());
        npcConfig.set(name + ".y", location.getY());
        npcConfig.set(name + ".z", location.getZ());

        try {
            npcConfig.save(plugin.getNpcFile());
        } catch (IOException e) {
            e.printStackTrace();
        }

        player.sendMessage("NPC created.");

        return true;
    }

    private void spawnNPC(String name, Location location) {
        World world = location.getWorld();

        // Create a Villager NPC
        Villager npc = (Villager) world.spawnEntity(location, EntityType.VILLAGER);

        // Set NPC properties
        npc.setCustomName(name);
        npc.setCustomNameVisible(true);
        npc.setAI(false);  // disable AI if you don't want the NPC to move

        // Assign equipment (Optional)
        ItemStack weapon = new ItemStack(Material.DIAMOND_SWORD);
        ItemStack armor = new ItemStack(Material.DIAMOND_HELMET);
        npc.getEquipment().setItemInMainHand(weapon);
        npc.getEquipment().setHelmet(armor);
        // Add additional customization as needed
    }
}
