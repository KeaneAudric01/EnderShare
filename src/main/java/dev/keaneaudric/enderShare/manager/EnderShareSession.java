package dev.keaneaudric.enderShare.manager;

import org.bukkit.inventory.Inventory;
import java.util.UUID;

/**
 * Represents a shared Ender Chest session between two players.
 * Contains both players’ UUIDs, the shared inventory, and a unique session identifier.
 */
public class EnderShareSession {
    private final UUID player1;
    private final UUID player2;
    private Inventory sharedInventory;
    private final String sessionId;

    /**
     * Creates a new sharing session with a freshly generated unique session identifier.
     *
     * @param player1         The first player's UUID.
     * @param player2         The second player's UUID.
     * @param sharedInventory The shared inventory to be used during the session.
     */
    public EnderShareSession(UUID player1, UUID player2, Inventory sharedInventory) {
        this.player1 = player1;
        this.player2 = player2;
        this.sharedInventory = sharedInventory;
        this.sessionId = UUID.randomUUID().toString();
    }

    /**
     * Constructs a session using data loaded from persistent storage.
     *
     * @param player1         The first player's UUID.
     * @param player2         The second player's UUID.
     * @param sharedInventory The shared inventory for the session.
     * @param sessionId       The session identifier read from storage.
     */
    public EnderShareSession(UUID player1, UUID player2, Inventory sharedInventory, String sessionId) {
        this.player1 = player1;
        this.player2 = player2;
        this.sharedInventory = sharedInventory;
        this.sessionId = sessionId;
    }

    /**
     * Returns the first player's UUID.
     *
     * @return The UUID of player1.
     */
    public UUID getPlayer1() {
        return player1;
    }

    /**
     * Returns the second player's UUID.
     *
     * @return The UUID of player2.
     */
    public UUID getPlayer2() {
        return player2;
    }

    /**
     * Returns the shared inventory for this session.
     *
     * @return The shared Inventory.
     */
    public Inventory getSharedInventory() {
        return sharedInventory;
    }

    /**
     * Updates the shared inventory held by this session.
     *
     * @param sharedInventory The new inventory state.
     */
    public void setSharedInventory(Inventory sharedInventory) {
        this.sharedInventory = sharedInventory;
    }

    /**
     * Returns the unique session identifier.
     *
     * @return The session ID.
     */
    public String getSessionId() {
        return sessionId;
    }
}