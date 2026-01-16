package org.tekkabyte.legendarys.storage;

import org.bukkit.inventory.ItemStack;

public interface TemplateStorage {
    void load();
    void save();

    int getMaxPages();
    int getPageSize();

    ItemStack get(int page, int slot);
    void set(int page, int slot, ItemStack item);
    boolean delete(int page, int slot);

    int saveNextFree(ItemStack item);

    int pageOf(int linearIndex);
    int slotOf(int linearIndex);
}
