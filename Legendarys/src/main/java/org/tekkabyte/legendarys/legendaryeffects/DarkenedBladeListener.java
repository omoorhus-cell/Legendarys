package org.tekkabyte.legendarys.legendaryeffects;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.tekkabyte.legendarys.LegendarysPlugin;
import org.tekkabyte.legendarys.legend.LegendaryService;
import org.tekkabyte.legendarys.util.CooldownManager;
import org.tekkabyte.legendarys.util.RegionBlocker;

import java.util.concurrent.ThreadLocalRandom;

public class DarkenedBladeListener implements Listener {

    private static final String ID = "darkened-blade";
    private final LegendarysPlugin plugin;
    private final LegendaryService legendaryService;
    private final CooldownManager cooldowns;

    public DarkenedBladeListener(LegendarysPlugin plugin, LegendaryService legendaryService, CooldownManager cooldowns) {
        this.plugin = plugin;
        this.legendaryService = legendaryService;
        this.cooldowns = cooldowns;
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p)) return;
        if (!(e.getEntity() instanceof Player victim)) return;

        ItemStack item = p.getInventory().getItemInMainHand();
        String id = legendaryService.getLegendaryId(item);
        if (!ID.equalsIgnoreCase(id)) return;

        if (RegionBlocker.isBlocked(p)) return;

        double chance = plugin.getConfig().getDouble("legendaries.darkened-blade.blindness.chance", 0.25);
        if (chance <= 0) return;

        long cd = plugin.getConfig().getLong("legendaries.darkened-blade.cooldown-ms", 0);
        if (cd > 0 && !cooldowns.isReady(p.getUniqueId(), ID)) return;

        if (ThreadLocalRandom.current().nextDouble() > chance) return;

        if (cd > 0) cooldowns.setCooldown(p.getUniqueId(), ID, cd);

        int durationTicks = plugin.getConfig().getInt("legendaries.darkened-blade.blindness.duration-ticks", 60);
        int amp = plugin.getConfig().getInt("legendaries.darkened-blade.blindness.amplifier", 0);

        victim.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, durationTicks, amp, true, false, true));
        victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_ENDERMAN_STARE, 0.6f, 0.6f);
        victim.getWorld().spawnParticle(Particle.SMOKE, victim.getLocation().add(0, 1.0, 0), 22, 0.35, 0.35, 0.35, 0.02);
    }
}
