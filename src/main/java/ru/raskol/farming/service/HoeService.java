package ru.raskol.farming.service;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import ru.raskol.farming.RaskolFarming;
import ru.raskol.farming.model.HoeTier;

import java.util.ArrayList;
import java.util.List;

public final class HoeService {

    private final RaskolFarming plugin;
    public final NamespacedKey hoeTierKey;
    public final NamespacedKey hoeFragmentKey;
    public final NamespacedKey goldenWheatKey;

    public HoeService(RaskolFarming plugin) {
        this.plugin = plugin;
        this.hoeTierKey = new NamespacedKey(plugin, "hoe_tier");
        this.hoeFragmentKey = new NamespacedKey(plugin, "hoe_fragment");
        this.goldenWheatKey = new NamespacedKey(plugin, "golden_wheat");
    }

    public boolean isHoe(ItemStack item) {
        return item != null && item.getType().name().endsWith("_HOE");
    }

    public HoeTier tierOf(ItemStack item) {
        if (!isHoe(item)) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return HoeTier.VANILLA;
        String tag = meta.getPersistentDataContainer().get(hoeTierKey, PersistentDataType.STRING);
        if (tag == null) return HoeTier.VANILLA;
        try { return HoeTier.valueOf(tag); }
        catch (IllegalArgumentException e) { return HoeTier.VANILLA; }
    }

    public boolean isCustomHoe(ItemStack item) {
        HoeTier tier = tierOf(item);
        return tier != null && tier != HoeTier.VANILLA;
    }

    public ItemStack createHoe(HoeTier tier) {
        Material base = switch (tier) {
            case GRANDPA -> Material.IRON_HOE;
            case EXPERIENCED -> Material.DIAMOND_HOE;
            case DULKLASS -> Material.NETHERITE_HOE;
            default -> Material.IRON_HOE;
        };
        ItemStack item = new ItemStack(base);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(tier.displayName);
        List<String> lore = new ArrayList<>();
        lore.add(tier.color + "Инструмент фермера уровня " + tier.name());
        if (tier == HoeTier.DULKLASS) {
            lore.add("§7Легендарная мотыга хранителя полей");
        }
        if (tier != HoeTier.VANILLA) {
            lore.add("§8Неломаемая");
            meta.setUnbreakable(true);
            meta.getPersistentDataContainer().set(hoeTierKey, PersistentDataType.STRING, tier.name());
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createFragment(HoeTier tier, int amount) {
        if (tier == HoeTier.VANILLA) return null;
        ItemStack item = new ItemStack(Material.PUMPKIN_SEEDS, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(tier.fragmentDisplayName);
        List<String> lore = new ArrayList<>();
        lore.add("§7Собери §e3 §7фрагмента для крафта:");
        lore.add("§7" + tier.displayName);
        lore.add("§8Шанс успеха крафта: 25%");
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(hoeFragmentKey, PersistentDataType.STRING, tier.name());
        item.setItemMeta(meta);
        return item;
    }

    public HoeTier fragmentTierOf(ItemStack item) {
        if (item == null || item.getType() != Material.PUMPKIN_SEEDS) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        String tag = meta.getPersistentDataContainer().get(hoeFragmentKey, PersistentDataType.STRING);
        if (tag == null) return null;
        try { return HoeTier.valueOf(tag); }
        catch (IllegalArgumentException e) { return null; }
    }

    public ItemStack createGoldenWheat(int amount) {
        ItemStack item = new ItemStack(Material.WHEAT, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName("§6✦ Золотая пшеница ✦");
        List<String> lore = new ArrayList<>();
        lore.add("§eРедчайший урожай");
        lore.add("§7Материал легендарных крафтов");
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(goldenWheatKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isGoldenWheat(ItemStack item) {
        if (item == null || item.getType() != Material.WHEAT) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(goldenWheatKey, PersistentDataType.BYTE);
    }
}
