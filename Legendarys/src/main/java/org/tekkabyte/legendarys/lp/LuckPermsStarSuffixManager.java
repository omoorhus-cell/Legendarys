package org.tekkabyte.legendarys.lp;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.SuffixNode;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.tekkabyte.legendarys.LegendarysPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LuckPermsStarSuffixManager {
    private final LegendarysPlugin plugin;
    private final LuckPerms luckPerms;
    private final Map<UUID, Applied> applied = new ConcurrentHashMap<>();

    public LuckPermsStarSuffixManager(LegendarysPlugin plugin) {
        this.plugin = plugin;
        this.luckPerms = LuckPermsProvider.get();
    }

    public void shutdown() {
        applied.clear();
    }

    public void update(Player player, Set<String> presentLegendaryIds) {
        if (player == null) return;

        boolean enabled = plugin.getConfig().getBoolean("legendary-star.enabled", true);
        if (!enabled) presentLegendaryIds = Collections.emptySet();

        final UUID uuid = player.getUniqueId();
        final int lpPriority = plugin.getConfig().getInt("legendary-star.lp-priority", 10000);

        final String value = buildColoredStars(presentLegendaryIds);
        final boolean shouldApply = value != null && !value.isEmpty();

        Applied last = applied.get(uuid);
        Applied next = shouldApply ? new Applied(value, lpPriority) : null;

        if (equalsApplied(last, next)) return;

        if (next == null) applied.remove(uuid);
        else applied.put(uuid, next);

        if (shouldApply) {
            apply(uuid, value, lpPriority, last);
        } else {
            remove(uuid, last);
        }
    }

    private boolean equalsApplied(Applied a, Applied b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.priority == b.priority && a.value.equals(b.value);
    }

    private String buildColoredStars(Set<String> presentIds) {
        if (presentIds == null || presentIds.isEmpty()) return "";

        List<String> ordered = new ArrayList<>();
        List<String> prio = plugin.getConfig().getStringList("legendary-star.match-priority");

        Set<String> remaining = new HashSet<>(presentIds);

        if (prio != null && !prio.isEmpty()) {
            for (String id : prio) {
                if (id == null) continue;
                if (remaining.remove(id)) ordered.add(id);
            }
        }

        List<String> rest = new ArrayList<>(remaining);
        rest.sort(String.CASE_INSENSITIVE_ORDER);
        ordered.addAll(rest);

        String token = plugin.getConfig().getString("legendary-star.token", "★");
        String separator = plugin.getConfig().getString("legendary-star.separator", "");
        if (token == null || token.isEmpty()) token = "★";
        if (separator == null) separator = "";

        String defaultColor = plugin.getConfig().getString("legendary-star.colors.default", "&6");
        if (defaultColor == null || defaultColor.isEmpty()) defaultColor = "&6";

        StringBuilder sb = new StringBuilder();

        String leading = plugin.getConfig().getString("legendary-star.leading", " ");
        if (leading == null) leading = " ";
        sb.append(leading);

        for (int i = 0; i < ordered.size(); i++) {
            String id = ordered.get(i);

            String color = plugin.getConfig().getString("legendary-star.colors." + id);
            if (color == null || color.isEmpty()) color = defaultColor;

            sb.append(color).append(token);

            if (i + 1 < ordered.size()) sb.append(separator);
        }

        String trailing = plugin.getConfig().getString("legendary-star.trailing", "");
        if (trailing == null) trailing = "";
        sb.append(trailing);

        return ChatColor.translateAlternateColorCodes('&', sb.toString());
    }

    private void apply(UUID uuid, String value, int priority, Applied old) {
        luckPerms.getUserManager().loadUser(uuid).thenAccept(user -> {
            boolean changed = false;

            if (old != null && !(old.value.equals(value) && old.priority == priority)) {
                changed |= removeNode(user, old.value, old.priority);
            }

            changed |= ensureNode(user, value, priority);

            if (changed) luckPerms.getUserManager().saveUser(user);
        });
    }

    private void remove(UUID uuid, Applied old) {
        if (old == null) return;

        luckPerms.getUserManager().loadUser(uuid).thenAccept(user -> {
            boolean changed = removeNode(user, old.value, old.priority);
            if (changed) luckPerms.getUserManager().saveUser(user);
        });
    }

    private boolean ensureNode(User user, String value, int priority) {
        for (Node n : user.data().toCollection()) {
            if (n instanceof SuffixNode sn
                    && value.equals(sn.getMetaValue())
                    && sn.getPriority() == priority) {
                return false;
            }
        }
        user.data().add(SuffixNode.builder(value, priority).build());
        return true;
    }

    private boolean removeNode(User user, String value, int priority) {
        boolean changed = false;
        for (Node n : user.data().toCollection()) {
            if (n instanceof SuffixNode sn
                    && value.equals(sn.getMetaValue())
                    && sn.getPriority() == priority) {
                user.data().remove(n);
                changed = true;
            }
        }
        return changed;
    }

    private static final class Applied {
        final String value;
        final int priority;

        Applied(String value, int priority) {
            this.value = value;
            this.priority = priority;
        }
    }
}