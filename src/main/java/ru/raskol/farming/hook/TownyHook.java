package ru.raskol.farming.hook;

import org.bukkit.entity.Player;
import ru.raskol.farming.RaskolFarming;
import java.lang.reflect.Method;

public final class TownyHook {
    private final RaskolFarming plugin;
    public TownyHook(RaskolFarming plugin) { this.plugin = plugin; }

    public boolean isAvailable() {
        return plugin.getServer().getPluginManager().getPlugin("Towny") != null;
    }

    public String residentTownName(Player player) {
        try {
            Class<?> c = Class.forName("com.palmergames.bukkit.towny.TownyUniverse");
            Object u = c.getMethod("getInstance").invoke(null);
            Object r = u.getClass().getMethod("getResident", java.util.UUID.class).invoke(u, player.getUniqueId());
            if (r == null) return null;
            Boolean has = (Boolean) r.getClass().getMethod("hasTown").invoke(r);
            if (has == null || !has) return null;
            Object t = r.getClass().getMethod("getTownOrNull").invoke(r);
            return t == null ? null : (String) t.getClass().getMethod("getName").invoke(t);
        } catch (Throwable t) { return null; }
    }

    public boolean isKing(Player player) {
        try {
            Class<?> c = Class.forName("com.palmergames.bukkit.towny.TownyUniverse");
            Object u = c.getMethod("getInstance").invoke(null);
            Object r = u.getClass().getMethod("getResident", java.util.UUID.class).invoke(u, player.getUniqueId());
            if (r == null) return false;
            Boolean k = (Boolean) r.getClass().getMethod("isKing").invoke(r);
            return k != null && k;
        } catch (Throwable t) { return false; }
    }

    public boolean depositTownBank(String townName, double amount, String reason) {
        try {
            Class<?> c = Class.forName("com.palmergames.bukkit.towny.TownyAPI");
            Object a = c.getMethod("getInstance").invoke(null);
            Object t = c.getMethod("getTown", String.class).invoke(a, townName);
            if (t == null) return false;
            Object acc = t.getClass().getMethod("getAccount").invoke(t);
            if (acc == null) return false;
            Method m = acc.getClass().getMethod("deposit", double.class, String.class);
            Object out = m.invoke(acc, amount, reason);
            return !(out instanceof Boolean b) || b;
        } catch (Throwable t) { return false; }
    }
}
