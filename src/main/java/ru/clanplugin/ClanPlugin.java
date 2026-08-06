package ru.clanplugin;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import ru.clanplugin.clan.ClanManager;
import ru.clanplugin.commands.ClanCommand;
import ru.clanplugin.glow.GlowManager;
import ru.clanplugin.listeners.ClanListener;
import ru.clanplugin.shop.ShopManager;
import ru.clanplugin.utils.PluginLogger;
import ru.clanplugin.clan.ClanTeleportManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ClanPlugin extends JavaPlugin {

    private static ClanPlugin instance;
    private Economy economy;
    private ClanManager clanManager;
    private ClanTeleportManager clanTeleportManager;
    private ShopManager shopManager;
    private GlowManager glowManager;
    private PluginLogger pluginLogger;

    /** Плагины, которые могут конфликтовать с работой ClanPlugin. */
    private static final Set<String> KNOWN_CONFLICTING_PLUGINS = Set.of(
            "SimpleClans", "Clans", "MCClans", "ClansBridge", "GoodClans", "ClanSystem", "ClanManager",
            "NametagEdit", "TAB", "Tab", "TabList", "Scoreboard", "UltraScoreboard",
            "AnimatedScoreboard", "Skript", "LuckPerms"
    );

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // Собственное логирование: создаём папку logs/ в папке плагина
        boolean loggingEnabled = getConfig().getBoolean("plugin.logging-enabled", true);
        pluginLogger = new PluginLogger(this, loggingEnabled);
        pluginLogger.startSession();
        // Собираем необработанные исключения из фоновых потоков в errors.log
        pluginLogger.installGlobalErrorHandler();

        try {
            enableInternal();
            pluginLogger.section("");
            pluginLogger.section(PluginLogger.SEPARATOR);
            pluginLogger.section("  Итог запуска: загружено успешно. Ошибок: "
                    + pluginLogger.getErrorCount() + ", предупреждений: "
                    + pluginLogger.getWarningCount() + ".");
            pluginLogger.section(PluginLogger.SEPARATOR);
            getLogger().info("§a§l⚔ ClanPlugin §asuccessfully enabled! "
                    + "Лог запуска: logs/latest.log (в папке плагина).");
        } catch (Throwable t) {
            pluginLogger.logException("Критическая ошибка при включении плагина", t);
            pluginLogger.error("Плагин не смог корректно запуститься — проверьте logs/errors.log.");
            getLogger().severe("§c§l⚔ ClanPlugin §cenable failed! См. logs/errors.log");
        }
    }

    private void enableInternal() {
        // ---- Диагностика окружения (записывается в logs/latest.log) ----
        writeStartupDiagnostics();

        // Setup economy
        if (!setupEconomy()) {
            pluginLogger.error("Vault не найден или нет экономического плагина! "
                    + "Экономические функции отключены.");
            pluginLogger.warn("Установите Vault и любой экономический плагин "
                    + "(EssentialsX, CMI, CoinsEngine и т.п.) для экономики.");
        }

        // Initialize managers
        clanManager = new ClanManager(this);
        clanTeleportManager = new ClanTeleportManager();
        glowManager = new GlowManager(this);
        shopManager = new ShopManager(this);

        // Register commands
        ClanCommand clanCmd = new ClanCommand(this);
        getCommand("clan").setExecutor(clanCmd);
        getCommand("clan").setTabCompleter(clanCmd);

        // Register listeners
        getServer().getPluginManager().registerEvents(new ClanListener(this), this);

        // Start shop rotation task
        shopManager.startRotationTask();

        // Initialize private clan glow (per-viewer packets + personal scoreboards)
        glowManager.enable();

        // ---- Повторная проверка интеграций после инициализации ----
        writeIntegrationReport();

        getLogger().info("§eClan system ready. Powered by ClanDev.");
    }

    @Override
    public void onDisable() {
        try {
            if (glowManager != null) {
                glowManager.shutdown();
            }
            if (clanManager != null) {
                clanManager.saveAll();
            }
            if (pluginLogger != null) {
                pluginLogger.section("Плагин выключен. Данные сохранены.");
                pluginLogger.uninstallGlobalErrorHandler();
            }
        } catch (Throwable t) {
            if (pluginLogger != null) {
                pluginLogger.logException("Ошибка при выключении плагина", t);
            }
        }
        getLogger().info("§c§l⚔ ClanPlugin §cdisabled. All data saved.");
    }

    /**
     * Пишет в logs/latest.log общую диагностику: версии, зависимости, подключения
     * к вспомогательным плагинам и найденные конфликтные плагины.
     */
    private void writeStartupDiagnostics() {
        PluginDescriptionFile desc = getDescription();

        pluginLogger.header("Диагностика запуска");
        pluginLogger.section("  Версия плагина ....... : " + desc.getVersion());
        pluginLogger.section("  API версия ........... : " + desc.getAPIVersion());
        pluginLogger.section("  Сервер (Bukkit) ...... : " + Bukkit.getVersion());
        pluginLogger.section("  Версия (API) ......... : " + Bukkit.getBukkitVersion());
        pluginLogger.section("  Майнкрафт ............ : " + Bukkit.getMinecraftVersion());
        pluginLogger.section("  Java .................. : " + System.getProperty("java.version")
                + " (" + System.getProperty("java.vendor") + ")");
        pluginLogger.section("  Папка плагина ......... : " + getDataFolder().getAbsolutePath());
        pluginLogger.section("");

        // Проверка конфликтных плагинов по командам
        List<String> commandConflicts = new ArrayList<>();
        for (Plugin p : getServer().getPluginManager().getPlugins()) {
            if (p.equals(this)) continue;
            Map<String, Map<String, Object>> commands = p.getDescription().getCommands();
            if (commands != null) {
                for (String cmd : new String[]{"clan", "c"}) {
                    if (commands.containsKey(cmd)) {
                        commandConflicts.add("'" + p.getName() + "' (v" + p.getDescription().getVersion()
                                + ") уже регистрирует команду /" + cmd);
                    }
                }
            }
        }

        // Проверка известных конфликтных плагинов по имени
        List<String> nameConflicts = new ArrayList<>();
        for (Plugin p : getServer().getPluginManager().getPlugins()) {
            if (p.equals(this)) continue;
            if (KNOWN_CONFLICTING_PLUGINS.contains(p.getName())) {
                nameConflicts.add("'" + p.getName() + "' (v" + p.getDescription().getVersion() + ")");
            }
        }

        pluginLogger.section("  Конфликты по командам .. : "
                + (commandConflicts.isEmpty() ? "не обнаружены"
                : String.join("; ", commandConflicts)));
        if (!commandConflicts.isEmpty()) {
            for (String c : commandConflicts) {
                pluginLogger.warn("Конфликт команды: " + c
                        + " — /clan может работать некорректно.");
            }
        }

        pluginLogger.section("  Известные конфликтные плагины : "
                + (nameConflicts.isEmpty() ? "не обнаружены" : String.join(", ", nameConflicts)));
        if (!nameConflicts.isEmpty()) {
            pluginLogger.warn("Обнаружены плагины, которые могут мешать работе ClanPlugin: "
                    + String.join(", ", nameConflicts));
            pluginLogger.warn("Совет: отключите их, если заметите конфликт команд или скорбордов.");
        }

        pluginLogger.section("  Всего загружено плагинов .. : "
                + getServer().getPluginManager().getPlugins().length);
        pluginLogger.section("");
    }

    /** Пишет итоговый отчёт по подключению к вспомогательным плагинам. */
    private void writeIntegrationReport() {
        pluginLogger.header("Подключение к вспомогательным плагинам");

        // ---- Vault / экономика ----
        Plugin vault = getServer().getPluginManager().getPlugin("Vault");
        if (vault == null) {
            pluginLogger.error("Vault НЕ найден — экономика недоступна.");
        } else if (hasEconomy()) {
            pluginLogger.info("Vault найден (v" + vault.getDescription().getVersion() + "), "
                    + "экономика подключена: " + economy.getName());
        } else {
            pluginLogger.warn("Vault найден (v" + vault.getDescription().getVersion() + "), "
                    + "но экономический провайдер не зарегистрирован — экономика отключена.");
        }

        // ---- ProtocolLib / приватное свечение ----
        Plugin protocolLib = getServer().getPluginManager().getPlugin("ProtocolLib");
        if (protocolLib == null) {
            pluginLogger.warn("ProtocolLib НЕ найден — приватное свечение (контур) отключено. "
                    + "Цвета никнеймов продолжают работать.");
        } else if (glowManager != null && glowManager.isPacketGlowActive()) {
            pluginLogger.info("ProtocolLib найден (v" + protocolLib.getDescription().getVersion()
                    + "), приватное свечение кланов активно.");
        } else {
            pluginLogger.warn("ProtocolLib найден (v" + protocolLib.getDescription().getVersion()
                    + "), но подключение не удалось — свечение отключено.");
        }
        pluginLogger.section("");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    public static ClanPlugin getInstance() {
        return instance;
    }

    public Economy getEconomy() {
        return economy;
    }

    public ClanManager getClanManager() {
        return clanManager;
    }

    public ClanTeleportManager getClanTeleportManager() {
        return clanTeleportManager;
    }
    public ShopManager getShopManager() {
        return shopManager;
    }

    public GlowManager getGlowManager() {
        return glowManager;
    }

    public PluginLogger getPluginLogger() {
        return pluginLogger;
    }

    public boolean hasEconomy() {
        return economy != null;
    }

    public String getPrefix() {
        return getConfig().getString("plugin.prefix", "&8[&6⚔ &bClan&6⚔&8] ");
    }
}