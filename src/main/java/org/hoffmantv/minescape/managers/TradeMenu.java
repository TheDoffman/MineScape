package org.hoffmantv.minescape.managers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TradeMenu implements Listener {

    private final JavaPlugin plugin;
    private final Map<UUID, UUID> tradePartners = new HashMap<>();
    private final Map<UUID, Boolean> playerConfirmed = new HashMap<>();
    private final Map<UUID, Inventory> openTradeInventories = new HashMap<>();

    public TradeMenu(JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Initiates a trade between two players.
     *
     * @param player1 The first player initiating the trade.
     * @param player2 The second player to trade with.
     */
    public void startTrade(Player player1, Player player2) {
        if (player1 == null || player2 == null || player1.equals(player2)) {
            player1.sendMessage(ChatColor.RED + "Invalid trade request.");
            return;
        }

        tradePartners.put(player1.getUniqueId(), player2.getUniqueId());
        tradePartners.put(player2.getUniqueId(), player1.getUniqueId());

        playerConfirmed.put(player1.getUniqueId(), false);
        playerConfirmed.put(player2.getUniqueId(), false);

        openTradeMenu(player1);
        openTradeMenu(player2);
    }

    /**
     * Opens the custom trade menu for a player.
     *
     * @param player The player to open the menu for.
     */
    private void openTradeMenu(Player player) {
        Inventory tradeMenu = Bukkit.createInventory(null, 54, ChatColor.DARK_GREEN + "Custom Trade Menu");

        // Add placeholder items for all slots
        for (int i = 0; i < 54; i++) {
            if (i < 27 || i > 35) { // Player trade areas and border
                tradeMenu.setItem(i, createPlaceholderItem());
            }
        }

        // Add confirm buttons (slots 27 and 35)
        tradeMenu.setItem(27, createConfirmButton(player.getName()));
        tradeMenu.setItem(35, createConfirmButton("Confirm Trade"));

        // Save the trade menu
        openTradeInventories.put(player.getUniqueId(), tradeMenu);

        // Open the trade menu for the player
        player.openInventory(tradeMenu);
    }

    /**
     * Creates a placeholder item for empty slots.
     *
     * @return A placeholder ItemStack.
     */
    private ItemStack createPlaceholderItem() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Creates a confirm button for the trade menu.
     *
     * @param name The name to display on the button.
     * @return A confirm button ItemStack.
     */
    private ItemStack createConfirmButton(String name) {
        ItemStack item = new ItemStack(Material.GREEN_WOOL);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + name);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (event.getView().getTitle().equals(ChatColor.DARK_GREEN + "Custom Trade Menu")) {
            event.setCancelled(true); // Prevents movement of placeholder items

            UUID playerUUID = player.getUniqueId();
            Inventory clickedInventory = event.getInventory();

            if (!openTradeInventories.containsKey(playerUUID)) return;

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null) return;

            // Handle confirm button click
            if (clickedItem.getType() == Material.GREEN_WOOL) {
                playerConfirmed.put(playerUUID, true);
                player.sendMessage(ChatColor.GREEN + "You confirmed your trade offer!");

                // Check if both players have confirmed
                UUID partnerUUID = tradePartners.get(playerUUID);
                if (playerConfirmed.get(playerUUID) && playerConfirmed.get(partnerUUID)) {
                    Player partner = Bukkit.getPlayer(partnerUUID);
                    if (partner != null) {
                        executeTrade(player, partner);
                    }
                }
            }
        }
    }

    /**
     * Executes the trade between two players.
     *
     * @param player1 The first player.
     * @param player2 The second player.
     */
    private void executeTrade(Player player1, Player player2) {
        Inventory inventory1 = openTradeInventories.get(player1.getUniqueId());
        Inventory inventory2 = openTradeInventories.get(player2.getUniqueId());

        if (inventory1 == null || inventory2 == null) {
            player1.sendMessage(ChatColor.RED + "Trade failed due to an internal error.");
            player2.sendMessage(ChatColor.RED + "Trade failed due to an internal error.");
            return;
        }

        // Validate the items before trade execution
        if (!validateTrade(inventory1, inventory2)) {
            player1.sendMessage(ChatColor.RED + "Trade validation failed.");
            player2.sendMessage(ChatColor.RED + "Trade validation failed.");
            return;
        }

        // Perform the item transfer
        transferItems(inventory1, player2);
        transferItems(inventory2, player1);

        // Close the trade menus and notify players
        player1.closeInventory();
        player2.closeInventory();
        player1.sendMessage(ChatColor.GREEN + "Trade completed successfully!");
        player2.sendMessage(ChatColor.GREEN + "Trade completed successfully!");

        // Cleanup
        tradePartners.remove(player1.getUniqueId());
        tradePartners.remove(player2.getUniqueId());
        playerConfirmed.remove(player1.getUniqueId());
        playerConfirmed.remove(player2.getUniqueId());
        openTradeInventories.remove(player1.getUniqueId());
        openTradeInventories.remove(player2.getUniqueId());
    }

    /**
     * Validates the items in both inventories.
     *
     * @param inventory1 The first inventory.
     * @param inventory2 The second inventory.
     * @return True if the trade is valid, false otherwise.
     */
    private boolean validateTrade(Inventory inventory1, Inventory inventory2) {
        // Add any validation logic for trade here (e.g., no banned items, sufficient space, etc.)
        return true;
    }

    /**
     * Transfers items from one inventory to the player's inventory.
     *
     * @param inventory The trade inventory to transfer items from.
     * @param player    The player to receive the items.
     */
    private void transferItems(Inventory inventory, Player player) {
        for (int i = 0; i < 27; i++) { // Player's trade area
            ItemStack item = inventory.getItem(i);
            if (item != null && item.getType() != Material.GRAY_STAINED_GLASS_PANE) {
                player.getInventory().addItem(item);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        if (event.getView().getTitle().equals(ChatColor.DARK_GREEN + "Custom Trade Menu")) {
            UUID playerUUID = player.getUniqueId();
            UUID partnerUUID = tradePartners.get(playerUUID);

            if (partnerUUID != null) {
                Player partner = Bukkit.getPlayer(partnerUUID);
                if (partner != null && partner.isOnline()) {
                    partner.sendMessage(ChatColor.RED + "The trade was canceled because the other player closed the menu.");
                    partner.closeInventory();
                }
            }

            // Cleanup
            tradePartners.remove(playerUUID);
            playerConfirmed.remove(playerUUID);
            openTradeInventories.remove(playerUUID);
        }
    }
}