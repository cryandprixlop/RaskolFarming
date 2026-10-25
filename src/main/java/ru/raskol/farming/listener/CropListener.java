package ru.raskol.farming.listener;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import ru.raskol.farming.RaskolFarming;
import ru.raskol.farming.model.FarmZone;
import ru.raskol.farming.model.HoeTier;
import ru.raskol.farming.model.PendingCrop;
import ru.raskol.farming.service.HoeService;
import ru.raskol.farming.store.FarmStore;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Сбор урожая:
 * 1) в зоне фермы — блок встаёт в очередь регенерации (interval-minutes);
 * 2) рулетка лута мотыги: золотая пшеница -> фрагмент -> множитель дропа.
 * Одна рулетка на игрока в окне 2 сек.
 */
public final class CropListener implements Listener {

    private static final long ROLL_WINDOW_MS = 2000;

    private static final Map<String, Double> DEFAULTS = Map.ofEntries(
            Map.entry("vanilla.golden-chance", 0.001),
            Map.entry("vanilla.next-fragment-chance", 5.0),
            Map.entry("vanilla.double-chance", 2.0),
            Map.entry("grandpa.golden-chance", 0.002),
            Map.entry("grandpa.next-fragment-chance", 4.0),
            Map.entry("grandpa.double-chance", 4.0),
            Map.entry("experienced.golden-chance", 0.01),
            Map.entry("experienced.next-fragment-chance", 4.0),
            Map.entry("experienced.double-chance", 20.0),
            Map.entry("dulklass.golden-chance", 0.5),
            Map.entry("dulklass.double-chance", 35.0),
            Map.entry("dulklass.triple-chance", 15.0),
            Map.entry("dulklass.quad-chance", 10.0)
    );

    private final RaskolFarming plugin;
    private final FarmStore store;
    private final HoeService hoeService;
    private final Map<UUID, Long> lastRoll = new HashMap<>();

    public CropListener(RaskolFarming plugin, FarmStore store, HoeService hoeService) {
        this.plugin = plugin;
        this.store = store;
        this.hoeService = hoeService;
        if (plugin.getConfig().getConfigurationSection("loot") == null) {
            plugin.getLogger().warning("[Farming] В серверном config.yml нет секции 'loot'! "
                    + "Удали plugins/RaskolFarming/config.yml и перезапусти. Используются дефолты.");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCropBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        String mat = block.getType().name();
        if (!plugin.getConfig().getStringList("crops").contains(mat)) return;

        Player player = event.getPlayer();

        // 1) Очередь регенерации — только в зоне фермы
        FarmZone zone = store.zoneAt(block.getLocation());
        if (zone != null) {
            PendingCrop p = new PendingCrop();
            p.world = block.getWorld().getName();
            p.x = block.getX();
            p.y = block.getY();
            p.z = block.getZ();
            p.material = mat;
            p.restoreAtEpochSeconds = System.currentTimeMillis() / 1000L
                    + (long) (plugin.getConfig().getDouble("regen.interval-minutes", 15.0) * 60.0);
            store.addPending(p);
        }

        // 2) Рулетка лута (мотыгой, одна на игрока в окне 2 сек)
        ItemStack tool = player.getInventory().getItemInMainHand();
        HoeTier tier = hoeService.tierOf(tool);
        if (tier == null) return;

        long now = System.currentTimeMillis();
        Long prev = lastRoll.get(player.getUniqueId());
        if (prev != null && now - prev < ROLL_WINDOW_MS) return;
        lastRoll.put(player.getUniqueId(), now);

        rollLoot(player, tier, block, tool);
    }

    private void rollLoot(Player player, HoeTier tier, Block block, ItemStack tool) {
        String section = sectionOf(tier);
        double roll = Math.random() * 100.0;
        double acc = 0.0;

        double golden = chance(section, "golden-chance");
        acc += golden;
        if (roll < acc) {
            give(player, hoeService.createGoldenWheat(1));
            player.sendMessage("§6✦ Редчайший урожай: §eЗолотая пшеница§6!");
            plugin.getLogger().info("[Farming] " + player.getName()
                    + " собрал золотую пшеницу (" + tier.name() + ")");
            return;
        }

        double fragChance = chance(section, "next-fragment-chance");
        acc += fragChance;
        if (roll < acc) {
            HoeTier fragTier = tier.dropsFragment();
            if (fragTier != null) {
                give(player, hoeService.createFragment(fragTier, 1));
                player.sendMessage("§7⚙ Найден " + fragTier.fragmentDisplayName + "§7!");
                plugin.getLogger().info("[Farming] " + player.getName()
                        + " нашёл фрагмент " + fragTier.name());
            }
            return;
        }

        double quad = chance(section, "quad-chance");
        double triple = chance(section, "triple-chance");
        double dbl = chance(section, "double-chance");
        double mRoll = Math.random() * 100.0;
        int multiplier = 1;
        if (mRoll < quad) multiplier = 4;
        else if (mRoll < quad + triple) multiplier = 3;
        else if (mRoll < quad + triple + dbl) multiplier = 2;

        if (multiplier > 1) {
            for (ItemStack drop : block.getDrops(tool)) {
                ItemStack extra = drop.clone();
                extra.setAmount(drop.getAmount() * (multiplier - 1));
                block.getWorld().dropItemNaturally(block.getLocation(), extra);
            }
            player.sendMessage("§e✦ Богатый урожай! §7×" + multiplier + " дропа");
            plugin.getLogger().info("[Farming] " + player.getName()
                    + ": множитель ×" + multiplier + " (" + tier.name() + ")");
        }
    }

    private double chance(String section, String key) {
        return plugin.getConfig().getDouble("loot." + section + "." + key,
                DEFAULTS.getOrDefault(section + "." + key, 0.0));
    }

    private String sectionOf(HoeTier tier) {
        return switch (tier) {
            case VANILLA -> "vanilla";
            case GRANDPA -> "grandpa";
            case EXPERIENCED -> "experienced";
            case DULKLASS -> "dulklass";
        };
    }

    private void give(Player player, ItemStack item) {
        if (item == null) return;
        for (ItemStack drop : player.getInventory().addItem(item).values()) {
            player.getWorld().dropItem(player.getLocation(), drop);
        }
    }
}
