package ru.raskol.farming.service;

import org.bukkit.Material;
import ru.raskol.farming.RaskolFarming;

import java.util.EnumSet;
import java.util.Set;

/**
 * Кеш whitelist'а блоков, которые можно ломать в зоне фермы.
 * Итоговый whitelist = crops ∪ farm.allowed-extra.
 */
public final class FarmZoneGuardService {

    private final RaskolFarming plugin;
    private Set<Material> allowed = EnumSet.noneOf(Material.class);

    public FarmZoneGuardService(RaskolFarming plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        allowed = EnumSet.noneOf(Material.class);
        for (String name : plugin.getConfig().getStringList("crops")) {
            Material m = Material.matchMaterial(name);
            if (m != null) allowed.add(m);
        }
        for (String name : plugin.getConfig().getStringList("farm.allowed-extra")) {
            Material m = Material.matchMaterial(name);
            if (m != null) allowed.add(m);
        }
        plugin.getLogger().info("[Farming] whitelist зоны фермы: " + allowed.size() + " блоков");
    }

    public boolean isAllowed(Material m) { return allowed.contains(m); }
}
