package org.tekkabyte.legendarys.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.tekkabyte.legendarys.glow.GlowManager;
import org.tekkabyte.legendarys.gui.LegendaryViewerGui;
import org.tekkabyte.legendarys.legend.LegendaryService;
import org.tekkabyte.legendarys.storage.TemplateStorage;

public class LegendaryViewerListener implements Listener {

    private final org.tekkabyte.legendarys.LegendarysPlugin plugin;
    private final LegendaryViewerGui gui;
    private final TemplateStorage storage;
    private final LegendaryService legendaryService;
    private final GlowManager glowManager;

    public LegendaryViewerListener(org.tekkabyte.legendarys.LegendarysPlugin plugin,
                                   LegendaryViewerGui gui,
                                   TemplateStorage storage,
                                   LegendaryService legendaryService,
                                   GlowManager glowManager) {
        this.plugin = plugin;
        this.gui = gui;
        this.storage = storage;
        this.legendaryService = legendaryService;
        this.glowManager = glowManager;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (!gui.isViewerInventory(top)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player player)) return;
        ItemStack clicked = e.getCurrentItem();
        int raw = e.getRawSlot();
        int page = gui.getPage(top);
        if (gui.isControl(clicked)) {
            if (raw == LegendaryViewerGui.SLOT_PREV) {
                gui.open(player, page - 1);
            } else if (raw == LegendaryViewerGui.SLOT_NEXT) {
                gui.open(player, page + 1);
            } else if (raw == LegendaryViewerGui.SLOT_CLOSE) {
                player.closeInventory();
            }
            return;
        }
        if (raw >= 0 && raw < storage.getPageSize()) {
            ItemStack stored = storage.get(page, raw);
            if (stored == null) return;

            boolean admin = player.hasPermission("legendarys.admin");

            switch (e.getClick()) {
                case LEFT, SHIFT_LEFT -> {
                    player.getInventory().addItem(stored.clone());
                    glowManager.requestUpdate(player);
                }
                case RIGHT, SHIFT_RIGHT -> {
                    if (!admin) {
                        player.sendMessage("§cNo permission to delete.");
                        return;
                    }
                    storage.delete(page, raw);
                    storage.save();
                    gui.open(player, page);
                }
                default -> {}
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (!gui.isViewerInventory(top)) return;
        e.setCancelled(true);
    }
}
