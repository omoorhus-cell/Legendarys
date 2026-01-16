package org.tekkabyte.legendarys.listener;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.tekkabyte.legendarys.LegendarysPlugin;
import org.tekkabyte.legendarys.glow.GlowManager;
import org.tekkabyte.legendarys.legend.LegendaryService;

import java.util.*;

public class LegendaryProtectionListener implements Listener {

    private final LegendarysPlugin plugin;
    private final LegendaryService legendaryService;
    private final GlowManager glowManager;

    private final MiniMessage mm = MiniMessage.miniMessage();
    private final Map<UUID, Long> msgCooldown = new HashMap<>();
    private final long cooldownMs;

    public LegendaryProtectionListener(LegendarysPlugin plugin,
                                       LegendaryService legendaryService,
                                       GlowManager glowManager) {
        this.plugin = plugin;
        this.legendaryService = legendaryService;
        this.glowManager = glowManager;
        this.cooldownMs = plugin.getConfig().getLong("messages.cooldown-ms", 900L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDrop(PlayerDropItemEvent e) {
        glowManager.requestUpdate(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent e) {
        Bukkit.getScheduler().runTask(plugin, () -> glowManager.requestUpdate(e.getEntity()));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;

        InventoryView view = e.getView();
        Inventory top = view.getTopInventory();
        Inventory clickedInv = e.getClickedInventory();
        switch (e.getAction()) {
            case DROP_ALL_CURSOR, DROP_ONE_CURSOR, DROP_ALL_SLOT, DROP_ONE_SLOT -> {
                Bukkit.getScheduler().runTask(plugin, () -> glowManager.requestUpdate(player));
                return;
            }
            default -> {}
        }

        boolean topRestricted = isRestrictedTopInventory(top, view);

        ItemStack cursor = e.getCursor();
        ItemStack current = e.getCurrentItem();
        if (wouldPlaceLegendaryIntoBundle(e)) {
            deny(player, e);
            return;
        }

        if (topRestricted) {
            if (wouldPutLegendaryIntoTop(e, player)) {
                deny(player, e);
                return;
            }
        }

        if (wouldPutLegendaryIntoNonPlayerInventory(e, player)) {
            deny(player, e);
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> glowManager.requestUpdate(player));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;

        InventoryView view = e.getView();
        Inventory top = view.getTopInventory();

        ItemStack cursor = e.getOldCursor();
        if (!legendaryService.isLegendary(cursor)) {
            return;
        }

        int topSize = top.getSize();
        boolean touchesTop = e.getRawSlots().stream().anyMatch(raw -> raw < topSize);
        if (touchesTop && isRestrictedTopInventory(top, view)) {
            deny(player, e);
            return;
        }

        if (e.getRawSlots().stream().anyMatch(raw -> {
            Inventory dest = (raw < topSize) ? top : view.getBottomInventory();
            return !isPlayerOwnedInventory(dest, player, view);
        })) {
            deny(player, e);
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> glowManager.requestUpdate(player));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryMove(InventoryMoveItemEvent e) {
        if (!legendaryService.isLegendary(e.getItem())) return;
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryPickup(InventoryPickupItemEvent e) {
        if (!legendaryService.isLegendary(e.getItem().getItemStack())) return;
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        glowManager.requestUpdate(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        glowManager.removeFromTeams(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPickup(PlayerAttemptPickupItemEvent e) {
        Bukkit.getScheduler().runTask(plugin, () -> glowManager.requestUpdate(e.getPlayer()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemHeld(PlayerItemHeldEvent e) {
        glowManager.requestUpdate(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent e) {
        if (!(e.getRightClicked() instanceof ItemFrame) && !(e.getRightClicked() instanceof ArmorStand)) return;

        ItemStack hand = e.getPlayer().getInventory().getItemInMainHand();
        ItemStack off = e.getPlayer().getInventory().getItemInOffHand();

        if (legendaryService.isLegendary(hand) || legendaryService.isLegendary(off)) {
            e.setCancelled(true);
            deny(e.getPlayer(), null);
        }
    }

    private boolean isMightVaultsVaultTop(Inventory top) {
        InventoryHolder holder = top.getHolder();
        return holder != null && holder.getClass().getName().equals("org.tekkabyte.mightVaults.gui.VaultGUI");
    }

    private boolean isRestrictedTopInventory(Inventory top, InventoryView view) {
        if (top.getType() == InventoryType.ENDER_CHEST) return true;
        if (isMightVaultsVaultTop(top)) return true;
        if (!(top.getHolder() instanceof Player)) return true;

        return false;
    }

    private boolean isPlayerOwnedInventory(Inventory inv, Player player, InventoryView view) {
        if (inv == null) return false;
        if (inv.equals(view.getBottomInventory())) return true;
        return inv.getHolder() instanceof Player && ((Player) inv.getHolder()).getUniqueId().equals(player.getUniqueId());
    }

    private boolean isBundle(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        return item.getType() == Material.BUNDLE;
    }

    private boolean wouldPlaceLegendaryIntoBundle(InventoryClickEvent e) {
        ItemStack cursor = e.getCursor();
        ItemStack current = e.getCurrentItem();
        if (legendaryService.isLegendary(cursor) && isBundle(current)) {
            switch (e.getAction()) {
                case PLACE_ALL, PLACE_ONE, PLACE_SOME, SWAP_WITH_CURSOR -> { return true; }
                default -> {}
            }
        }
        return false;
    }

    private boolean wouldPutLegendaryIntoTop(InventoryClickEvent e, Player player) {
        InventoryView view = e.getView();
        Inventory top = view.getTopInventory();
        Inventory clicked = e.getClickedInventory();
        ItemStack cursor = e.getCursor();
        ItemStack current = e.getCurrentItem();
        if (clicked != null && clicked.equals(top)) {
            if (legendaryService.isLegendary(cursor)) {
                switch (e.getAction()) {
                    case PLACE_ALL, PLACE_ONE, PLACE_SOME, SWAP_WITH_CURSOR -> { return true; }
                    default -> {}
                }
            }
            if (e.getClick() == ClickType.NUMBER_KEY) {
                int btn = e.getHotbarButton();
                if (btn >= 0) {
                    ItemStack hotbar = player.getInventory().getItem(btn);
                    if (legendaryService.isLegendary(hotbar)) return true;
                }
            }
            if (e.getClick() == ClickType.SWAP_OFFHAND) {
                ItemStack off = player.getInventory().getItemInOffHand();
                if (legendaryService.isLegendary(off)) return true;
            }
        }
        if (e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            if (clicked != null && clicked.equals(view.getBottomInventory())) {
                if (legendaryService.isLegendary(current)) return true;
            }
        }
        return false;
    }

    private boolean wouldPutLegendaryIntoNonPlayerInventory(InventoryClickEvent e, Player player) {
        InventoryView view = e.getView();
        Inventory top = view.getTopInventory();
        Inventory bottom = view.getBottomInventory();
        Inventory clicked = e.getClickedInventory();

        ItemStack cursor = e.getCursor();
        ItemStack current = e.getCurrentItem();
        if (clicked != null && !isPlayerOwnedInventory(clicked, player, view)) {
            if (legendaryService.isLegendary(cursor)) {
                switch (e.getAction()) {
                    case PLACE_ALL, PLACE_ONE, PLACE_SOME, SWAP_WITH_CURSOR -> { return true; }
                    default -> {}
                }
            }
            if (e.getClick() == ClickType.NUMBER_KEY) {
                int btn = e.getHotbarButton();
                if (btn >= 0) {
                    ItemStack hotbar = player.getInventory().getItem(btn);
                    if (legendaryService.isLegendary(hotbar)) return true;
                }
            }
            if (e.getClick() == ClickType.SWAP_OFFHAND) {
                ItemStack off = player.getInventory().getItemInOffHand();
                if (legendaryService.isLegendary(off)) return true;
            }
        }
        if (e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            if (legendaryService.isLegendary(current)) {
                if (clicked != null && clicked.equals(bottom) && !isPlayerOwnedInventory(top, player, view)) return true;
            }
        }
        return false;
    }

    private void deny(Player player, Cancellable event) {
        if (event != null) event.setCancelled(true);
        deny(player);
    }

    private void deny(Player player) {
        long now = System.currentTimeMillis();
        long last = msgCooldown.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < cooldownMs) return;
        msgCooldown.put(player.getUniqueId(), now);

        String msg = plugin.getConfig().getString(
                "messages.blocked-actionbar",
                "<red>You can't store Legendary items. Drop them or die.</red>"
        );
        try {
            if (msg == null) msg = "";
            player.sendActionBar(mm.deserialize(msg));
        } catch (Throwable t) {
            try {
                player.sendActionBar(net.kyori.adventure.text.Component.text("You can't store Legendary items."));
            } catch (Throwable ignored) {
            }
        }
    }
}
