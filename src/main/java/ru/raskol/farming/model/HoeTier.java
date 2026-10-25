package ru.raskol.farming.model;

public enum HoeTier {
    VANILLA("§7Обычная мотыга", "§7", "GRANDPA", null, null),
    GRANDPA("§eМотыга деда", "§e", "EXPERIENCED", "GRANDPA", "§eФрагмент мотыги деда"),
    EXPERIENCED("§bМотыга опытного фермера", "§b", "DULKLASS", "EXPERIENCED", "§bФрагмент мотыги опытного фермера"),
    DULKLASS("§5Мотыга Дулкласа", "§5", null, "DULKLASS", "§5Фрагмент мотыги Дулкласа");

    public final String displayName;
    public final String color;
    public final String fragmentDisplayName;
    private final String dropsFragmentName;
    private final String craftFragmentName;

    HoeTier(String displayName, String color, String dropsFragmentName,
            String craftFragmentName, String fragmentDisplayName) {
        this.displayName = displayName;
        this.color = color;
        this.dropsFragmentName = dropsFragmentName;
        this.craftFragmentName = craftFragmentName;
        this.fragmentDisplayName = fragmentDisplayName;
    }

    public static HoeTier fromName(String name) {
        if (name == null) return VANILLA;
        try { return HoeTier.valueOf(name); }
        catch (IllegalArgumentException e) { return VANILLA; }
    }

    public HoeTier dropsFragment() {
        return dropsFragmentName == null ? null : valueOf(dropsFragmentName);
    }

    public HoeTier craftFragment() {
        return craftFragmentName == null ? null : valueOf(craftFragmentName);
    }
}
