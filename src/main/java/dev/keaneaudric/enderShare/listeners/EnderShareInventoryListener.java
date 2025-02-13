package dev.keaneaudric.enderShare.listeners;

import dev.keaneaudric.enderShare.EnderShare;
import dev.keaneaudric.enderShare.manager.EnderShareManager;
import dev.keaneaudric.enderShare.manager.EnderShareSession;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Listener for managing inventory events on the shared Ender Chest.
 * Handles click, drag, and close events with a debounce mechanism to
 * batch rapid changes before saving to persistent storage.
 */
public class EnderShareInventoryListener implements Listener {
    // Stores scheduled update tasks for sessions (used for debouncing).
    private final Map<EnderShareSession, BukkitTask> scheduledTasks = new HashMap<>();

    // Delay in ticks for debouncing (20 ticks ~ 1 second).
    private static final long DEBOUNCE_DELAY = 20L;

    /**
     * Listens for item click events in the shared Ender Chest inventory.
     * Schedules an inventory update after a debounce delay.
     *
     * @param event The inventory click event.
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!"Shared Ender Chest".equals(title)) return;
        Inventory inv = event.getView().getTopInventory();
        EnderShareSession session = getSessionFromInventory(inv);
        if (session == null) return;
        scheduleDebouncedUpdate(session);
    }

    /**
     * Listens for item drag events in the shared Ender Chest inventory.
     * Schedules an inventory update after a debounce delay.
     *
     * @param event The inventory drag event.
     */
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        String title = event.getView().getTitle();
        if (!"Shared Ender Chest".equals(title)) return;
        Inventory inv = event.getView().getTopInventory();
        EnderShareSession session = getSessionFromInventory(inv);
        if (session == null) return;
        scheduleDebouncedUpdate(session);
    }

    /**
     * Called when the shared Ender Chest inventory is closed.
     * Cancels any pending update and forces an immediate save.
     *
     * @param event The inventory close event.
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        String title = event.getView().getTitle();
        if (!"Shared Ender Chest".equals(title)) return;
        Inventory inv = event.getView().getTopInventory();
        EnderShareSession session = getSessionFromInventory(inv);
        if (session == null) return;

        // Cancel any scheduled update for this session.
        if (scheduledTasks.containsKey(session)) {
            scheduledTasks.get(session).cancel();
            scheduledTasks.remove(session);
        }
        // Immediately update the session inventory.
        EnderShareManager.updateSessionInventory(inv, session);
    }

    /**
     * Schedules a delayed update for the given session to handle rapid inventory changes.
     *
     * @param session The EnderShare session to update.
     */
    private void scheduleDebouncedUpdate(final EnderShareSession session) {
        // Cancel any previously scheduled update.
        if (scheduledTasks.containsKey(session)) {
            scheduledTasks.get(session).cancel();
        }
        // Schedule a new update after the debounce delay.
        BukkitTask task = Bukkit.getScheduler().runTaskLater(EnderShare.getInstance(), () -> {
            EnderShareManager.updateSessionInventory(session.getSharedInventory(), session);
            scheduledTasks.remove(session);
        }, DEBOUNCE_DELAY);
        scheduledTasks.put(session, task);
    }

    /**
     * Retrieves the EnderShare session associated with a given inventory.
     *
     * @param inventory The inventory to match.
     * @return The corresponding EnderShareSession, or null if none match.
     */
    private EnderShareSession getSessionFromInventory(Inventory inventory) {
        Collection<EnderShareSession> sessions = EnderShareManager.getAllSessions();
        for (EnderShareSession session : sessions) {
            if (session.getSharedInventory() == inventory) {
                return session;
            }
        }
        return null;
    }
}