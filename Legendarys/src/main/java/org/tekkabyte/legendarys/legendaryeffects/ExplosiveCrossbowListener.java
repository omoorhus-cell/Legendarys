package org.tekkabyte.legendarys.legendaryeffects;

import org.bukkit.*;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.tekkabyte.legendarys.LegendarysPlugin;
import org.tekkabyte.legendarys.legend.LegendaryService;
import org.tekkabyte.legendarys.util.CooldownManager;
import org.tekkabyte.legendarys.util.LegendaryFx;
import org.tekkabyte.legendarys.util.RegionBlocker;

public class ExplosiveCrossbowListener implements Listener {

    private static final String ID = "explosive-crossbow";

    private final LegendarysPlugin plugin;
    private final LegendaryService legendaryService;
    private final CooldownManager cooldowns;

    private final NamespacedKey KEY_SONIC;

    public ExplosiveCrossbowListener(LegendarysPlugin plugin, LegendaryService legendaryService, CooldownManager cooldowns) {
        this.plugin = plugin;
        this.legendaryService = legendaryService;
        this.cooldowns = cooldowns;
        this.KEY_SONIC = new NamespacedKey(plugin, "sonic_proj");
    }

    @EventHandler
    public void onShoot(EntityShootBowEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;

        ItemStack bow = e.getBow();
        if (bow == null) return;

        String id = legendaryService.getLegendaryId(bow);
        if (!ID.equalsIgnoreCase(id)) return;

        if (RegionBlocker.isBlocked(p)) {
            p.sendMessage(ChatColor.translateAlternateColorCodes(
                    '&',
                    plugin.getConfig().getString("region-blocker.message", "&cYou cannot use this here.")
            ));
            e.setCancelled(true);
            return;
        }

        long cd = plugin.getConfig().getLong("legendaries.explosive-crossbow.cooldown-ms", 800);
        if (!cooldowns.isReady(p.getUniqueId(), ID)) {
            e.setCancelled(true);
            return;
        }
        cooldowns.setCooldown(p.getUniqueId(), ID, cd);
        cooldowns.applyVanillaCooldown(p, bow, cd);

        if (!(e.getProjectile() instanceof AbstractArrow arrow)) return;

        arrow.getPersistentDataContainer().set(KEY_SONIC, PersistentDataType.BYTE, (byte) 1);

        double mult = plugin.getConfig().getDouble("legendaries.explosive-crossbow.projectile-speed-multiplier", 1.15);
        arrow.setVelocity(arrow.getVelocity().multiply(mult));

        LegendaryFx.sonicLaunch(p.getWorld(), p.getLocation());

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!arrow.isValid() || arrow.isDead() || arrow.isOnGround() || arrow.getLocation().getWorld() == null) {
                    cancel();
                    return;
                }
                ticks++;
                Location loc = arrow.getLocation();
                World w = loc.getWorld();
                if (w == null) {
                    cancel();
                    return;
                }

