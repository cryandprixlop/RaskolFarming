package ru.raskol.farming.model;

import org.bukkit.Location;

public final class Region {
    public String world;
    public int x1, y1, z1;
    public int x2, y2, z2;

    public boolean contains(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        if (!loc.getWorld().getName().equals(world)) return false;
        int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
        return x >= Math.min(x1, x2) && x <= Math.max(x1, x2)
                && y >= Math.min(y1, y2) && y <= Math.max(y1, y2)
                && z >= Math.min(z1, z2) && z <= Math.max(z1, z2);
    }

    public boolean contains(Region other) {
        if (!other.world.equals(world)) return false;
        int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);
        int oMinX = Math.min(other.x1, other.x2), oMaxX = Math.max(other.x1, other.x2);
        int oMinY = Math.min(other.y1, other.y2), oMaxY = Math.max(other.y1, other.y2);
        int oMinZ = Math.min(other.z1, other.z2), oMaxZ = Math.max(other.z1, other.z2);
        return oMinX >= minX && oMaxX <= maxX
                && oMinY >= minY && oMaxY <= maxY
                && oMinZ >= minZ && oMaxZ <= maxZ;
    }
}
