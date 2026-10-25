package ru.raskol.farming.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemDamageEvent;
import ru.raskol.farming.RaskolFarming;
import ru.raskol.farming.service.HoeService;

public final class HoeDurabilityListener implements Listener {

    private final RaskolFarming plugin;
    private final HoeService hoeService;

    public HoeDurabilityListener(RaskolFarming plugin, HoeService hoeService) {
        this.plugin = plugin;
        this.hoeService = hoeService;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemDamage(PlayerItemDamageEvent event) {
        if (hoeService.isCustomHoe(event.getItem())) {
            event.setCancelled(true);
        }
    }
}
