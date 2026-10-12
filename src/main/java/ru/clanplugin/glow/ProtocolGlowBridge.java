package ru.clanplugin.glow;

import org.bukkit.entity.Player;
import ru.clanplugin.ClanPlugin;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

/**
 * Опциональная интеграция с ProtocolLib (через рефлексию, чтобы не ломать сборку без него).
 *
 * Именно этот класс обеспечивает приватность свечения: бит свечения (0x40)
 * подставляется в пакет метаданных только для зрителей из того же клана.
 * Для всех посторонних бит свечения принудительно снимается, поэтому
 * посторонние игроки не видят свечение участников клана вообще.
 *
 * Проверено против ProtocolLib 5.3.0 (Minecraft 1.20.5 - 1.21.x).
 */
final class ProtocolGlowBridge {

    /** Колбэки для GlowManager. Вызываются из пакетного потока — используйте только потокобезопасные кэши. */
    interface Handler {
        /** Сервер отправляет пакет метаданных сущности зрителю. Список DataValue можно модифицировать. */
        void onEntityMetadataSending(Player viewer, int entityId, MetadataHandle meta);

        /** Сущность (игрок) спавнится в зоне видимости зрителя. */
        void onEntitySpawnSending(Player viewer, int entityId);
    }

    /** Контролируемый доступ к списку WrappedDataValue исходящего пакета метаданных. */
    interface MetadataHandle {
        /** Принудительно выставить либо снять бит свечения (0x40) во флагах сущности. */
        void setGlowBit(boolean glow);
    }

    // Бит свечения в байте общих флагов сущности (индекс 0)
    private static final byte GLOW_BIT = 0x40;

    private final ClanPlugin plugin;
    private final Handler handler;

    private boolean active = false;
    private Object protocolManager;
    private Object packetListener;

    // ---- Reflection handles ----
    private Object entityMetadataType;
    private Object spawnEntityType;
    private Method createPacketMethod;
    private Method sendPacketMethod;
    private Method removeListenerMethod;
    private Method getPacketMethod;
    private Method getPlayerMethod;
    private Method getTypeMethod;
    private Method getIntegersMethod;
    private Method getDataValuesMethod;
    private Method modifierReadMethod;
    private Method modifierWriteMethod;
    private Method wdGetIndexMethod;
    private Method wdGetValueMethod;
    private Constructor<?> wdValueConstructor;
    private Object byteSerializer;

    private ProtocolGlowBridge(ClanPlugin plugin, Handler handler) {
        this.plugin = plugin;
        this.handler = handler;
    }

    /**
     * Попытаться создать мост. Возвращает null, если ProtocolLib отсутствует
     * или его API не совпал (в этом случае приватное свечение просто отключено).
     */
    static ProtocolGlowBridge create(ClanPlugin plugin, Handler handler) {
        ProtocolGlowBridge bridge = new ProtocolGlowBridge(plugin, handler);
        return bridge.init() ? bridge : null;
    }

