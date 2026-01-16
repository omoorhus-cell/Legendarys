package org.tekkabyte.legendarys.legendaryeffects;

import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.tekkabyte.legendarys.LegendarysPlugin;
import org.tekkabyte.legendarys.legend.LegendaryService;
import org.tekkabyte.legendarys.util.CooldownManager;
import org.tekkabyte.legendarys.util.LegendaryFx;
import org.tekkabyte.legendarys.util.RegionBlocker;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ExcaliburListener implements Listener {

    private static final String ID = "excalibur";
    private final LegendarysPlugin plugin;
    private final LegendaryService legendaryService;
    private final CooldownManager cooldowns;

    public ExcaliburListener(LegendarysPlugin plugin, LegendaryService legendaryService, CooldownManager cooldowns) {
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
        if (!ID.equalsIgnoreCase(legendaryService.getLegendaryId(item))) return;

        if (RegionBlocker.isBlocked(p)) {
            p.sendMessage(ChatColor.translateAlternateColorCodes(
                    '&',
                    plugin.getConfig().getString("region-blocker.message", "&cYou cannot use this here.")
            ));
            return;
        }

        long cd = plugin.getConfig().getLong("legendaries.excalibur.cooldown-ms", 900);
        if (!cooldowns.isReady(p.getUniqueId(), ID)) return;
        cooldowns.setCooldown(p.getUniqueId(), ID, cd);
        cooldowns.applyVanillaCooldown(p, item, cd);

        double damage = plugin.getConfig().getDouble("legendaries.excalibur.damage", 6.0);
        double hitRadius = plugin.getConfig().getDouble("legendaries.excalibur.hit-radius", 1.4);

        int rings = plugin.getConfig().getInt("legendaries.excalibur.wifi.rings", 5);
        double ringSpacingForward = plugin.getConfig().getDouble("legendaries.excalibur.wifi.ring-spacing", 2.0);
        double radiusStep = plugin.getConfig().getDouble("legendaries.excalibur.wifi.radius-step", 1.25);
        double arcDegrees = plugin.getConfig().getDouble("legendaries.excalibur.wifi.arc-degrees", 120.0);
        int pointsPerArc = plugin.getConfig().getInt("legendaries.excalibur.wifi.points", 22);
        double forwardCurve = plugin.getConfig().getDouble("legendaries.excalibur.wifi.forward-curve", 0.9);
        double originOffset = plugin.getConfig().getDouble("legendaries.excalibur.wifi.origin-offset", 1.2);

        double kbStrength = plugin.getConfig().getDouble("legendaries.excalibur.knockback.strength", 0.65);
        double kbUpward = plugin.getConfig().getDouble("legendaries.excalibur.knockback.upward", 0.12);

        World w = p.getWorld();
        w.playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.9f, 1.25f);
        w.playSound(p.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.7f, 1.6f);

        Vector forward = p.getEyeLocation().getDirection().normalize();
        Vector right = forward.clone().crossProduct(new Vector(0, 1, 0)).normalize();

        Particle.DustOptions gold = new Particle.DustOptions(Color.fromRGB(255, 215, 0), 1.25f);
        Particle.DustOptions cyan = new Particle.DustOptions(Color.fromRGB(0, 255, 255), 1.25f);

        Set<UUID> hit = new HashSet<>();

        Location origin = p.getEyeLocation().clone().add(forward.clone().multiply(originOffset));
        double halfArc = Math.toRadians(arcDegrees / 2.0);
        double baseRadius = Math.max(0.0001, radiusStep);

        for (int r = 1; r <= rings; r++) {
            double forwardDist = r * ringSpacingForward;
            double radius = r * radiusStep;

            Particle.DustOptions dust = (r % 2 == 0) ? cyan : gold;
            Location arcCenter = origin.clone().add(forward.clone().multiply(forwardDist));
            double curveScale = radius / baseRadius;

            for (int i = 0; i <= pointsPerArc; i++) {
                double t = (double) i / (double) pointsPerArc;
                double angle = (-halfArc) + (t * (2.0 * halfArc));

                Vector sideways = right.clone().multiply(Math.sin(angle) * radius);
                double forwardFactor = Math.cos(angle);
                Vector forwardBend = forward.clone().multiply(forwardFactor * forwardCurve * curveScale);

                Location pos = arcCenter.clone().add(sideways).add(forwardBend);
                w.spawnParticle(Particle.DUST, pos, 1, 0.02, 0.02, 0.02, 0.0, dust);

                for (LivingEntity le : pos.getNearbyLivingEntities(hitRadius)) {
                    if (le.getUniqueId().equals(p.getUniqueId())) continue;

                    if (le instanceof Player targetPlayer) {
                        if (RegionBlocker.isBlocked(targetPlayer)) continue;
                    }

                    if (hit.add(le.getUniqueId())) {
                        LegendaryFx.impactSlice(w, le.getLocation());

                        if (le instanceof Player targetPlayer) {
                            targetPlayer.setMetadata("legendarys:excalibur_hit",
                                    new org.bukkit.metadata.FixedMetadataValue(plugin, System.currentTimeMillis()));
                        }

                        le.damage(damage, p);

                        Vector kb = le.getLocation().toVector().subtract(p.getLocation().toVector());
                        kb.setY(0);
                        if (kb.lengthSquared() < 1e-6) kb = forward.clone();
                        kb.normalize().multiply(kbStrength);
                        kb.setY(kbUpward);
                        le.setVelocity(le.getVelocity().add(kb));

                        w.playSound(le.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.9f, 1.2f);
                    }
                }
            }
        }
    }
}