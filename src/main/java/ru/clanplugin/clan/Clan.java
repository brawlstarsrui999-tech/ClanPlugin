package ru.clanplugin.clan;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;

public class Clan {

    private final String id;
    private String name;
    private UUID owner;
    private final Set<UUID> members;
    private final Set<UUID> officers; // members with special permissions
    private final Map<String, Boolean> officerPerms; // what officers can do
    private double balance;
    private final long createdAt;
    private boolean pvpEnabled;
    private final Map<String, Location> homes;
    private int shopLevel; // 1-4
    private final Set<String> purchasedFeatures; // clan-tp, buff-strength-1, etc.
    private final Set<UUID> pendingInvites;

    // Daily purchase limits: itemId -> count purchased today (per clan)
    private final Map<String, Integer> dailyPurchases;
    private long lastDailyReset; // millis at last reset (for day check)
    private String glowColor = "YELLOW"; // default glow color for clan members (Bukkit ChatColor name)
    // Per-member glow color overrides: member uuid -> ChatColor name or GLOW_NONE.
    // If a member has no override, the clan default glowColor is used.
    private final Map<UUID, String> memberGlowColors = new HashMap<>();

    // Special value meaning "glow disabled" for a member
    public static final String GLOW_NONE = "NONE";

    // Officer permissions keys
    public static final String PERM_PVP = "pvp";
    public static final String PERM_SETHOME = "sethome";
    public static final String PERM_UPGRADE_SHOP = "upgrade_shop";
    public static final String PERM_UPGRADE_FEATURES = "upgrade_features";

    public Clan(String id, String name, UUID owner) {
        this.id = id;
        this.name = name;
        this.owner = owner;
        this.members = new HashSet<>();
        this.officers = new HashSet<>();
        this.officerPerms = new HashMap<>();
        this.balance = 0;
        this.createdAt = System.currentTimeMillis();
        this.pvpEnabled = false;
        this.homes = new HashMap<>();
        this.shopLevel = 1;
        this.purchasedFeatures = new HashSet<>();
        this.pendingInvites = new HashSet<>();
        this.dailyPurchases = new HashMap<>();
        this.lastDailyReset = System.currentTimeMillis();
        this.members.add(owner);

        // Default officer permissions - all disabled
        officerPerms.put(PERM_PVP, false);
        officerPerms.put(PERM_SETHOME, false);
        officerPerms.put(PERM_UPGRADE_SHOP, false);
        officerPerms.put(PERM_UPGRADE_FEATURES, false);
    }

