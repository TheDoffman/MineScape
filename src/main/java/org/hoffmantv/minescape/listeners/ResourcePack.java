package org.hoffmantv.minescape.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class ResourcePack implements Listener {

    private JavaPlugin plugin;

    public ResourcePack(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        boolean isEnabled = plugin.getConfig().getBoolean("ResourcePack.enabled");
        String resourcePackURL = plugin.getConfig().getString("ResourcePack.url");

        if (isEnabled && resourcePackURL != null && !resourcePackURL.isEmpty()) {
            Player player = event.getPlayer();
            player.setResourcePack(resourcePackURL);
        }
    }
}
