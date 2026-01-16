package org.tekkabyte.legendarys.legendaryeffects;

import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.tekkabyte.legendarys.LegendarysPlugin;
import org.tekkabyte.legendarys.legend.LegendaryService;
import org.tekkabyte.legendarys.util.CooldownManager;
import org.tekkabyte.legendarys.util.LegendaryFx;
import org.tekkabyte.legendarys.util.RegionBlocker;
import org.tekkabyte.legendarys.util.TeleportUtil;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EnderKatanaListener implements Listener {

    private static final String ID = "ender-katana";

    private final LegendarysPlugin plugin;
    private final LegendaryService legendaryService;
    private final CooldownManager cooldowns;

    private final Set<UUID> ignoreNextMeleeSweep = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Integer> lastSweepTick = new ConcurrentHashMap<>();

    public EnderKatanaListener(LegendarysPlugin plugin, LegendaryService legendaryService, CooldownManager cooldowns) {
        this.plugin = plugin;
        this.legendaryService = legendaryService;
        this.cooldowns = cooldowns;
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent e) {
        if (!e.getAction().isRightClick()) return;

        Player p = e.getPlayer();

        ItemStack item = p.getInventory().getItem(e.getHand());
        if (item == null) return;
        if (!ID.equalsIgnoreCase(legendaryService.getLegendaryId(item))) return;

        if (RegionBlocker.isBlocked(p)) {
            p.sendMessage(ChatColor.translateAlternateColorCodes(
                    '&',
                    plugin.getConfig().getString("region-blocker.message", "&cYou cannot use this here.")
            ));
            return;
        }

        double distance = plugin.getConfig().getDouble("legendaries.ender-katana.distance", 5.0);
        double hitRadius = plugin.getConfig().getDouble("legendaries.ender-katana.hit-radius", 1.25);
        double slashDamage = plugin.getConfig().getDouble("legendaries.ender-katana.slash-damage", 7.0);

        Location start = p.getLocation().clone();
        Location end = computeBlinkTarget(p, distance);

        end = clampOutsideBlocked(start, end);
        if (end == null) {
            p.sendMessage(ChatColor.translateAlternateColorCodes(
                    '&',
                    plugin.getConfig().getString("region-blocker.message", "&cYou cannot use this here.")
            ));
            return;
        }

        final Location endLoc = end;

        long cd = plugin.getConfig().getLong("legendaries.ender-katana.cooldown-ms", 900);
        if (!cooldowns.isReady(p.getUniqueId(), ID)) return;
        cooldowns.setCooldown(p.getUniqueId(), ID, cd);
        cooldowns.applyVanillaCooldown(p, item, cd);

        World w = p.getWorld();

        LivingEntity hit = w.getLivingEntities().stream()
                .filter(le -> !le.getUniqueId().equals(p.getUniqueId()))
                .filter(le -> intersectsPath(start, endLoc, le.getLocation(), hitRadius))
                .min(Comparator.comparingDouble(le -> le.getLocation().distanceSquared(start)))
                .orElse(null);

        w.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.9f, 1.2f);
        w.spawnParticle(Particle.PORTAL, p.getLocation().add(0, 1.0, 0), 24, 0.35, 0.35, 0.35, 0.1);

        Vector vel = p.getVelocity().clone();
        double mult = plugin.getConfig().getDouble("legendaries.ender-katana.carry-velocity-multiplier", 1.0);
        vel.multiply(mult);

        boolean preserveY = plugin.getConfig().getBoolean("legendaries.ender-katana.preserve-vertical", true);
        if (!preserveY) vel.setY(0);

        double maxAbsY = plugin.getConfig().getDouble("legendaries.ender-katana.max-abs-vertical", 1.2);
        if (Math.abs(vel.getY()) > maxAbsY) vel.setY(Math.signum(vel.getY()) * maxAbsY);

        double nudge = plugin.getConfig().getDouble("legendaries.ender-katana.forward-nudge", 0.12);
        Vector dir = horizontalDirectionFromYaw(start);
        Vector carry = vel.add(dir.multiply(nudge));

        TeleportUtil.teleportRetainingRideStack(plugin, p, end, carry);

        w.spawnParticle(Particle.PORTAL, end.clone().add(0, 1.0, 0), 24, 0.35, 0.35, 0.35, 0.1);
        w.playSound(end, Sound.ENTITY_ENDERMAN_TELEPORT, 0.9f, 1.35f);

        if (hit != null) {
            if (hit instanceof Player hp && RegionBlocker.isBlocked(hp)) return;

            playSweepFx(p);
            ignoreNextMeleeSweep.add(p.getUniqueId());
            plugin.getServer().getScheduler().runTask(plugin, () -> ignoreNextMeleeSweep.remove(p.getUniqueId()));

            LegendaryFx.impactSlice(w, hit.getLocation());
            hit.damage(slashDamage, p);
        }
    }

    @EventHandler
    public void onMelee(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p)) return;

        ItemStack item = p.getInventory().getItemInMainHand();
        if (!ID.equalsIgnoreCase(legendaryService.getLegendaryId(item))) return;

        if (RegionBlocker.isBlocked(p)) return;

        if (e.getEntity() instanceof Player victim && RegionBlocker.isBlocked(victim)) return;

        if (ignoreNextMeleeSweep.contains(p.getUniqueId())) return;

        int tick = plugin.getServer().getCurrentTick();
        Integer last = lastSweepTick.put(p.getUniqueId(), tick);
        if (last != null && last == tick) return;

        playSweepFx(p);
    }

    private void playSweepFx(Player p) {
        Location base = p.getLocation().clone();
        Vector forward = base.getDirection().normalize();
        Location fxLoc = base.add(forward.multiply(1.1)).add(0, 1.0, 0);
        World w = p.getWorld();
        w.spawnParticle(Particle.SWEEP_ATTACK, fxLoc, 1, 0, 0, 0, 0);
        w.playSound(fxLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.9f, 1.2f);
    }

    private Location computeBlinkTarget(Player p, double maxDistance) {
        Location from = p.getLocation().clone();
        Vector dir = horizontalDirectionFromYaw(from);

        World w = p.getWorld();
        Location rayStart = p.getEyeLocation().clone();

        RayTraceResult hit = w.rayTraceBlocks(
                rayStart,
                dir,
                maxDistance,
                FluidCollisionMode.NEVER,
                true
        );

        double finalDistance = maxDistance;
        if (hit != null && hit.getHitPosition() != null) {
            finalDistance = Math.max(0.0, rayStart.toVector().distance(hit.getHitPosition()) - 0.30);
        }

        Location target = from.clone().add(dir.multiply(finalDistance));
        target.setY(from.getY());
        target.setYaw(from.getYaw());
        target.setPitch(from.getPitch());
        return target;
    }

    private Vector horizontalDirectionFromYaw(Location loc) {
        double yawRad = Math.toRadians(loc.getYaw());
        return new Vector(-Math.sin(yawRad), 0, Math.cos(yawRad)).normalize();
    }

    private Location clampOutsideBlocked(Location start, Location desiredEnd) {
        if (start == null || desiredEnd == null) return null;
        if (start.getWorld() == null || desiredEnd.getWorld() == null) return null;
        if (!start.getWorld().equals(desiredEnd.getWorld())) return desiredEnd;

        if (!RegionBlocker.isBlocked(desiredEnd)) return desiredEnd;

        double step = plugin.getConfig().getDouble("legendaries.ender-katana.spawn-clamp-step", 0.25);
        if (step <= 0) step = 0.25;

        Vector a = start.toVector();
        Vector b = desiredEnd.toVector();
        Vector ab = b.clone().subtract(a);

        double dist = ab.length();
        if (dist < 1e-6) return null;

        Vector dir = ab.clone().multiply(1.0 / dist);

        for (double back = 0.0; back <= dist; back += step) {
            Vector v = b.clone().subtract(dir.clone().multiply(back));
            Location candidate = desiredEnd.clone();
            candidate.setX(v.getX());
            candidate.setY(start.getY());
            candidate.setZ(v.getZ());

            if (!RegionBlocker.isBlocked(candidate)) {
                return candidate;
            }
        }

        return null;
    }

    private boolean intersectsPath(Location a, Location b, Location point, double radius) {
        Vector a2 = a.toVector(); a2.setY(0);
        Vector b2 = b.toVector(); b2.setY(0);
        Vector p2 = point.toVector(); p2.setY(0);

        Vector ap = p2.clone().subtract(a2);
        Vector ab = b2.clone().subtract(a2);

        double ab2 = ab.lengthSquared();
        if (ab2 < 1e-6) return false;

        double t = ap.dot(ab) / ab2;
        if (t < 0 || t > 1) return false;

        Vector closest = a2.add(ab.multiply(t));
        return closest.distanceSquared(p2) <= (radius * radius);
    }
}