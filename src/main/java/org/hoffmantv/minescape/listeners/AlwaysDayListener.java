package org.hoffmantv.minescape.listeners;

import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class AlwaysDayListener implements Listener {

    private final JavaPlugin plugin;

    public AlwaysDayListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        if (plugin.getConfig().getBoolean("always-day")) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    World world = event.getWorld();
                    world.setTime(1000);  // This sets the world time to day
                }
            }.runTaskTimer(plugin, 0, 100);
        }
    }
}
