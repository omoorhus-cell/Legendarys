package org.tekkabyte.legendarys.legendaryeffects;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.tekkabyte.legendarys.LegendarysPlugin;
import org.tekkabyte.legendarys.legend.LegendaryService;
import org.tekkabyte.legendarys.util.CooldownManager;

public class SwordOfFlightListener implements Listener {

    private static final String ID = "sword-of-flight";
    private final LegendarysPlugin plugin;
    private final LegendaryService legendaryService;
    private final CooldownManager cooldowns;

    public SwordOfFlightListener(LegendarysPlugin plugin, LegendaryService legendaryService, CooldownManager cooldowns) {
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

        String id = legendaryService.getLegendaryId(item);
        if (!ID.equalsIgnoreCase(id)) return;

        long cd = plugin.getConfig().getLong("legendaries.sword-of-flight.cooldown-ms", 900);
        if (!cooldowns.isReady(p.getUniqueId(), ID)) return;
        cooldowns.setCooldown(p.getUniqueId(), ID, cd);
        cooldowns.applyVanillaCooldown(p, item, cd);

        double strength = plugin.getConfig().getDouble("legendaries.sword-of-flight.dash-strength", 1.35);
        double up = plugin.getConfig().getDouble("legendaries.sword-of-flight.dash-upward", 0.15);

        Vector dir = p.getEyeLocation().getDirection().normalize().multiply(strength);
        dir.setY(dir.getY() + up);

        p.setVelocity(p.getVelocity().add(dir));

        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 0.7f, 1.4f);
        p.getWorld().spawnParticle(Particle.CLOUD, p.getLocation().add(0, 1.0, 0), 18, 0.25, 0.25, 0.25, 0.02);
    }
}