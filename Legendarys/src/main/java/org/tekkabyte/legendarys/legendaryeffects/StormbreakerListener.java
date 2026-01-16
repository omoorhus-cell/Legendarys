package org.tekkabyte.legendarys.legendaryeffects;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.tekkabyte.legendarys.LegendarysPlugin;
import org.tekkabyte.legendarys.legend.LegendaryService;
import org.tekkabyte.legendarys.util.CooldownManager;
import org.tekkabyte.legendarys.util.LegendaryFx;
import org.tekkabyte.legendarys.util.RegionBlocker;

public class StormbreakerListener implements Listener {

    private static final String ID = "stormbreaker";
    private final LegendarysPlugin plugin;
    private final LegendaryService legendaryService;
    private final CooldownManager cooldowns;

    public StormbreakerListener(LegendarysPlugin plugin, LegendaryService legendaryService, CooldownManager cooldowns) {
        this.plugin = plugin;
        this.legendaryService = legendaryService;
        this.cooldowns = cooldowns;
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p)) return;
        if (!(e.getEntity() instanceof LivingEntity target)) return;

        ItemStack item = p.getInventory().getItemInMainHand();
        String id = legendaryService.getLegendaryId(item);
        if (!ID.equalsIgnoreCase(id)) return;

        if (target instanceof Player victim) {
            if (RegionBlocker.isBlocked(p, victim)) return;
        } else {
            if (RegionBlocker.isBlocked(p)) return;
        }

        long cd = plugin.getConfig().getLong("legendaries.stormbreaker.cooldown-ms", 0);

        if (cd > 0) {
            if (!cooldowns.isReady(p.getUniqueId(), ID)) return;
            cooldowns.setCooldown(p.getUniqueId(), ID, cd);
            cooldowns.applyVanillaCooldown(p, item, cd);
            int ticks = (int) Math.max(1, Math.round(cd / 50.0));
            Material mat = item.getType();
            p.setCooldown(mat, ticks);
        }

        if (e.getDamage() <= 0) return;

        e.setDamage(0);

        double trueDamage = plugin.getConfig().getDouble("legendaries.stormbreaker.true-damage", 7.0);
        double newHealth = Math.max(0.0, target.getHealth() - trueDamage);
        target.setHealth(newHealth);

        Location loc = target.getLocation();
        World w = loc.getWorld();
        if (w != null) {
            w.strikeLightningEffect(loc);
            LegendaryFx.thunder(w, loc);
        }
    }
}