    // ---- Getters & Setters ----

    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public UUID getOwner() { return owner; }
    public void setOwner(UUID owner) { this.owner = owner; }
    public Set<UUID> getMembers() { return members; }
    public Set<UUID> getOfficers() { return officers; }
    public Map<String, Boolean> getOfficerPerms() { return officerPerms; }
    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }
    public long getCreatedAt() { return createdAt; }
    public boolean isPvpEnabled() { return pvpEnabled; }
    public void setPvpEnabled(boolean pvpEnabled) { this.pvpEnabled = pvpEnabled; }
    public Map<String, Location> getHomes() { return homes; }
    public int getShopLevel() { return shopLevel; }
    public void setShopLevel(int shopLevel) { this.shopLevel = shopLevel; }
    public Set<String> getPurchasedFeatures() { return purchasedFeatures; }
    public Set<UUID> getPendingInvites() { return pendingInvites; }

    public Map<String, Integer> getDailyPurchases() { return dailyPurchases; }

    public long getLastDailyReset() { return lastDailyReset; }
    public void setLastDailyReset(long lastDailyReset) { this.lastDailyReset = lastDailyReset; }

    public String getGlowColor() { return glowColor; }
    public void setGlowColor(String glowColor) { this.glowColor = glowColor != null ? glowColor.toUpperCase() : "YELLOW"; }

    public org.bukkit.ChatColor getGlowChatColor() {
        try {
            return org.bukkit.ChatColor.valueOf(glowColor);
        } catch (Exception e) {
            return org.bukkit.ChatColor.YELLOW;
        }
    }

    // ---- Per-member glow colors ----

    /**
     * Glow color of a specific member: per-member override if set, otherwise the clan default.
     * May return {@link #GLOW_NONE} meaning glow is disabled for this member.
     */
    public String getGlowColor(UUID member) {
        String color = memberGlowColors.get(member);
        return color != null ? color : glowColor;
    }

    /**
     * Set per-member glow color (ChatColor name or {@link #GLOW_NONE}).
     * Pass {@code null} to reset the member back to the clan default color.
     */
    public void setGlowColor(UUID member, String color) {
        if (color == null) {
            memberGlowColors.remove(member);
        } else {
            memberGlowColors.put(member, color.toUpperCase());
        }
    }

    public Map<UUID, String> getMemberGlowColors() {
        return memberGlowColors;
    }

    public boolean isGlowEnabled(UUID member) {
        return !GLOW_NONE.equalsIgnoreCase(getGlowColor(member));
    }

    public org.bukkit.ChatColor getGlowChatColor(UUID member) {
        try {
            return org.bukkit.ChatColor.valueOf(getGlowColor(member));
        } catch (Exception e) {
            return org.bukkit.ChatColor.YELLOW;
        }
    }

    // ---- Helper Methods ----

    public void addMember(UUID uuid) {
        members.add(uuid);
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
        officers.remove(uuid);
        memberGlowColors.remove(uuid);
    }


    public boolean isMember(UUID uuid) {
        return members.contains(uuid);
    }

    public boolean isOwner(UUID uuid) {
        return owner.equals(uuid);
    }

    public boolean isOfficer(UUID uuid) {
        return officers.contains(uuid);
    }

    public void addOfficer(UUID uuid) {
        if (isMember(uuid)) {
            officers.add(uuid);
        }
    }

    public void removeOfficer(UUID uuid) {
        officers.remove(uuid);
    }

    public boolean hasOfficerPerm(UUID uuid, String perm) {
        if (isOwner(uuid)) return true;
        if (!isOfficer(uuid)) return false;
        return officerPerms.getOrDefault(perm, false);
    }

    public void setOfficerPerm(String perm, boolean value) {
        officerPerms.put(perm, value);
    }

    public void setHome(String name, Location location) {
        homes.put(name, location);
    }

    public Location getHome(String name) {
        return homes.get(name);
    }

    public boolean hasHome(String name) {
        return homes.containsKey(name);
    }

    public void removeHome(String name) {
        homes.remove(name);
    }

    public void deposit(double amount) {
        this.balance += amount;
    }

    public boolean withdraw(double amount) {
        if (balance >= amount) {
            balance -= amount;
            return true;
        }
        return false;
    }

    public boolean hasFeature(String feature) {
        return purchasedFeatures.contains(feature);
    }

    public void addFeature(String feature) {
        purchasedFeatures.add(feature);
    }

    public void addInvite(UUID uuid) {
        pendingInvites.add(uuid);
    }

    public void removeInvite(UUID uuid) {
        pendingInvites.remove(uuid);
    }

    public boolean hasPendingInvite(UUID uuid) {
        return pendingInvites.contains(uuid);
    }

    // Daily purchase limit helpers
    public int getDailyPurchased(String itemId) {
        checkDailyReset();
        return dailyPurchases.getOrDefault(itemId, 0);
    }

    public boolean canPurchaseDaily(String itemId, int limit) {
        if (limit <= 0) return true;
        checkDailyReset();
        int purchased = dailyPurchases.getOrDefault(itemId, 0);
        return purchased < limit;
    }

    public void incrementDailyPurchase(String itemId) {
        checkDailyReset();
        int current = dailyPurchases.getOrDefault(itemId, 0);
        dailyPurchases.put(itemId, current + 1);
    }

    private void checkDailyReset() {
        long now = System.currentTimeMillis();
        // Reset if > 24h since last reset
        if (now - lastDailyReset > 24 * 60 * 60 * 1000L) {
            dailyPurchases.clear();
            lastDailyReset = now;
        }
    }

    public int getMemberCount() {
        return members.size();
    }

    public List<Player> getOnlineMembers() {
        List<Player> online = new ArrayList<>();
        for (UUID uuid : members) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) online.add(p);
        }
        return online;
    }

    public String getCreatedAtFormatted() {
        long diff = System.currentTimeMillis() - createdAt;
        long days = diff / (1000 * 60 * 60 * 24);
        long hours = (diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60);
        long minutes = (diff % (1000 * 60 * 60)) / (1000 * 60);
        if (days > 0) return days + "д " + hours + "ч " + minutes + "м";
        if (hours > 0) return hours + "ч " + minutes + "м";
        return minutes + " минут";
    }
}