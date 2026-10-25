package ru.raskol.farming.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import ru.raskol.farming.RaskolFarming;
import ru.raskol.farming.service.LicenseService;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Без лицензии фермера урожай не собирается (HIGH — раньше лута и регена). */
public final class FarmLicenseListener implements Listener {

    private final RaskolFarming plugin;
    private final LicenseService licenseService;
    private final Map<UUID, Long> lastWarn = new HashMap<>();

    public FarmLicenseListener(RaskolFarming plugin, LicenseService licenseService) {
        this.plugin = plugin;
        this.licenseService = licenseService;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCropBreak(BlockBreakEvent event) {
        String mat = event.getBlock().getType().name();
        if (!plugin.getConfig().getStringList("crops").contains(mat)) return;

        Player player = event.getPlayer();
        if (player.hasPermission("farming.admin")) return;
        if (licenseService.hasValidLicense(player)) return;

        event.setCancelled(true);
        warn(player, "§cДля работы на ферме нужна лицензия фермера! §e/farm license buy");
    }

    private void warn(Player player, String message) {
        long now = System.currentTimeMillis();
        Long prev = lastWarn.get(player.getUniqueId());
        if (prev != null && now - prev < 3000) return;
        lastWarn.put(player.getUniqueId(), now);
        player.sendMessage(message);
    }
}
