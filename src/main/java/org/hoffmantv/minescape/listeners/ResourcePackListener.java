package org.hoffmantv.minescape.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class ResourcePackListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String resourcePackURL = "http://example.com/my-resource-pack.zip";

        player.setResourcePack(resourcePackURL);
    }
}
