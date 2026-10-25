package ru.raskol.farming;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import ru.raskol.farming.command.FarmCommand;
import ru.raskol.farming.hook.SelectionHook;
import ru.raskol.farming.hook.TownyHook;
import ru.raskol.farming.hook.VaultHook;
import ru.raskol.farming.listener.CropListener;
import ru.raskol.farming.listener.FarmLicenseListener;
import ru.raskol.farming.listener.HoeCraftListener;
import ru.raskol.farming.listener.HoeDurabilityListener;
import ru.raskol.farming.model.HoeTier;
import ru.raskol.farming.service.HoeService;
import ru.raskol.farming.service.LicenseService;
import ru.raskol.farming.store.FarmStore;
import ru.raskol.farming.store.LicenseStore;
import ru.raskol.farming.task.CropRegenTask;

public final class RaskolFarming extends JavaPlugin {

    private FarmStore store;
    private LicenseStore licenseStore;
    private SelectionHook selectionHook;
    private TownyHook townyHook;
    private VaultHook vaultHook;
    private HoeService hoeService;
    private LicenseService licenseService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        store = new FarmStore(this);
        store.load();
        licenseStore = new LicenseStore(this);
        licenseStore.load();
        selectionHook = new SelectionHook(this);
        townyHook = new TownyHook(this);
        vaultHook = new VaultHook(this);
        hoeService = new HoeService(this);
        licenseService = new LicenseService(this, licenseStore, vaultHook, townyHook);

        registerHoeRecipes();

        PluginCommand cmd = getCommand("farm");
        if (cmd != null) {
            FarmCommand executor = new FarmCommand(this, store, selectionHook,
                    hoeService, licenseService, licenseStore, townyHook);
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        }

        getServer().getPluginManager().registerEvents(new FarmLicenseListener(this, licenseService), this);
        getServer().getPluginManager().registerEvents(new CropListener(this, store, hoeService), this);
        getServer().getPluginManager().registerEvents(new HoeCraftListener(this, hoeService), this);
        getServer().getPluginManager().registerEvents(new HoeDurabilityListener(this, hoeService), this);

        getServer().getScheduler().runTaskTimer(this,
                new CropRegenTask(this, store), 200L, 20L * 60);

        getLogger().info("RaskolFarming v" + getDescription().getVersion()
                + " включён. Ферм: " + store.all().size()
                + ", в очереди регенерации: " + store.pending().size()
                + ", лицензий активно: " + licenseStore.activeCount()
                + ". Интервал: " + getConfig().getDouble("regen.interval-minutes", 15.0) + " мин."
                + ". Towny: " + (townyHook.isAvailable() ? "да" : "нет")
                + ". Vault: " + (vaultHook.isAvailable() ? "да" : "нет") + ".");
    }

    private void registerHoeRecipes() {
        int count = 0;
        for (HoeTier tier : new HoeTier[]{HoeTier.GRANDPA, HoeTier.EXPERIENCED, HoeTier.DULKLASS}) {
            NamespacedKey key = new NamespacedKey(this, "hoe_craft_" + tier.name().toLowerCase());
            if (getServer().getRecipe(key) != null) continue;
            ShapedRecipe recipe = new ShapedRecipe(key, new ItemStack(Material.IRON_HOE));
            recipe.shape("   ", "FFF", "   ");
            recipe.setIngredient('F', Material.PUMPKIN_SEEDS);
            getServer().addRecipe(recipe);
            count++;
        }
        getLogger().info("[Farming] рецептов мотыг зарегистрировано: " + count);
    }

    @Override
    public void onDisable() {
        if (store != null) store.save();
        if (licenseStore != null) licenseStore.save();
        getLogger().info("RaskolFarming выключен.");
    }

    public FarmStore getStore() { return store; }
    public HoeService getHoeService() { return hoeService; }
    public LicenseService getLicenseService() { return licenseService; }
}
