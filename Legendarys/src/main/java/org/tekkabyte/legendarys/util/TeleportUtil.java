package org.tekkabyte.legendarys.util;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public final class TeleportUtil {

    private TeleportUtil() {}

    public static void teleportRetainingRideStack(JavaPlugin plugin, Player player, Location dest) {
        teleportRetainingRideStack(plugin, player, dest, null);
    }

    public static void teleportRetainingRideStack(JavaPlugin plugin, Player player, Location dest, Vector carryVelocity) {
        if (plugin == null || player == null || dest == null) return;

        Entity root = getTopVehicle(player);
        RideNode tree = captureRideTree(root);

        detachRideTree(tree);
        root.teleport(dest);

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            reattachRideTree(tree);

            applyVelocities(tree);

            if (carryVelocity != null && player.isValid()) {
                player.setVelocity(carryVelocity);
            }
        });
    }

    private static Entity getTopVehicle(Entity e) {
        Entity cur = e;
        while (cur.getVehicle() != null) cur = cur.getVehicle();
        return cur;
    }

    private static RideNode captureRideTree(Entity root) {
        RideNode node = new RideNode(root, root.getVelocity());
        for (Entity passenger : new ArrayList<>(root.getPassengers())) {
            node.passengers.add(captureRideTree(passenger));
        }
        return node;
    }

    private static void detachRideTree(RideNode node) {
        for (RideNode child : node.passengers) detachRideTree(child);
        node.entity.eject();
    }

    private static void reattachRideTree(RideNode node) {
        for (RideNode child : node.passengers) {
            if (!node.entity.isValid() || !child.entity.isValid()) continue;
            node.entity.addPassenger(child.entity);
            reattachRideTree(child);
        }
    }

    private static void applyVelocities(RideNode node) {
        if (node.entity.isValid()) node.entity.setVelocity(node.velocity);
        for (RideNode child : node.passengers) applyVelocities(child);
    }

    private static final class RideNode {
        private final Entity entity;
        private final Vector velocity;
        private final List<RideNode> passengers = new ArrayList<>();
        private RideNode(Entity entity, Vector velocity) {
            this.entity = entity;
            this.velocity = velocity;
        }
    }
}