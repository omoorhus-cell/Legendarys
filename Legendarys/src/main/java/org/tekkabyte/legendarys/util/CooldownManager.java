package org.tekkabyte.legendarys.util;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {
    private final Map<String, Long> nextUse = new HashMap<>();

    private String key(UUID uuid, String abilityKey) {
        return uuid.toString() + ":" + abilityKey;
    }

    public boolean isReady(UUID uuid, String abilityKey) {
        long now = System.currentTimeMillis();
        return now >= nextUse.getOrDefault(key(uuid, abilityKey), 0L);
    }

    public long remainingMs(UUID uuid, String abilityKey) {
        long now = System.currentTimeMillis();
        return Math.max(0L, nextUse.getOrDefault(key(uuid, abilityKey), 0L) - now);
    }

    public void setCooldown(UUID uuid, String abilityKey, long cooldownMs) {
        nextUse.put(key(uuid, abilityKey), System.currentTimeMillis() + Math.max(0L, cooldownMs));
    }

    public void applyVanillaCooldown(Player p, ItemStack item, long cooldownMs) {
        if (p == null || item == null) return;
        if (cooldownMs <= 0) return;
        int ticks = (int) Math.max(1, Math.round(cooldownMs / 50.0));
        p.setCooldown(item.getType(), ticks);
    }

}