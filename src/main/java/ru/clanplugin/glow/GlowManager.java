package ru.clanplugin.glow;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import ru.clanplugin.ClanPlugin;
import ru.clanplugin.clan.Clan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Управляет приватным свечением участников клана.
 *
 * Принцип работы:
 * 1) Светимость (контур) подменяется на пакетном уровне (см. {@link ProtocolGlowBridge}):
 *    бит свечения в метаданных энтити подставляется ТОЛЬКО зрителям из того же клана,
 *    а для всех посторонних принудительно снимается. Реальный флаг {@code setGlowing}
 *    на сервере не выставляется вовсе, поэтому посторонние игроки не видят свечение никогда.
 * 2) Цвет свечения (и никнейма) задаётся командами (Team) в ПЕРСОНАЛЬНОМ скорборде
 *    каждого зрителя, что позволяет давать каждому участнику свой цвет.
 *
 * Без ProtocolLib пакетный контур отключается целиком (приватность не нарушается,
 * никто свечения не видит), а персональные цвета никнеймов продолжают работать.
 */
public class GlowManager {

    /** Префикс внутренних команд в персональных скорбордах. */
    private static final String TEAM_PREFIX = "cg";

    /** Префикс команд старых версий плагина в главном скорборде (чистим при старте). */
    private static final String LEGACY_TEAM_PREFIX = "clan_";

    private final ClanPlugin plugin;
    private ProtocolGlowBridge bridge;

    /** Персональный скорборд каждого зрителя (только main thread). */
    private final Map<UUID, Scoreboard> boards = new HashMap<>();

    // ---- Кэши: пишутся на главном потоке, читаются из пакетного потока ----
    private final Map<UUID, String> clanByPlayer = new ConcurrentHashMap<>();   // uuid -> clanId
    private final Map<UUID, String> colorByPlayer = new ConcurrentHashMap<>();  // uuid -> цвет (или NONE)
    private final Map<Integer, UUID> playerByEntityId = new ConcurrentHashMap<>();

    private int periodicTaskId = -1;

    public GlowManager(ClanPlugin plugin) {
        this.plugin = plugin;
    }

    // ===================== Lifecycle =====================

    public void enable() {
        // Удаляем команды прошлых версий из главного скорборда
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager != null) {
            for (Team team : new ArrayList<>(manager.getMainScoreboard().getTeams())) {
                if (team.getName().startsWith(LEGACY_TEAM_PREFIX)) {
                    team.unregister();
                }
            }
        }

        // На случай /reload: снимаем реальное свечение с участников загруженных кланов
        for (Clan clan : plugin.getClanManager().getAllClans()) {
            for (Player member : clan.getOnlineMembers()) {
                member.setGlowing(false);
            }
        }

