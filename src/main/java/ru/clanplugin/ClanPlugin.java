package ru.clanplugin;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
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
}
