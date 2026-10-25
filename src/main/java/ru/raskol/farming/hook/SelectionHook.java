package ru.raskol.farming.hook;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import org.bukkit.entity.Player;
import ru.raskol.farming.RaskolFarming;

public final class SelectionHook {
    private final RaskolFarming plugin;
    public SelectionHook(RaskolFarming plugin) { this.plugin = plugin; }

    public boolean isAvailable() {
        return plugin.getServer().getPluginManager().getPlugin("WorldEdit") != null;
    }

    public ru.raskol.farming.model.Region getSelection(Player player) {
        try {
            Class<?> c = Class.forName("com.sk89q.worldedit.WorldEdit");
            Object we = c.getMethod("getInstance").invoke(null);
            Object session = we.getClass()
                    .getMethod("getSessionManager")
                    .invoke(we)
                    .getClass()
                    .getMethod("get", com.sk89q.worldedit.entity.Player.class)
                    .invoke(we.getClass().getMethod("getSessionManager").invoke(we),
                            BukkitAdapter.adapt(player));
            Object sel = session.getClass()
                    .getMethod("getSelection", World.class)
                    .invoke(session, (Object) null);
            if (sel == null) return null;
            BlockVector3 min = ((Region) sel).getMinimumPoint();
            BlockVector3 max = ((Region) sel).getMaximumPoint();
            ru.raskol.farming.model.Region r = new ru.raskol.farming.model.Region();
            r.world = player.getWorld().getName();
            r.x1 = min.x(); r.y1 = min.y(); r.z1 = min.z();
            r.x2 = max.x(); r.y2 = max.y(); r.z2 = max.z();
            return r;
        } catch (Throwable t) {
            return null;
        }
    }
}
