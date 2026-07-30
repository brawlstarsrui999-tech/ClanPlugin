package ru.clanplugin.listeners;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import ru.clanplugin.ClanPlugin;
import ru.clanplugin.clan.Clan;
import ru.clanplugin.gui.*;
import ru.clanplugin.shop.ShopItem;

import java.text.DecimalFormat;

public class ClanListener implements Listener {

    private final ClanPlugin plugin;
    private static final DecimalFormat FORMAT = new DecimalFormat("#,###");

    public ClanListener(ClanPlugin plugin) {
        this.plugin = plugin;
    }

    // ===================== PVP PROTECTION =====================
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;

        Clan victimClan = plugin.getClanManager().getPlayerClan(victim.getUniqueId());
        Clan attackerClan = plugin.getClanManager().getPlayerClan(attacker.getUniqueId());

        if (victimClan == null || attackerClan == null) return;
        if (!victimClan.getId().equals(attackerClan.getId())) return;

        // Same clan
        if (!victimClan.isPvpEnabled()) {
            event.setCancelled(true);
            attacker.sendMessage(c(plugin.getPrefix() + "&cУрон по соклановцам отключён! &7/clan pvp"));
            attacker.playSound(attacker.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }
    }

    // ===================== JOIN/QUIT =====================
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan != null) {
            // Update glow for this clan (adds player to team)
            plugin.updateClanGlow(clan);

            // Notify clan members
            for (Player m : clan.getOnlineMembers()) {
                if (!m.equals(player)) {
                    m.sendMessage(c(plugin.getPrefix() + "&a▶ &e" + player.getName() + " &aвошёл в игру!"));
                }
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan != null) {
            for (Player m : clan.getOnlineMembers()) {
                if (!m.equals(player)) {
                    m.sendMessage(c(plugin.getPrefix() + "&c◀ &e" + player.getName() + " &cвышел из игры."));
                }
            }
            // Re-sync glow (player removed automatically on quit)
            plugin.updateClanGlow(clan);
        }
    }

    // ===================== INVENTORY CLICK =====================
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
        if (!event.getView().title().equals(net.kyori.adventure.text.Component.empty())) {
            // Check title
        }

        String titleRaw = LegacyComponentSerializer.legacySection()
                .serialize(event.getView().title());
        String title = titleRaw.replace("§", "&");

        // ====== MAIN MENU ======
        if (title.contains("Клан:")) {
            event.setCancelled(true);
            handleMainMenuClick(player, event.getSlot(), event.getCurrentItem());
            return;
        }

        // ====== SHOP ======
        if (title.contains("Магазин клана")) {
            event.setCancelled(true);
            handleShopClick(player, event.getSlot(), event.getCurrentItem());
            return;
        }

        // ====== UPGRADES ======
        if (title.contains("Улучшения клана")) {
            event.setCancelled(true);
            handleUpgradesClick(player, event.getSlot(), event.getCurrentItem());
            return;
        }

        // ====== MEMBERS ======
        if (title.contains("Участники клана")) {
            event.setCancelled(true);
            handleMembersClick(player, event.getSlot(), event.getCurrentItem());
            return;
        }

        // ====== PERMISSIONS ======
        if (title.contains("Права офицеров")) {
            event.setCancelled(true);
            handlePermissionsClick(player, event.getSlot(), event.getCurrentItem());
            return;
        }

        // ====== HOMES ======
        if (title.contains("Базы клана")) {
            event.setCancelled(true);
            handleHomesClick(player, event.getSlot(), event.getCurrentItem(), title);
            return;
        }

        // ====== TOP ======
        if (title.contains("Топ кланов")) {
            event.setCancelled(true);
            handleTopClick(player, event.getSlot());
            return;
        }

        // ====== BUFF MENU ======
        if (title.contains("Активация баффов")) {
            event.setCancelled(true);
            handleBuffMenuClick(player, event.getSlot(), event.getCurrentItem());
            return;
        }
    }

    // ============== MAIN MENU CLICK ==============
    private void handleMainMenuClick(Player player, int slot, ItemStack item) {
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) return;

        String name = itemName(item);

        if (name.contains("Участники клана")) {
            ClanMembersMenu.open(player, plugin);
            playSound(player, Sound.BLOCK_CHEST_OPEN);
        } else if (name.contains("Базы клана")) {
            ClanHomesMenu.open(player, plugin);
            playSound(player, Sound.BLOCK_CHEST_OPEN);
        } else if (name.contains("Магазин клана")) {
            ClanShopMenu.open(player, plugin);
            playSound(player, Sound.BLOCK_CHEST_OPEN);
        } else if (name.contains("Улучшения клана")) {
            ClanUpgradesMenu.open(player, plugin);
            playSound(player, Sound.BLOCK_CHEST_OPEN);
        } else if (name.contains("Топ кланов")) {
            ClanTopMenu.open(player, plugin);
            playSound(player, Sound.BLOCK_CHEST_OPEN);
        } else if (name.contains("Права участников")) {
            if (clan.isOwner(player.getUniqueId())) {
                ClanPermissionsMenu.open(player, plugin);
                playSound(player, Sound.BLOCK_CHEST_OPEN);
            }
        } else if (name.contains("Дружественный огонь")) {
            boolean canToggle = clan.isOwner(player.getUniqueId()) ||
                    clan.hasOfficerPerm(player.getUniqueId(), Clan.PERM_PVP);
            if (canToggle) {
                clan.setPvpEnabled(!clan.isPvpEnabled());
                plugin.getClanManager().saveImmediately();
                boolean pvpOn = clan.isPvpEnabled();
                String status = pvpOn ? "&c&lВКЛЮЧЁН" : "&a&lОТКЛЮЧЁН";
                for (Player m : clan.getOnlineMembers()) {
                    m.sendMessage(c(plugin.getPrefix() + "&7Дружественный огонь " + status));
                    m.playSound(m.getLocation(), pvpOn ? Sound.BLOCK_NOTE_BLOCK_BASS : Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
                }
                ClanMainMenu.open(player, plugin);
            } else {
                player.sendMessage(c(plugin.getPrefix() + "&cНет прав!"));
                playSound(player, Sound.ENTITY_VILLAGER_NO);
            }
        } else if (name.contains("Казна клана")) {
            player.closeInventory();
            // Show money info
            player.sendMessage(c(plugin.getPrefix() + "&7Казна: &6" + FORMAT.format((int) clan.getBalance()) + " монет"));
        } else if (name.contains("Расформировать клан")) {
            player.closeInventory();
            player.performCommand("clan disband");
        } else if (name.contains("Покинуть клан")) {
            player.closeInventory();
            player.performCommand("clan leave");
        }
    }

    // ============== SHOP CLICK ==============
    private void handleShopClick(Player player, int slot, ItemStack item) {
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) return;

        String name = itemName(item);

        if (name.contains("← Назад")) {
            ClanMainMenu.open(player, plugin);
            playSound(player, Sound.UI_BUTTON_CLICK);
            return;
        }

        if (name.contains("Улучшить магазин")) {
            boolean isOwner = clan.isOwner(player.getUniqueId());
            boolean canUpgrade = isOwner || clan.hasOfficerPerm(player.getUniqueId(), Clan.PERM_UPGRADE_SHOP);
            if (!canUpgrade) {
                player.sendMessage(c(plugin.getPrefix() + "&cНет прав для улучшения!"));
                playSound(player, Sound.ENTITY_VILLAGER_NO);
                return;
            }
            if (clan.getShopLevel() >= 4) {
                player.sendMessage(c(plugin.getPrefix() + "&aМагазин уже максимального уровня!"));
                return;
            }
            int nextLevel = clan.getShopLevel() + 1;
            long cost = plugin.getConfig().getLong("upgrades.shop.level-" + nextLevel, 999999);
            if (!clan.withdraw(cost)) {
                player.sendMessage(c(plugin.getPrefix() + "&cНедостаточно средств в казне! &7(Нужно: &6" + FORMAT.format(cost) + "&7)"));
                playSound(player, Sound.ENTITY_VILLAGER_NO);
                return;
            }
            clan.setShopLevel(nextLevel);
            plugin.getClanManager().saveImmediately();
            player.sendMessage(c(plugin.getPrefix() + "&a&lМагазин улучшен до уровня &e" + nextLevel + "&a!"));
            playSound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE);
            for (Player m : clan.getOnlineMembers()) {
                if (!m.equals(player)) m.sendMessage(c(plugin.getPrefix() + "&aМагазин клана улучшен до &eур. " + nextLevel + "&a!"));
            }
            ClanShopMenu.open(player, plugin);
            return;
        }

        // Check if it's a shop item by checking current rotation
        java.util.List<ShopItem> items = plugin.getShopManager().getItemsForLevel(clan.getShopLevel());
        for (ShopItem shopItem : items) {
            if (name.contains(shopItem.getDisplayName())) {
                // NEW: daily limit check
                if (!clan.canPurchaseDaily(shopItem.getId(), shopItem.getDailyLimit())) {
                    player.sendMessage(c(plugin.getPrefix() + "&cВы достигли дневного лимита на этот товар!"));
                    playSound(player, Sound.ENTITY_VILLAGER_NO);
                    return;
                }

                // Purchase logic
                double totalPrice = shopItem.getTotalPrice();
                boolean useClanBalance = !plugin.hasEconomy();

                if (useClanBalance) {
                    if (!clan.withdraw(totalPrice)) {
                        player.sendMessage(c(plugin.getPrefix() + "&cНедостаточно средств в казне! &7Нужно: &6" + FORMAT.format((int) totalPrice) + " монет"));
                        playSound(player, Sound.ENTITY_VILLAGER_NO);
                        return;
                    }
                } else {
                    if (!plugin.getEconomy().has(player, totalPrice)) {
                        player.sendMessage(c(plugin.getPrefix() + "&cНедостаточно средств! &7Нужно: &6" + FORMAT.format((int) totalPrice) + " монет"));
                        playSound(player, Sound.ENTITY_VILLAGER_NO);
                        return;
                    }
                    plugin.getEconomy().withdrawPlayer(player, totalPrice);
                }

                // Record purchase for limit
                clan.incrementDailyPurchase(shopItem.getId());

                plugin.getClanManager().saveImmediately();
                giveItem(player, shopItem);
                String payFrom = useClanBalance ? "из казны" : "";
                player.sendMessage(c(plugin.getPrefix() + "&aКуплено: &e" + shopItem.getDisplayName() +
                        " &7за &6" + FORMAT.format((int) totalPrice) + " монет " + payFrom));
                playSound(player, Sound.ENTITY_PLAYER_LEVELUP);
                ClanShopMenu.open(player, plugin);
                return;
            }
        }
    }

    private void giveItem(Player player, ShopItem shopItem) {
        ItemStack giveItem = shopItem.createGiveItem();
        if (player.getInventory().firstEmpty() == -1) {
            player.getWorld().dropItem(player.getLocation(), giveItem);
            player.sendMessage(c(plugin.getPrefix() + "&cИнвентарь полон! Предмет выброшен рядом."));
        } else {
            player.getInventory().addItem(giveItem);
        }
    }

    // ============== UPGRADES CLICK ==============
    private void handleUpgradesClick(Player player, int slot, ItemStack item) {
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) return;

        String name = itemName(item);

        if (name.contains("← Назад")) {
            ClanMainMenu.open(player, plugin);
            playSound(player, Sound.UI_BUTTON_CLICK);
            return;
        }

        if (name.contains("Активировать баффы")) {
            ClanBuffMenu.open(player, plugin);
            playSound(player, Sound.BLOCK_CHEST_OPEN);
            return;
        }

        boolean isOwner = clan.isOwner(player.getUniqueId());
        boolean canUpgradeShop = isOwner || clan.hasOfficerPerm(player.getUniqueId(), Clan.PERM_UPGRADE_SHOP);
        boolean canUpgradeFeatures = isOwner || clan.hasOfficerPerm(player.getUniqueId(), Clan.PERM_UPGRADE_FEATURES);

        // Shop upgrades
        if (name.contains("Магазин Ур. 2")) tryBuyShopLevel(player, clan, 2, 60000, canUpgradeShop);
        else if (name.contains("Магазин Ур. 3")) tryBuyShopLevel(player, clan, 3, 130000, canUpgradeShop);
        else if (name.contains("Магазин Ур. 4")) tryBuyShopLevel(player, clan, 4, 250000, canUpgradeShop);

            // Feature purchases
        else if (name.contains("Телепортация")) tryBuyFeature(player, clan, "clan-tp", 150000, canUpgradeFeatures);
        else if (name.contains("Бафф Сила I") && !name.contains("II")) tryBuyFeature(player, clan, "buff-strength-1", 30000, canUpgradeFeatures);
        else if (name.contains("Бафф Сила II")) tryBuyFeature(player, clan, "buff-strength-2", 60000, canUpgradeFeatures);
        else if (name.contains("Бафф Скорость I") && !name.contains("II")) tryBuyFeature(player, clan, "buff-speed-1", 20000, canUpgradeFeatures);
        else if (name.contains("Бафф Скорость II")) tryBuyFeature(player, clan, "buff-speed-2", 50000, canUpgradeFeatures);
        else if (name.contains("Огнестойкость")) tryBuyFeature(player, clan, "buff-fire-resistance", 40000, canUpgradeFeatures);
        else if (name.contains("Невидимость")) tryBuyFeature(player, clan, "buff-invisibility", 50000, canUpgradeFeatures);
    }

    private void tryBuyShopLevel(Player player, Clan clan, int level, long cost, boolean canBuy) {
        if (!canBuy) { player.sendMessage(c(plugin.getPrefix() + "&cНет прав!")); playSound(player, Sound.ENTITY_VILLAGER_NO); return; }
        if (clan.getShopLevel() >= level) { player.sendMessage(c(plugin.getPrefix() + "&cУже куплено!")); return; }
        if (clan.getShopLevel() < level - 1) { player.sendMessage(c(plugin.getPrefix() + "&cСначала купите предыдущий уровень!")); return; }
        if (!clan.withdraw(cost)) { player.sendMessage(c(plugin.getPrefix() + "&cНедостаточно средств! &7Нужно: &6" + FORMAT.format(cost))); playSound(player, Sound.ENTITY_VILLAGER_NO); return; }
        clan.setShopLevel(level);
        plugin.getClanManager().saveImmediately();
        player.sendMessage(c(plugin.getPrefix() + "&a&lМагазин улучшен до уровня &e" + level + "&a!"));
        playSound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE);
        for (Player m : clan.getOnlineMembers()) { if (!m.equals(player)) m.sendMessage(c(plugin.getPrefix() + "&aМагазин улучшен до ур. &e" + level)); }
        ClanUpgradesMenu.open(player, plugin);
    }

    private void tryBuyFeature(Player player, Clan clan, String feature, long cost, boolean canBuy) {
        if (!canBuy) { player.sendMessage(c(plugin.getPrefix() + "&cНет прав!")); playSound(player, Sound.ENTITY_VILLAGER_NO); return; }
        if (clan.hasFeature(feature)) { player.sendMessage(c(plugin.getPrefix() + "&aУже куплено!")); return; }
        if (!clan.withdraw(cost)) { player.sendMessage(c(plugin.getPrefix() + "&cНедостаточно средств! &7Нужно: &6" + FORMAT.format(cost))); playSound(player, Sound.ENTITY_VILLAGER_NO); return; }
        clan.addFeature(feature);
        plugin.getClanManager().saveImmediately();
        player.sendMessage(c(plugin.getPrefix() + "&a&lУлучшение куплено!"));
        playSound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE);
        for (Player m : clan.getOnlineMembers()) { if (!m.equals(player)) m.sendMessage(c(plugin.getPrefix() + "&aНовое улучшение клана разблокировано!")); }
        ClanUpgradesMenu.open(player, plugin);
    }

    // ============== MEMBERS CLICK ==============
    private void handleMembersClick(Player player, int slot, ItemStack item) {
        String name = itemName(item);
        if (name.contains("← Назад")) {
            ClanMainMenu.open(player, plugin);
            playSound(player, Sound.UI_BUTTON_CLICK);
        }
    }

    // ============== PERMISSIONS CLICK ==============
    private void handlePermissionsClick(Player player, int slot, ItemStack item) {
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null || !clan.isOwner(player.getUniqueId())) return;

        String name = itemName(item);

        if (name.contains("← Назад")) {
            ClanMainMenu.open(player, plugin);
            playSound(player, Sound.UI_BUTTON_CLICK);
            return;
        }

        if (name.contains("Управление PvP")) {
            toggle(clan, Clan.PERM_PVP, player);
        } else if (name.contains("Установка баз")) {
            toggle(clan, Clan.PERM_SETHOME, player);
        } else if (name.contains("Улучшение магазина")) {
            toggle(clan, Clan.PERM_UPGRADE_SHOP, player);
        } else if (name.contains("Улучшение способностей")) {
            toggle(clan, Clan.PERM_UPGRADE_FEATURES, player);
        }
    }

    private void toggle(Clan clan, String perm, Player player) {
        boolean current = clan.getOfficerPerms().getOrDefault(perm, false);
        clan.setOfficerPerm(perm, !current);
        plugin.getClanManager().saveImmediately();
        player.sendMessage(c(plugin.getPrefix() + "&7Разрешение " + (!current ? "&aвключено" : "&cотключено") + " для офицеров."));
        playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING);
        ClanPermissionsMenu.open(player, plugin);
    }

    // ============== HOMES CLICK ==============
    private void handleHomesClick(Player player, int slot, ItemStack item, String title) {
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) return;

        String name = itemName(item);

        if (name.contains("← Назад")) {
            ClanMainMenu.open(player, plugin);
            playSound(player, Sound.UI_BUTTON_CLICK);
            return;
        }

        // Check if it's a home item
        if (name.startsWith("&d&l") || name.contains("🏠")) {
            // Extract home name
            String homeName = name.replace("&d&l", "").replace("🏠 ", "").replace("§d§l", "").replace("§", "").trim();
            // Clean color codes
            homeName = homeName.replaceAll("[&§][0-9a-fk-orA-FK-OR]", "").trim();

            if (clan.hasHome(homeName)) {
                player.closeInventory();
                player.teleport(clan.getHome(homeName));
                player.sendMessage(c(plugin.getPrefix() + "&aТелепортация на базу &e" + homeName + "!"));
                playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT);
            }
        }
    }

    // ============== TOP CLICK ==============
    private void handleTopClick(Player player, int slot) {
        if (slot == 45) {
            Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
            if (clan != null) {
                ClanMainMenu.open(player, plugin);
            } else {
                player.closeInventory();
            }
            playSound(player, Sound.UI_BUTTON_CLICK);
        }
    }

    // ============== BUFF MENU CLICK ==============
    private void handleBuffMenuClick(Player player, int slot, ItemStack item) {
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) return;

        String name = itemName(item);

        if (name.contains("← Назад")) {
            ClanUpgradesMenu.open(player, plugin);
            playSound(player, Sound.UI_BUTTON_CLICK);
            return;
        }

        boolean isOwner = clan.isOwner(player.getUniqueId());
        boolean canActivate = isOwner || clan.hasOfficerPerm(player.getUniqueId(), Clan.PERM_UPGRADE_FEATURES);
        if (!canActivate) {
            player.sendMessage(c(plugin.getPrefix() + "&cНет прав!"));
            playSound(player, Sound.ENTITY_VILLAGER_NO);
            return;
        }

        String featureId = null;
        long activationCost = 0;

        if (name.contains("✔") && name.contains("💪 Сила I") && !name.contains("II")) { featureId = "buff-strength-1"; activationCost = 3000; }
        else if (name.contains("✔") && name.contains("💪 Сила II")) { featureId = "buff-strength-2"; activationCost = 6000; }
        else if (name.contains("✔") && name.contains("👟 Скорость I") && !name.contains("II")) { featureId = "buff-speed-1"; activationCost = 2000; }
        else if (name.contains("✔") && name.contains("👟 Скорость II")) { featureId = "buff-speed-2"; activationCost = 5000; }
        else if (name.contains("✔") && name.contains("🔥 Огнестойкость")) { featureId = "buff-fire-resistance"; activationCost = 4000; }
        else if (name.contains("✔") && name.contains("👻 Невидимость")) { featureId = "buff-invisibility"; activationCost = 5000; }

        if (featureId != null) {
            if (!clan.hasFeature(featureId)) {
                player.sendMessage(c(plugin.getPrefix() + "&cСначала купите это улучшение!"));
                return;
            }
            if (!clan.withdraw(activationCost)) {
                player.sendMessage(c(plugin.getPrefix() + "&cНедостаточно средств в казне! &7Нужно: &6" + FORMAT.format(activationCost)));
                playSound(player, Sound.ENTITY_VILLAGER_NO);
                return;
            }
            plugin.getClanManager().saveImmediately();
            ClanBuffMenu.applyBuff(clan, featureId, plugin);
            player.sendMessage(c(plugin.getPrefix() + "&a&lБафф активирован для всех участников клана!"));
            playSound(player, Sound.BLOCK_BEACON_POWER_SELECT);
            ClanBuffMenu.open(player, plugin);
        }
    }

    // ============== HELPERS ==============
    private String itemName(ItemStack item) {
        if (item == null || item.getItemMeta() == null) return "";
        String name = LegacyComponentSerializer.legacySection()
                .serialize(item.getItemMeta().displayName() != null
                        ? item.getItemMeta().displayName() : net.kyori.adventure.text.Component.empty());
        return name.replace("§", "&");
    }

    private void playSound(Player player, Sound sound) {
        player.playSound(player.getLocation(), sound, 1f, 1f);
    }

    private net.kyori.adventure.text.Component c(String msg) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(msg);
    }
}