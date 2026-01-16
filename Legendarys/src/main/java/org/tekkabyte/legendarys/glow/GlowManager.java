package org.tekkabyte.legendarys.glow;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.tekkabyte.legendarys.LegendarysPlugin;
import org.tekkabyte.legendarys.legend.LegendaryService;
import org.tekkabyte.legendarys.lp.LuckPermsStarSuffixManager;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GlowManager {

    private final LegendarysPlugin plugin;
    private final LegendaryService legendaryService;
    private final LuckPermsStarSuffixManager lpStar;

    private int taskId = -1;

    private final Set<UUID> pending = ConcurrentHashMap.newKeySet();

    public GlowManager(LegendarysPlugin plugin, LegendaryService legendaryService, LuckPermsStarSuffixManager lpStar) {
        this.plugin = plugin;
        this.legendaryService = legendaryService;
        this.lpStar = lpStar;
    }

    public void start() {
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                updateNow(p);
            }
        }, 40L, 40L);
    }

    public void stop() {
        if (taskId != -1) Bukkit.getScheduler().cancelTask(taskId);
        taskId = -1;
    }

    public void requestUpdate(Player player) {
        if (player == null) return;

        UUID id = player.getUniqueId();
        if (!pending.add(id)) return;

        Bukkit.getScheduler().runTask(plugin, () -> {
            pending.remove(id);
            if (!player.isOnline()) return;
            updateNow(player);
        });
    }

    private void updateNow(Player player) {
        boolean enabled = plugin.getConfig().getBoolean("legendary-visuals.enabled", true);

        removeFromTeams(player);

        if (!enabled) {
            player.setGlowing(false);
            if (lpStar != null) lpStar.update(player, Set.of());
            return;
        }

        Set<String> presentIds = getPresentLegendaryIds(player);

        boolean hasAnyLegendary = !presentIds.isEmpty();
        player.setGlowing(hasAnyLegendary);

        if (lpStar != null) {
            lpStar.update(player, presentIds);
        }
    }

    public void removeFromTeams(Player player) {
        if (player == null) return;

        Scoreboard sb = player.getScoreboard();
        for (Team t : sb.getTeams()) {
            if (t.getName().startsWith("legendarys_")) {
                t.removeEntry(player.getName());
            }
        }
    }

    private Set<String> getPresentLegendaryIds(Player player) {
        Set<String> ids = ConcurrentHashMap.newKeySet();

        for (ItemStack it : player.getInventory().getContents()) {
            String id = legendaryService.getLegendaryId(it);
            if (id != null && !id.isEmpty()) ids.add(id);
        }

        String off = legendaryService.getLegendaryId(player.getInventory().getItemInOffHand());
        if (off != null && !off.isEmpty()) ids.add(off);

        return ids;
    }
}