    private boolean init() {
        try {
            Class<?> protocolLibraryClass = Class.forName("com.comphenix.protocol.ProtocolLibrary");
            Class<?> protocolManagerClass = Class.forName("com.comphenix.protocol.ProtocolManager");
            Class<?> packetEventClass = Class.forName("com.comphenix.protocol.events.PacketEvent");
            Class<?> packetListenerClass = Class.forName("com.comphenix.protocol.events.PacketListener");
            Class<?> whitelistClass = Class.forName("com.comphenix.protocol.events.ListeningWhitelist");
            Class<?> packetTypeClass = Class.forName("com.comphenix.protocol.PacketType");
            Class<?> serverTypesClass = Class.forName("com.comphenix.protocol.PacketType$Play$Server");
            Class<?> packetContainerClass = Class.forName("com.comphenix.protocol.events.PacketContainer");
            Class<?> structureModifierClass = Class.forName("com.comphenix.protocol.reflect.StructureModifier");
            Class<?> registryClass = Class.forName("com.comphenix.protocol.wrappers.WrappedDataWatcher$Registry");
            Class<?> serializerClass = Class.forName("com.comphenix.protocol.wrappers.WrappedDataWatcher$Serializer");
            Class<?> wrappedDataValueClass = Class.forName("com.comphenix.protocol.wrappers.WrappedDataValue");

            protocolManager = protocolLibraryClass.getMethod("getProtocolManager").invoke(null);
            if (protocolManager == null) return false;

            entityMetadataType = serverTypesClass.getField("ENTITY_METADATA").get(null);
            spawnEntityType = serverTypesClass.getField("SPAWN_ENTITY").get(null);

            createPacketMethod = protocolManagerClass.getMethod("createPacket", packetTypeClass);
            sendPacketMethod = protocolManagerClass.getMethod("sendServerPacket", Player.class, packetContainerClass, boolean.class);
            Method addListenerMethod = protocolManagerClass.getMethod("addPacketListener", packetListenerClass);
            removeListenerMethod = protocolManagerClass.getMethod("removePacketListener", packetListenerClass);

            getPacketMethod = packetEventClass.getMethod("getPacket");
            getPlayerMethod = packetEventClass.getMethod("getPlayer");
            getTypeMethod = packetContainerClass.getMethod("getType");
            getIntegersMethod = packetContainerClass.getMethod("getIntegers");
            getDataValuesMethod = packetContainerClass.getMethod("getDataValueCollectionModifier");
            modifierReadMethod = structureModifierClass.getMethod("read", int.class);
            modifierWriteMethod = structureModifierClass.getMethod("write", int.class, Object.class);
            wdGetIndexMethod = wrappedDataValueClass.getMethod("getIndex");
            wdGetValueMethod = wrappedDataValueClass.getMethod("getValue");
            wdValueConstructor = wrappedDataValueClass.getConstructor(int.class, serializerClass, Object.class);
            byteSerializer = registryClass.getMethod("get", Class.class).invoke(null, Byte.class);

            // Whitelist отправки: ENTITY_METADATA + SPAWN_ENTITY
            Object builder = whitelistClass.getMethod("newBuilder").invoke(null);
            builder = builder.getClass().getMethod("normal").invoke(builder);
            Object typesArray = Array.newInstance(packetTypeClass, 2);
            Array.set(typesArray, 0, entityMetadataType);
            Array.set(typesArray, 1, spawnEntityType);
            Method typesMethod = null;
            for (Method m : builder.getClass().getMethods()) {
                if (m.getName().equals("types") && m.getParameterCount() == 1 && m.getParameterTypes()[0].isArray()) {
                    typesMethod = m;
                    break;
                }
            }
            if (typesMethod == null) return false;
            builder = typesMethod.invoke(builder, typesArray);
            Object sendingWhitelist = builder.getClass().getMethod("build").invoke(builder);
            Object emptyWhitelist = whitelistClass.getField("EMPTY_WHITELIST").get(null);

            // Прокси-реализация PacketListener (интерфейс ProtocolLib)
            packetListener = Proxy.newProxyInstance(
                    packetListenerClass.getClassLoader(),
                    new Class<?>[]{packetListenerClass},
                    (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "onPacketSending":
                                onPacketSending(args[0]);
                                return null;
                            case "onPacketReceiving":
                                return null; // не используем
                            case "getSendingWhitelist":
                                return sendingWhitelist;
                            case "getReceivingWhitelist":
                                return emptyWhitelist;
                            case "getPlugin":
                                return plugin;
                            case "toString":
                                return "ClanPluginGlowListener";
                            case "hashCode":
                                return System.identityHashCode(proxy);
                            case "equals":
                                return proxy == args[0];
                            default:
                                return null;
                        }
                    });

            addListenerMethod.invoke(protocolManager, packetListener);
            active = true;
            plugin.getLogger().info("ProtocolLib найден: приватное свечение кланов активно.");
            return true;
        } catch (Throwable t) {
            plugin.getLogger().warning("Не удалось подключиться к ProtocolLib: " + t);
            return false;
        }
    }

    boolean isActive() {
        return active;
    }

    void shutdown() {
        active = false;
        if (protocolManager != null && packetListener != null && removeListenerMethod != null) {
            try {
                removeListenerMethod.invoke(protocolManager, packetListener);
            } catch (Throwable ignored) {}
        }
    }

    // ===================== Входящий поток (server -> client) =====================

    private void onPacketSending(Object event) {
        try {
            Object packet = getPacketMethod.invoke(event);
            Object type = getTypeMethod.invoke(packet);
            Player viewer = (Player) getPlayerMethod.invoke(event);
            if (viewer == null) return;

            Integer entityId = (Integer) modifierReadMethod.invoke(getIntegersMethod.invoke(packet), 0);
            if (entityId == null) return;

            if (entityMetadataType.equals(type)) {
                MetadataHandle meta = buildMetadataHandle(packet);
                handler.onEntityMetadataSending(viewer, entityId, meta);
            } else if (spawnEntityType.equals(type)) {
                handler.onEntitySpawnSending(viewer, entityId);
            }
        } catch (Throwable ignored) {
            // никогда не роняем пакетный поток
        }
    }

    private MetadataHandle buildMetadataHandle(Object packet) {
        return glow -> {
            try {
                Object listModifier = getDataValuesMethod.invoke(packet);
                @SuppressWarnings("unchecked")
                List<Object> values = (List<Object>) modifierReadMethod.invoke(listModifier, 0);
                if (values == null || values.isEmpty()) return;

                List<Object> updated = null;
                for (int i = 0; i < values.size(); i++) {
                    Object value = values.get(i);
                    int index = (Integer) wdGetIndexMethod.invoke(value);
                    if (index != 0) continue;
                    Object raw = wdGetValueMethod.invoke(value);
                    if (!(raw instanceof Byte)) continue;
                    byte current = (Byte) raw;
                    byte flags = glow ? (byte) (current | GLOW_BIT) : (byte) (current & ~GLOW_BIT);
                    if (flags == current) continue;
                    if (updated == null) updated = new ArrayList<>(values);
                    updated.set(i, wdValueConstructor.newInstance(0, byteSerializer, flags));
                }
                if (updated != null) {
                    modifierWriteMethod.invoke(listModifier, 0, updated);
                }
            } catch (Throwable ignored) {}
        };
    }

    // ===================== Исходящая подмена состояния =====================

    /**
     * Отправить зрителю синтетический пакет метаданных с новым байтом флагов сущности.
     * Используется для мгновенного включения/выключения свечения без изменения
     * реального состояния энтити на сервере.
     */
    void sendGlowFlags(Player viewer, int entityId, byte flags) {
        if (!active || viewer == null || !viewer.isOnline()) return;
        try {
            Object packet = createPacketMethod.invoke(protocolManager, entityMetadataType);
            modifierWriteMethod.invoke(getIntegersMethod.invoke(packet), 0, entityId);
            List<Object> values = new ArrayList<>();
            values.add(wdValueConstructor.newInstance(0, byteSerializer, flags));
            modifierWriteMethod.invoke(getDataValuesMethod.invoke(packet), 0, values);
            sendPacketMethod.invoke(protocolManager, viewer, packet, false);
        } catch (Throwable t) {
            plugin.getLogger().fine("Не удалось отправить пакет свечения: " + t.getMessage());
        }
    }
}