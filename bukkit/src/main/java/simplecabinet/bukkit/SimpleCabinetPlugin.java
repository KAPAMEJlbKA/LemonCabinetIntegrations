package simplecabinet.bukkit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import simplecabinet.api.SimpleCabinetAPI;
import simplecabinet.api.SimpleCabinetEconomy;
import simplecabinet.api.dto.UserDto;
import simplecabinet.bukkit.commands.EconomyCommand;
import simplecabinet.bukkit.commands.ShopCommand;
import simplecabinet.bukkit.economy.VaultEconomyBridge;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

public final class SimpleCabinetPlugin extends JavaPlugin {
    private transient final Logger logger = getLogger();
    public SimpleCabinetAPI api;
    public SimpleCabinetEconomy economy;
    public Config config;
    public Path dataFolder;
    private final Gson configGson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    @Override
    public void onEnable() {
        // Plugin startup logic
        try {
            dataFolder = getDataFolder().toPath();
            Files.createDirectories(dataFolder);
            loadConfig(dataFolder.resolve("config.json"));
            if(config.url == null || config.url.equals("URL")) {
                logger.severe("Please configure config.json");
                return;
            }
            api = new SimpleCabinetAPI(config.url, config.token);
            if(config.testOnStartup) {
                UserDto result = api.<UserDto>adminGet("/auth/userinfo", UserDto.class).getOrThrow();
                logger.info(String.format("Logged-in %s", result.username));
            }
            getCommand("shop").setExecutor(new ShopCommand(this));
            // Economy
            economy = new SimpleCabinetEconomy(api);
            Bukkit.getServer().getServicesManager().register(SimpleCabinetEconomy.class, economy, this, ServicePriority.High);
            if(config.economy.vault) {
                Bukkit.getServer().getServicesManager().register(Economy.class, new VaultEconomyBridge(this), this, ServicePriority.Highest);
            }
            getCommand("economy").setExecutor(new EconomyCommand(this));
            logger.info("SimpleCabinet Plugin loaded");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadConfig(Path path) throws IOException {
        if(!Files.exists(path)) {
            config = new Config();
            saveConfig(path);
            return;
        }
        try(Reader reader = new FileReader(path.toFile())) {
            config = configGson.fromJson(reader, Config.class);
        }
    }

    public void saveConfig(Path path) throws IOException {
        try(Writer writer = new FileWriter(path.toFile())) {
            configGson.toJson(config, writer);
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        try {
            saveConfig(dataFolder.resolve("config.json"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
