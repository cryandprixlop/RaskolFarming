package ru.raskol.farming.store;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.raskol.farming.RaskolFarming;
import ru.raskol.farming.model.FarmZone;
import ru.raskol.farming.model.PendingCrop;
import ru.raskol.farming.model.Region;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FarmStore {

    private final RaskolFarming plugin;
    private final Map<String, FarmZone> zones = new LinkedHashMap<>();
    private final List<PendingCrop> pending = new ArrayList<>();

    public FarmStore(RaskolFarming plugin) { this.plugin = plugin; }

    public void load() {
        zones.clear();
        pending.clear();
        File file = new File(plugin.getDataFolder(), "farming.yml");
        if (!file.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection zs = cfg.getConfigurationSection("zones");
        if (zs != null) {
            for (String name : zs.getKeys(false)) {
                ConfigurationSection s = zs.getConfigurationSection(name);
                if (s == null) continue;
                FarmZone z = new FarmZone();
                z.name = name;
                z.region = readRegion(s.getConfigurationSection("region"));
                if (z.region != null) zones.put(name, z);
            }
        }
        ConfigurationSection ps = cfg.getConfigurationSection("pending");
        if (ps != null) {
            for (String key : ps.getKeys(false)) {
                ConfigurationSection s = ps.getConfigurationSection(key);
                if (s == null) continue;
                PendingCrop p = new PendingCrop();
                p.world = s.getString("world", "world");
                p.x = s.getInt("x");
                p.y = s.getInt("y");
                p.z = s.getInt("z");
                p.material = s.getString("material", "WHEAT");
                p.restoreAtEpochSeconds = s.getLong("restore-at", 0);
                pending.add(p);
            }
        }
    }

    public void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (FarmZone z : zones.values()) {
            ConfigurationSection s = cfg.createSection("zones." + z.name);
            writeRegion(s.createSection("region"), z.region);
        }
        for (int i = 0; i < pending.size(); i++) {
            PendingCrop p = pending.get(i);
            ConfigurationSection s = cfg.createSection("pending." + i);
            s.set("world", p.world);
            s.set("x", p.x);
            s.set("y", p.y);
            s.set("z", p.z);
            s.set("material", p.material);
            s.set("restore-at", p.restoreAtEpochSeconds);
        }
        try {
            cfg.save(new File(plugin.getDataFolder(), "farming.yml"));
        } catch (Exception e) {
            plugin.getLogger().severe("[Farming] не удалось сохранить farming.yml: " + e.getMessage());
        }
    }

    private Region readRegion(ConfigurationSection s) {
        if (s == null) return null;
        Region r = new Region();
        r.world = s.getString("world", "world");
        r.x1 = s.getInt("x1"); r.y1 = s.getInt("y1"); r.z1 = s.getInt("z1");
        r.x2 = s.getInt("x2"); r.y2 = s.getInt("y2"); r.z2 = s.getInt("z2");
        return r;
    }

    private void writeRegion(ConfigurationSection s, Region r) {
        s.set("world", r.world);
        s.set("x1", r.x1); s.set("y1", r.y1); s.set("z1", r.z1);
        s.set("x2", r.x2); s.set("y2", r.y2); s.set("z2", r.z2);
    }

    public void addZone(FarmZone z) { zones.put(z.name, z); save(); }
    public void removeZone(String name) { zones.remove(name); save(); }
    public FarmZone get(String name) { return zones.get(name); }
    public List<FarmZone> all() { return new ArrayList<>(zones.values()); }

    public FarmZone zoneAt(org.bukkit.Location loc) {
        for (FarmZone z : zones.values()) {
            if (z.region != null && z.region.contains(loc)) return z;
        }
        return null;
    }

    public void addPending(PendingCrop p) { pending.add(p); save(); }
    public List<PendingCrop> pending() { return pending; }
}
