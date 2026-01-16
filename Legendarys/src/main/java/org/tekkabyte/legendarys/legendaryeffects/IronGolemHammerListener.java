package org.tekkabyte.legendarys.legendaryeffects;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.tekkabyte.legendarys.LegendarysPlugin;
import org.tekkabyte.legendarys.legend.LegendaryService;
import org.tekkabyte.legendarys.util.CooldownManager;
import org.tekkabyte.legendarys.util.RegionBlocker;

public class IronGolemHammerListener implements Listener {

    private static final String ID = "iron-golem-hammer";
    private final LegendarysPlugin plugin;
    private final LegendaryService legendaryService;
    private final CooldownManager cooldowns;

    public IronGolemHammerListener(LegendarysPlugin plugin, LegendaryService legendaryService, CooldownManager cooldowns) {
        this.plugin = plugin;
        this.legendaryService = legendaryService;
        this.cooldowns = cooldowns;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p)) return;
        if (!(e.getEntity() instanceof LivingEntity target)) return;

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

        if (target instanceof Player tp && RegionBlocker.isBlocked(tp)) return;

        boolean playersOnly = plugin.getConfig().getBoolean("legendaries.iron-golem-hammer.players-only", true);
        if (playersOnly && !(target instanceof Player)) return;

        long cd = plugin.getConfig().getLong("legendaries.iron-golem-hammer.cooldown-ms", 0);
        if (cd > 0) {
            if (!cooldowns.isReady(p.getUniqueId(), ID)) return;
            cooldowns.setCooldown(p.getUniqueId(), ID, cd);
            cooldowns.applyVanillaCooldown(p, item, cd);
        }

        double baseY = plugin.getConfig().getDouble("legendaries.iron-golem-hammer.base-y", 0.45);
        double perDmg = plugin.getConfig().getDouble("legendaries.iron-golem-hammer.per-damage-y", 0.06);
        double minY = plugin.getConfig().getDouble("legendaries.iron-golem-hammer.min-y", 0.35);
        double maxY = plugin.getConfig().getDouble("legendaries.iron-golem-hammer.max-y", 1.25);
        double horizontal = plugin.getConfig().getDouble("legendaries.iron-golem-hammer.horizontal", 0.2);

        double finalDamage = e.getFinalDamage();
        double y = baseY + (finalDamage * perDmg);
        y = Math.max(minY, Math.min(maxY, y));

        Vector away = target.getLocation().toVector().subtract(p.getLocation().toVector());
        away.setY(0);
        if (away.lengthSquared() < 1e-6) away = p.getLocation().getDirection().setY(0);
        away.normalize().multiply(horizontal);

        Vector desired = target.getVelocity().clone();
        desired.setX(desired.getX() + away.getX());
        desired.setZ(desired.getZ() + away.getZ());
        desired.setY(Math.max(desired.getY(), y));

        World w = target.getWorld();
        w.playSound(target.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0f, 0.8f);
        w.spawnParticle(Particle.BLOCK, target.getLocation().add(0, 0.2, 0),
                18, 0.35, 0.05, 0.35, 0.2, Material.STONE.createBlockData());
        w.spawnParticle(Particle.EXPLOSION, target.getLocation().add(0, 0.3, 0), 1, 0, 0, 0, 0);

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!target.isValid() || target.isDead()) return;

            target.setFallDistance(0f);

            Vector cur = target.getVelocity();
            Vector v = desired.clone();

            v.setY(Math.max(cur.getY(), desired.getY()));

            target.setVelocity(v);

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (!target.isValid() || target.isDead()) return;
                target.setFallDistance(0f);

                Vector cur2 = target.getVelocity();
                Vector v2 = desired.clone();
                v2.setY(Math.max(cur2.getY(), desired.getY()));
                target.setVelocity(v2);
            });
        });
    }
}
