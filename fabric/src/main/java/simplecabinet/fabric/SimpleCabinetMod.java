package simplecabinet.fabric;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;

import me.lucko.fabric.api.permissions.v0.Permissions;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import simplecabinet.api.SimpleCabinetAPI;
import simplecabinet.api.SimpleCabinetEconomy;
import simplecabinet.api.SimpleCabinetPing;
import simplecabinet.api.SimpleCabinetResponse;
import simplecabinet.api.dto.*;

import java.awt.*;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static net.minecraft.server.command.CommandManager.*;

public class SimpleCabinetMod implements ModInitializer {

	public static final Logger LOGGER = LoggerFactory.getLogger("LemonBrick cabinet integrator");
	public static Config CONFIG;
	public static SimpleCabinetAPI api;
	public static SimpleCabinetEconomy economy;
	public static Gson configGson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	private MinecraftServer server;

	@Override
	public void onInitialize() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			this.server = server;
		});

		LOGGER.info("LemonBrick cabinet integrator");
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

		ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
		executor.scheduleAtFixedRate(this::sendPlayerCountToAPI, 6000, CONFIG.playerCountUpdateInterval, TimeUnit.MILLISECONDS);

		if(CONFIG.testOnStartup) {
			UserDto result = api.<UserDto>adminGet("/auth/userinfo", UserDto.class).getOrThrow();
			LOGGER.info(String.format("Logged-in %s", result.username));
		}
        String serverName = CONFIG.serverName;
        String serverFromServer = CONFIG.serverName;
        api.adminPost("/servers/new", new ServerRequest(serverName, serverFromServer), ServersDto.class).getOrThrow();

		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			LOGGER.info("Server is stopping. Cleaning up resources...");
			executor.shutdown();
		});

        CommandRegistrationCallback.EVENT.register((dispatcher, register, env) -> {
			dispatcher.register(literal("ping").requires(Permissions.require("simplecabinet.server.ping"))
					.executes(context -> {
						ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
						int currentPlayerCount = player.getServer().getCurrentPlayerCount();
						int maxPlayerCount = player.getServer().getMaxPlayerCount();
						List<String> users = (List) player.getPlayerListName();
						api.adminPost(String.format("/servers/name/%s/ping",CONFIG.serverName), new SimpleCabinetPing.PingRequest(currentPlayerCount,maxPlayerCount ,users ), ServersDto.PingResponseDto.class).getOrThrow();
						player.sendMessage(Text.of(String.format("Current Players: %d, Max Players: %s", currentPlayerCount,users )));
						return 1;
					}));
			dispatcher.register(literal("shop").then(literal("all").requires(Permissions.require("simplecabinet.shop.all"))
					.executes(context -> {
						ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
						Type type = new TypeToken<PageDto<ItemDeliveryDto>>() {}.getType();
						SimpleCabinetResponse<PageDto<ItemDeliveryDto>> result = api.adminGet(String.format("admin/delivery/user/%s/0", player.getGameProfile().getName()), type);
						if(!result.isSuccess()) {
							player.sendMessage(Text.of(String.format("Failed: %s", result.error)));
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

	private void sendPlayerCountToAPI() {
		if (server != null) {
			PlayerManager playerManager = server.getPlayerManager();
			Iterable<ServerPlayerEntity> players = playerManager.getPlayerList();
			int currentPlayerCount = 0;
			List<String> playerNames = new ArrayList<>();
			for (ServerPlayerEntity player : players) {
				currentPlayerCount++;
				playerNames.add(player.getGameProfile().getName());
			}
			int maxPlayerCount = server.getMaxPlayerCount();
			LOGGER.info("Send data to Api");
			api.adminPost(String.format("/servers/name/%s/ping", CONFIG.serverName),
					new SimpleCabinetPing.PingRequest(currentPlayerCount, maxPlayerCount, playerNames),
					ServersDto.PingResponseDto.class).getOrThrow();
		}
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
	public static class ServerRequest {
		public String name;
		public String displayName;


		public ServerRequest(String serverName, String serverFromServer) {
			this.name = serverName;
			this.displayName = serverFromServer;
		}
	}
}

