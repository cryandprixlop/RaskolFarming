package ru.raskol.farming.hook;

import org.bukkit.entity.Player;
import ru.raskol.farming.RaskolFarming;
import ru.raskol.farming.model.Region;

public final class SelectionHook {

    private final RaskolFarming plugin;

    public SelectionHook(RaskolFarming plugin) { this.plugin = plugin; }

    public boolean isAvailable() {
        return plugin.getServer().getPluginManager().getPlugin("WorldEdit") != null;
    }

    public Region getSelection(Player player) {
        try {
            Object wep = plugin.getServer().getPluginManager().getPlugin("WorldEdit");
            if (wep == null) return null;
            Object session = wep.getClass().getMethod("getSession", Player.class).invoke(wep, player);
            if (session == null) return null;
            Object weWorld = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter")
                    .getMethod("adapt", org.bukkit.World.class).invoke(null, player.getWorld());
            Object region = session.getClass()
                    .getMethod("getSelection", Class.forName("com.sk89q.worldedit.world.World"))
                    .invoke(session, weWorld);
            if (region == null) return null;
            Object min = region.getClass().getMethod("getMinimumPoint").invoke(region);
            Object max = region.getClass().getMethod("getMaximumPoint").invoke(region);
            Region r = new Region();
            r.world = player.getWorld().getName();
            r.x1 = (Integer) min.getClass().getMethod("getX").invoke(min);
            r.y1 = (Integer) min.getClass().getMethod("getY").invoke(min);
            r.z1 = (Integer) min.getClass().getMethod("getZ").invoke(min);
            r.x2 = (Integer) max.getClass().getMethod("getX").invoke(max);
            r.y2 = (Integer) max.getClass().getMethod("getY").invoke(max);
            r.z2 = (Integer) max.getClass().getMethod("getZ").invoke(max);
            return r;
        } catch (Throwable t) {
            return null;
        }
    }
}
