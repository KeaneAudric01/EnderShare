package dev.keaneaudric.enderShare.listeners;

import dev.keaneaudric.enderShare.manager.EnderShareManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Listener for handling offline Ender Chest restorations.
 * When a player joins the server, if a pending restoration exists, it restores their Ender Chest.
 */
public class EnderShareOfflineRestorationListener implements Listener {

    /**
     * Called when a player joins the server.
     * If pending Ender Chest contents exist for the player, clear the current chest and restore the saved items.
     *
     * @param event The player join event.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Check if there is a pending restoration for this player.
        if (EnderShareManager.hasPendingRestoration(player.getUniqueId())) {
            ItemStack[] restorationItems = EnderShareManager.getPendingRestoration(player.getUniqueId());
            if (restorationItems != null) {
                // Clear the player's current Ender Chest and restore the saved items.
                player.getEnderChest().clear();
                player.getEnderChest().setContents(restorationItems);
                player.sendMessage(ChatColor.YELLOW + "Your Ender Chest has been restored from a previous EnderShare session.");
            }
        }
    }
}