package org.tekkabyte.legendarys.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.tekkabyte.legendarys.gui.LegendaryViewerGui;

public class ViewLegendariesCommand implements CommandExecutor {

    private final LegendaryViewerGui gui;

    public ViewLegendariesCommand(LegendaryViewerGui gui) {
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        int page = 1;
        if (args.length >= 1) {
            try { page = Integer.parseInt(args[0]); } catch (Exception ignored) {}
        }
        gui.open(player, Math.max(1, page));
        return true;
    }
}
