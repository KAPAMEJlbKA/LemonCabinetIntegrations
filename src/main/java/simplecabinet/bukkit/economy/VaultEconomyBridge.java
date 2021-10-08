package simplecabinet.bukkit.economy;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import simplecabinet.api.SimpleCabinetAPI;
import simplecabinet.api.SimpleCabinetEconomy;
import simplecabinet.api.dto.BalanceTransactionDto;
import simplecabinet.bukkit.SimpleCabinetPlugin;

import java.util.List;

public class VaultEconomyBridge implements Economy {
    private final SimpleCabinetPlugin plugin;
    private final SimpleCabinetEconomy economy;
    private static final EconomyResponse NOT_IMPLEMENTED = new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Not implemented");

    public VaultEconomyBridge(SimpleCabinetPlugin plugin) {
        this.plugin = plugin;
        this.economy = plugin.economy;
    }

    @Override
    public boolean isEnabled() {
        return plugin.config.economy.enabled;
    }

    @Override
    public String getName() {
        return "SimpleCabinet";
    }

    @Override
    public boolean hasBankSupport() {
        return plugin.config.economy.bankEmulator;
    }

    @Override
    public int fractionalDigits() {
        return -1;
    }

    @Override
    public String format(double amount) {
        return String.format("%.2f", amount);
    }

    @Override
    public String currencyNamePlural() {
        return plugin.config.economy.defaultCurrency;
    }

    @Override
    public String currencyNameSingular() {
        return plugin.config.economy.defaultCurrency;
    }

    @Override
    @Deprecated
    public boolean hasAccount(String playerName) {
        return true;
    }

    @Override
    public boolean hasAccount(OfflinePlayer player) {
        return true;
    }

    @Override
    public boolean hasAccount(String playerName, String worldName) {
        return true;
    }

    @Override
    public boolean hasAccount(OfflinePlayer player, String worldName) {
        return true;
    }

    @Override
    @Deprecated
    public double getBalance(String playerName) {
        return getBalance(Bukkit.getOfflinePlayer(playerName));
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        return economy.getBalance(player.getUniqueId(), plugin.config.economy.defaultCurrency);
    }

    @Override
    @Deprecated
    public double getBalance(String playerName, String world) {
        return getBalance(playerName);
    }

    @Override
    public double getBalance(OfflinePlayer player, String world) {
        return getBalance(player);
    }

    @Override
    @Deprecated
    public boolean has(String playerName, double amount) {
        return has(Bukkit.getOfflinePlayer(playerName), amount);
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return getBalance(player) >= amount;
    }

    @Override
    @Deprecated
    public boolean has(String playerName, String worldName, double amount) {
        return has(playerName, amount);
    }

    @Override
    public boolean has(OfflinePlayer player, String worldName, double amount) {
        return has(player, amount);
    }

    @Override
    @Deprecated
    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        return withdrawPlayer(Bukkit.getOfflinePlayer(playerName), amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        try {
            BalanceTransactionDto transaction = economy.removeMoney(player.getUniqueId(), plugin.config.economy.defaultCurrency, amount, plugin.config.economy.transactionComment);
            return new EconomyResponse(transaction.fromCount, economy.getCachedBalance(player.getUniqueId(), plugin.config.economy.defaultCurrency)
                    , EconomyResponse.ResponseType.SUCCESS, null);
        } catch (SimpleCabinetAPI.SimpleCabinetException exception) {
            return new EconomyResponse(0, economy.getCachedBalance(player.getUniqueId(), plugin.config.economy.defaultCurrency), EconomyResponse.ResponseType.FAILURE, exception.getMessage());
        }
    }

    @Override
    @Deprecated
    public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
        return withdrawPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) {
        return withdrawPlayer(player, amount);
    }

    @Override
    @Deprecated
    public EconomyResponse depositPlayer(String playerName, double amount) {
        return depositPlayer(Bukkit.getOfflinePlayer(playerName), amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        try {
            BalanceTransactionDto transaction = economy.addMoney(player.getUniqueId(), plugin.config.economy.defaultCurrency, amount, plugin.config.economy.transactionComment);
            return new EconomyResponse(transaction.fromCount, economy.getCachedBalance(player.getUniqueId(), plugin.config.economy.defaultCurrency)
                    , EconomyResponse.ResponseType.SUCCESS, null);
        } catch (SimpleCabinetAPI.SimpleCabinetException exception) {
            return new EconomyResponse(0, economy.getCachedBalance(player.getUniqueId(), plugin.config.economy.defaultCurrency), EconomyResponse.ResponseType.FAILURE, exception.getMessage());
        }
    }

    @Override
    @Deprecated
    public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
        return depositPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) {
        return depositPlayer(player, amount);
    }

    @Override
    public EconomyResponse createBank(String name, String player) {
        return null;
    }

    @Override
    public EconomyResponse createBank(String name, OfflinePlayer player) {
        return null;
    }

    @Override
    public EconomyResponse deleteBank(String name) {
        return null;
    }

    @Override
    public EconomyResponse bankBalance(String name) {
        return null;
    }

    @Override
    public EconomyResponse bankHas(String name, double amount) {
        return null;
    }

    @Override
    public EconomyResponse bankWithdraw(String name, double amount) {
        return null;
    }

    @Override
    public EconomyResponse bankDeposit(String name, double amount) {
        return null;
    }

    @Override
    public EconomyResponse isBankOwner(String name, String playerName) {
        return null;
    }

    @Override
    public EconomyResponse isBankOwner(String name, OfflinePlayer player) {
        return null;
    }

    @Override
    public EconomyResponse isBankMember(String name, String playerName) {
        return null;
    }

    @Override
    public EconomyResponse isBankMember(String name, OfflinePlayer player) {
        return null;
    }

    @Override
    public List<String> getBanks() {
        return null;
    }

    @Override
    public boolean createPlayerAccount(String playerName) {
        return false;
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player) {
        return false;
    }

    @Override
    public boolean createPlayerAccount(String playerName, String worldName) {
        return false;
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player, String worldName) {
        return false;
    }
}
