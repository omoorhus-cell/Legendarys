package org.tekkabyte.legendarys.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class LegendaryViewerHolder implements InventoryHolder {
    private final int page;

    public LegendaryViewerHolder(int page) {
        this.page = page;
    }

    public int getPage() {
        return page;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
