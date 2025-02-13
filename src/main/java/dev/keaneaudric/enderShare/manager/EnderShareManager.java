package dev.keaneaudric.enderShare.manager;

import dev.keaneaudric.enderShare.EnderShare;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Manages active sharing sessions, pending Ender Chest restorations, and pending sharing invitations.
 * Provides persistent storage of session data and restoration items.
 */
public class EnderShareManager {

    // Active sharing sessions are stored by mapping each player UUID to their session.
    private static Map<UUID, EnderShareSession> sessions = new HashMap<>();
    private static JavaPlugin plugin;

    // Stores arrays of items for players whose Ender Chest restorations are pending.
    private static Map<UUID, ItemStack[]> pendingRestorations = new HashMap<>();

    // Stores pending sharing invitations keyed by the invitee's UUID.
    private static Map<UUID, PendingInvitation> pendingInvitations = new HashMap<>();

    /**
     * Represents a pending invitation from one player to another.
     */
    public static class PendingInvitation {
        private UUID inviter;
        private UUID invitee;
        private long timestamp;

        /**
         * Constructs a pending invitation.
         *
         * @param inviter The UUID of the player sending the invitation.
         * @param invitee The UUID of the player receiving the invitation.
         */
        public PendingInvitation(UUID inviter, UUID invitee) {
            this.inviter = inviter;
            this.invitee = invitee;
            this.timestamp = System.currentTimeMillis();
        }

        /**
         * Returns the inviter's UUID.
         *
         * @return The UUID of the inviter.
         */
        public UUID getInviter() {
            return inviter;
        }

        /**
         * Returns the invitee's UUID.
         *
         * @return The UUID of the invitee.
         */
        public UUID getInvitee() {
            return invitee;
        }

        /**
         * Returns the timestamp when the invitation was created.
         *
         * @return The creation timestamp in milliseconds.
         */
        public long getTimestamp() {
            return timestamp;
        }
    }

    /**
     * Initializes the EnderShareManager.
     * Loads any existing sessions from persistent storage.
     *
     * @param p The plugin instance.
     */
    public static void initialize(JavaPlugin p) {
        plugin = p;
        loadSessions();
    }

