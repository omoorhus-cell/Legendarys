package org.tekkabyte.legendarys.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.tekkabyte.legendarys.LegendarysPlugin;
import org.tekkabyte.legendarys.legend.LegendaryService;

public class SwordOfFlightHoldTask {

    private static final String ID = "sword-of-flight";

    private final LegendarysPlugin plugin;
    private final LegendaryService legendaryService;

    public SwordOfFlightHoldTask(LegendarysPlugin plugin, LegendaryService legendaryService) {
        this.plugin = plugin;
        this.legendaryService = legendaryService;
    }

    public void start() {
        long period = plugin.getConfig().getLong("legendaries.sword-of-flight.slow-falling.refresh-ticks", 10L);
        int duration = plugin.getConfig().getInt("legendaries.sword-of-flight.slow-falling.duration-ticks", 40);
        int amp = plugin.getConfig().getInt("legendaries.sword-of-flight.slow-falling.amplifier", 0);

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (isHolding(p)) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, duration, amp, true, false, true));
                }
            }
        }, 1L, period);
    }

    private boolean isHolding(Player p) {
        ItemStack main = p.getInventory().getItemInMainHand();
        ItemStack off = p.getInventory().getItemInOffHand();

        String idMain = legendaryService.getLegendaryId(main);
        if (ID.equalsIgnoreCase(idMain)) return true;

        String idOff = legendaryService.getLegendaryId(off);
        return ID.equalsIgnoreCase(idOff);
    }
}