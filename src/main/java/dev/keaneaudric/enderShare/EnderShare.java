package dev.keaneaudric.enderShare;

import dev.keaneaudric.enderShare.commands.EnderShareCommand;
import dev.keaneaudric.enderShare.listeners.EnderShareListener;
import dev.keaneaudric.enderShare.listeners.EnderShareOfflineRestorationListener;
import dev.keaneaudric.enderShare.listeners.EnderShareInventoryListener;
import dev.keaneaudric.enderShare.manager.EnderShareManager;
import dev.keaneaudric.enderShare.utils.EnderShareTabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for EnderShare.
 * Initializes configuration, persistent session data, and registers command executors and event listeners.
 */
public final class EnderShare extends JavaPlugin {

    private static EnderShare instance;

    /**
     * Called when the plugin is enabled.
     * Sets up configuration, initializes manager data, registers commands and event listeners.
     */
    @Override
    public void onEnable() {
        instance = this;
        // Save the default configuration file if one does not exist.
        saveDefaultConfig();

        // Initialize sessions, pending restorations, and invitations.
        EnderShareManager.initialize(this);
        EnderShareManager.loadPendingRestorations();

        // Register the /endershare command with its executor and tab completer.
        EnderShareCommand commandExecutor = new EnderShareCommand();
        getCommand("endershare").setExecutor(commandExecutor);
        getCommand("endershare").setTabCompleter(new EnderShareTabCompleter());

        // Register event listeners.
        getServer().getPluginManager().registerEvents(new EnderShareListener(), this);
        getServer().getPluginManager().registerEvents(new EnderShareOfflineRestorationListener(), this);
        getServer().getPluginManager().registerEvents(new EnderShareInventoryListener(), this);
    }

    /**
     * Called when the plugin is disabled.
     * Saves any pending Ender Chest restorations to persistent storage.
     */
    @Override
    public void onDisable() {
        EnderShareManager.savePendingRestorations();
    }

    /**
     * Returns the plugin instance.
     *
     * @return the singleton EnderShare instance.
     */
    public static EnderShare getInstance() {
        return instance;
    }
}