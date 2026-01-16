package org.tekkabyte.legendarys.util;

import org.bukkit.Bukkit;
import org.bukkit.World;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.*;

public final class PlayerDatScanner {

    private PlayerDatScanner() {}

    public static int countLegendaryItemsInPlayerDat(File datFile, String pbvKeyLegendary) {
        Object root = readCompressedNbt(datFile);
        if (root == null) return 0;

        int inv = countFromList(root, "Inventory", pbvKeyLegendary);
        int ender = countFromList(root, "EnderItems", pbvKeyLegendary);

        return inv + ender;
    }

    public static List<File> resolveAllPlayerdataFolders() {
        List<File> out = new ArrayList<>();
        for (World w : Bukkit.getWorlds()) {
            File dir = new File(w.getWorldFolder(), "playerdata");
            if (dir.isDirectory()) out.add(dir);
        }
        return out;
    }

    public static UUID uuidFromDatName(String name) {
        if (name == null || !name.endsWith(".dat")) return null;
        String raw = name.substring(0, name.length() - 4);
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static int countFromList(Object compoundTag, String listName, String pbvKeyLegendary) {
        Object list = getList(compoundTag, listName, 10);
        if (list == null) return 0;

        int size = listSize(list);
        int total = 0;

        for (int i = 0; i < size; i++) {
            Object item = listGet(list, i);
            if (item == null) continue;

            if (isLegendaryItemCompound(item, pbvKeyLegendary)) {
                int count = readItemCount(item);
                if (count <= 0) count = 1;
                total += count;
            }
        }

        return total;
    }

    private static boolean isLegendaryItemCompound(Object itemCompound, String pbvKeyLegendary) {
        Object pbv = findPublicBukkitValues(itemCompound);
        if (pbv == null) return false;

        Object val = getTag(pbv, pbvKeyLegendary);
        if (val == null) return false;

        return readNumericAsInt(val) == 1;
    }

    private static Object findPublicBukkitValues(Object itemCompound) {
        Object tag = getCompound(itemCompound, "tag");
        if (tag != null) {
            Object pbv = getCompound(tag, "PublicBukkitValues");
            if (pbv != null) return pbv;
        }

        Object components = getCompound(itemCompound, "components");
        if (components != null) {
            Object customData = getCompound(components, "minecraft:custom_data");
            if (customData != null) {
                Object pbv = getCompound(customData, "PublicBukkitValues");
                if (pbv != null) return pbv;
            }
        }

        return null;
    }

    private static int readItemCount(Object itemCompound) {
        Object legacy = getTag(itemCompound, "Count");
        if (legacy != null) return readNumericAsInt(legacy);

        Object modern = getTag(itemCompound, "count");
        if (modern != null) return readNumericAsInt(modern);

        return 1;
    }

    private static Object readCompressedNbt(File f) {
        try (InputStream in = new FileInputStream(f)) {
            Class<?> nbtIo = Class.forName("net.minecraft.nbt.NbtIo");
            Method readCompressed = null;

            for (Method m : nbtIo.getDeclaredMethods()) {
                if (!m.getName().equals("readCompressed")) continue;
                Class<?>[] p = m.getParameterTypes();
                if (p.length == 1 && InputStream.class.isAssignableFrom(p[0])) {
                    readCompressed = m;
                    break;
                }
            }
            if (readCompressed == null) return null;

            readCompressed.setAccessible(true);
            return readCompressed.invoke(null, in);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object getCompound(Object compound, String key) {
        if (compound == null) return null;
        try {
            Method m = compound.getClass().getMethod("getCompound", String.class);
            return m.invoke(compound, key);
        } catch (Throwable ignored) {
            return getTag(compound, key);
        }
    }

    private static Object getList(Object compound, String key, int expectedTypeId) {
        if (compound == null) return null;
        try {
            Method m = compound.getClass().getMethod("getList", String.class, int.class);
            return m.invoke(compound, key, expectedTypeId);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object getTag(Object compound, String key) {
        if (compound == null) return null;
        try {
            Method m = compound.getClass().getMethod("get", String.class);
            return m.invoke(compound, key);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static int listSize(Object listTag) {
        if (listTag == null) return 0;
        try {
            Method m = listTag.getClass().getMethod("size");
            Object r = m.invoke(listTag);
            if (r instanceof Integer) return (Integer) r;
        } catch (Throwable ignored) {}
        return 0;
    }

    private static Object listGet(Object listTag, int index) {
        if (listTag == null) return null;
        try {
            Method m = listTag.getClass().getMethod("get", int.class);
            return m.invoke(listTag, index);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static int readNumericAsInt(Object tag) {
        if (tag == null) return 0;

        try {
            Method m = tag.getClass().getMethod("getAsInt");
            Object r = m.invoke(tag);
            if (r instanceof Integer) return (Integer) r;
        } catch (Throwable ignored) {}

        try {
            Method m = tag.getClass().getMethod("getAsByte");
            Object r = m.invoke(tag);
            if (r instanceof Byte) return ((Byte) r) & 0xFF;
        } catch (Throwable ignored) {}

        try {
            String s = String.valueOf(tag).replaceAll("[^0-9\\-]", "");
            if (s.isEmpty()) return 0;
            return Integer.parseInt(s);
        } catch (Throwable ignored) {}

        return 0;
    }
}