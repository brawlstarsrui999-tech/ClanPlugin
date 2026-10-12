package ru.clanplugin.utils;

import org.bukkit.ChatColor;
import ru.clanplugin.ClanPlugin;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Ведёт собственные логи плагина в папке <code>logs</code> внутри папки плагина.
 *
 * <p>Создаёт два файла:
 * <ul>
 *   <li><b>latest.log</b> — полный журнал текущей сессии (диагностика запуска,
 *       предупреждения, ошибки). Перезаписывается при каждом старте сервера.</li>
 *   <li><b>errors.log</b> — накопительный сбор ВСЕХ ошибок и исключений
 *       (дозаписывается, никогда не перезаписывается).</li>
 * </ul>
 *
 * <p>Всё, что попадает в консоль, дублируется в файлы; ошибки дополнительно
 * собираются в errors.log вместе с полным стектрейсом.
 */
public class PluginLogger {

    /** Разделитель разделов для удобочитаемости файла. */
    public static final String SEPARATOR =
            "============================================================";

    private static final SimpleDateFormat TIMESTAMP =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private final ClanPlugin plugin;
    private final File logsFolder;
    private final File latestFile;
    private final File errorsFile;
    private final boolean enabled;

    /** Счётчик ошибок за текущую сессию (для итогового отчёта в логе). */
    private int errorCount;
    private int warningCount;

    public PluginLogger(ClanPlugin plugin, boolean enabled) {
        this.plugin = plugin;
        this.enabled = enabled;
        this.logsFolder = new File(plugin.getDataFolder(), "logs");
        this.latestFile = new File(logsFolder, "latest.log");
        this.errorsFile = new File(logsFolder, "errors.log");
        if (enabled) {
            try {
                if (!logsFolder.exists()) logsFolder.mkdirs();
            } catch (Exception ignored) {
                // если папку создать не удалось — пишем только в консоль
            }
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    /** Прежний глобальный обработчик исключений (восстанавливается при выключении). */
    private Thread.UncaughtExceptionHandler previousHandler;
    /** Установленный нами обработчик (для проверки перед восстановлением). */
    private Thread.UncaughtExceptionHandler installedHandler;

    /**
     * Перехватывает необработанные исключения из фоновых/асинхронных потоков
     * и пишет их в errors.log (+ в консоль), не отменяя серверный обработчик.
     */
    public void installGlobalErrorHandler() {
        if (!enabled) return;
        try {
            Thread.UncaughtExceptionHandler prev = Thread.getDefaultUncaughtExceptionHandler();
            installedHandler = (thread, throwable) -> {
                try {
                    logException("Необработанное исключение в потоке «"
                            + thread.getName() + "»", throwable);
                } finally {
                    if (prev != null) {
                        prev.uncaughtException(thread, throwable);
                    }
                }
            };
            previousHandler = prev;
            Thread.setDefaultUncaughtExceptionHandler(installedHandler);
        } catch (Throwable ignored) {
            // не должны ронять плагин, если установить обработчик не удалось
        }
    }

    /** Восстанавливает прежний глобальный обработчик при выключении плагина. */
    public void uninstallGlobalErrorHandler() {
        if (installedHandler == null) return;
        try {
            // Восстанавливаем прежний обработчик, только если он не был заменён другими плагинами
            if (Thread.getDefaultUncaughtExceptionHandler() == installedHandler
                    || Thread.getDefaultUncaughtExceptionHandler() == null) {
                Thread.setDefaultUncaughtExceptionHandler(previousHandler);
            }
        } catch (Throwable ignored) {
        } finally {
            installedHandler = null;
            previousHandler = null;
        }
    }

    public File getLogsFolder() {
        return logsFolder;
    }

    public File getLatestFile() {
        return latestFile;
    }

    public File getErrorsFile() {
        return errorsFile;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public int getWarningCount() {
        return warningCount;
    }

    /** Открывает новую сессию: пишет шапку в latest.log и пересоздаёт файл. */
    public void startSession() {
        if (!enabled) return;
        try {
            if (latestFile.exists()) latestFile.delete();
        } catch (Exception ignored) {}
        header("ClanPlugin " + plugin.getDescription().getVersion()
                + " — запуск сервера");
    }

    /** Печатает красивый раздел-заголовок. */
    public void header(String title) {
        if (!enabled) return;
        writeRaw(latestFile, "");
        writeRaw(latestFile, SEPARATOR);
        writeRaw(latestFile, "  " + title + "   [" + now() + "]");
        writeRaw(latestFile, SEPARATOR);
    }

    /** Печатает строку в разделе диагностики (без дублирования в консоль). */
    public void section(String message) {
        if (!enabled) return;
        writeRaw(latestFile, message);
    }

    /** Информационное сообщение: в latest.log + консоль. */
    public void info(String message) {
        log("INFO", message);
    }

    /** Предупреждение: в latest.log + консоль (warning). */
    public void warn(String message) {
        warningCount++;
        log("WARN", message);
    }

    /** Ошибка: в latest.log + консоль (severe) + errors.log. */
    public void error(String message) {
        errorCount++;
        log("ERROR", message);
        appendError("ERROR", message, null);
    }

    /** Полностью залогировать исключение (со стектрейсом) во все журналы. */
    public void logException(String context, Throwable throwable) {
        errorCount++;
        log("ERROR", (context == null ? "Исключение" : context)
                + " — " + throwable);
        appendError("EXCEPTION", context + " — " + throwable, throwable);
        // Стек в latest.log тоже продублируем
        StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));
        writeRaw(latestFile, sw.toString());
    }

    /** Пишет строку в файл и консоль (без errors.log). */
    private void log(String level, String message) {
        if (enabled) {
            writeRaw(latestFile, "[" + now() + "] [" + level + "] " + message);
        }
        String clean = ChatColor.stripColor(message);
        switch (level) {
            case "ERROR" -> plugin.getLogger().severe(clean);
            case "WARN" -> plugin.getLogger().warning(clean);
            default -> plugin.getLogger().info(clean);
        }
    }

    /** Дозаписывает ошибку в накопительный errors.log. */
    private void appendError(String type, String message, Throwable throwable) {
        if (!enabled) return;
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(now()).append("] [").append(type).append("] ")
                .append(ChatColor.stripColor(message)).append('\n');
        if (throwable != null) {
            StringWriter sw = new StringWriter();
            throwable.printStackTrace(new PrintWriter(sw));
            sb.append(sw).append('\n');
        }
        try {
            Files.write(errorsFile.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
            // не пишем в консоль, чтобы не зациклиться на ошибках логирования
        }
    }

    /** Пишет одну строку в указанный файл (UTF-8, дозапись). */
    private void writeRaw(File file, String line) {
        if (!enabled) return;
        try {
            String text = line + System.lineSeparator();
            Files.write(file.toPath(), text.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
            // не роняем плагин из-за проблем с файлом логов
        }
    }

    private String now() {
        return TIMESTAMP.format(new Date());
    }
}