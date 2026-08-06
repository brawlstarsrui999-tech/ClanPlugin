package ru.clanplugin.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import ru.clanplugin.ClanPlugin;
import ru.clanplugin.clan.Clan;
import ru.clanplugin.clan.ClanTeleportManager;
import ru.clanplugin.gui.*;

import java.text.DecimalFormat;
import java.util.*;

public class ClanCommand implements CommandExecutor, TabCompleter {

    private final ClanPlugin plugin;
    private static final DecimalFormat FORMAT = new DecimalFormat("#,###");
    private final Map<UUID, Long> disbandConfirm = new HashMap<>();

    public ClanCommand(ClanPlugin plugin) {
        this.plugin = plugin;
    }

    private Component c(String msg) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(msg);
    }

    private void msg(CommandSender sender, String msg) {
        sender.sendMessage(c(plugin.getPrefix() + msg));
    }

    private void playSound(Player p, Sound sound) {
        p.playSound(p.getLocation(), sound, 1.0f, 1.0f);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Эта команда только для игроков!");
            return true;
        }

        if (args.length == 0) {
            // Open main menu
            Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
            if (clan == null) {
                showHelp(player);
            } else {
                ClanMainMenu.open(player, plugin);
                playSound(player, Sound.BLOCK_CHEST_OPEN);
            }
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "create" -> handleCreate(player, args);
            case "invite" -> handleInvite(player, args);
            case "accept" -> handleAccept(player, args);
            case "leave" -> handleLeave(player);
            case "disband" -> handleDisband(player);
            case "sethome" -> handleSetHome(player, args);
            case "home" -> handleHome(player, args);
            case "delhome" -> handleDelHome(player, args);
            case "pvp" -> handlePvp(player);
            case "money" -> handleMoney(player);
            case "invest" -> handleInvest(player, args);
            case "withdraw" -> handleWithdraw(player, args);
            case "shop" -> handleShop(player);
            case "top" -> handleTop(player);
            case "tp" -> handleTp(player, args);
            case "tpaccept" -> handleTpAccept(player, args);
            case "tpdeny" -> handleTpDeny(player, args);
            case "menu" -> handleMenu(player);
            case "officer" -> handleOfficer(player, args);
            case "kick" -> handleKick(player, args);
            case "help" -> showHelp(player);
            default -> {
                msg(player, "&cНеизвестная подкоманда. &7Используйте &e/clan help");
                playSound(player, Sound.ENTITY_VILLAGER_NO);
            }
        }
        return true;
    }

    private void handleCreate(Player player, String[] args) {
        if (plugin.getClanManager().isInClan(player.getUniqueId())) {
            msg(player, "&cВы уже состоите в клане!");
            playSound(player, Sound.ENTITY_VILLAGER_NO);
            return;
        }

        // Стоимость создания клана (по умолчанию 10 000 монет)
        long cost = plugin.getConfig().getLong("clan.creation-cost", 10000);

        if (args.length < 2) {
            if (cost > 0) {
                msg(player, "&cИспользование: &e/clan create <название> &7(цена: &6" + FORMAT.format(cost) + " монет&7)");
            } else {
                msg(player, "&cИспользование: &e/clan create <название>");
            }
            return;
        }
        String name = args[1];
        if (name.length() < 2 || name.length() > 16) {
            msg(player, "&cНазвание клана должно быть от 2 до 16 символов!");
            return;
        }
        if (plugin.getClanManager().clanNameExists(name)) {
            msg(player, "&cКлан с таким именем уже существует!");
            return;
        }

        // Списываем оплату за создание клана
        if (cost > 0) {
            if (!plugin.hasEconomy()) {
                msg(player, "&cЭкономика не настроена! Создание кланов временно недоступно.");
                msg(player, "&7Сообщите об этом администрации сервера.");
                playSound(player, Sound.ENTITY_VILLAGER_NO);
                return;
            }
            if (!plugin.getEconomy().has(player, cost)) {
                msg(player, "&cНедостаточно средств! &7Создание клана стоит &6" + FORMAT.format(cost) + " монет&7.");
                msg(player, "&7У вас: &6" + FORMAT.format((int) plugin.getEconomy().getBalance(player)) + " монет");
                playSound(player, Sound.ENTITY_VILLAGER_NO);
                return;
            }
            net.milkbowl.vault.economy.EconomyResponse response = plugin.getEconomy().withdrawPlayer(player, cost);
            if (!response.transactionSuccess()) {
                msg(player, "&cОшибка при списании средств: &7" + response.errorMessage);
                playSound(player, Sound.ENTITY_VILLAGER_NO);
                return;
            }
        }

        Clan clan = plugin.getClanManager().createClan(name, player.getUniqueId());

        player.sendMessage(c(""));
        player.sendMessage(c("&8&m                                        "));
        player.sendMessage(c("  &6&l⚔ КЛАН СОЗДАН! &8⚔"));
        player.sendMessage(c("  &7Название: &e" + clan.getName()));
        player.sendMessage(c("  &7Вы стали &6&lЛидером &7клана!"));
        if (cost > 0) {
            player.sendMessage(c("  &7Списано за создание: &c-" + FORMAT.format(cost) + " монет"));
        }
        player.sendMessage(c("  &7Откройте меню: &e/clan"));
        player.sendMessage(c("&8&m                                        "));
        player.sendMessage(c(""));

        playSound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE);
    }

    private void handleInvite(Player player, String[] args) {
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            msg(player, "&cВы не состоите в клане!");
            return;
        }
        if (!clan.isOwner(player.getUniqueId()) && !clan.isOfficer(player.getUniqueId())) {
            msg(player, "&cТолько лидер или офицер может приглашать!");
            return;
        }
        if (args.length < 2) {
            msg(player, "&cИспользование: &e/clan invite <игрок>");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            msg(player, "&cИгрок не найден или не в сети!");
            return;
        }
        if (plugin.getClanManager().isInClan(target.getUniqueId())) {
            msg(player, "&cЭтот игрок уже состоит в клане!");
            return;
        }
        if (clan.hasPendingInvite(target.getUniqueId())) {
            msg(player, "&cПриглашение уже отправлено!");
            return;
        }

        clan.addInvite(target.getUniqueId());

        // Cancel invite after 60 seconds
        Bukkit.getScheduler().runTaskLater(plugin, () -> clan.removeInvite(target.getUniqueId()), 1200L);

        msg(player, "&aПриглашение отправлено игроку &e" + target.getName());
        playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP);

        target.sendMessage(c(""));
        target.sendMessage(c("&8&m                                        "));
        target.sendMessage(c("  &6&l⚔ ПРИГЛАШЕНИЕ В КЛАН &8⚔"));
        target.sendMessage(c("  &7Клан: &e" + clan.getName()));
        target.sendMessage(c("  &7Лидер: &a" + player.getName()));
        target.sendMessage(c("  &7Введите &a/clan accept &7для вступления"));
        target.sendMessage(c("  &7(Действует 60 секунд)"));
        target.sendMessage(c("&8&m                                        "));
        target.sendMessage(c(""));
        target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
    }

    private void handleAccept(Player player, String[] args) {
        if (plugin.getClanManager().isInClan(player.getUniqueId())) {
            msg(player, "&cВы уже состоите в клане!");
            return;
        }

        // Find clan that invited this player
        Clan targetClan = null;
        for (Clan c : plugin.getClanManager().getAllClans()) {
            if (c.hasPendingInvite(player.getUniqueId())) {
                targetClan = c;
                break;
            }
        }

        if (targetClan == null) {
            msg(player, "&cУ вас нет активных приглашений!");
            return;
        }

        targetClan.removeInvite(player.getUniqueId());
        plugin.getClanManager().joinClan(player.getUniqueId(), targetClan);

        player.sendMessage(c(""));
        player.sendMessage(c("&8&m                                        "));
        player.sendMessage(c("  &a&l✔ ВЫ ВСТУПИЛИ В КЛАН!"));
        player.sendMessage(c("  &7Клан: &e" + targetClan.getName()));
        player.sendMessage(c("  &7Добро пожаловать! &7/clan — меню"));
        player.sendMessage(c("&8&m                                        "));
        player.sendMessage(c(""));
        playSound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE);

        // Notify clan members
        String finalMsg = plugin.getPrefix() + "&a&l" + player.getName() + " &aвступил в клан! Поприветствуем!";
        for (Player m : targetClan.getOnlineMembers()) {
            if (!m.equals(player)) {
                m.sendMessage(c(finalMsg));
                playSound(m, Sound.ENTITY_PLAYER_LEVELUP);
            }
        }
    }

    private void handleLeave(Player player) {
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            msg(player, "&cВы не состоите в клане!");
            return;
        }
        if (clan.isOwner(player.getUniqueId())) {
            msg(player, "&cВы лидер! Используйте &e/clan disband &cдля роспуска клана.");
            return;
        }

        String clanName = clan.getName();
        plugin.getClanManager().leaveClan(player.getUniqueId());

        // Disable glow for leaving player
        player.setGlowing(false);

        msg(player, "&eВы покинули клан &6" + clanName);
        playSound(player, Sound.ENTITY_ITEM_BREAK);

        // Notify
        for (Player m : clan.getOnlineMembers()) {
            m.sendMessage(c(plugin.getPrefix() + "&c" + player.getName() + " покинул клан."));
        }
    }

    private void handleDisband(Player player) {
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            msg(player, "&cВы не состоите в клане!");
            return;
        }
        if (!clan.isOwner(player.getUniqueId())) {
            msg(player, "&cТолько лидер может расформировать клан!");
            return;
        }

        long now = System.currentTimeMillis();
        if (disbandConfirm.containsKey(player.getUniqueId()) &&
                now - disbandConfirm.get(player.getUniqueId()) < 15000) {
            // Confirmed!
            disbandConfirm.remove(player.getUniqueId());
            String clanName = clan.getName();
            List<Player> members = clan.getOnlineMembers();
            plugin.getClanManager().disbandClan(clan);
            for (Player m : members) {
                m.sendMessage(c(plugin.getPrefix() + "&c&lКлан &4" + clanName + " &c&lбыл расформирован!"));
                playSound(m, Sound.ENTITY_WITHER_DEATH);
            }
        } else {
            disbandConfirm.put(player.getUniqueId(), now);
            msg(player, "&c&lВНИМАНИЕ! &cЭто удалит клан безвозвратно!");
            msg(player, "&7Введите &c/clan disband &7ещё раз для подтверждения (15 сек)");
            playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS);
        }
    }

    private void handleSetHome(Player player, String[] args) {
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            msg(player, "&cВы не состоите в клане!");
            return;
        }
        if (!clan.isOwner(player.getUniqueId()) &&
                !clan.hasOfficerPerm(player.getUniqueId(), Clan.PERM_SETHOME)) {
            msg(player, "&cТолько лидер или офицер с правами может устанавливать базы!");
            return;
        }
        if (args.length < 2) {
            msg(player, "&cИспользование: &e/clan sethome <название>");
            return;
        }
        String homeName = args[1];
        if (homeName.length() > 16) {
            msg(player, "&cНазвание базы не может быть длиннее 16 символов!");
            return;
        }
        clan.setHome(homeName, player.getLocation());
        plugin.getClanManager().saveImmediately();
        msg(player, "&aБаза &e" + homeName + " &aустановлена!");
        playSound(player, Sound.BLOCK_BEACON_ACTIVATE);
    }

    private void handleHome(Player player, String[] args) {
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            msg(player, "&cВы не состоите в клане!");
            return;
        }

        if (clan.getHomes().isEmpty()) {
            msg(player, "&cУ клана нет установленных баз!");
            return;
        }

        if (args.length < 2) {
            // Show homes menu
            ClanHomesMenu.open(player, plugin);
            playSound(player, Sound.BLOCK_CHEST_OPEN);
            return;
        }

        String homeName = args[1];
        if (!clan.hasHome(homeName)) {
            msg(player, "&cБаза &e" + homeName + " &cне найдена!");
            msg(player, "&7Доступные базы: &e" + String.join(", ", clan.getHomes().keySet()));
            return;
        }

        player.teleport(clan.getHome(homeName));
        msg(player, "&aТелепортация на базу &e" + homeName + "!");
        playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT);
    }

    private void handleDelHome(Player player, String[] args) {
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) { msg(player, "&cВы не состоите в клане!"); return; }
        if (!clan.isOwner(player.getUniqueId()) && !clan.hasOfficerPerm(player.getUniqueId(), Clan.PERM_SETHOME)) {
            msg(player, "&cНет прав!"); return;
        }
        if (args.length < 2) { msg(player, "&cИспользование: &e/clan delhome <название>"); return; }
        String homeName = args[1];
        if (!clan.hasHome(homeName)) { msg(player, "&cТакой базы нет!"); return; }
        clan.removeHome(homeName);
        plugin.getClanManager().saveImmediately();
        msg(player, "&aБаза &e" + homeName + " &aудалена!");
        playSound(player, Sound.BLOCK_BEACON_DEACTIVATE);
    }

    private void handlePvp(Player player) {
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            msg(player, "&cВы не состоите в клане!");
            return;
        }
        if (!clan.isOwner(player.getUniqueId()) &&
                !clan.hasOfficerPerm(player.getUniqueId(), Clan.PERM_PVP)) {
            msg(player, "&cНет прав на управление PvP!");
            return;
        }

        boolean newState = !clan.isPvpEnabled();
        clan.setPvpEnabled(newState);
        plugin.getClanManager().saveImmediately();

        String status = newState ? "&c&lВКЛЮЧЁН" : "&a&lОТКЛЮЧЁН";
        String broadcastMsg = plugin.getPrefix() + "&7Дружественный огонь " + status + " &7игроком &e" + player.getName();

        for (Player m : clan.getOnlineMembers()) {
            m.sendMessage(c(broadcastMsg));
            playSound(m, newState ? Sound.BLOCK_NOTE_BLOCK_BASS : Sound.BLOCK_NOTE_BLOCK_PLING);
        }
    }

    private void handleMoney(Player player) {
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            msg(player, "&cВы не состоите в клане!");
            return;
        }

        player.sendMessage(c(""));
        player.sendMessage(c("&8&m                                        "));
        player.sendMessage(c("  &6&l💰 КАЗНА КЛАНА &e" + clan.getName()));
        player.sendMessage(c("  &7Баланс: &6" + FORMAT.format((int) clan.getBalance()) + " монет"));
        if (plugin.hasEconomy()) {
            player.sendMessage(c("  &7Ваш баланс: &a" + FORMAT.format((int) plugin.getEconomy().getBalance(player)) + " монет"));
        }
        player.sendMessage(c("  &7Пополнить: &e/clan invest <сумма>"));
        player.sendMessage(c("  &7Снять: &e/clan withdraw <сумма>"));
        player.sendMessage(c("&8&m                                        "));
        player.sendMessage(c(""));
        playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
    }

    private void handleInvest(Player player, String[] args) {
        if (!plugin.hasEconomy()) {
            msg(player, "&cЭкономика не настроена!");
            return;
        }
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            msg(player, "&cВы не состоите в клане!");
            return;
        }
        if (args.length < 2) {
            msg(player, "&cИспользование: &e/clan invest <сумма>");
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            msg(player, "&cНеверная сумма!");
            return;
        }
        if (amount <= 0) {
            msg(player, "&cСумма должна быть больше 0!");
            return;
        }
        if (!plugin.getEconomy().has(player, amount)) {
            msg(player, "&cУ вас недостаточно средств! &7(У вас: &6" + FORMAT.format((int) plugin.getEconomy().getBalance(player)) + "&7)");
            return;
        }

        plugin.getEconomy().withdrawPlayer(player, amount);
        clan.deposit(amount);
        plugin.getClanManager().saveImmediately();

        msg(player, "&aВы вложили &6" + FORMAT.format((int) amount) + " монет &aв казну клана!");
        msg(player, "&7Баланс казны: &6" + FORMAT.format((int) clan.getBalance()) + " монет");
        playSound(player, Sound.ENTITY_PLAYER_LEVELUP);

        // Notify members
        for (Player m : clan.getOnlineMembers()) {
            if (!m.equals(player)) {
                m.sendMessage(c(plugin.getPrefix() + "&a" + player.getName() + " вложил &6" + FORMAT.format((int) amount) + " монет &aв казну клана!"));
            }
        }
    }

    private void handleWithdraw(Player player, String[] args) {
        if (!plugin.hasEconomy()) {
            msg(player, "&cЭкономика не настроена!");
            return;
        }
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            msg(player, "&cВы не состоите в клане!");
            return;
        }
        if (!clan.isOwner(player.getUniqueId())) {
            msg(player, "&cТолько лидер может снимать деньги с казны!");
            return;
        }
        if (args.length < 2) {
            msg(player, "&cИспользование: &e/clan withdraw <сумма>");
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            msg(player, "&cНеверная сумма!");
            return;
        }
        if (amount <= 0) {
            msg(player, "&cСумма должна быть больше 0!");
            return;
        }
        if (!clan.withdraw(amount)) {
            msg(player, "&cВ казне недостаточно средств! &7(Казна: &6" + FORMAT.format((int) clan.getBalance()) + "&7)");
            return;
        }

        plugin.getEconomy().depositPlayer(player, amount);
        plugin.getClanManager().saveImmediately();

        msg(player, "&aВы сняли &6" + FORMAT.format((int) amount) + " монет &aиз казны клана!");
        playSound(player, Sound.ENTITY_PLAYER_LEVELUP);

        for (Player m : clan.getOnlineMembers()) {
            if (!m.equals(player)) {
                m.sendMessage(c(plugin.getPrefix() + "&e" + player.getName() + " снял &6" + FORMAT.format((int) amount) + " монет &eиз казны."));
            }
        }
    }

    private void handleShop(Player player) {
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            msg(player, "&cВы не состоите в клане!");
            return;
        }
        ClanShopMenu.open(player, plugin);
        playSound(player, Sound.BLOCK_CHEST_OPEN);
    }

    private void handleTop(Player player) {
        ClanTopMenu.open(player, plugin);
        playSound(player, Sound.BLOCK_CHEST_OPEN);
    }

    private void handleTp(Player player, String[] args) {
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            msg(player, "&cВы не состоите в клане!");
            return;
        }
        if (!clan.hasFeature("clan-tp")) {
            msg(player, "&cФункция телепортации не куплена! &7Купите в /clan menu → Улучшения");
            return;
        }
        if (args.length < 2) {
            msg(player, "&cИспользование: &e/clan tp <игрок>&c, &e/clan tpaccept&c, &e/clan tpdeny");
            // Show online members
            StringBuilder sb = new StringBuilder("&7Онлайн участники: ");
            for (Player m : clan.getOnlineMembers()) {
                if (!m.equals(player)) sb.append("&e").append(m.getName()).append("&7, ");
            }
            msg(player, sb.toString());
            return;
        }

        String targetArg = args[1];
        if (targetArg.equalsIgnoreCase("accept")) {
            handleTpAccept(player, args);
            return;
        }
        if (targetArg.equalsIgnoreCase("deny")) {
            handleTpDeny(player, args);
            return;
        }

        Player target = Bukkit.getPlayer(targetArg);
        if (target == null) {
            msg(player, "&cИгрок не найден или не в сети!");
            return;
        }
        if (!clan.isMember(target.getUniqueId())) {
            msg(player, "&cЭтот игрок не является участником вашего клана!");
            return;
        }
        if (target.equals(player)) {
            msg(player, "&cНельзя телепортироваться к себе!");
            return;
        }

        plugin.getClanTeleportManager().createRequest(player.getUniqueId(), target.getUniqueId());

        msg(player, "&aЗапрос на телепортацию отправлен игроку &e" + target.getName() + "&a!");
        playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP);

        target.sendMessage(c(""));
        target.sendMessage(c("&8&m                                        "));
        target.sendMessage(c("  &b&l✈ ЗАПРОС НА ТЕЛЕПОРТАЦИЮ &8✈"));
        target.sendMessage(c("  &7Игрок: &e" + player.getName()));
        target.sendMessage(c("  &7Введите &a/clan tpaccept &7или &a/clan tp accept &7для принятия"));
        target.sendMessage(c("  &7Введите &c/clan tpdeny &7или &c/clan tp deny &7для отклонения"));
        target.sendMessage(c("  &7(Запрос действует 60 секунд)"));
        target.sendMessage(c("&8&m                                        "));
        target.sendMessage(c(""));
        target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
    }

    private void handleTpAccept(Player player, String[] args) {
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            msg(player, "&cВы не состоите в клане!");
            return;
        }

        ClanTeleportManager tpManager = plugin.getClanTeleportManager();
        List<ClanTeleportManager.TeleportRequest> incoming = tpManager.getActiveRequestsTo(player.getUniqueId());

        if (incoming.isEmpty()) {
            msg(player, "&cУ вас нет активных запросов на телепортацию!");
            return;
        }

        ClanTeleportManager.TeleportRequest request = null;

        String senderNameArg = null;
        if (args[0].equalsIgnoreCase("tpaccept") && args.length >= 2) {
            senderNameArg = args[1];
        } else if (args[0].equalsIgnoreCase("tp") && args.length >= 3) {
            senderNameArg = args[2];
        }

        if (senderNameArg != null) {
            Player senderPlayer = Bukkit.getPlayer(senderNameArg);
            if (senderPlayer != null) {
                UUID senderUuid = senderPlayer.getUniqueId();
                for (ClanTeleportManager.TeleportRequest r : incoming) {
                    if (r.getSender().equals(senderUuid)) {
                        request = r;
                        break;
                    }
                }
            }
            if (request == null) {
                msg(player, "&cНе найден активный запрос от игрока &e" + senderNameArg + "&c!");
                return;
            }
        } else {
            request = incoming.get(0);
        }

        UUID senderUuid = request.getSender();
        Player sender = Bukkit.getPlayer(senderUuid);
        if (sender == null || !sender.isOnline()) {
            msg(player, "&cОтправитель запроса сейчас не в сети!");
            tpManager.removeRequest(senderUuid);
            return;
        }

        Clan senderClan = plugin.getClanManager().getPlayerClan(senderUuid);
        if (senderClan == null || !senderClan.getId().equals(clan.getId())) {
            msg(player, "&cЭтот игрок больше не состоит в вашем клане!");
            tpManager.removeRequest(senderUuid);
            return;
        }

        tpManager.removeRequest(senderUuid);

        msg(player, "&aВы приняли запрос на телепортацию от &e" + sender.getName() + "&a!");
        playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP);

        sender.sendMessage(c(""));
        sender.sendMessage(c(plugin.getPrefix() + "&e" + player.getName() + " &aпринял ваш запрос на телепортацию!"));

        int warmup = plugin.getConfig().getInt("teleport.warmup", 3);
        sender.sendMessage(c(plugin.getPrefix() + "&aТелепортация через &e" + warmup + " секунды&a... Не двигайтесь!"));
        playSound(sender, Sound.BLOCK_PORTAL_TRIGGER);

        org.bukkit.Location startLoc = sender.getLocation();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!sender.isOnline()) return;
            if (sender.getLocation().distanceSquared(startLoc) > 1) {
                sender.sendMessage(c(plugin.getPrefix() + "&cТелепортация отменена — вы сдвинулись с места!"));
                player.sendMessage(c(plugin.getPrefix() + "&cТелепортация игрока &e" + sender.getName() + " &cотменена, так как он сдвинулся с места."));
                return;
            }
            if (!player.isOnline()) {
                sender.sendMessage(c(plugin.getPrefix() + "&cТелепортация отменена — цель не в сети!"));
                return;
            }
            sender.teleport(player.getLocation());
            sender.sendMessage(c(plugin.getPrefix() + "&aТелепортация к &e" + player.getName() + " &aвыполнена!"));
            player.sendMessage(c(plugin.getPrefix() + "&e" + sender.getName() + " &aтелепортировался к вам!"));
            playSound(sender, Sound.ENTITY_ENDERMAN_TELEPORT);
            playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT);
        }, warmup * 20L);
    }

    private void handleTpDeny(Player player, String[] args) {
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            msg(player, "&cВы не состоите в клане!");
            return;
        }

        ClanTeleportManager tpManager = plugin.getClanTeleportManager();
        List<ClanTeleportManager.TeleportRequest> incoming = tpManager.getActiveRequestsTo(player.getUniqueId());

        if (incoming.isEmpty()) {
            msg(player, "&cУ вас нет активных запросов на телепортацию!");
            return;
        }

        ClanTeleportManager.TeleportRequest request = null;

        String senderNameArg = null;
        if (args[0].equalsIgnoreCase("tpdeny") && args.length >= 2) {
            senderNameArg = args[1];
        } else if (args[0].equalsIgnoreCase("tp") && args.length >= 3) {
            senderNameArg = args[2];
        }

        if (senderNameArg != null) {
            Player senderPlayer = Bukkit.getPlayer(senderNameArg);
            if (senderPlayer != null) {
                UUID senderUuid = senderPlayer.getUniqueId();
                for (ClanTeleportManager.TeleportRequest r : incoming) {
                    if (r.getSender().equals(senderUuid)) {
                        request = r;
                        break;
                    }
                }
            }
            if (request == null) {
                msg(player, "&cНе найден активный запрос от игрока &e" + senderNameArg + "&c!");
                return;
            }
        } else {
            request = incoming.get(0);
        }

        UUID senderUuid = request.getSender();
        tpManager.removeRequest(senderUuid);

        msg(player, "&cВы отклонили запрос на телепортацию.");
        playSound(player, Sound.ENTITY_VILLAGER_NO);

        Player sender = Bukkit.getPlayer(senderUuid);
        if (sender != null && sender.isOnline()) {
            sender.sendMessage(c(plugin.getPrefix() + "&e" + player.getName() + " &cотклонил ваш запрос на телепортацию."));
            playSound(sender, Sound.ENTITY_VILLAGER_NO);
        }
    }

    private void handleMenu(Player player) {
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            showHelp(player);
            return;
        }
        ClanMainMenu.open(player, plugin);
        playSound(player, Sound.BLOCK_CHEST_OPEN);
    }

    private void handleOfficer(Player player, String[] args) {
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) { msg(player, "&cВы не состоите в клане!"); return; }
        if (!clan.isOwner(player.getUniqueId())) { msg(player, "&cТолько лидер!"); return; }
        if (args.length < 2) { msg(player, "&cИспользование: &e/clan officer <игрок>"); return; }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { msg(player, "&cИгрок не найден!"); return; }
        if (!clan.isMember(target.getUniqueId())) { msg(player, "&cЭтот игрок не в вашем клане!"); return; }
        if (clan.isOwner(target.getUniqueId())) { msg(player, "&cЛидер уже имеет все права!"); return; }

        if (clan.isOfficer(target.getUniqueId())) {
            clan.removeOfficer(target.getUniqueId());
            msg(player, "&e" + target.getName() + " &7лишён звания офицера.");
            target.sendMessage(c(plugin.getPrefix() + "&cВы лишены звания офицера."));
        } else {
            clan.addOfficer(target.getUniqueId());
            msg(player, "&e" + target.getName() + " &aназначен офицером!");
            target.sendMessage(c(plugin.getPrefix() + "&aВы назначены &6&lОфицером &aклана!"));
            playSound(target, Sound.ENTITY_PLAYER_LEVELUP);
        }
        plugin.getClanManager().saveImmediately();
    }

    private void handleKick(Player player, String[] args) {
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) { msg(player, "&cВы не состоите в клане!"); return; }
        if (!clan.isOwner(player.getUniqueId()) && !clan.isOfficer(player.getUniqueId())) {
            msg(player, "&cНет прав!"); return;
        }
        if (args.length < 2) { msg(player, "&cИспользование: &e/clan kick <игрок>"); return; }

        Player target = Bukkit.getPlayer(args[1]);
        UUID targetUuid = target != null ? target.getUniqueId() : null;

        // Try offline
        if (targetUuid == null) {
            org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(args[1]);
            if (op.hasPlayedBefore()) targetUuid = op.getUniqueId();
        }

        if (targetUuid == null) { msg(player, "&cИгрок не найден!"); return; }
        if (!clan.isMember(targetUuid)) { msg(player, "&cЭтот игрок не в вашем клане!"); return; }
        if (clan.isOwner(targetUuid)) { msg(player, "&cНельзя исключить лидера!"); return; }

        plugin.getClanManager().leaveClan(targetUuid);

        if (target != null) {
            target.sendMessage(c(plugin.getPrefix() + "&cВас исключили из клана &e" + clan.getName()));
            playSound(target, Sound.ENTITY_ITEM_BREAK);
        }
        msg(player, "&aИгрок &e" + args[1] + " &aисключён из клана!");
    }

    private void showHelp(Player player) {
        player.sendMessage(c(""));
        player.sendMessage(c("&8&m                                        "));
        player.sendMessage(c("    &6&l⚔ CLAN PLUGIN &8— &7Помощь"));
        player.sendMessage(c("&8&m                                        "));
        player.sendMessage(c("  &e/clan &8— &7Открыть меню клана"));
        player.sendMessage(c("  &e/clan create <название> &8— &7Создать клан &6("
                + FORMAT.format(plugin.getConfig().getLong("clan.creation-cost", 10000)) + " монет)"));
        player.sendMessage(c("  &e/clan invite <игрок> &8— &7Пригласить игрока"));
        player.sendMessage(c("  &e/clan accept &8— &7Принять приглашение"));
        player.sendMessage(c("  &e/clan leave &8— &7Покинуть клан"));
        player.sendMessage(c("  &e/clan pvp &8— &7Переключить PvP"));
        player.sendMessage(c("  &e/clan sethome <имя> &8— &7Установить базу"));
        player.sendMessage(c("  &e/clan home [имя] &8— &7Тп на базу"));
        player.sendMessage(c("  &e/clan money &8— &7Баланс казны"));
        player.sendMessage(c("  &e/clan invest <сумма> &8— &7Пополнить казну"));
        player.sendMessage(c("  &e/clan withdraw <сумма> &8— &7Снять с казны"));
        player.sendMessage(c("  &e/clan shop &8— &7Магазин клана"));
        player.sendMessage(c("  &e/clan tp <игрок> &8— &7Запрос на Тп к соклановцу"));
        player.sendMessage(c("  &e/clan tpaccept [игрок] &8— &7Принять запрос на Тп"));
        player.sendMessage(c("  &e/clan tpdeny [игрок] &8— &7Отклонить запрос на Тп"));
        player.sendMessage(c("  &e/clan top &8— &7Топ кланов"));
        player.sendMessage(c("  &e/clan officer <игрок> &8— &7Назначить офицера"));
        player.sendMessage(c("  &e/clan kick <игрок> &8— &7Исключить участника"));
        player.sendMessage(c("  &d✨ Свечение участников &8— &7в меню клана (/clan), &cтолько для лидера"));
        player.sendMessage(c("&8&m                                        "));
        player.sendMessage(c(""));
        playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) return Collections.emptyList();

        if (args.length == 1) {
            List<String> subs = Arrays.asList("create", "invite", "accept", "leave", "disband",
                    "sethome", "home", "delhome", "pvp", "money", "invest", "withdraw",
                    "shop", "top", "tp", "tpaccept", "tpdeny", "menu", "officer", "kick", "help");
            return subs.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .sorted()
                    .toList();
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());

            if (sub.equals("tp")) {
                List<String> options = new ArrayList<>();
                options.add("accept");
                options.add("deny");
                options.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
                return options.stream()
                        .filter(o -> o.toLowerCase().startsWith(args[1].toLowerCase()))
                        .toList();
            }

            if (Arrays.asList("invite", "kick", "officer", "tpaccept", "tpdeny").contains(sub)) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                        .toList();
            }

            if ((sub.equals("home") || sub.equals("delhome")) && clan != null) {
                return new ArrayList<>(clan.getHomes().keySet()).stream()
                        .filter(h -> h.toLowerCase().startsWith(args[1].toLowerCase()))
                        .toList();
            }
        }

        if (args.length == 3) {
            String sub = args[0].toLowerCase();
            String subSub = args[1].toLowerCase();
            if (sub.equals("tp") && (subSub.equals("accept") || subSub.equals("deny"))) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase()))
                        .toList();
            }
        }

        return Collections.emptyList();
    }
}
