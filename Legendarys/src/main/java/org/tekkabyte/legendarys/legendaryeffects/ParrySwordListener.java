package org.tekkabyte.legendarys.legendaryeffects;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.UUID;

public class ParrySwordListener implements Listener {

    private static final String LEGENDARY_ID = "parry-sword";

    private final Plugin plugin;
    private final org.tekkabyte.legendarys.legend.LegendaryService legendaryService;

    private final HashMap<UUID, Long> parryStates = new HashMap<>();
    private final HashMap<UUID, Long> parryCooldowns = new HashMap<>();

    public ParrySwordListener(Plugin plugin, org.tekkabyte.legendarys.legend.LegendaryService legendaryService) {
        this.plugin = plugin;
        this.legendaryService = legendaryService;
    }

    private boolean isParrySword(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        if (!legendaryService.isLegendary(item)) return false;
        return LEGENDARY_ID.equalsIgnoreCase(legendaryService.getLegendaryId(item));
    }

    private long cooldownMs() {
        return plugin.getConfig().getLong("legendaries.parry-sword.cooldown-ms", 4000L);
    }

    private double meleeWindowSec() {
        return plugin.getConfig().getDouble("legendaries.parry-sword.melee-window-sec", 0.12);
    }

    private double projectileWindowSec() {
        return plugin.getConfig().getDouble("legendaries.parry-sword.projectile-window-sec", 0.9);
    }

    private double reflectMultiplier() {
        return plugin.getConfig().getDouble("legendaries.parry-sword.reflect-multiplier", 1.5);
    }

    private double soundRadius() {
        return plugin.getConfig().getDouble("legendaries.parry-sword.sound-radius", 18.0);
    }

    private double kbStrength() {
        return plugin.getConfig().getDouble("legendaries.parry-sword.knockback.strength", 1.1);
    }

    private double kbY() {
        return plugin.getConfig().getDouble("legendaries.parry-sword.knockback.y", 0.25);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Action a = event.getAction();
        if (a != Action.RIGHT_CLICK_AIR && a != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (!isParrySword(item)) return;

        long now = System.currentTimeMillis();
        long cd = cooldownMs();

        Long last = parryCooldowns.get(player.getUniqueId());
        if (last != null && (last + cd) > now) return;

        parryStates.put(player.getUniqueId(), now);
        parryCooldowns.put(player.getUniqueId(), now);

        long ticks = Math.max(1L, cd / 50L);
        player.setCooldown(item.getType(), (int) ticks);

        long removeTicks = Math.max(1L, (long) Math.ceil(projectileWindowSec() * 20.0));
        new BukkitRunnable() {
            @Override
            public void run() {
                parryStates.remove(player.getUniqueId());
            }
        }.runTaskLater(plugin, removeTicks);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!isParrySword(hand)) return;

        Long armedAt = parryStates.get(player.getUniqueId());
        if (armedAt == null) return;

        Entity damager = event.getDamager();

        boolean projectileHit = damager instanceof Projectile;
        double windowSec = projectileHit ? projectileWindowSec() : meleeWindowSec();

        long parryTimeMs = System.currentTimeMillis() - armedAt;
        if (parryTimeMs > (long) (windowSec * 1000.0)) return;

        event.setCancelled(true);
        parryStates.remove(player.getUniqueId());

        doParryFx(player);

        double reflectedDamage = event.getDamage() * reflectMultiplier();

        if (damager instanceof Projectile proj) {
            ProjectileSource src = proj.getShooter();
            proj.remove();

            if (src instanceof LivingEntity shooter) {
                shooter.damage(reflectedDamage, player);
                applyKnockbackFromPlayer(player, shooter);
            }
        } else if (damager instanceof LivingEntity living) {
            living.damage(reflectedDamage, player);
            applyKnockbackFromPlayer(player, living);
        }

        String parryMessage = plugin.getConfig().getString("messages.parry-msg");
        if (parryMessage != null && !parryMessage.isBlank()) {
            player.sendMessage(parryMessage);
        }

        player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_HURT, 1.0f, 1.5f);
    }

    private void doParryFx(Player player) {
        Vector forward = player.getLocation().getDirection().normalize();
        var fxLoc = player.getLocation().clone().add(forward.multiply(0.9)).add(0, 1.0, 0);

        player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, fxLoc, 1, 0, 0, 0, 0);

        double radius = soundRadius();
        double r2 = radius * radius;

        for (Player p : player.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(player.getLocation()) <= r2) {
                p.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.2f, 1.2f);
                p.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.6f, 1.8f);
            }
        }
    }

    private void applyKnockbackFromPlayer(Player player, LivingEntity target) {
        if (target == null || target.isDead()) return;

        double strength = kbStrength();
        double y = kbY();

        Vector dir = target.getLocation().toVector().subtract(player.getLocation().toVector());
        dir.setY(0);

        if (dir.lengthSquared() < 0.0001) {
            dir = player.getLocation().getDirection().clone();
            dir.setY(0);
        }

        dir.normalize().multiply(strength);
        dir.setY(y);

        target.setVelocity(target.getVelocity().clone().add(dir));
    }
}