package dev.keaneaudric.enderShare.listeners;

import dev.keaneaudric.enderShare.EnderShare;
import dev.keaneaudric.enderShare.manager.EnderShareManager;
import dev.keaneaudric.enderShare.manager.EnderShareSession;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Listener for player interactions with Ender Chests.
 * If a player is currently in an active sharing session, the default
 * behavior is overridden and the shared inventory is opened instead.
 */
public class EnderShareListener implements Listener {

    /**
     * Processes a right-click action on an Ender Chest.
     * Cancels the event’s default behavior and opens the shared inventory if the player is in a session.
     *
     * @param event The player interact event.
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Only continue if the action is a right-click on a block.
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        // Verify that the clicked block exists and is an Ender Chest.
        if (clickedBlock == null || clickedBlock.getType() != Material.ENDER_CHEST) {
            return;
        }

        Player player = event.getPlayer();
        // If the player is in an active session, override default behavior.
        if (EnderShareManager.isSharing(player.getUniqueId())) {
            event.setCancelled(true); // Prevent the vanilla Ender Chest inventory from opening.
            EnderShareSession session = EnderShareManager.getSession(player.getUniqueId());
            if (session != null) {
                player.openInventory(session.getSharedInventory());
                player.sendMessage(ChatColor.GREEN + "Shared Ender Chest opened.");
            }
        }
    }
}