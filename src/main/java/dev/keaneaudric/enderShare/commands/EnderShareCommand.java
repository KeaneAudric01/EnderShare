package dev.keaneaudric.enderShare.commands;

import dev.keaneaudric.enderShare.manager.EnderShareManager;
import dev.keaneaudric.enderShare.manager.EnderShareManager.PendingInvitation;
import dev.keaneaudric.enderShare.manager.EnderShareSession;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Command executor for the /endershare command.
 * Supports subcommands: invite, accept, unshare, and status.
 */
public class EnderShareCommand implements CommandExecutor {

    /**
     * Called when the /endershare command is executed.
     *
     * @param sender  The source of the command.
     * @param command The command which was executed.
     * @param label   The alias used.
     * @param args    The command arguments.
     * @return true as the command was processed.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Only allow players to execute the command.
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can execute this command.");
            return true;
        }
        Player player = (Player) sender;

        // Show usage if no subcommand is provided.
        if (args.length < 1) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /endershare <invite|accept|unshare|status>");
            return true;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "invite":
                return handleInvite(player, args);
            case "accept":
                return handleAccept(player, args);
            case "unshare":
                return handleUnshare(player);
            case "status":
                return handleStatus(player);
            default:
                player.sendMessage(ChatColor.YELLOW + "Unknown subcommand. Use /endershare <invite|accept|unshare|status>");
                return true;
        }
    }

    /**
     * Processes the "invite" subcommand.
     * Validates the target, ensures neither party is already sharing, and sends an invitation.
     *
     * @param player The player sending the invite.
     * @param args   The command arguments.
     * @return true after processing.
     */
    private boolean handleInvite(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /endershare invite <player>");
            return true;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Target player not found.");
            return true;
        }
        if (player.getUniqueId().equals(target.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You cannot invite yourself.");
            return true;
        }
        if (EnderShareManager.isSharing(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You are already sharing your Ender Chest.");
            return true;
        }
        if (EnderShareManager.isSharing(target.getUniqueId())) {
            player.sendMessage(ChatColor.RED + target.getName() + " is already sharing their Ender Chest.");
            return true;
        }
        // Create a pending invitation with timeout.
        EnderShareManager.createPendingInvitation(player.getUniqueId(), target.getUniqueId());
        player.sendMessage(ChatColor.GREEN + "Invitation sent to " + target.getName());
        target.sendMessage(ChatColor.AQUA + "You have received an EnderShare invitation from " +
                player.getName() + ". Type '/endershare accept " + player.getName() + "' to accept.");
        return true;
    }

    /**
     * Processes the "accept" subcommand.
     * Validates that a valid invitation exists and creates a shared inventory.
     *
     * @param player The player accepting the invitation.
     * @param args   The command arguments.
     * @return true after processing.
     */
    private boolean handleAccept(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /endershare accept <player>");
            return true;
        }
        // Validate that a valid invitation exists from the specified inviter.
        Player inviter = Bukkit.getPlayer(args[1]);
        if (inviter == null) {
            player.sendMessage(ChatColor.RED + "Inviter not found.");
            return true;
        }
        PendingInvitation invitation = EnderShareManager.getPendingInvitation(player.getUniqueId());
        if (invitation == null || !invitation.getInviter().equals(inviter.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "No valid invitation found from " + inviter.getName());
            return true;
        }
        // Create a shared inventory with 54 slots.
        Inventory sharedInventory = Bukkit.createInventory(null, 54, "Shared Ender Chest");

        // Migrate contents: first 27 slots from the inviter and next 27 slots from the acceptor.
        Inventory inviterEnder = inviter.getEnderChest();
        Inventory accepterEnder = player.getEnderChest();

        sharedInventory.setContents(inviterEnder.getContents());
        ItemStack[] accepterItems = accepterEnder.getContents();
        for (int i = 0; i < accepterItems.length && i < 27; i++) {
            sharedInventory.setItem(27 + i, accepterItems[i]);
        }
        // Clear each player’s original Ender Chest.
        inviterEnder.clear();
        accepterEnder.clear();

        // Start the sharing session.
        EnderShareManager.startSession(inviter, player, sharedInventory);
        inviter.sendMessage(ChatColor.GREEN + "You are now sharing your Ender Chest with " + player.getName());
        player.sendMessage(ChatColor.GREEN + "You are now sharing your Ender Chest with " + inviter.getName());
        inviter.openInventory(sharedInventory);
        player.openInventory(sharedInventory);
        return true;
    }

