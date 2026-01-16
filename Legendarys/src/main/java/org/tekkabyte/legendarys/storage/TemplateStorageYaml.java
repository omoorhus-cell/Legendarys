package org.tekkabyte.legendarys.storage;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.tekkabyte.legendarys.LegendarysPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class TemplateStorageYaml implements TemplateStorage {

    private final LegendarysPlugin plugin;
    private final File file;
    private YamlConfiguration yaml;
    private final Map<Integer, Map<Integer, ItemStack>> data = new HashMap<>();

    public TemplateStorageYaml(LegendarysPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "templates.yml");
    }

    @Override
    public void load() {
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        this.yaml = YamlConfiguration.loadConfiguration(file);
        data.clear();

        ConfigurationSection pages = yaml.getConfigurationSection("pages");
        if (pages == null) return;

        for (String pageKey : pages.getKeys(false)) {
            int page;
            try { page = Integer.parseInt(pageKey); } catch (Exception e) { continue; }

            ConfigurationSection slots = pages.getConfigurationSection(pageKey);
            if (slots == null) continue;

            Map<Integer, ItemStack> pageMap = new HashMap<>();
            for (String slotKey : slots.getKeys(false)) {
                int slot;
                try { slot = Integer.parseInt(slotKey); } catch (Exception e) { continue; }

                String b64 = slots.getString(slotKey);
                if (b64 == null || b64.isBlank()) continue;

                ItemStack item = ItemStackCodec.fromBase64(b64);
                if (item != null) pageMap.put(slot, item);
            }

            if (!pageMap.isEmpty()) data.put(page, pageMap);
        }
    }

    @Override
    public void save() {
        if (yaml == null) yaml = new YamlConfiguration();
        yaml.set("pages", null);

        for (var pageEntry : data.entrySet()) {
            int page = pageEntry.getKey();
            for (var slotEntry : pageEntry.getValue().entrySet()) {
                int slot = slotEntry.getKey();
                ItemStack item = slotEntry.getValue();
                if (item == null) continue;
                yaml.set("pages." + page + "." + slot, ItemStackCodec.toBase64(item));
            }
        }

        try {
            yaml.save(file);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save templates.yml: " + e.getMessage());
        }
    }

    @Override
    public int getMaxPages() {
        return plugin.getConfig().getInt("storage.max-pages", 20);
    }

    @Override
    public int getPageSize() {
        return plugin.getConfig().getInt("storage.page-size", 45);
    }

    @Override
    public ItemStack get(int page, int slot) {
        Map<Integer, ItemStack> pageMap = data.get(page);
        if (pageMap == null) return null;
        ItemStack it = pageMap.get(slot);
        return it == null ? null : it.clone();
    }

    @Override
    public void set(int page, int slot, ItemStack item) {
        if (page < 1 || page > getMaxPages()) return;
        if (slot < 0 || slot >= getPageSize()) return;

        data.computeIfAbsent(page, k -> new HashMap<>());
        data.get(page).put(slot, item == null ? null : item.clone());
        data.get(page).values().removeIf(v -> v == null);
        if (data.get(page).isEmpty()) data.remove(page);
    }

    @Override
    public boolean delete(int page, int slot) {
        Map<Integer, ItemStack> pageMap = data.get(page);
        if (pageMap == null) return false;
        ItemStack removed = pageMap.remove(slot);
        if (pageMap.isEmpty()) data.remove(page);
        return removed != null;
    }

    @Override
    public int saveNextFree(ItemStack item) {
        int maxPages = getMaxPages();
        int pageSize = getPageSize();

        for (int p = 1; p <= maxPages; p++) {
            Map<Integer, ItemStack> pageMap = data.computeIfAbsent(p, k -> new HashMap<>());
            for (int s = 0; s < pageSize; s++) {
                if (!pageMap.containsKey(s)) {
                    pageMap.put(s, item.clone());
                    return (p - 1) * pageSize + s;
                }
            }
        }
        return -1;
    }

    @Override
    public int pageOf(int linearIndex) {
        return (linearIndex / getPageSize()) + 1;
    }

    @Override
    public int slotOf(int linearIndex) {
        return linearIndex % getPageSize();
    }
}
