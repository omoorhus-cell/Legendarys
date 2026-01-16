package org.tekkabyte.legendarys.legend;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.tekkabyte.legendarys.LegendarysPlugin;

public class LegendaryServiceImpl implements LegendaryService {

    private final NamespacedKey KEY_LEGENDARY;
    private final NamespacedKey KEY_LEGENDARY_ID;

    public LegendaryServiceImpl(LegendarysPlugin plugin) {
        this.KEY_LEGENDARY = new NamespacedKey(plugin, "legendary");
        this.KEY_LEGENDARY_ID = new NamespacedKey(plugin, "legendary_id");
    }

    @Override
    public boolean isLegendary(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        Byte b = meta.getPersistentDataContainer().get(KEY_LEGENDARY, PersistentDataType.BYTE);
        return b != null && b == (byte) 1;
    }

    @Override
    public String getLegendaryId(ItemStack item) {
        if (!isLegendary(item)) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(KEY_LEGENDARY_ID, PersistentDataType.STRING);
    }

    @Override
    public ItemStack applyLegendary(ItemStack base, String id) {
        if (base == null || base.getType().isAir()) return base;
        ItemStack clone = base.clone();
        ItemMeta meta = clone.getItemMeta();
        if (meta == null) return clone;

        meta.getPersistentDataContainer().set(KEY_LEGENDARY, PersistentDataType.BYTE, (byte) 1);
        meta.getPersistentDataContainer().set(KEY_LEGENDARY_ID, PersistentDataType.STRING, id);

        clone.setItemMeta(meta);
        return clone;
    }
}