        bridge = ProtocolGlowBridge.create(plugin, new ProtocolGlowBridge.Handler() {
            @Override
            public void onEntityMetadataSending(Player viewer, int entityId, ProtocolGlowBridge.MetadataHandle meta) {
                UUID target = playerByEntityId.get(entityId);
                if (target == null) return;
                String clanId = clanByPlayer.get(target);
                if (clanId == null) return; // не участник клана — не трогаем пакет
                // Зрителю из клана — свечение ВКЛ (если у цели не выключено), постороннему — ВЫКЛ
                meta.setGlowBit(shouldGlow(viewer.getUniqueId(), target));
            }

            @Override
            public void onEntitySpawnSending(Player viewer, int entityId) {
                UUID targetUuid = playerByEntityId.get(entityId);
                if (targetUuid == null) return;
                // Клиент сбрасывает метаданные при спавне энтити — повторно применим свечение
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    Player v = Bukkit.getPlayer(viewer.getUniqueId());
                    Player t = Bukkit.getPlayer(targetUuid);
                    if (v != null && t != null) {
                        pushGlowState(v, t);
                    }
                }, 2L);
            }
        });

        if (bridge == null) {
            plugin.getLogger().warning("===================================================");
            plugin.getLogger().warning(" ProtocolLib не установлен!");
            plugin.getLogger().warning(" Приватное свечение участников (контур) отключено.");
            plugin.getLogger().warning(" Цвета никнеймов в табе/над головой работают.");
            plugin.getLogger().warning(" Установите ProtocolLib, чтобы включить свечение.");
            plugin.getLogger().warning("===================================================");
        }

        // Первичная инициализация (после полной загрузки сервера)
        Bukkit.getScheduler().runTaskLater(plugin, this::refreshAll, 5L);
        // Периодическая страховочная синхронизация (раз в минуту)
        periodicTaskId = Bukkit.getScheduler().runTaskTimer(plugin, this::refreshAll, 1200L, 1200L).getTaskId();
    }

    public void shutdown() {
        if (periodicTaskId != -1) {
            Bukkit.getScheduler().cancelTask(periodicTaskId);
            periodicTaskId = -1;
        }
        if (bridge != null) {
            bridge.shutdown();
            bridge = null;
        }

        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager != null) {
            for (Map.Entry<UUID, Scoreboard> entry : boards.entrySet()) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null && player.getScoreboard().equals(entry.getValue())) {
                    // снимаем игрока с свечения и возвращаем главный скорборд
                    player.setScoreboard(manager.getMainScoreboard());
                }
            }
        }
        boards.clear();
        clanByPlayer.clear();
        colorByPlayer.clear();
        playerByEntityId.clear();
    }

    // ===================== События из слушателя =====================

    public void handleJoin(Player player) {
        delayRefresh(10L);
    }

    public void handleQuit(Player player) {
        UUID uuid = player.getUniqueId();
        boards.remove(uuid);
        clanByPlayer.remove(uuid);
        colorByPlayer.remove(uuid);
        playerByEntityId.values().remove(uuid);
        delayRefresh(5L);
    }

    /** Плановое обновление через несколько тиков (респавн, смена мира, вход и т.п.). */
    public void handleRefreshSoon() {
        delayRefresh(5L);
    }

    private void delayRefresh(long delay) {
        Bukkit.getScheduler().runTaskLater(plugin, this::refreshAll, delay);
    }

    // ===================== Ядро =====================

    /** Полная пересинхронизация: кэши, скорборды-цвета и состояние свечения. Вызывать на главном потоке. */
    public void refreshAll() {
        rebuildCaches();
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            refreshBoard(viewer);
        }
        // Повторно разослать состояние свечения всем парам (зритель -> цель)
        for (Player target : Bukkit.getOnlinePlayers()) {
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                pushGlowState(viewer, target);
            }
        }
    }

    private void rebuildCaches() {
        Set<UUID> online = new HashSet<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            online.add(uuid);
            Clan clan = plugin.getClanManager().getPlayerClan(uuid);
            if (clan != null) {
                clanByPlayer.put(uuid, clan.getId());
                colorByPlayer.put(uuid, clan.getGlowColor(uuid));
            } else {
                clanByPlayer.remove(uuid);
                colorByPlayer.remove(uuid);
            }
            playerByEntityId.put(player.getEntityId(), uuid);
        }
        clanByPlayer.keySet().removeIf(uuid -> !online.contains(uuid));
        colorByPlayer.keySet().removeIf(uuid -> !online.contains(uuid));
        playerByEntityId.values().removeIf(uuid -> !online.contains(uuid));
    }

    /** Должен ли {@code viewer} видеть свечение {@code target}: тот же клан и у цели не выключено свечение. */
    private boolean shouldGlow(UUID viewer, UUID target) {
        String clanId = clanByPlayer.get(target);
        if (clanId == null || !clanId.equals(clanByPlayer.get(viewer))) return false;
        String color = colorByPlayer.get(target);
        return color != null && !color.equalsIgnoreCase(Clan.GLOW_NONE);
    }

    // ===================== Цвета: персональные скорборды =====================

    private void refreshBoard(Player viewer) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;

        Scoreboard board = boards.get(viewer.getUniqueId());
        String viewerClanId = clanByPlayer.get(viewer.getUniqueId());

        if (board == null) {
            // Посторонним (без клана) персональный скорборд вообще не нужен
            if (viewerClanId == null) return;
            // Ставим свой скорборд только если у игрока ещё дефолтный,
            // чтобы не конфликтовать со сторонними scoreboard-плагинами
            if (!viewer.getScoreboard().equals(manager.getMainScoreboard())) return;
            board = manager.getNewScoreboard();
            viewer.setScoreboard(board);
            boards.put(viewer.getUniqueId(), board);
        } else if (!viewer.getScoreboard().equals(board)) {
            // Сторонний плагин перехватил скорборд — не вмешиваемся
            return;
        }

        Set<String> desiredTeams = new HashSet<>();
        if (viewerClanId != null) {
            Clan clan = plugin.getClanManager().getClan(viewerClanId);
            if (clan != null) {
                for (UUID memberUuid : clan.getMembers()) {
                    Player member = Bukkit.getPlayer(memberUuid);
                    if (member == null) continue;
                    String colorName = clan.getGlowColor(memberUuid);
                    if (colorName == null || colorName.equalsIgnoreCase(Clan.GLOW_NONE)) continue;

                    String teamName = teamNameFor(clan.getId(), memberUuid);
                    Team team = board.getTeam(teamName);
                    if (team == null) {
                        team = board.registerNewTeam(teamName);
                    }
                    ChatColor color = parseColor(colorName);
                    if (team.getColor() != color) {
                        team.setColor(color);
                    }
                    if (!team.hasEntry(member.getName())) {
                        team.addEntry(member.getName());
                    }
                    desiredTeams.add(teamName);
                }
            }
        }

        // Удалить наши команды, которые больше не нужны (игрок вышел из клана/оффлайн/цвет снят)
        for (Team team : new ArrayList<>(board.getTeams())) {
            if (team.getName().startsWith(TEAM_PREFIX) && !desiredTeams.contains(team.getName())) {
                team.unregister();
            }
        }

        // Цветов больше нет (например, вышел из клана) — возвращаем главный скорборд
        if (desiredTeams.isEmpty()) {
            viewer.setScoreboard(manager.getMainScoreboard());
            boards.remove(viewer.getUniqueId());
        }
    }

    private static String teamNameFor(String clanId, UUID memberUuid) {
        int hash = 31 * clanId.hashCode() ^ memberUuid.hashCode();
        return TEAM_PREFIX + Integer.toHexString(hash); // <= 16 символов
    }

    private static ChatColor parseColor(String colorName) {
        try {
            return ChatColor.valueOf(colorName.toUpperCase());
        } catch (Exception e) {
            return ChatColor.YELLOW;
        }
    }

    // ===================== Контур: пакетная подмена =====================

    /** Отправить зрителю актуальное состояние свечения цели. */
    private void pushGlowState(Player viewer, Player target) {
        if (bridge == null || !bridge.isActive()) return;

        byte flags = computeBaseFlags(target);
        if (target.isGlowing()) flags |= 0x40; // «честное» свечение из других источников

        if (clanByPlayer.get(target.getUniqueId()) != null) {
            // Участник клана: свечение решаем сами — видно только соклановцам
            flags &= ~0x40;
            if (shouldGlow(viewer.getUniqueId(), target.getUniqueId())) {
                flags |= 0x40;
            }
        }
        // Пакет шлём всегда (даже когда видимости нет): это снимает устаревшее
        // свечение на клиенте зрителя, если цель только что покинула клан.
        bridge.sendGlowFlags(viewer, target.getEntityId(), flags);
    }

    /** Текущий байт общих флагов сущности по состоянию Bukkit (бит 0x40 свечения сюда НЕ входит). */
    private static byte computeBaseFlags(Player player) {
        byte flags = 0;
        if (player.getFireTicks() > 0 || player.isVisualFire()) flags |= 0x01;
        if (player.isSneaking()) flags |= 0x02;
        if (player.isSprinting()) flags |= 0x08;
        if (player.isSwimming()) flags |= 0x10;
        if (player.isInvisible()) flags |= 0x20;
        if (player.isGliding()) flags |= 0x80;
        return flags;
    }

    /** Активна ли пакетная часть (ProtocolLib). */
    public boolean isPacketGlowActive() {
        return bridge != null && bridge.isActive();
    }
}