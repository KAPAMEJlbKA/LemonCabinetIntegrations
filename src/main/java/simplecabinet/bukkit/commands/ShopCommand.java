package simplecabinet.bukkit.commands;

import com.google.gson.reflect.TypeToken;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import simplecabinet.api.SimpleCabinetAPI;
import simplecabinet.api.SimpleCabinetResponse;
import simplecabinet.api.dto.ItemDeliveryDto;
import simplecabinet.api.dto.PageDto;
import simplecabinet.bukkit.ItemDeliveryHelper;
import simplecabinet.bukkit.SimpleCabinetPlugin;

import java.io.IOException;
import java.lang.reflect.Type;

public class ShopCommand implements CommandExecutor {
    private final SimpleCabinetPlugin plugin;

    public ShopCommand(SimpleCabinetPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
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
        if(args[0].equals("all")) {
            try {
                Type type = new TypeToken<PageDto<ItemDeliveryDto>>() {}.getType();
                SimpleCabinetResponse<PageDto<ItemDeliveryDto>> result = plugin.api.adminGet(String.format("admin/delivery/user/%s/0", username), type);
                if(!result.isSuccess()) {
                    System.out.println(result.error);
                    sender.sendMessage("Failed (1)");
                    return true;
                }
                PageDto<ItemDeliveryDto> list = result.getOrThrow();
                for(ItemDeliveryDto e : list.data) {
                    int undelivered = ItemDeliveryHelper.deliveryItemToPlayer(player, e, (int) e.part);
                    SimpleCabinetResponse<Void> resultDelivery = plugin.api.adminPost(String.format("admin/delivery/id/%d/setpart", e.id), new SetPartRequest(e.part - undelivered), Void.class);
                    if(!resultDelivery.isSuccess()) {
                        plugin.getLogger().warning(String.format("ItemDelivery %d failed", e.id));
                    }
                }
                sender.sendMessage(String.format("Gived %d/%d items", list.data.size(), list.totalElements));
                return true;
            } catch (SimpleCabinetAPI.SimpleCabinetException e) {
                e.printStackTrace();
                return true;
            }
        }
        return false;
    }

    public static class SetPartRequest {
        public long deliveredPart;

        public SetPartRequest(long deliveredPart) {
            this.deliveredPart = deliveredPart;
        }
    }
}
