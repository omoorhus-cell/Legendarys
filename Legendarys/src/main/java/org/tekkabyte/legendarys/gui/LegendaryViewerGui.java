package org.tekkabyte.legendarys.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.tekkabyte.legendarys.LegendarysPlugin;
import org.tekkabyte.legendarys.legend.LegendaryService;
import org.tekkabyte.legendarys.storage.TemplateStorage;

import java.util.List;

public class LegendaryViewerGui {

    private final LegendarysPlugin plugin;
    private final LegendaryService legendaryService;
    private final TemplateStorage storage;

    private final MiniMessage mm = MiniMessage.miniMessage();
    private final NamespacedKey CONTROL_KEY;

    public static final int SLOT_PREV = 45;
    public static final int SLOT_NEXT = 53;
    public static final int SLOT_INFO = 49;
    public static final int SLOT_CLOSE = 50;

    public LegendaryViewerGui(LegendarysPlugin plugin, LegendaryService legendaryService, TemplateStorage storage) {
        this.plugin = plugin;
        this.legendaryService = legendaryService;
        this.storage = storage;
        this.CONTROL_KEY = new NamespacedKey(plugin, "gui_control");
    }

    public void open(Player player, int page) {
        int max = storage.getMaxPages();
        if (page < 1) page = 1;
        if (page > max) page = max;

        String titleFmt = plugin.getConfig().getString("storage.title", "Legendaries (Page {page})");
        String title = titleFmt.replace("{page}", String.valueOf(page));

        Inventory inv = Bukkit.createInventory(new LegendaryViewerHolder(page), 54, Component.text(title));

        for (int slot = 0; slot < storage.getPageSize(); slot++) {
            ItemStack it = storage.get(page, slot);
            if (it != null) inv.setItem(slot, it.clone());
        }

        inv.setItem(SLOT_PREV, controlItem(Material.ARROW, "<yellow>Previous</yellow>", List.of("<gray>Go to page " + (page - 1) + "</gray>")));
        inv.setItem(SLOT_NEXT, controlItem(Material.ARROW, "<yellow>Next</yellow>", List.of("<gray>Go to page " + (page + 1) + "</gray>")));
        inv.setItem(SLOT_INFO, controlItem(Material.PAPER, "<gold>Page " + page + "/" + max + "</gold>", List.of("<gray>Left click item: get copy</gray>", "<gray>Right click: delete (admin)</gray>")));
        inv.setItem(SLOT_CLOSE, controlItem(Material.BARRIER, "<red>Close</red>", List.of()));

        player.openInventory(inv);
    }

    public boolean isViewerInventory(Inventory inv) {
        return inv != null && inv.getHolder() instanceof LegendaryViewerHolder;
    }

    public int getPage(Inventory inv) {
        if (inv.getHolder() instanceof LegendaryViewerHolder h) return h.getPage();
        return 1;
    }

    public boolean isControl(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        Byte b = meta.getPersistentDataContainer().get(CONTROL_KEY, PersistentDataType.BYTE);
        return b != null && b == (byte) 1;
    }

    private ItemStack controlItem(Material mat, String nameMm, List<String> loreMm) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(mm.deserialize(nameMm));
        if (!loreMm.isEmpty()) {
            meta.lore(loreMm.stream().map(mm::deserialize).toList());
        }
        meta.getPersistentDataContainer().set(CONTROL_KEY, PersistentDataType.BYTE, (byte) 1);
        it.setItemMeta(meta);
        return it;
    }
}
