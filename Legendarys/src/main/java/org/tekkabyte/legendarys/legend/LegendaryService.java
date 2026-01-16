package org.tekkabyte.legendarys.legend;

import org.bukkit.inventory.ItemStack;

public interface LegendaryService {
    boolean isLegendary(ItemStack item);
    String getLegendaryId(ItemStack item);
    ItemStack applyLegendary(ItemStack base, String id);
}
