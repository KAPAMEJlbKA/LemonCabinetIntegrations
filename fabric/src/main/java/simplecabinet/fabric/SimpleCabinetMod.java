package simplecabinet.fabric;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import java.util.*;
import java.util.List;
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
	private Map<String, String> languageMap = new HashMap<>();

	@Override
	public void onInitialize() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			this.server = server;
		});
		LOGGER.info("Lemon brick integrator");
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
		LOGGER.info("Server info registered.");
		loadLanguageFiles(CONFIG.language);
		LOGGER.info("Language load");
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			LOGGER.info("Server is stopping. Cleaning up resources...");
			executor.shutdown();
		});

        CommandRegistrationCallback.EVENT.register((dispatcher, register, env) -> {
			dispatcher.register(literal("ping").requires(Permissions.require("simplecabinet.admin.server.ping"))
					.executes(context -> {
						ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
						int currentPlayerCount = player.getServer().getCurrentPlayerCount();
						int maxPlayerCount = player.getServer().getMaxPlayerCount();
						List<String> users = (List) player.getPlayerListName();
						Path configDir = FabricLoader.getInstance().getConfigDir();
						Path lemonCabinetDir = configDir.resolve("LemonCabinet");
						Path langDir = lemonCabinetDir.resolve("lang");
						api.adminPost(String.format("/servers/name/%s/ping",CONFIG.serverName), new SimpleCabinetPing.PingRequest(currentPlayerCount,maxPlayerCount ,users ), ServersDto.PingResponseDto.class).getOrThrow();
                        try {
                            player.sendMessage(Text.of(String.format(Localizations.getMessage("commands.ping.success", CONFIG.language), currentPlayerCount,users )));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        return 1;
					}));
			dispatcher.register(literal("card").then(literal("all").requires(Permissions.require("economy.card.all"))
					.executes(context -> {
						ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
						Type type = new TypeToken<PageDto<ItemDeliveryDto>>() {}.getType();
						SimpleCabinetResponse<PageDto<ItemDeliveryDto>> result = api.adminGet(String.format("admin/delivery/user/%s/0", player.getGameProfile().getName()), type);
						if(!result.isSuccess()) {
							try {
							player.sendMessage(Text.of(String.format(Localizations.getMessage("shop.fall", CONFIG.language), result.error)));
							} catch (IOException ex) {
								throw new RuntimeException(ex);
							}
							return 1;
						}
						PageDto<ItemDeliveryDto> list = result.getOrThrow();
						for(ItemDeliveryDto e : list.data) {
							var parsed = ItemDeliveryHelper.parse(e);
							int delivered = ItemDeliveryHelper.delivery(parsed, player, (int) e.part);
							SimpleCabinetResponse<Void> resultDelivery = api.adminPost(String.format("admin/delivery/id/%d/setpart", e.id), new SetPartRequest(delivered), Void.class);
							if(!resultDelivery.isSuccess()) {
								try {
								LOGGER.warn(String.format(Localizations.getMessage("shop.item.fall", CONFIG.language), e.id));
								} catch (IOException ex) {
									throw new RuntimeException(ex);
								}
							}
						}
						try {
						player.sendMessage(Text.of(String.format(Localizations.getMessage("shop.secsess", CONFIG.language), list.data.size(), list.totalElements)));
						} catch (IOException ex) {
							throw new RuntimeException(ex);
						}
						return 1;
					})));
			dispatcher.register(literal("balance").requires(Permissions.require("economy.balance")).executes(context -> {
				String currency = CONFIG.defaultCurrency;
				PlayerEntity player = context.getSource().getPlayerOrThrow();
				try {
				player.sendMessage(Text.of(String.format(Localizations.getMessage("economy.balance", CONFIG.language), economy.getBalance(player.getUuid(), currency), currency)));
					} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
				return 1;
			}));
			dispatcher.register(literal("pay").requires(Permissions.require("economy.pay"))
									.then(argument("user", EntityArgumentType.player())
									.then(argument("amount", IntegerArgumentType.integer(1))
									.then(argument("comment", StringArgumentType.string()).executes(context -> {
						String currency = CONFIG.defaultCurrency;
						PlayerEntity player = context.getSource().getPlayerOrThrow();
						PlayerEntity target = EntityArgumentType.getPlayer(context, "user");
						int amount = IntegerArgumentType.getInteger(context, "amount");
						String comment = StringArgumentType.getString(context, "comment");
						Text playername_text = player.getName();
						String playername= playername_text.toString();
						Text targetname_text = target.getName();
						String targetname= targetname_text.toString();
						try {
							BalanceTransactionDto dto = economy.transfer(player.getUuid(), currency, target.getUuid(), currency, amount,true,
									comment, false);
							try {
								player.sendMessage(Text.of(String.format(Localizations.getMessage("economy.transfer.successful.player",CONFIG.language),targetname.replace("literal", "").replace("{", "").replace("}", ""))));
								target.sendMessage(Text.of(String.format(Localizations.getMessage("economy.transfer.successful.target",CONFIG.language),playername.replace("literal", "").replace("{", "").replace("}", ""),amount,currency)));
							} catch (IOException ex) {
								throw new RuntimeException(ex);
							}
						} catch (SimpleCabinetAPI.SimpleCabinetException e) {
                            try {
                                player.sendMessage(Text.of(String.format(Localizations.getMessage("economy.transfer.aborted", CONFIG.language), e.getMessage())));
                            } catch (IOException ex) {
                                throw new RuntimeException(ex);
                            }
                        }
						return 1;
					})))));
			dispatcher.register(literal("cashe").requires(Permissions.require("economy.cashe"))
					.then(argument("user", EntityArgumentType.player())
							.then(argument("amount", IntegerArgumentType.integer(1))
									.then(argument("comment", StringArgumentType.string()).executes(context -> {
										String currency = CONFIG.defaultCurrency;
										PlayerEntity player = context.getSource().getPlayerOrThrow();
										PlayerEntity target = EntityArgumentType.getPlayer(context, "user");
										int amount = IntegerArgumentType.getInteger(context, "amount");
										String comment = StringArgumentType.getString(context, "comment");
										Text targetname_text = target.getName();
										String targetname= targetname_text.toString();
										try {
											BalanceTransactionDto dto = economy.addMoney(target.getUuid(), currency, amount, comment);
											try {
												player.sendMessage(Text.of(String.format(Localizations.getMessage("economy.cashe.successful.player",CONFIG.language), targetname.replace("literal", "").replace("{", "").replace("}", ""), amount)));
												target.sendMessage(Text.of(String.format(Localizations.getMessage("economy.cashe.successful.target",CONFIG.language), amount, currency)));
											}
											catch (IOException ex) {
												throw new RuntimeException(ex);
											}
										} catch (SimpleCabinetAPI.SimpleCabinetException e) {
											try {
												player.sendMessage(Text.of(String.format(Localizations.getMessage("economy.cashe.aborted", CONFIG.language), e.getMessage())));
											} catch (IOException ex) {
												throw new RuntimeException(ex);
											}
										}
										return 1;
									})))));
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
		Path configDir = FabricLoader.getInstance().getConfigDir();
		Path lemonCabinetDir = configDir.resolve("LemonCabinet");
		if (!Files.exists(lemonCabinetDir)) {
			Files.createDirectories(lemonCabinetDir);
		}
		Path configPath = lemonCabinetDir.resolve("config.json");

		if(!Files.exists(configPath)) {
			CONFIG = new Config();
			try(Writer writer = new FileWriter(configPath.toFile())) {
				configGson.toJson(CONFIG, writer);
			}
		}

		try(Reader reader = new FileReader(configPath.toFile())) {
			CONFIG = configGson.fromJson(reader, Config.class);
		}
	}

	private void loadLanguageFiles(String languageCode) {
		Path configDir = FabricLoader.getInstance().getConfigDir();
		Path lemonCabinetDir = configDir.resolve("LemonCabinet");
		Path langDir = lemonCabinetDir.resolve("lang");
		Path langFile = langDir.resolve(languageCode + ".json");

		if (Files.exists(langFile)) {
			try (FileReader fileReader = new FileReader(langFile.toFile())) {
				JsonObject jsonObject = JsonParser.parseReader(fileReader).getAsJsonObject();
				Gson gson = new Gson();
				languageMap = gson.fromJson(jsonObject, new TypeToken<Map<String, String>>() {}.getType());
			} catch (IOException e) {
				LOGGER.error("Failed to load language file: " + langFile.toString(), e);
			}
		} else {
			LOGGER.warn("Language file not found: " + langFile.toString());
			try {
				Files.createDirectories(langDir); // Создаем каталог, если его нет
				Localizations.createMissingLanguageFiles(langDir);
				LOGGER.info("Missing language files created successfully.");
			} catch (IOException e) {
				LOGGER.error("Failed to create missing language files.", e);
			}
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

