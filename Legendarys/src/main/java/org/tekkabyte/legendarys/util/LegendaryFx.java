package org.tekkabyte.legendarys.util;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class LegendaryFx {
    private LegendaryFx() {}

    public static void swing(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.0f);
        p.getWorld().spawnParticle(Particle.SWEEP_ATTACK, p.getLocation().add(0, 1.0, 0), 1, 0.0, 0.0, 0.0, 0.0);
    }

    public static void impactSlice(World w, Location loc) {
        w.spawnParticle(Particle.SWEEP_ATTACK, loc.clone().add(0, 1.0, 0), 1, 0, 0, 0, 0);
        w.spawnParticle(Particle.CRIT, loc.clone().add(0, 1.0, 0), 10, 0.35, 0.35, 0.35, 0.05);
        w.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.9f, 1.15f);
    }

    public static void whoosh(World w, Location loc) {
        w.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.5f, 0.75f);
    }

    public static void thunder(World w, Location loc) {
        w.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
        w.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.8f, 1.2f);
    }

    public static void sonicLaunch(World w, Location loc) {
        w.playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 0.6f);
        w.playSound(loc, Sound.ENTITY_WARDEN_ROAR, 0.35f, 1.2f);
    }

    public static void sonicImpact(World w, Location loc) {
        w.playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 0.85f);
        w.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 1.0f);
        w.spawnParticle(Particle.SONIC_BOOM, loc.clone().add(0, 0.6, 0), 1, 0, 0, 0, 0);
        w.spawnParticle(Particle.EXPLOSION, loc.clone().add(0, 0.4, 0), 1, 0, 0, 0, 0);
    }

    public static void trail(World w, Location loc, Particle particle, int count) {
        w.spawnParticle(particle, loc, count, 0.15, 0.15, 0.15, 0.0);
    }

    public static Vector flatForward(Player p) {
        Vector v = p.getLocation().getDirection().clone();
        v.setY(0);
        if (v.lengthSquared() < 1e-6) return new Vector(0, 0, 0);
        return v.normalize();
    }
}