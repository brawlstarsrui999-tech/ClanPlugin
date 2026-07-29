package ru.clanplugin;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import ru.clanplugin.clan.Clan;
import ru.clanplugin.clan.ClanManager;
import ru.clanplugin.commands.ClanCommand;
import ru.clanplugin.listeners.ClanListener;
import ru.clanplugin.shop.ShopManager;

import java.util.logging.Level;

public class ClanPlugin extends JavaPlugin {

    private static ClanPlugin instance;
    private Economy economy;
    private ClanManager clanManager;
    private ShopManager shopManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // Setup economy
        if (!setupEconomy()) {
            getLogger().severe("Vault not found or no economy plugin! Economy features disabled.");
        }

        // Initialize managers
        clanManager = new ClanManager(this);
        shopManager = new ShopManager(this);

        // Register commands
        ClanCommand clanCmd = new ClanCommand(this);
        getCommand("clan").setExecutor(clanCmd);
        getCommand("clan").setTabCompleter(clanCmd);

        // Register listeners
        getServer().getPluginManager().registerEvents(new ClanListener(this), this);

        // Start shop rotation task
        shopManager.startRotationTask();

        // Initialize glow for existing clans
        Bukkit.getScheduler().runTaskLater(this, this::updateAllClanGlows, 20L);

        getLogger().info("§a§l⚔ ClanPlugin §av1.0.0 successfully enabled!");
        getLogger().info("§eClan system ready. Powered by ClanDev.");
    }

    @Override
    public void onDisable() {
        if (clanManager != null) {
            clanManager.saveAll();
        }
        getLogger().info("§c§l⚔ ClanPlugin §cdisabled. All data saved.");
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

    public ShopManager getShopManager() {
        return shopManager;
    }

    public boolean hasEconomy() {
        return economy != null;
    }

    public String getPrefix() {
        return getConfig().getString("plugin.prefix", "&8[&6⚔ &bClan&6⚔&8] ");
    }

    // ===== GLOW / TEAM MANAGEMENT FOR CLAN GLOW =====
    private Scoreboard scoreboard;

    public Scoreboard getScoreboard() {
        if (scoreboard == null) {
            scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        }
        return scoreboard;
    }

    public void updateClanGlow(Clan clan) {
        if (clan == null) return;
        String teamName = "clan_" + clan.getId();
        Team team = getScoreboard().getTeam(teamName);
        if (team == null) {
            team = getScoreboard().registerNewTeam(teamName);
        }

        // Set glow color
        ChatColor color = clan.getGlowChatColor();
        team.setColor(color);
        team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
        team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);

        // Clear and re-add online members
        for (String entry : team.getEntries()) {
            team.removeEntry(entry);
        }
        for (org.bukkit.entity.Player p : clan.getOnlineMembers()) {
            team.addEntry(p.getName());
            p.setGlowing(true);
        }
    }

    public void removeClanGlow(Clan clan) {
        if (clan == null) return;
        String teamName = "clan_" + clan.getId();
        Team team = getScoreboard().getTeam(teamName);
        if (team != null) {
            for (String entry : new java.util.ArrayList<>(team.getEntries())) {
                team.removeEntry(entry);
            }
            // disable glowing on remaining
            for (org.bukkit.entity.Player p : clan.getOnlineMembers()) {
                p.setGlowing(false);
            }
            team.unregister();
        }
    }

    public void updateAllClanGlows() {
        for (Clan clan : clanManager.getAllClans()) {
            updateClanGlow(clan);
        }
    }
}