package dev.keaneaudric.enderShare.utils;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides tab completion for the /endershare command.
 * Suggests subcommands based on the current argument count.
 */
public class EnderShareTabCompleter implements TabCompleter {

    /**
     * Returns a list of possible completions for the command.
     *
     * @param sender  The command sender.
     * @param command The command being executed.
     * @param alias   The alias used to call the command.
     * @param args    The arguments provided so far.
     * @return List of subcommand suggestions.
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            // Provide subcommands when the first argument is being entered.
            completions.add("invite");
            completions.add("accept");
            completions.add("unshare");
            completions.add("status");
        }
        return completions;
    }
}