package ru.raskol.farming.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.raskol.farming.RaskolFarming;
import ru.raskol.farming.hook.SelectionHook;
import ru.raskol.farming.hook.TownyHook;
import ru.raskol.farming.model.FarmZone;
import ru.raskol.farming.model.HoeTier;
import ru.raskol.farming.model.License;
import ru.raskol.farming.model.Region;
import ru.raskol.farming.service.HoeService;
import ru.raskol.farming.service.LicenseService;
import ru.raskol.farming.store.FarmStore;
import ru.raskol.farming.store.LicenseStore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class FarmCommand implements CommandExecutor, TabCompleter {

    private final RaskolFarming plugin;
    private final FarmStore store;
    private final SelectionHook selection;
    private final HoeService hoeService;
    private final LicenseService licenseService;
    private final LicenseStore licenseStore;
    private final TownyHook towny;

    public FarmCommand(RaskolFarming plugin, FarmStore store, SelectionHook selection,
                       HoeService hoeService, LicenseService licenseService,
                       LicenseStore licenseStore, TownyHook towny) {
        this.plugin = plugin;
        this.store = store;
        this.selection = selection;
        this.hoeService = hoeService;
        this.licenseService = licenseService;
        this.licenseStore = licenseStore;
        this.towny = towny;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) { sendHelp(sender); return true; }
        switch (args[0].toLowerCase()) {
            case "create"   -> handleCreate(sender, args);
            case "delete"   -> handleDelete(sender, args);
            case "list"     -> handleList(sender);
            case "reload"   -> handleReload(sender);
            case "license"  -> handleLicense(sender, args);
            case "hoe"      -> handleHoe(sender, args);
            case "fragment" -> handleFragment(sender, args);
            case "golden"   -> handleGolden(sender, args);
            default         -> sendHelp(sender);
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6=== RaskolFarming ===");
        sender.sendMessage("§e/farm license buy §7— купить лицензию фермера");
        sender.sendMessage("§e/farm license status §7— статус лицензии");
        sender.sendMessage("§e/farm license setprice <цена> §7— (король) цена в своём городе");
        sender.sendMessage("§e/farm create <имя> §7— создать зону фермы из WE-выделения");
        sender.sendMessage("§e/farm delete <имя> §7— удалить зону");
        sender.sendMessage("§e/farm list §7— список зон и очередь регенерации");
        sender.sendMessage("§e/farm hoe give <tier> <игрок> [кол-во] §7— выдать мотыгу");
        sender.sendMessage("§e/farm fragment give <tier> <игрок> [кол-во] §7— выдать фрагмент");
        sender.sendMessage("§e/farm golden give <игрок> [кол-во] §7— выдать золотую пшеницу");
        sender.sendMessage("§e/farm reload §7— перезагрузить конфиг");
        sender.sendMessage("§7Тиры: VANILLA, GRANDPA, EXPERIENCED, DULKLASS");
    }

    private boolean admin(CommandSender sender) {
        if (sender.hasPermission("farming.admin")) return true;
        sender.sendMessage("§cНет прав.");
        return false;
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (!admin(sender)) return;
        if (!(sender instanceof Player p)) { sender.sendMessage("§cТолько игроки."); return; }
        if (args.length < 2) { p.sendMessage("§c/farm create <имя>"); return; }
        String name = args[1];
        if (store.get(name) != null) { p.sendMessage("§cЗона " + name + " уже существует."); return; }
        Region sel = selection.getSelection(p);
        if (sel == null) { p.sendMessage("§cСначала сделай выделение WorldEdit."); return; }
        FarmZone z = new FarmZone();
        z.name = name;
        z.region = sel;
        store.addZone(z);
        p.sendMessage("§aФерма §e" + name + " §aсоздана. Регион: "
                + sel.x1 + "," + sel.y1 + "," + sel.z1 + " -> " + sel.x2 + "," + sel.y2 + "," + sel.z2);
        plugin.getLogger().info("[Farming] zone created: " + name + " by " + p.getName());
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (!admin(sender)) return;
        if (args.length < 2) { sender.sendMessage("§c/farm delete <имя>"); return; }
        String name = args[1];
        if (store.get(name) == null) { sender.sendMessage("§cЗоны " + name + " нет."); return; }
        store.removeZone(name);
        sender.sendMessage("§cФерма " + name + " удалена.");
    }

    private void handleList(CommandSender sender) {
        List<FarmZone> all = store.all();
        sender.sendMessage("§6=== Ферм: " + all.size() + " ===");
        for (FarmZone z : all) {
            Region r = z.region;
            sender.sendMessage("§e" + z.name + " §7— " + r.world + " ["
                    + r.x1 + "," + r.y1 + "," + r.z1 + " → " + r.x2 + "," + r.y2 + "," + r.z2 + "]");
        }
        sender.sendMessage("§7В очереди регенерации: §e" + store.pending().size() + " §7блоков.");
    }

    private void handleReload(CommandSender sender) {
        if (!admin(sender)) return;
        plugin.reloadConfig();
        sender.sendMessage("§aКонфиг перезагружен.");
    }

    private void handleLicense(CommandSender sender, String[] args) {
        if (args.length < 2) { sender.sendMessage("§c/farm license <buy|status|setprice>"); return; }
        switch (args[1].toLowerCase()) {
            case "buy" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("§cТолько игроки."); return; }
                p.sendMessage(licenseService.buy(p).message);
            }
            case "status" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("§cТолько игроки."); return; }
                License lic = licenseStore.get(p.getUniqueId().toString());
                if (lic == null || !lic.isValid()) {
                    p.sendMessage("§7Лицензия неактивна. Купи: §e/farm license buy");
                } else {
                    p.sendMessage("§aЛицензия фермера активна. Осталось: §e" + lic.remainingFormatted());
                }
            }
            case "setprice" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("§cТолько игроки."); return; }
                if (args.length < 3) { p.sendMessage("§c/farm license setprice <цена>"); return; }
                String town = towny.residentTownName(p);
                if (town == null && !p.hasPermission("farming.admin")) {
                    p.sendMessage("§cТы должен быть жителем города.");
                    return;
                }
                if (!p.hasPermission("farming.admin") && !towny.isKing(p)) {
                    p.sendMessage("§cТолько король может менять цену лицензии.");
                    return;
                }
                double price;
                try { price = Double.parseDouble(args[2]); }
                catch (NumberFormatException e) { p.sendMessage("§cЦена должна быть числом."); return; }
                if (price < 0) { p.sendMessage("§cЦена не может быть отрицательной."); return; }
                if (town != null) licenseStore.setTownPrice(town, price);
                p.sendMessage("§aЦена лицензии фермера для " + town + " теперь §b" + price + "§a.");
            }
            default -> sender.sendMessage("§cНеизвестная подкоманда.");
        }
    }

    private void handleHoe(CommandSender sender, String[] args) {
        if (!admin(sender)) return;
        if (args.length < 4 || !args[1].equalsIgnoreCase("give")) {
            sender.sendMessage("§c/farm hoe give <VANILLA|GRANDPA|EXPERIENCED|DULKLASS> <игрок> [кол-во]");
            return;
        }
        HoeTier tier;
        try { tier = HoeTier.valueOf(args[2].toUpperCase()); }
        catch (IllegalArgumentException e) { sender.sendMessage("§cНеизвестный тир."); return; }
        Player target = plugin.getServer().getPlayer(args[3]);
        if (target == null) { sender.sendMessage("§cИгрок не найден."); return; }
        int amount = parseInt(args.length >= 5 ? args[4] : "1", 1);
        for (int i = 0; i < amount; i++) target.getInventory().addItem(hoeService.createHoe(tier));
        sender.sendMessage("§aВыдано " + amount + " × " + tier.displayName + " §aигроку " + target.getName());
    }

    private void handleFragment(CommandSender sender, String[] args) {
        if (!admin(sender)) return;
        if (args.length < 4 || !args[1].equalsIgnoreCase("give")) {
            sender.sendMessage("§c/farm fragment give <GRANDPA|EXPERIENCED|DULKLASS> <игрок> [кол-во]");
            return;
        }
        HoeTier tier;
        try { tier = HoeTier.valueOf(args[2].toUpperCase()); }
        catch (IllegalArgumentException e) { sender.sendMessage("§cНеизвестный тир."); return; }
        if (tier == HoeTier.VANILLA) { sender.sendMessage("§cУ обычной мотыги нет фрагмента."); return; }
        Player target = plugin.getServer().getPlayer(args[3]);
        if (target == null) { sender.sendMessage("§cИгрок не найден."); return; }
        int amount = parseInt(args.length >= 5 ? args[4] : "1", 1);
        target.getInventory().addItem(hoeService.createFragment(tier, amount));
        sender.sendMessage("§aВыдано " + amount + " × " + tier.fragmentDisplayName + " §aигроку " + target.getName());
    }

    private void handleGolden(CommandSender sender, String[] args) {
        if (!admin(sender)) return;
        if (args.length < 3 || !args[1].equalsIgnoreCase("give")) {
            sender.sendMessage("§c/farm golden give <игрок> [кол-во]");
            return;
        }
        Player target = plugin.getServer().getPlayer(args[2]);
        if (target == null) { sender.sendMessage("§cИгрок не найден."); return; }
        int amount = parseInt(args.length >= 4 ? args[3] : "1", 1);
        target.getInventory().addItem(hoeService.createGoldenWheat(amount));
        sender.sendMessage("§aВыдано " + amount + " × §6✦ Золотая пшеница ✦ §aигроку " + target.getName());
    }

    private int parseInt(String s, int def) {
        try { return Integer.parseInt(s); }
        catch (NumberFormatException e) { return def; }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("license", "create", "delete", "list", "reload",
                    "hoe", "fragment", "golden"), args[0]);
        }
        if (args.length == 2) {
            return switch (args[0].toLowerCase()) {
                case "license" -> filter(Arrays.asList("buy", "status", "setprice"), args[1]);
                case "hoe", "fragment", "golden" -> filter(List.of("give"), args[1]);
                case "delete" -> zoneNames(args[1]);
                default -> Collections.emptyList();
            };
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("hoe") && args[1].equalsIgnoreCase("give")) {
            return filter(Arrays.asList("VANILLA", "GRANDPA", "EXPERIENCED", "DULKLASS"), args[2]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("fragment") && args[1].equalsIgnoreCase("give")) {
            return filter(Arrays.asList("GRANDPA", "EXPERIENCED", "DULKLASS"), args[2]);
        }
        return Collections.emptyList();
    }

    private List<String> zoneNames(String prefix) {
        List<String> names = new ArrayList<>();
        for (FarmZone z : store.all()) names.add(z.name);
        return filter(names, prefix);
    }

    private List<String> filter(List<String> list, String prefix) {
        String lower = prefix.toLowerCase();
        List<String> out = new ArrayList<>();
        for (String s : list) if (s.toLowerCase().startsWith(lower)) out.add(s);
        return out;
    }
}