    /**
     * Processes the "unshare" subcommand.
     * Ends the current session and restores the original Ender Chest contents for both players.
     *
     * @param player The player issuing the command.
     * @return true after processing.
     */
    private boolean handleUnshare(Player player) {
        if (!EnderShareManager.isSharing(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You are not currently in a sharing session.");
            return true;
        }
        EnderShareSession session = EnderShareManager.getSession(player.getUniqueId());
        if (session != null) {
            Inventory sharedInv = session.getSharedInventory();

            ItemStack[] p1Items = new ItemStack[27];
            ItemStack[] p2Items = new ItemStack[27];

            // Split the shared inventory: first half for one player...
            for (int i = 0; i < 27; i++) {
                p1Items[i] = sharedInv.getItem(i);
            }
            // ...and second half for the other.
            for (int i = 27; i < 54; i++) {
                p2Items[i - 27] = sharedInv.getItem(i);
            }

            // Retrieve the involved players (online check needed).
            Player p1 = Bukkit.getPlayer(session.getPlayer1());
            Player p2 = Bukkit.getPlayer(session.getPlayer2());

            // Close the shared inventory if open.
            if (p1 != null && p1.isOnline()) {
                p1.closeInventory();
            }
            if (p2 != null && p2.isOnline()) {
                p2.closeInventory();
            }

            // Restore Ender Chest contents for player1.
            if (p1 != null && p1.isOnline()) {
                p1.getEnderChest().clear();
                p1.getEnderChest().setContents(p1Items);
                p1.sendMessage(ChatColor.YELLOW + "Your EnderShare session has ended, and your Ender Chest has been restored.");
            } else {
                EnderShareManager.setPendingRestoration(session.getPlayer1(), p1Items);
                Bukkit.getLogger().info("Pending Ender Chest restoration set for offline player: " + session.getPlayer1());
            }

            // Restore Ender Chest contents for player2.
            if (p2 != null && p2.isOnline()) {
                p2.getEnderChest().clear();
                p2.getEnderChest().setContents(p2Items);
                p2.sendMessage(ChatColor.YELLOW + "Your EnderShare session has ended, and your Ender Chest has been restored.");
            } else {
                EnderShareManager.setPendingRestoration(session.getPlayer2(), p2Items);
                Bukkit.getLogger().info("Pending Ender Chest restoration set for offline player: " + session.getPlayer2());
            }
        }
        EnderShareManager.removeSession(player.getUniqueId());
        return true;
    }

    /**
     * Processes the "status" subcommand.
     * Displays the name of the other participant in the active sharing session.
     *
     * @param player The player requesting a status update.
     * @return true after processing.
     */
    private boolean handleStatus(Player player) {
        if (!EnderShareManager.isSharing(player.getUniqueId())) {
            player.sendMessage(ChatColor.YELLOW + "You are not in an active sharing session.");
        } else {
            EnderShareSession currSession = EnderShareManager.getSession(player.getUniqueId());
            String otherPlayer = Bukkit.getOfflinePlayer(
                    currSession.getPlayer1().equals(player.getUniqueId())
                            ? currSession.getPlayer2()
                            : currSession.getPlayer1()
            ).getName();
            player.sendMessage(ChatColor.GREEN + "You are sharing with: " + otherPlayer);
        }
        return true;
    }
}