    /**
     * Loads sharing sessions from disk. Files are stored in the "chestdata" folder.
     */
    private static void loadSessions() {
        sessions.clear();
        File chestDataFolder = new File(plugin.getDataFolder(), "chestdata");
        if (!chestDataFolder.exists()) {
            chestDataFolder.mkdirs();
            return;
        }
        File[] sessionFiles = chestDataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (sessionFiles == null) return;

        for (File sessionFile : sessionFiles) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(sessionFile);
            String sessionId = sessionFile.getName().replace(".yml", "");
            UUID p1 = UUID.fromString(config.getString("player1"));
            UUID p2 = UUID.fromString(config.getString("player2"));
            // Create a new shared inventory.
            Inventory inv = Bukkit.createInventory(null, 54, "Shared Ender Chest");
            // Load the stored inventory items.
            for (String key : config.getConfigurationSection("inventory").getKeys(false)) {
                int slot = Integer.parseInt(key);
                ItemStack item = config.getItemStack("inventory." + key);
                inv.setItem(slot, item);
            }
            EnderShareSession session = new EnderShareSession(p1, p2, inv, sessionId);
            sessions.put(p1, session);
            sessions.put(p2, session);
        }
    }

    /**
     * Checks if the given player is currently in a sharing session.
     *
     * @param playerId The player's UUID.
     * @return true if the player is sharing; false otherwise.
     */
    public static boolean isSharing(UUID playerId) {
        return sessions.containsKey(playerId);
    }

    /**
     * Retrieves the sharing session associated with the given player.
     *
     * @param playerId The player's UUID.
     * @return The EnderShareSession if one exists, otherwise null.
     */
    public static EnderShareSession getSession(UUID playerId) {
        return sessions.get(playerId);
    }

    /**
     * Starts a new sharing session between two players.
     *
     * @param p1  The first player.
     * @param p2  The second player.
     * @param inv The shared inventory to use.
     */
    public static void startSession(Player p1, Player p2, Inventory inv) {
        EnderShareSession session = new EnderShareSession(p1.getUniqueId(), p2.getUniqueId(), inv);
        addSession(session);
    }

    /**
     * Adds a new sharing session to active sessions and persists it.
     *
     * @param session The session to add.
     */
    public static void addSession(EnderShareSession session) {
        sessions.put(session.getPlayer1(), session);
        sessions.put(session.getPlayer2(), session);
        saveSession(session);
    }

    /**
     * Updates a session's shared inventory contents and saves the update.
     *
     * @param inv     The updated shared inventory.
     * @param session The session to update.
     */
    public static void updateSessionInventory(Inventory inv, EnderShareSession session) {
        session.setSharedInventory(inv);
        saveSession(session);
    }

    /**
     * Removes an active session and deletes its persistent record.
     *
     * @param playerId The UUID of one of the session participants.
     */
    public static void removeSession(UUID playerId) {
        EnderShareSession session = sessions.get(playerId);
        if (session != null) {
            sessions.remove(session.getPlayer1());
            sessions.remove(session.getPlayer2());
            removeSessionFromFile(session.getSessionId());
        }
    }

    /**
     * Saves a sharing session to a YML file in the "chestdata" folder.
     *
     * @param session The session to save.
     */
    private static void saveSession(EnderShareSession session) {
        File chestDataFolder = new File(plugin.getDataFolder(), "chestdata");
        if (!chestDataFolder.exists()) {
            chestDataFolder.mkdirs();
        }
        File sessionFile = new File(chestDataFolder, session.getSessionId() + ".yml");
        YamlConfiguration config = new YamlConfiguration();
        config.set("session_id", session.getSessionId());
        config.set("player1", session.getPlayer1().toString());
        config.set("player2", session.getPlayer2().toString());
        // Save non-null items from the shared inventory.
        for (int i = 0; i < session.getSharedInventory().getSize(); i++) {
            ItemStack item = session.getSharedInventory().getItem(i);
            if (item != null) {
                config.set("inventory." + i, item);
            }
        }
        try {
            config.save(sessionFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Deletes the session file corresponding to the given session ID.
     *
     * @param sessionId The session identifier.
     */
    private static void removeSessionFromFile(String sessionId) {
        File chestDataFolder = new File(plugin.getDataFolder(), "chestdata");
        File sessionFile = new File(chestDataFolder, sessionId + ".yml");
        if (sessionFile.exists()) {
            sessionFile.delete();
        }
    }

    // ----- Pending Restoration Methods -----

    /**
     * Sets the pending restoration items for a player.
     * These items will later be restored to the player's Ender Chest when they come online.
     *
     * @param playerId The player's UUID.
     * @param items    The array of ItemStacks to restore.
     */
    public static void setPendingRestoration(UUID playerId, ItemStack[] items) {
        pendingRestorations.put(playerId, items);
    }

    /**
     * Retrieves and removes pending restoration items for a player.
     *
     * @param playerId The player's UUID.
     * @return The saved ItemStack array, or null if none exist.
     */
    public static ItemStack[] getPendingRestoration(UUID playerId) {
        return pendingRestorations.remove(playerId);
    }

    /**
     * Checks if a pending restoration exists for the specified player.
     *
     * @param playerId The player's UUID.
     * @return true if a restoration is pending; false otherwise.
     */
    public static boolean hasPendingRestoration(UUID playerId) {
        return pendingRestorations.containsKey(playerId);
    }

    /**
     * Serializes an array of ItemStacks to a YAML-formatted String.
     *
     * @param items The ItemStack array.
     * @return The YAML string representation.
     */
    public static String serializeItemArray(ItemStack[] items) {
        YamlConfiguration config = new YamlConfiguration();
        for (int i = 0; i < items.length; i++) {
            ItemStack item = items[i];
            if (item != null) {
                config.set("slot." + i, item);
            }
        }
        return config.saveToString();
    }

    /**
     * Deserializes a YAML string into an ItemStack array of a given size.
     *
     * @param data The YAML string.
     * @param size The expected array size.
     * @return The deserialized ItemStack array.
     */
    public static ItemStack[] deserializeItemArray(String data, int size) {
        Inventory inv = Bukkit.createInventory(null, size, "temp");
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.loadFromString(data);
        } catch (Exception e) {
            e.printStackTrace();
            return inv.getContents();
        }
        for (String key : config.getKeys(false)) {
            if (key.startsWith("slot.")) {
                int slot = Integer.parseInt(key.substring(5));
                if (slot < size) {
                    inv.setItem(slot, config.getItemStack(key));
                }
            }
        }
        return inv.getContents();
    }

    /**
     * Saves all pending restorations to disk so that they persist across server restarts.
     */
    public static void savePendingRestorations() {
        File pendingFile = new File(plugin.getDataFolder(), "pendingRestorations.yml");
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, ItemStack[]> entry : pendingRestorations.entrySet()) {
            String serialized = serializeItemArray(entry.getValue());
            config.set(entry.getKey().toString(), serialized);
        }
        try {
            config.save(pendingFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads pending restorations from persistent storage.
     */
    public static void loadPendingRestorations() {
        File pendingFile = new File(plugin.getDataFolder(), "pendingRestorations.yml");
        if (!pendingFile.exists()) {
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(pendingFile);
        for (String key : config.getKeys(false)) {
            String serialized = config.getString(key);
            if (serialized != null) {
                try {
                    UUID playerUuid = UUID.fromString(key);
                    ItemStack[] items = deserializeItemArray(serialized, 27);
                    pendingRestorations.put(playerUuid, items);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // ----- Pending Invitation Methods -----

    /**
     * Creates a pending sharing invitation from the inviter to the invitee.
     * Also schedules the invitation to expire after a configured timeout.
     *
     * @param inviter The UUID of the player sending the invitation.
     * @param invitee The UUID of the receiving player.
     */
    public static void createPendingInvitation(UUID inviter, UUID invitee) {
        PendingInvitation invitation = new PendingInvitation(inviter, invitee);
        pendingInvitations.put(invitee, invitation);
        int timeoutSeconds = EnderShare.getInstance().getConfig().getInt("penting_invitation_timeout", 60);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            PendingInvitation inv = pendingInvitations.get(invitee);
            if (inv != null && System.currentTimeMillis() - inv.getTimestamp() >= (timeoutSeconds * 1000L)) {
                pendingInvitations.remove(invitee);
                Player inviteePlayer = Bukkit.getPlayer(invitee);
                if (inviteePlayer != null && inviteePlayer.isOnline()) {
                    inviteePlayer.sendMessage(ChatColor.YELLOW + "Your invitation from " +
                            Bukkit.getOfflinePlayer(inviter).getName() + " has expired.");
                }
            }
        }, timeoutSeconds * 20L);
    }

    /**
     * Retrieves and removes a pending invitation for the given invitee.
     * Ensures that the invitation is still valid based on the timeout.
     *
     * @param invitee The UUID of the invitee.
     * @return The valid PendingInvitation if it exists; null otherwise.
     */
    public static PendingInvitation getPendingInvitation(UUID invitee) {
        PendingInvitation invitation = pendingInvitations.get(invitee);
        if (invitation == null)
            return null;
        int timeoutSeconds = EnderShare.getInstance().getConfig().getInt("penting_invitation_timeout", 60);
        if (System.currentTimeMillis() - invitation.getTimestamp() > (timeoutSeconds * 1000L)) {
            pendingInvitations.remove(invitee);
            return null;
        }
        pendingInvitations.remove(invitee);
        return invitation;
    }

    /**
     * Creates a shared inventory by merging the first 27 slots from each player's Ender Chest.
     * The resulting inventory has 54 slots with the first half for player1 and the second half for player2.
     *
     * @param p1 The first player.
     * @param p2 The second player.
     * @return The combined shared inventory.
     */
    public static Inventory createSharedInventory(Player p1, Player p2) {
        Inventory sharedInventory = Bukkit.createInventory(null, 54, "Shared Ender Chest");
        Inventory p1Ender = p1.getEnderChest();
        Inventory p2Ender = p2.getEnderChest();
        for (int i = 0; i < 27; i++) {
            sharedInventory.setItem(i, p1Ender.getItem(i));
        }
        for (int i = 0; i < 27; i++) {
            sharedInventory.setItem(27 + i, p2Ender.getItem(i));
        }
        return sharedInventory;
    }

    /**
     * Retrieves all active sharing sessions.
     *
     * @return A collection of EnderShareSession instances.
     */
    public static Collection<EnderShareSession> getAllSessions() {
        return new HashSet<>(sessions.values());
    }
}