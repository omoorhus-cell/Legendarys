package org.tekkabyte.legendarys.legendaryeffects;

import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EvokerFangs;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.bukkit.scheduler.BukkitRunnable;
import org.tekkabyte.legendarys.LegendarysPlugin;
import org.tekkabyte.legendarys.legend.LegendaryService;
import org.tekkabyte.legendarys.util.CooldownManager;
import org.tekkabyte.legendarys.util.RegionBlocker;

public class EvokerWandListener implements Listener {

    private static final String ID = "evoker-wand";

    private final LegendarysPlugin plugin;
    private final LegendaryService legendaryService;
    private final CooldownManager cooldowns;

    public EvokerWandListener(LegendarysPlugin plugin, LegendaryService legendaryService, CooldownManager cooldowns) {
        this.plugin = plugin;
        this.legendaryService = legendaryService;
        this.cooldowns = cooldowns;
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (!e.getAction().isRightClick()) return;

        Player p = e.getPlayer();

        ItemStack item = p.getInventory().getItemInMainHand();
        String id = legendaryService.getLegendaryId(item);
        if (!ID.equalsIgnoreCase(id)) return;

        if (RegionBlocker.isBlocked(p)) {
            p.sendMessage(ChatColor.translateAlternateColorCodes(
                    '&',
                    plugin.getConfig().getString("region-blocker.message", "&cYou cannot use this here.")
            ));
            return;
        }

        long cd = plugin.getConfig().getLong("legendaries.evoker-wand.cooldown-ms", 1200);
        if (!cooldowns.isReady(p.getUniqueId(), ID)) return;
        cooldowns.setCooldown(p.getUniqueId(), ID, cd);
        cooldowns.applyVanillaCooldown(p, item, cd);

        double length = plugin.getConfig().getDouble("legendaries.evoker-wand.length", 10.0);
        double spacing = plugin.getConfig().getDouble("legendaries.evoker-wand.spacing", 1.0);
        double angleDeg = plugin.getConfig().getDouble("legendaries.evoker-wand.angle-deg", 22.0);

        World w = p.getWorld();
        w.playSound(p.getLocation(), Sound.ENTITY_EVOKER_PREPARE_ATTACK, 1.0f, 1.0f);
        w.spawnParticle(Particle.ENCHANT, p.getLocation().add(0, 1.1, 0), 30, 0.35, 0.35, 0.35, 0.0);

        Location start = p.getLocation();
        Vector forward = start.getDirection().setY(0).normalize();

        Vector left = rotateYaw(forward, -angleDeg);
        Vector right = rotateYaw(forward, angleDeg);

        spawnLine(p, start, forward, length, spacing);
        spawnLine(p, start, left, length, spacing);
        spawnLine(p, start, right, length, spacing);
    }

    private void spawnLine(Player owner, Location start, Vector dir, double length, double spacing) {
        World w = owner.getWorld();

        final double damage = plugin.getConfig().getDouble("legendaries.evoker-wand.damage", 6.0);
        final double hitRadius = plugin.getConfig().getDouble("legendaries.evoker-wand.hit-radius", 1.15);

        final int attackDelay = plugin.getConfig().getInt("legendaries.evoker-wand.attack-delay-ticks", 6);

        final int syncOffset = plugin.getConfig().getInt("legendaries.evoker-wand.sync-offset-ticks", 1);

        final double damp = plugin.getConfig().getDouble("legendaries.evoker-wand.kb.dampen", 0.25);
        final double kbStrength = plugin.getConfig().getDouble("legendaries.evoker-wand.kb.strength", 0.18);
        final double kbY = plugin.getConfig().getDouble("legendaries.evoker-wand.kb.y", 0.02);

        for (double d = 1.0; d <= length; d += spacing) {
            Location at = start.clone().add(dir.clone().multiply(d));
            int groundY = w.getHighestBlockYAt(at);
            at.setY(groundY + 0.1);

            EvokerFangs f = w.spawn(at, EvokerFangs.class);
            f.setOwner(owner);

            try {
                f.setAttackDelay(attackDelay);
            } catch (Throwable ignored) {}

            w.spawnParticle(Particle.SOUL, at.clone().add(0, 0.2, 0), 6, 0.15, 0.05, 0.15, 0.0);

            int hitAtTicksLived = attackDelay + Math.max(0, syncOffset);

            new BukkitRunnable() {
                private boolean done = false;

                @Override
                public void run() {
                    if (done) {
                        cancel();
                        return;
                    }
                    if (!owner.isOnline()) {
                        cancel();
                        return;
                    }
                    if (!f.isValid()) {
                        cancel();
                        return;
                    }

                    if (f.getTicksLived() < hitAtTicksLived) return;

                    done = true;

                    Location loc = f.getLocation();
                    World fw = loc.getWorld();
                    if (fw == null) {
                        cancel();
                        return;
                    }

                    fw.spawnParticle(Particle.CRIT, loc.clone().add(0, 0.2, 0), 10, 0.25, 0.10, 0.25, 0.0);

                    for (Entity ent : fw.getNearbyEntities(loc, hitRadius, 1.4, hitRadius)) {
                        if (!(ent instanceof LivingEntity le)) continue;
                        if (!le.isValid() || le.isDead()) continue;
                        if (le.getUniqueId().equals(owner.getUniqueId())) continue;

                        le.damage(damage, owner);

                        Vector cur = le.getVelocity();
                        Vector base = cur.multiply(clamp01(damp));

                        Vector push = le.getLocation().toVector().subtract(loc.toVector());
                        push.setY(0);
                        if (push.lengthSquared() > 0.0001) push.normalize().multiply(kbStrength);
                        else push.zero();

                        Vector out = base.add(push);
                        out.setY(Math.max(out.getY(), kbY));
                        le.setVelocity(out);
                    }

                    cancel();
                }
            }.runTaskTimer(plugin, 0L, 1L);
        }
    }

    private static Vector rotateYaw(Vector v, double degrees) {
        double r = Math.toRadians(degrees);
        double cos = Math.cos(r);
        double sin = Math.sin(r);

        double x = v.getX();
        double z = v.getZ();

        double nx = (x * cos) - (z * sin);
        double nz = (x * sin) + (z * cos);

        return new Vector(nx, 0.0, nz).normalize();
    }

    private static double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}