                w.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 2, 0.03, 0.03, 0.03, 0.0);
                w.spawnParticle(Particle.SCULK_CHARGE, loc.clone().add(0, 0.05, 0), 1, 0, 0, 0, 0);
                w.spawnParticle(Particle.SONIC_BOOM, loc.clone().add(0, 0.2, 0), 1, 0, 0, 0, 0);

                if (ticks > 200) cancel();
            }
        }.runTaskTimer(plugin, 1L, 2L);
    }

    @EventHandler
    public void onHit(ProjectileHitEvent e) {
        if (!(e.getEntity() instanceof AbstractArrow arrow)) return;

        Byte b = arrow.getPersistentDataContainer().get(KEY_SONIC, PersistentDataType.BYTE);
        if (b == null || b != (byte) 1) return;

        Player shooter = (arrow.getShooter() instanceof Player p) ? p : null;
        if (shooter != null && RegionBlocker.isBlocked(shooter)) {
            arrow.remove();
            return;
        }

        Location loc = arrow.getLocation();
        World w = loc.getWorld();
        if (w == null) return;

        arrow.remove();

        double radiusCfg = plugin.getConfig().getDouble("legendaries.explosive-crossbow.explosion.radius", 4.5);
        double aoeDamage = plugin.getConfig().getDouble("legendaries.explosive-crossbow.explosion.damage", 6.0);
        double directBonus = plugin.getConfig().getDouble("legendaries.explosive-crossbow.direct-hit.bonus-damage", 6.0);
        double knockback = plugin.getConfig().getDouble("legendaries.explosive-crossbow.explosion.knockback", 0.8);

        double radius = radiusCfg * 1.45;

        LegendaryFx.sonicImpact(w, loc);

        w.playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.4f, 0.85f);
        w.playSound(loc, Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 1.2f, 1.15f);
        w.playSound(loc, Sound.BLOCK_SCULK_CATALYST_BLOOM, 1.2f, 0.95f);

        w.spawnParticle(Particle.SONIC_BOOM, loc.clone().add(0, 0.6, 0), 3, 0, 0, 0, 0);
        w.spawnParticle(Particle.SCULK_SOUL, loc.clone().add(0, 0.2, 0), 130, 0.85, 0.35, 0.85, 0.03);
        w.spawnParticle(Particle.SOUL, loc.clone().add(0, 0.2, 0), 80, 0.85, 0.35, 0.85, 0.03);
        w.spawnParticle(Particle.SCULK_CHARGE_POP, loc.clone().add(0, 0.2, 0), 60, 0.55, 0.25, 0.55, 0.0);
        w.spawnParticle(Particle.SMOKE, loc.clone().add(0, 0.2, 0), 35, 0.55, 0.20, 0.55, 0.03);

        spawnBlastRadiusRing(w, loc, radius);

        new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                t++;
                if (t > 10) {
                    cancel();
                    return;
                }

                double r = 0.8 + (t * 0.75);
                int points = 30;

                for (int i = 0; i < points; i++) {
                    double ang = (Math.PI * 2.0) * ((double) i / (double) points);
                    double x = Math.cos(ang) * r;
                    double z = Math.sin(ang) * r;
                    Location p = loc.clone().add(x, 0.15, z);
                    w.spawnParticle(Particle.SCULK_CHARGE, p, 1, 0, 0, 0, 0);
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, p, 1, 0, 0, 0, 0);
                    if ((i & 3) == 0) w.spawnParticle(Particle.SCULK_SOUL, p.clone().add(0, 0.12, 0), 1, 0, 0, 0, 0);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        if (e.getHitEntity() instanceof LivingEntity direct && shooter != null) {
            if (!direct.getUniqueId().equals(shooter.getUniqueId())) {
                if (!(direct instanceof Player pl) || !RegionBlocker.isBlocked(pl)) {
                    direct.damage(directBonus, shooter);
                }
            }
        }

        double r2 = radius * radius;

        for (LivingEntity target : w.getLivingEntities()) {
            if (target.getLocation().distanceSquared(loc) > r2) continue;
            if (shooter != null && target.getUniqueId().equals(shooter.getUniqueId())) continue;
            if (target instanceof Player pl && RegionBlocker.isBlocked(pl)) continue;

            if (shooter != null) target.damage(aoeDamage, shooter);
            else target.damage(aoeDamage);

            Vector push = target.getLocation().toVector().subtract(loc.toVector());
            push.setY(0);

            if (push.lengthSquared() > 1e-6) push.normalize().multiply(knockback);
            else push.zero();

            push.setY(0.25);
            target.setVelocity(target.getVelocity().add(push));
        }
    }

    private void spawnBlastRadiusRing(World w, Location center, double radius) {
        int points = 80;
        double y = 0.15;

        Location base = center.clone();
        base.setY(base.getY() + y);

        for (int i = 0; i < points; i++) {
            double ang = (Math.PI * 2.0) * ((double) i / (double) points);
            double x = Math.cos(ang) * radius;
            double z = Math.sin(ang) * radius;
            Location p = base.clone().add(x, 0, z);

            w.spawnParticle(Particle.SCULK_CHARGE, p, 1, 0, 0, 0, 0);
            w.spawnParticle(Particle.SOUL_FIRE_FLAME, p, 1, 0, 0, 0, 0);

            if ((i & 3) == 0) {
                w.spawnParticle(Particle.SCULK_SOUL, p.clone().add(0, 0.12, 0), 1, 0, 0, 0, 0);
            }
        }
    }
}