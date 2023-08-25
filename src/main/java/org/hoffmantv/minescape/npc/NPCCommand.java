package org.hoffmantv.minescape.npc;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.NPC;
import org.bukkit.entity.Player;

public class NPCCommand implements CommandExecutor {

    private final NPCManager npcManager;

    public NPCCommand(NPCManager npcManager) {
        this.npcManager = npcManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sender.sendMessage("Please provide an NPC name.");
            return false;
        }

        String name = args[0];
        Location loc = player.getLocation();
        NPC npc = npcManager.createNPC(name, loc);
        player.sendMessage("NPC " + npc.getName() + " spawned at your location!");

        return true;
    }
}
