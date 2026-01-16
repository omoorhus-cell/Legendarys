package org.tekkabyte.legendarys.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.Locale;

public final class RegionBlocker {

    private static final String DEFAULT_REGION_ID = "spawn";

    private RegionBlocker() {}

    public static boolean isBlocked(Player player) {
        if (player == null) return false;
        return isBlocked(player.getLocation());
    }

    public static boolean isBlocked(Location loc) {
        return isInWorldGuardRegion(loc, DEFAULT_REGION_ID);
    }

    public static boolean isBlocked(Player attacker, Player victim) {
        if (attacker == null && victim == null) return false;

        Location aLoc = attacker != null ? attacker.getLocation() : null;
        Location vLoc = victim != null ? victim.getLocation() : null;

        return isBlocked(aLoc) || isBlocked(vLoc);
    }

    private static boolean isInWorldGuardRegion(Location loc, String regionId) {
        if (loc == null || loc.getWorld() == null) return false;
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") == null) return false;

        try {
            Class<?> wgClass = Class.forName("com.sk89q.worldguard.WorldGuard");
            Object wg = wgClass.getMethod("getInstance").invoke(null);
            Object platform = wgClass.getMethod("getPlatform").invoke(wg);
            Object regionContainer = platform.getClass().getMethod("getRegionContainer").invoke(platform);
            Object query = regionContainer.getClass().getMethod("createQuery").invoke(regionContainer);

            Class<?> adapterClass = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
            Object weLoc = adapterClass.getMethod("adapt", Location.class).invoke(null, loc);

            Method getApplicableRegions = null;
            for (Method m : query.getClass().getMethods()) {
                if (!m.getName().equals("getApplicableRegions")) continue;
                Class<?>[] params = m.getParameterTypes();
                if (params.length == 1) {
                    getApplicableRegions = m;
                    break;
                }
            }
            if (getApplicableRegions == null) return false;

            Object applicable = getApplicableRegions.invoke(query, weLoc);
            Method getRegions = applicable.getClass().getMethod("getRegions");
            Object regionsSet = getRegions.invoke(applicable);

            if (regionsSet instanceof Iterable<?> iterable) {
                String wanted = regionId.toLowerCase(Locale.ROOT);
                for (Object pr : iterable) {
                    Method getId = pr.getClass().getMethod("getId");
                    Object idObj = getId.invoke(pr);
                    if (idObj != null && idObj.toString().equalsIgnoreCase(wanted)) {
                        return true;
                    }
                }
            }

            return false;
        } catch (Throwable ignored) {
            return false;
        }
    }
}