package ru.raskol.farming.listener;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import ru.raskol.farming.RaskolFarming;
import ru.raskol.farming.model.FarmZone;
import ru.raskol.farming.service.FarmZoneGuardService;
import ru.raskol.farming.store.FarmStore;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Защита зон фермы: в нашей зоне ломаются только блоки из whitelist
 * (crops ∪ farm.allowed-extra). Towny решает остальное (застройка вне зон).
 */
public final class FarmZoneGuardListener implements Listener {

    private final RaskolFarming plugin;
    private final FarmStore store;
    private final FarmZoneGuardService guard;
    private final Map<UUID, Long> lastWarn = new HashMap<>();

    public FarmZoneGuardListener(RaskolFarming plugin, FarmStore store, FarmZoneGuardService guard) {
        this.plugin = plugin;
        this.store = store;
        this.guard = guard;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return; // Towny уже запретил — выходим молча

        Block block = event.getBlock();
        FarmZone zone = store.zoneAt(block.getLocation());
        if (zone == null) return; // не наша зона — Towny рулит

        Player player = event.getPlayer();
        if (player.hasPermission("farming.admin")) return;

        if (guard.isAllowed(block.getType())) return;

        event.setCancelled(true);
        warn(player, "§cВ зоне фермы §e" + zone.name + " §cможно ломать только урожай и почву для посадки.");
    }

    private void warn(Player player, String message) {
        long now = System.currentTimeMillis();
        Long prev = lastWarn.get(player.getUniqueId());
        if (prev != null && now - prev < 3000) return;
        lastWarn.put(player.getUniqueId(), now);
        player.sendMessage(message);
    }
}
