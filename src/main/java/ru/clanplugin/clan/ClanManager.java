package ru.clanplugin.clan;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.clanplugin.ClanPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ClanManager {

    private final ClanPlugin plugin;
    private final Map<String, Clan> clans;       // id -> Clan
    private final Map<UUID, String> playerClan;   // player UUID -> clan id
    private File dataFile;
    private FileConfiguration data;

    public ClanManager(ClanPlugin plugin) {
        this.plugin = plugin;
        this.clans = new HashMap<>();
        this.playerClan = new HashMap<>();
        loadData();
    }

    // ---- Data Persistence ----

    private void loadData() {
        dataFile = new File(plugin.getDataFolder(), "clans.yml");
        if (!dataFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try { dataFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        data = YamlConfiguration.loadConfiguration(dataFile);

        ConfigurationSection clansSection = data.getConfigurationSection("clans");
        if (clansSection == null) return;

        for (String id : clansSection.getKeys(false)) {
            ConfigurationSection cs = clansSection.getConfigurationSection(id);
            if (cs == null) continue;

            String name = cs.getString("name", id);
            UUID owner = UUID.fromString(cs.getString("owner", UUID.randomUUID().toString()));
            Clan clan = new Clan(id, name, owner);

            clan.setBalance(cs.getDouble("balance", 0));
            clan.setPvpEnabled(cs.getBoolean("pvp", false));
            clan.setShopLevel(cs.getInt("shopLevel", 1));

            // Load members
            List<String> memberList = cs.getStringList("members");
            for (String m : memberList) {
                try { clan.addMember(UUID.fromString(m)); } catch (Exception ignored) {}
            }

            // Load officers
            List<String> officerList = cs.getStringList("officers");
            for (String o : officerList) {
                try { clan.addOfficer(UUID.fromString(o)); } catch (Exception ignored) {}
            }

            // Load officer permissions
            ConfigurationSection permsCs = cs.getConfigurationSection("officerPerms");
            if (permsCs != null) {
                for (String key : permsCs.getKeys(false)) {
                    clan.setOfficerPerm(key, permsCs.getBoolean(key, false));
                }
            }

            // Load homes
            ConfigurationSection homesCs = cs.getConfigurationSection("homes");
            if (homesCs != null) {
                for (String homeName : homesCs.getKeys(false)) {
                    ConfigurationSection hcs = homesCs.getConfigurationSection(homeName);
                    if (hcs == null) continue;
                    String worldName = hcs.getString("world");
                    if (worldName == null) continue;
                    World world = Bukkit.getWorld(worldName);
                    if (world == null) continue;
                    double x = hcs.getDouble("x");
                    double y = hcs.getDouble("y");
                    double z = hcs.getDouble("z");
                    float yaw = (float) hcs.getDouble("yaw");
                    float pitch = (float) hcs.getDouble("pitch");
                    clan.setHome(homeName, new Location(world, x, y, z, yaw, pitch));
                }
            }

            // Load purchased features
            List<String> features = cs.getStringList("features");
            for (String f : features) clan.addFeature(f);

            // Load daily purchases
            ConfigurationSection dailyCs = cs.getConfigurationSection("dailyPurchases");
            if (dailyCs != null) {
                for (String itemId : dailyCs.getKeys(false)) {
                    clan.getDailyPurchases().put(itemId, dailyCs.getInt(itemId, 0));
                }
            }
            clan.setLastDailyReset(cs.getLong("lastDailyReset", System.currentTimeMillis()));

            clan.setGlowColor(cs.getString("glowColor", "YELLOW"));

            // Load per-member glow colors
            ConfigurationSection glowCs = cs.getConfigurationSection("memberGlowColors");
            if (glowCs != null) {
                for (String uuidStr : glowCs.getKeys(false)) {
                    try {
                        clan.setGlowColor(UUID.fromString(uuidStr), glowCs.getString(uuidStr));
                    } catch (Exception ignored) {}
                }
            }

            clans.put(id, clan);

            // Map members to clan
            for (UUID memberUuid : clan.getMembers()) {
                playerClan.put(memberUuid, id);
            }
        }

        plugin.getLogger().info("Loaded " + clans.size() + " clans.");
    }

    public void saveAll() {
        data.set("clans", null);
        for (Clan clan : clans.values()) {
            saveClan(clan);
        }
        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save clans.yml!");
            e.printStackTrace();
        }
    }

    private void saveClan(Clan clan) {
        String path = "clans." + clan.getId();
        data.set(path + ".name", clan.getName());
        data.set(path + ".owner", clan.getOwner().toString());
        data.set(path + ".balance", clan.getBalance());
        data.set(path + ".pvp", clan.isPvpEnabled());
        data.set(path + ".shopLevel", clan.getShopLevel());

        // Members
        List<String> memberList = clan.getMembers().stream()
                .map(UUID::toString).collect(Collectors.toList());
        data.set(path + ".members", memberList);

        // Officers
        List<String> officerList = clan.getOfficers().stream()
                .map(UUID::toString).collect(Collectors.toList());
        data.set(path + ".officers", officerList);

        // Officer perms
        for (Map.Entry<String, Boolean> entry : clan.getOfficerPerms().entrySet()) {
            data.set(path + ".officerPerms." + entry.getKey(), entry.getValue());
        }

        // Homes
        for (Map.Entry<String, Location> entry : clan.getHomes().entrySet()) {
            Location loc = entry.getValue();
            if (loc.getWorld() == null) continue;
            String homePath = path + ".homes." + entry.getKey();
            data.set(homePath + ".world", loc.getWorld().getName());
            data.set(homePath + ".x", loc.getX());
            data.set(homePath + ".y", loc.getY());
            data.set(homePath + ".z", loc.getZ());
            data.set(homePath + ".yaw", (double) loc.getYaw());
            data.set(homePath + ".pitch", (double) loc.getPitch());
        }

        // Features
        data.set(path + ".features", new ArrayList<>(clan.getPurchasedFeatures()));

        // Daily purchases
        for (Map.Entry<String, Integer> entry : clan.getDailyPurchases().entrySet()) {
            data.set(path + ".dailyPurchases." + entry.getKey(), entry.getValue());
        }
        data.set(path + ".lastDailyReset", clan.getLastDailyReset());
        data.set(path + ".glowColor", clan.getGlowColor());

        // Per-member glow colors
        for (Map.Entry<UUID, String> entry : clan.getMemberGlowColors().entrySet()) {
            data.set(path + ".memberGlowColors." + entry.getKey(), entry.getValue());
        }
    }

    // ---- Clan Operations ----

    public Clan createClan(String name, UUID owner) {
        String id = name.toLowerCase().replaceAll("[^a-z0-9]", "");
        if (id.isEmpty()) id = "clan" + System.currentTimeMillis();

        Clan clan = new Clan(id, name, owner);
        clans.put(id, clan);
        playerClan.put(owner, id);
        saveImmediately();
        // schedule glow refresh
        plugin.getServer().getScheduler().runTask(plugin, () -> plugin.getGlowManager().refreshAll());
        return clan;
    }

    public boolean clanNameExists(String name) {
        return clans.values().stream()
                .anyMatch(c -> c.getName().equalsIgnoreCase(name));
    }

    public boolean clanIdExists(String id) {
        return clans.containsKey(id);
    }

    public Clan getClan(String id) {
        return clans.get(id);
    }

    public Clan getClanByName(String name) {
        return clans.values().stream()
                .filter(c -> c.getName().equalsIgnoreCase(name))
                .findFirst().orElse(null);
    }

    public Clan getPlayerClan(UUID playerUuid) {
        String id = playerClan.get(playerUuid);
        if (id == null) return null;
        return clans.get(id);
    }

    public boolean isInClan(UUID playerUuid) {
        return playerClan.containsKey(playerUuid);
    }

    public void joinClan(UUID playerUuid, Clan clan) {
        clan.addMember(playerUuid);
        playerClan.put(playerUuid, clan.getId());
        saveImmediately();
        // Refresh glow for new member
        plugin.getServer().getScheduler().runTask(plugin, () -> plugin.getGlowManager().refreshAll());
    }

    public void leaveClan(UUID playerUuid) {
        String clanId = playerClan.remove(playerUuid);
        if (clanId == null) return;
        Clan clan = clans.get(clanId);
        if (clan != null) {
            clan.removeMember(playerUuid);
            if (clan.getMemberCount() == 0) {
                disbandClan(clan);
            } else {
                // refresh glow without the leaving player
                plugin.getServer().getScheduler().runTask(plugin, () -> plugin.getGlowManager().refreshAll());
                saveImmediately();
            }
        }
    }

    public void disbandClan(Clan clan) {
        for (UUID member : new HashSet<>(clan.getMembers())) {
            playerClan.remove(member);
        }
        clans.remove(clan.getId());
        saveImmediately();
        plugin.getServer().getScheduler().runTask(plugin, () -> plugin.getGlowManager().refreshAll());
    }

    public List<Clan> getAllClans() {
        return new ArrayList<>(clans.values());
    }

    public List<Clan> getTopClansByMoney(int limit) {
        return clans.values().stream()
                .sorted((a, b) -> Double.compare(b.getBalance(), a.getBalance()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    public void saveImmediately() {
        data.set("clans", null);
        for (Clan clan : clans.values()) {
            saveClan(clan);
        }
        try {
            data.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}