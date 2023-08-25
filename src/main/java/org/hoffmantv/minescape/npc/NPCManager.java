package org.hoffmantv.minescape.npc;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.NPC;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NPCManager {

    private List<NPC> npcs = new ArrayList<>();
    private FileConfiguration config;
    private File npcFile;
    private JavaPlugin plugin;

    public NPCManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadNPCs();
    }

    public NPC createNPC(String name, Location location) {
        CustomNPC npc = new CustomNPC(name, location);
        npcs.add(npc);
        saveNPCs();  // Save the NPCs when a new one is created.
        return npc;
    }

    public void loadNPCs() {
        npcFile = new File(plugin.getDataFolder(), "npcs.yml");
        config = YamlConfiguration.loadConfiguration(npcFile);

        if (config.contains("npcs")) {
            for (String key : config.getConfigurationSection("npcs").getKeys(false)) {
                String name = config.getString("npcs." + key + ".name");
                Location location = (Location) config.get("npcs." + key + ".location");
                CustomNPC npc = new CustomNPC(name, location);
                npcs.add(npc);
            }
        }
    }

    public void saveNPCs() {
        for (NPC npc : npcs) {
            config.set("npcs." + npc.getUUID() + ".name", npc.getName());
            config.set("npcs." + npc.getUUID() + ".location", npc.getLocation());
        }

        try {
            config.save(npcFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public NPC getNPCByName(String name) {
        for (NPC npc : npcs) {
            if (npc.getName().equalsIgnoreCase(name)) {
                return npc;
            }
        }
        return null;
    }

    public void removeNPC(NPC npc) {
        npcs.remove(npc);
        config.set("npcs." + npc.getUUID(), null); // Remove from config
        saveNPCs();
    }
}



