package simplecabinet.bukkit.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import simplecabinet.api.SimpleCabinetAPI;
import simplecabinet.api.SimpleCabinetEconomy;
import simplecabinet.api.dto.BalanceTransactionDto;
import simplecabinet.bukkit.SimpleCabinetPlugin;

import java.util.Arrays;
import java.util.UUID;

public class EconomyCommand implements CommandExecutor {
    private final SimpleCabinetPlugin plugin;
    private final SimpleCabinetEconomy economy;

    public EconomyCommand(SimpleCabinetPlugin plugin) {
        this.plugin = plugin;
        this.economy = plugin.economy;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player)) {
            sender.sendMessage("Only player can use this command");
            return true;
        }
        Player player = (Player) sender;
        String username = player.getName();
        if(args.length == 0) {
            return false;
        }
        if(!player.hasPermission("simplecabinet.commands.economy")) {
            sender.sendMessage("Permissions denied");
            return true;
        }
        if(args[0].equals("transfer")) {
            if(args.length < 3) {
                sender.sendMessage("Use: /economy transfer [playerName] [amount] (comment)");
                return true;
            }
            if(!player.hasPermission("simplecabinet.commands.economy.transfer")) {
                sender.sendMessage("Permissions denied");
                return true;
            }
            UUID targetUUID;
            OfflinePlayer target = Bukkit.getPlayer(args[1]);
            if(target == null) {
                target = Bukkit.getOfflinePlayer(args[1]);
            }
            if(target == null) {
                sender.sendMessage(String.format("User %s not found", args[1]));
                return true;
            }
            targetUUID = target.getUniqueId();
            try {
                double amount = Double.parseDouble(args[2]);
                BalanceTransactionDto dto = economy.transfer(player.getUniqueId(), plugin.config.economy.defaultCurrency, targetUUID, plugin.config.economy.defaultCurrency, amount,true,
                        args.length > 3 ? join(" ", args, 3) : plugin.config.economy.transactionComment, false);
                sender.sendMessage("Successful");
                return true;
            } catch (NumberFormatException e) {
                sender.sendMessage("amount is not number");
                return true;
            } catch (SimpleCabinetAPI.SimpleCabinetException e) {
                sender.sendMessage(String.format("Transaction error: %s", e.getMessage()));
                return true;
            }
        } else if(args[0].equals("balance")) {
            if(!player.hasPermission("simplecabinet.commands.economy.balance")) {
                sender.sendMessage("Permissions denied");
                return true;
            }
            String currency = plugin.config.economy.defaultCurrency;
            sender.sendMessage(String.format("Balance %.2f %s", economy.getBalance(player.getUniqueId(), currency), currency));
            return true;
        }
        return false;
    }

    private String join(CharSequence delimiter, String[] args, int i) {
        if(args.length == i) {
            return "";
        }
        if(args.length == i+1) {
            return args[i];
        }
        StringBuilder builder = new StringBuilder();
        builder.append(args[i]);
        for(int j=i+1;j<args.length;++j) {
            builder.append(delimiter);
            builder.append(args[j]);
        }
        return builder.toString();
    }
}
