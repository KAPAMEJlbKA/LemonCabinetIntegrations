package simplecabinet.fabric;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import simplecabinet.api.SimpleCabinetAPI;
import simplecabinet.api.SimpleCabinetEconomy;
import simplecabinet.api.SimpleCabinetResponse;
import simplecabinet.api.dto.BalanceTransactionDto;
import simplecabinet.api.dto.ItemDeliveryDto;
import simplecabinet.api.dto.PageDto;
import simplecabinet.api.dto.UserDto;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;

import static net.minecraft.server.command.CommandManager.*;

public class SimpleCabinetMod implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger("simplecabinet-fabric-integration");
	public static Config CONFIG;
	public static SimpleCabinetAPI api;
	public static SimpleCabinetEconomy economy;

	public static Gson configGson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("SimpleCabinet Integrations");
		try {
			loadConfig();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		if(CONFIG.url == null || CONFIG.url.equals("URL")) {
			LOGGER.error("Please configure config.json");
			return;
		}
		api = new SimpleCabinetAPI(CONFIG.url, CONFIG.token);
		economy = new SimpleCabinetEconomy(api);
		if(CONFIG.testOnStartup) {
			UserDto result = api.<UserDto>adminGet("/auth/userinfo", UserDto.class).getOrThrow();
			LOGGER.info(String.format("Logged-in %s", result.username));
		}
		CommandRegistrationCallback.EVENT.register((dispatcher, register, env) -> {
			dispatcher.register(literal("shop").then(literal("all").requires(Permissions.require("simplecabinet.shop.all"))
					.executes(context -> {
						ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
						Type type = new TypeToken<PageDto<ItemDeliveryDto>>() {}.getType();
						SimpleCabinetResponse<PageDto<ItemDeliveryDto>> result = api.adminGet(String.format("admin/delivery/user/%s/0", player.getGameProfile().getName()), type);
						if(!result.isSuccess()) {
							System.out.println(result.error);
							player.sendMessage(Text.of("Failed (1)"));
							return 1;
						}
						PageDto<ItemDeliveryDto> list = result.getOrThrow();
						for(ItemDeliveryDto e : list.data) {
							var parsed = ItemDeliveryHelper.parse(e);
							int delivered = ItemDeliveryHelper.delivery(parsed, player, (int) e.part);
							SimpleCabinetResponse<Void> resultDelivery = api.adminPost(String.format("admin/delivery/id/%d/setpart", e.id), new SetPartRequest(delivered), Void.class);
							if(!resultDelivery.isSuccess()) {
								LOGGER.warn(String.format("ItemDelivery %d failed", e.id));
							}
						}
						player.sendMessage(Text.of(String.format("Gived %d/%d items", list.data.size(), list.totalElements)));
						return 1;
					})));
			dispatcher.register(literal("economy")
					.then(literal("balance").requires(Permissions.require("simplecabinet.economy.balance")).executes(context -> {
				String currency = CONFIG.defaultCurrency;
				PlayerEntity player = context.getSource().getPlayerOrThrow();
				player.sendMessage(Text.of(String.format("Balance %.2f %s", economy.getBalance(player.getUuid(), currency), currency)));
				return 1;
			}))
					.then(literal("transfer").requires(Permissions.require("simplecabinet.economy.transfer"))
									.then(argument("user", EntityArgumentType.player())
									.then(argument("amount", IntegerArgumentType.integer(1))
									.then(argument("comment", StringArgumentType.string()).executes(context -> {
						String currency = CONFIG.defaultCurrency;
						PlayerEntity player = context.getSource().getPlayerOrThrow();
						PlayerEntity target = EntityArgumentType.getPlayer(context, "user");
						int amount = IntegerArgumentType.getInteger(context, "amount");
						String comment = StringArgumentType.getString(context, "comment");
						try {
							BalanceTransactionDto dto = economy.transfer(player.getUuid(), currency, target.getUuid(), currency, amount,true,
									comment, false);
							player.sendMessage(Text.of("Successful"));
						} catch (SimpleCabinetAPI.SimpleCabinetException e) {
							player.sendMessage(Text.of(String.format("Transaction aborted: %s", e.getMessage())));
						}
						return 1;
					}))))));
		});
	}

	private void loadConfig() throws IOException {
		Path path = FabricLoader.getInstance().getConfigDir().resolve("simplecabinet.json");
		if(!Files.exists(path)) {
			CONFIG = new Config();
			try(Writer writer = new FileWriter(path.toFile())) {
				configGson.toJson(CONFIG, writer);
			}
		}
		try(Reader reader = new FileReader(path.toFile())) {
			CONFIG = configGson.fromJson(reader, Config.class);
		}
	}

	public static class SetPartRequest {
		public long deliveredPart;

		public SetPartRequest(long deliveredPart) {
			this.deliveredPart = deliveredPart;
		}
	}
}
