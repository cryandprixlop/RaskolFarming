package ru.raskol.farming.task;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import ru.raskol.farming.RaskolFarming;
import ru.raskol.farming.model.PendingCrop;
import ru.raskol.farming.store.FarmStore;

import java.util.ArrayList;
import java.util.List;

/** Раз в минуту возвращает собранный урожай (сразу спелым) в зонах фермы. */
public final class CropRegenTask implements Runnable {

    private final RaskolFarming plugin;
    private final FarmStore store;

    public CropRegenTask(RaskolFarming plugin, FarmStore store) {
        this.plugin = plugin;
        this.store = store;
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis() / 1000L;
        List<PendingCrop> done = new ArrayList<>();
        int restored = 0;

        for (PendingCrop p : store.pending()) {
            if (p.restoreAtEpochSeconds > now) continue;

            World w = Bukkit.getWorld(p.world);
            if (w == null) continue;

            Block b = w.getBlockAt(p.x, p.y, p.z);
            Material current = b.getType();
            if (current != Material.AIR && current != Material.CAVE_AIR) continue; // место занято — ретрай

            Material crop = Material.matchMaterial(p.material);
            if (crop == null) { done.add(p); continue; }

            b.setType(crop, false);
            BlockData bd = b.getBlockData();
            if (bd instanceof Ageable ag) {
                ag.setAge(ag.getMaximumAge());
                b.setBlockData(ag);
            }
            // Многосегментные: тростник/кактус/бамбук — достраиваем стебель
            if (crop == Material.SUGAR_CANE || crop == Material.CACTUS || crop == Material.BAMBOO) {
                int height = (crop == Material.BAMBOO) ? 5 : 3;
                for (int i = 1; i < height; i++) {
                    Block above = w.getBlockAt(p.x, p.y + i, p.z);
                    if (above.getType() == Material.AIR) above.setType(crop, false);
                    else break;
                }
            }
            restored++;
            done.add(p);
        }

        if (!done.isEmpty()) {
            store.pending().removeAll(done);
            store.save();
            if (restored > 0) {
                plugin.getLogger().info("[Farming] регенерация: восстановлено урожая: " + restored
                        + ", в очереди осталось: " + store.pending().size());
            }
        }
    }
}
