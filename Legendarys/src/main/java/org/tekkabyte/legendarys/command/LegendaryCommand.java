package org.tekkabyte.legendarys.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.tekkabyte.legendarys.LegendarysPlugin;
import org.tekkabyte.legendarys.glow.GlowManager;
import org.tekkabyte.legendarys.gui.LegendaryViewerGui;
import org.tekkabyte.legendarys.legend.LegendaryService;
import org.tekkabyte.legendarys.storage.TemplateStorage;
import org.tekkabyte.legendarys.util.PlayerDatScanner;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class LegendaryCommand implements CommandExecutor, TabCompleter {

    private final LegendarysPlugin plugin;
    private final LegendaryService legendaryService;
    private final TemplateStorage templateStorage;
    private final LegendaryViewerGui viewerGui;
    private final GlowManager glowManager;

    public LegendaryCommand(LegendarysPlugin plugin,
                            LegendaryService legendaryService,
                            TemplateStorage templateStorage,
                            LegendaryViewerGui viewerGui,
                            GlowManager glowManager) {
        this.plugin = plugin;
        this.legendaryService = legendaryService;
        this.templateStorage = templateStorage;
        this.viewerGui = viewerGui;
        this.glowManager = glowManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (!p.hasPermission("legendarys.admin")) {
            p.sendMessage("§cNo permission.");
            return true;
        }

        if (args.length == 0) {
            p.sendMessage("§e/legendary set <id> [--save]");
            p.sendMessage("§e/legendary save");
            p.sendMessage("§e/legendary delete <page> <slot>");
            p.sendMessage("§e/legendary replace <page> <slot>");
            p.sendMessage("§e/legendary give <page> <slot> [player]");
            p.sendMessage("§e/legendary view <page>");
            p.sendMessage("§e/legendary holders");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {

            case "holders" -> {
                p.sendMessage(color("&7Scanning offline+online playerdata across all worlds..."));

                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    try {
                        List<File> dirs = PlayerDatScanner.resolveAllPlayerdataFolders();
                        Map<UUID, Integer> counts = new HashMap<>();
                        int scanned = 0;

                        for (File dir : dirs) {
                            File[] files = dir.listFiles((d, name) -> name.endsWith(".dat"));
                            if (files == null) continue;

                            for (File f : files) {
                                UUID uuid = PlayerDatScanner.uuidFromDatName(f.getName());
                                if (uuid == null) continue;

                                int c = PlayerDatScanner.countLegendaryItemsInPlayerDat(f, "legendarys:legendary");
                                scanned++;

                                if (c > 0) counts.merge(uuid, c, Integer::sum);
                            }
                        }

                        List<Map.Entry<UUID, Integer>> sorted = counts.entrySet().stream()
                                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                                .collect(Collectors.toList());

                        int totalPlayers = sorted.size();
                        int totalItems = sorted.stream().mapToInt(Map.Entry::getValue).sum();
                        int scannedFiles = scanned;

                        Bukkit.getScheduler().runTask(plugin, () -> {
                            p.sendMessage(color("&8&m------------------------------------------------"));
                            p.sendMessage(color("&eLegendarys Holders &7(offline + online)"));
                            p.sendMessage(color("&7Scanned .dat files: &f" + scannedFiles));
                            p.sendMessage(color("&7Players with legendaries: &f" + totalPlayers));
                            p.sendMessage(color("&7Total legendaries: &f" + totalItems));
                            p.sendMessage(color("&8&m------------------------------------------------"));

                            if (sorted.isEmpty()) {
                                p.sendMessage(color("&cNone found."));
                                p.sendMessage(color("&8&m------------------------------------------------"));
                                return;
                            }

                            for (Map.Entry<UUID, Integer> e : sorted) {
                                OfflinePlayer op = Bukkit.getOfflinePlayer(e.getKey());
                                String name = (op.getName() != null) ? op.getName() : e.getKey().toString();
                                p.sendMessage(color("&6" + name + " &7- &e" + e.getValue()));
                            }

                            p.sendMessage(color("&8&m------------------------------------------------"));
                        });

                    } catch (Throwable t) {
                        Bukkit.getScheduler().runTask(plugin, () ->
                                p.sendMessage(color("&cScan failed: &f" + t.getClass().getSimpleName() + ": " + t.getMessage())));
                        t.printStackTrace();
                    }
                });

                return true;
            }

            case "set" -> {
                if (args.length < 2) {
                    p.sendMessage("§cUsage: /legendary set <id> [--save]");
                    return true;
                }
                String id = args[1];
                boolean save = args.length >= 3 && args[2].equalsIgnoreCase("--save");

                ItemStack hand = p.getInventory().getItemInMainHand();
                if (hand == null || hand.getType().isAir()) {
                    p.sendMessage("§cHold an item in your main hand.");
                    return true;
                }

                ItemStack tagged = legendaryService.applyLegendary(hand, id);
                p.getInventory().setItemInMainHand(tagged);
                p.sendMessage("§aSet legendary id: §f" + id);

                if (save) {
                    int flat = templateStorage.saveNextFree(tagged);
                    if (flat < 0) {
                        p.sendMessage("§cNo space left in template storage.");
                    } else {
                        templateStorage.save();
                        int page = templateStorage.pageOf(flat);
                        int slot = templateStorage.slotOf(flat);
                        p.sendMessage("§aSaved template at §e/page " + page + " §a slot §e" + slot);
                    }
                }
                return true;
            }

            case "save" -> {
                ItemStack hand = p.getInventory().getItemInMainHand();
                if (hand == null || hand.getType().isAir()) {
                    p.sendMessage("§cHold an item in your main hand.");
                    return true;
                }

                int flat = templateStorage.saveNextFree(hand);
                if (flat < 0) {
                    p.sendMessage("§cNo space left in template storage.");
                } else {
                    templateStorage.save();
                    int page = templateStorage.pageOf(flat);
                    int slot = templateStorage.slotOf(flat);
                    p.sendMessage("§aSaved template at §e/page " + page + " §a slot §e" + slot);
                }
                return true;
            }

            case "delete" -> {
                if (args.length < 3) {
                    p.sendMessage("§cUsage: /legendary delete <page> <slot>");
                    return true;
                }
                Integer page = parseInt(args[1]);
                Integer slot = parseInt(args[2]);
                if (page == null || slot == null) {
                    p.sendMessage("§cPage/slot must be numbers.");
                    return true;
                }

                boolean ok = templateStorage.delete(page, slot);
                if (ok) {
                    templateStorage.save();
                    p.sendMessage("§aDeleted template at §e/page " + page + " §a slot §e" + slot);
                } else {
                    p.sendMessage("§cNothing found at that page/slot.");
                }
                return true;
            }

            case "replace" -> {
                if (args.length < 3) {
                    p.sendMessage("§cUsage: /legendary replace <page> <slot>");
                    return true;
                }
                Integer page = parseInt(args[1]);
                Integer slot = parseInt(args[2]);
                if (page == null || slot == null) {
                    p.sendMessage("§cPage/slot must be numbers.");
                    return true;
                }

                ItemStack hand = p.getInventory().getItemInMainHand();
                if (hand == null || hand.getType().isAir()) {
                    p.sendMessage("§cHold an item in your main hand.");
                    return true;
                }

                templateStorage.set(page, slot, hand.clone());
                templateStorage.save();
                p.sendMessage("§aReplaced template at §e/page " + page + " §a slot §e" + slot);
                return true;
            }

            case "give" -> {
                if (args.length < 3) {
                    p.sendMessage("§cUsage: /legendary give <page> <slot> [player]");
                    return true;
                }
                Integer page = parseInt(args[1]);
                Integer slot = parseInt(args[2]);
                if (page == null || slot == null) {
                    p.sendMessage("§cPage/slot must be numbers.");
                    return true;
                }

                Player target = p;
                if (args.length >= 4) {
                    target = Bukkit.getPlayerExact(args[3]);
                    if (target == null) {
                        p.sendMessage("§cPlayer not found: §f" + args[3]);
                        return true;
                    }
                }

                ItemStack item = templateStorage.get(page, slot);
                if (item == null || item.getType() == Material.AIR) {
                    p.sendMessage("§cNo template in that page/slot.");
                    return true;
                }

                target.getInventory().addItem(item.clone());
                p.sendMessage("§aGave template to §f" + target.getName());
                if (!target.equals(p)) target.sendMessage("§aYou received a legendary item.");
                return true;
            }

            case "view" -> {
                int page = 0;
                if (args.length >= 2) {
                    Integer parsed = parseInt(args[1]);
                    if (parsed == null) {
                        p.sendMessage("§cUsage: /legendary view <page>");
                        return true;
                    }
                    page = parsed;
                }
                viewerGui.open(p, page);
                return true;
            }

            default -> {
                p.sendMessage("§cUnknown subcommand.");
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String p = args[0].toLowerCase(Locale.ROOT);
            List<String> base = Arrays.asList("set", "save", "delete", "replace", "give", "view", "holders");
            return base.stream().filter(s -> s.startsWith(p)).sorted().collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private Integer parseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}