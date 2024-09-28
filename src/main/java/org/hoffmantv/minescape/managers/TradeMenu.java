package org.hoffmantv.minescape.managers;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TradeMenu implements Listener {

    private final JavaPlugin plugin;
    private final Map<Player, TradeSession> tradeSessions = new HashMap<>();
    private final Map<Player, Player> pendingRequests = new HashMap<>();

    public TradeMenu(JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Sends a trade request to a target player with a clickable message.
     */
    public void sendTradeRequest(Player player, Player target) {
        if (pendingRequests.containsKey(target)) {
            player.sendMessage(ChatColor.RED + "This player already has a pending trade request.");
            return;
        }

        pendingRequests.put(target, player); // Store the request in the map

        TextComponent message = new TextComponent(ChatColor.LIGHT_PURPLE + player.getName() + " has sent you a trade request. ");
        TextComponent acceptButton = new TextComponent(ChatColor.GREEN + "[Click here to accept]");
        acceptButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click to accept the trade request")));
        acceptButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/accepttrade " + player.getName()));

        message.addExtra(acceptButton);
        target.spigot().sendMessage(message);

        player.sendMessage(ChatColor.LIGHT_PURPLE + "Trade request sent to " + target.getName());
    }

    public void startTrade(Player player, Player target) {
        if (!hasPendingRequest(target, player)) {
            player.sendMessage(ChatColor.RED + "No pending trade request from " + target.getName());
            return;
        }

        if (tradeSessions.containsKey(player) || tradeSessions.containsKey(target)) {
            player.sendMessage(ChatColor.RED + "One of you is already in a trade.");
            return;
        }

        removePendingRequest(target, player);

        TradeSession tradeSession = new TradeSession(player, target);
        tradeSessions.put(player, tradeSession);
        tradeSessions.put(target, tradeSession);

        player.openInventory(tradeSession.getTradeInventory(player));
        target.openInventory(tradeSession.getTradeInventory(target));
    }

    public boolean hasPendingRequest(Player target, Player player) {
        return pendingRequests.containsKey(target) && pendingRequests.get(target).equals(player);
    }

    public void removePendingRequest(Player target, Player player) {
        pendingRequests.remove(target, player);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        TradeSession session = tradeSessions.get(player);
        if (session == null || !session.isInTradeInventory(event.getInventory())) return;

        event.setCancelled(true); // Prevent normal inventory interactions

        session.handleInventoryClick(event);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        TradeSession session = tradeSessions.get(player);

        if (session != null) {
            session.cancelTrade();
            tradeSessions.remove(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        TradeSession session = tradeSessions.get(player);

        if (session != null) {
            session.cancelTrade();
            tradeSessions.remove(player);
        }
    }

    private class TradeSession {
        private final Player player1;
        private final Player player2;
        private final Inventory tradeInventory;
        private boolean confirmedPlayer1;
        private boolean confirmedPlayer2;
        private boolean tradeCancelled = false;

        public TradeSession(Player player1, Player player2) {
            this.player1 = player1;
            this.player2 = player2;
            this.tradeInventory = createTradeInventory(player1, player2);
            this.confirmedPlayer1 = false;
            this.confirmedPlayer2 = false;

            // Play sound when trade menu is opened
            playSound(player1, Sound.BLOCK_CHEST_OPEN);
            playSound(player2, Sound.BLOCK_CHEST_OPEN);
        }

        public Inventory getTradeInventory(Player player) {
            return tradeInventory;
        }

        public boolean isInTradeInventory(Inventory inventory) {
            return inventory.equals(tradeInventory);
        }

        public void handleInventoryClick(InventoryClickEvent event) {
            Player player = (Player) event.getWhoClicked();
            Inventory clickedInventory = event.getClickedInventory();

            if (clickedInventory == null) return; // No inventory clicked
            if (clickedInventory.equals(player.getInventory())) {
                // Player's inventory is clicked
                if (event.isShiftClick()) {
                    // Handle shift-click from player inventory
                    offerItemFromPlayerInventory(player, event.getCurrentItem());
                }
            } else if (clickedInventory.equals(tradeInventory)) {
                // Trade inventory is clicked
                int slot = event.getRawSlot();

                if (slot >= 0 && slot < 6 || slot >= 9 && slot < 15) {
                    // Player's own trade slot is clicked
                    if (event.getCurrentItem() != null && !event.getCurrentItem().getType().isAir()) {
                        // Remove item from trade inventory back to player inventory
                        returnItemToPlayer(player, slot);
                    }
                } else if (slot == 31) { // Confirm button slot
                    toggleConfirmation(player);
                } else if (slot == 32) { // Cancel button slot
                    cancelTrade();
                }
            }

            event.setCancelled(true); // Prevent normal inventory interactions
        }

        // Method to reset confirmation status
        public void resetConfirmationStatus() {
            confirmedPlayer1 = false;
            confirmedPlayer2 = false;
            updateStatusItem();
        }

        private void offerItemFromPlayerInventory(Player player, ItemStack item) {
            if (item == null || item.getType() == Material.AIR) return; // No item to offer

            int emptySlot = findEmptyTradeSlot(player);
            if (emptySlot != -1) {
                // Remove the item from player inventory
                player.getInventory().removeItem(item);

                // Add the item to the trade inventory
                tradeInventory.setItem(emptySlot, item);

                // Reset confirmation status and update both players' inventories
                resetConfirmationStatus();
                updateTradeInventoryForBothPlayers();

                // Play sound for adding item
                playSound(player, Sound.ENTITY_ITEM_PICKUP);
            } else {
                player.sendMessage(ChatColor.RED + "No empty slot available in trade window.");
            }
        }

        private void returnItemToPlayer(Player player, int slot) {
            ItemStack item = tradeInventory.getItem(slot);
            if (item != null && !isPlaceholder(item)) {
                player.getInventory().addItem(item);
                tradeInventory.setItem(slot, createPlaceholder());

                // Reset confirmation status and update both players' inventories
                resetConfirmationStatus();
                updateTradeInventoryForBothPlayers();

                // Play sound for removing item
                playSound(player, Sound.ENTITY_ITEM_PICKUP);
            }
        }

        private boolean isPlaceholder(ItemStack item) {
            if (item == null) return false;
            if (item.getType() == Material.BLACK_STAINED_GLASS_PANE) {
                ItemMeta meta = item.getItemMeta();
                return meta != null && meta.hasDisplayName() && meta.getDisplayName().equals(ChatColor.GRAY + "Empty");
            }
            return false;
        }

        private int findEmptyTradeSlot(Player player) {
            int startSlot = player.equals(player1) ? 0 : 9;
            int endSlot = player.equals(player1) ? 6 : 15;

            for (int i = startSlot; i < endSlot; i++) {
                ItemStack item = tradeInventory.getItem(i);
                if (item == null || item.getType() == Material.BLACK_STAINED_GLASS_PANE) {
                    return i;
                }
            }
            return -1; // No empty slot found
        }

        private void toggleConfirmation(Player player) {
            if (player.equals(player1)) {
                confirmedPlayer1 = !confirmedPlayer1;
            } else {
                confirmedPlayer2 = !confirmedPlayer2;
            }

            updateStatusItem();

            // Play sound for confirming trade
            playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING);

            if (confirmedPlayer1 && confirmedPlayer2) {
                completeTrade();
            }
        }

        private void updateTradeInventoryForBothPlayers() {
            player1.updateInventory();
            player2.updateInventory();
        }

        private void updateStatusItem() {
            if (confirmedPlayer1 && confirmedPlayer2) {
                updateConfirmationStatusItem(ChatColor.GREEN + "Both Confirmed");
            } else if (confirmedPlayer1) {
                updateConfirmationStatusItem(ChatColor.YELLOW + player1.getName() + ": Confirmed");
            } else if (confirmedPlayer2) {
                updateConfirmationStatusItem(ChatColor.YELLOW + player2.getName() + ": Confirmed");
            } else {
                updateConfirmationStatusItem(ChatColor.RED + "Not Ready");
            }
        }

        private void updateConfirmationStatusItem(String status) {
            ItemStack statusItem = createButtonItem(status, Material.PAPER);
            tradeInventory.setItem(22, statusItem);
            updateTradeInventoryForBothPlayers();
        }

        private void completeTrade() {
            // Play sound for completing trade
            playSound(player1, Sound.ENTITY_VILLAGER_YES);
            playSound(player2, Sound.ENTITY_VILLAGER_YES);

            player1.sendMessage(ChatColor.GREEN + "Trade completed successfully!");
            player2.sendMessage(ChatColor.GREEN + "Trade completed successfully!");

            player1.closeInventory();
            player2.closeInventory();

            // Transfer items between players
            transferItems();

            // Remove offered items from each player's inventory
            removeOfferedItemsFromPlayers();
        }

        private void transferItems() {
            // Transfer items from player1 to player2
            for (int i = 0; i < 6; i++) {
                ItemStack item = tradeInventory.getItem(i);
                if (item != null && !isPlaceholder(item)) {
                    player2.getInventory().addItem(item);
                }
            }

            // Transfer items from player2 to player1
            for (int i = 9; i < 15; i++) {
                ItemStack item = tradeInventory.getItem(i);
                if (item != null && !isPlaceholder(item)) {
                    player1.getInventory().addItem(item);
                }
            }
        }

        private void removeOfferedItemsFromPlayers() {
            // Remove items offered by player1
            for (int i = 0; i < 6; i++) { // Player 1's offer slots
                ItemStack item = tradeInventory.getItem(i);
                if (item != null && !isPlaceholder(item)) {
                    player1.getInventory().removeItem(item);
                }
            }

            // Remove items offered by player2
            for (int i = 9; i < 15; i++) { // Player 2's offer slots
                ItemStack item = tradeInventory.getItem(i);
                if (item != null && !isPlaceholder(item)) {
                    player2.getInventory().removeItem(item);
                }
            }
        }

        public void cancelTrade() {
            if (tradeCancelled) return;

            tradeCancelled = true;
            player1.sendMessage(ChatColor.RED + "Trade cancelled.");
            player2.sendMessage(ChatColor.RED + "Trade cancelled.");
            player1.closeInventory();
            player2.closeInventory();

            // Play sound for canceling trade
            playSound(player1, Sound.ENTITY_VILLAGER_NO);
            playSound(player2, Sound.ENTITY_VILLAGER_NO);

            returnItemsToPlayers();
        }

        private void returnItemsToPlayers() {
            for (int i = 0; i < 6; i++) {
                ItemStack item = tradeInventory.getItem(i);
                if (item != null && !isPlaceholder(item)) {
                    player1.getInventory().addItem(item);
                }
            }

            for (int i = 9; i < 15; i++) {
                ItemStack item = tradeInventory.getItem(i);
                if (item != null && !isPlaceholder(item)) {
                    player2.getInventory().addItem(item);
                }
            }
        }

        private Inventory createTradeInventory(Player player1, Player player2) {
            Inventory tradeInventory = Bukkit.createInventory(null, 45, ChatColor.GOLD + "Trading With: " + player1.getName() + " ↔ " + player2.getName());

            // Fill placeholders for both offers
            for (int i = 0; i < 6; i++) {
                tradeInventory.setItem(i, createPlaceholder());
                tradeInventory.setItem(i + 9, createPlaceholder());
            }

            for (int i = 27; i < 33; i++) {
                tradeInventory.setItem(i, createPlaceholder());
                tradeInventory.setItem(i + 9, createPlaceholder());
            }

            // Add labels for "Your Offer" and "Opponent's Offer"
            tradeInventory.setItem(0, createLabelItem(ChatColor.YELLOW + "Your Offer"));
            tradeInventory.setItem(27, createLabelItem(ChatColor.YELLOW + "Opponent's Offer"));

            // Add status and buttons
            tradeInventory.setItem(22, createButtonItem(ChatColor.RED + "Not Ready", Material.PAPER));
            tradeInventory.setItem(31, createButtonItem(ChatColor.GREEN + "Accept", Material.LIME_CONCRETE));
            tradeInventory.setItem(32, createButtonItem(ChatColor.RED + "Decline", Material.RED_CONCRETE));

            return tradeInventory;
        }

        private ItemStack createPlaceholder() {
            ItemStack placeholder = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
            ItemMeta meta = placeholder.getItemMeta();
            meta.setDisplayName(ChatColor.GRAY + "Empty");
            placeholder.setItemMeta(meta);
            return placeholder;
        }

        private ItemStack createButtonItem(String name, Material material) {
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(name);
            item.setItemMeta(meta);
            return item;
        }

        private ItemStack createLabelItem(String name) {
            ItemStack item = new ItemStack(Material.NAME_TAG);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(name);
            meta.setLore(Collections.singletonList("Label")); // Mark this item as a label
            item.setItemMeta(meta);
            return item;
        }

        private void playSound(Player player, Sound sound) {
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        }
    }
}