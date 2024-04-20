package simplecabinet.fabric;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.nio.file.Path;

public class Localizations {
    private static final Map<String, String> EN_US = new HashMap<>();
    private static final Map<String, String> RU_RU = new HashMap<>();
    private static final Map<String, Map<String, String>> LANGUAGES = new HashMap<>();


    static {
        // Здесь добавьте все текстовые сообщения на английском языке (en_us)
        EN_US.put("startup.loggedIn","Logged-in %s");
        EN_US.put("startup.info.transfer","Server info registered.");
        EN_US.put("server.stop","Server is stopping. Cleaning up resources...");
        EN_US.put("commands.ping.success", "Current Players: %d, Max Players: %d");
        EN_US.put("shop.fall","Failed: %s");
        EN_US.put("shop.item.fall","ItemDelivery %d failed");
        EN_US.put("shop.secsess","Gived %d/%d items");
        EN_US.put("economy.balance","Balance %.2f %s");
        EN_US.put("economy.transfer.successful.player","Money transfer to %s");
        EN_US.put("economy.transfer.successful.target","Money received %s in the amount %d %s");
        EN_US.put("economy.transfer.aborted","Transfer aborted!: %s");
        EN_US.put("economy.cashe.successful.player","Money is credited to the player: %s in the amount %d");
        EN_US.put("economy.cashe.successful.target","You have bean credited with money in the amount %d %s");
        EN_US.put("economy.cashe.aborted","Transaction aborted: %s");
        LANGUAGES.put("en_us", EN_US);
        // Здесь добавьте все текстовые сообщения на русском языке (ru_ru)
        RU_RU.put("startup.loggedIn","Вход по имененм %s выполнен");
        RU_RU.put("startup.info.transfer","Сервер зарегистрирован");
        RU_RU.put("server.stop","Сервер останавливается. Очистка ресурсов...");
        RU_RU.put("commands.ping.success", "Текущие игроки: %d, Максимальное количество игроков: %d");
        RU_RU.put("shop.fall","Ошибка: %s");
        RU_RU.put("shop.item.fall","Ошибка передачи предмета %d");
        RU_RU.put("shop.secsess","Получено %d  из %d предметов");
        RU_RU.put("economy.balance","Ваш текущий баланс %.2f %s");
        RU_RU.put("economy.transfer.successful.player","Деньги отправлены игроку %s");
        RU_RU.put("economy.transfer.successful.target","Вам перечисленны средства от %s в размере %d %s");
        RU_RU.put("economy.transfer.aborted","Перевод отменен!: %s");
        RU_RU.put("economy.cashe.successful.player","Начисленны средства игроку: %s в размере %d");
        RU_RU.put("economy.cashe.successful.target","Вам начисленны средства в размере %d %s");
        RU_RU.put("economy.cashe.aborted","Транзакция не удалась: %s");
        LANGUAGES.put("ru_ru", RU_RU);
        // Добавьте другие сообщения по мере необходимости
    }

    public static String getMessage(String key, String languageCode) throws IOException {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        Path lemonCabinetDir = configDir.resolve("LemonCabinet");
        Path langDir = lemonCabinetDir.resolve("lang");
        Path langFile = langDir.resolve(languageCode + ".json");
        if (Files.exists(langFile)) {
            try (FileReader fileReader = new FileReader(langFile.toFile())) {
                JsonObject jsonObject = JsonParser.parseReader(fileReader).getAsJsonObject();
                String message = jsonObject.get(key).getAsString();
                return message != null ? message : "Message not found";
            } catch (IOException e) {
                throw new IOException("Failed to read language file: " + langFile.toString(), e);
            }
        } else {
            throw new IOException("Language file not found: " + langFile.toString());
        }
    }

    public static void createMissingLanguageFiles(Path langDir) throws IOException {
        for (Map.Entry<String, Map<String, String>> entry : LANGUAGES.entrySet()) {
            String languageCode = entry.getKey();
            Map<String, String> messages = entry.getValue();
            Path langFile = langDir.resolve(languageCode + ".json");
            if (!Files.exists(langFile)) {
                Files.createFile(langFile);
                try (FileWriter writer = new FileWriter(langFile.toFile())) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("{\n");
                    for (Map.Entry<String, String> msgEntry : messages.entrySet()) {
                        String msgKey = msgEntry.getKey();
                        String msgValue = msgEntry.getValue();
                        sb.append(String.format("  \"%s\": \"%s\",\n", msgKey, msgValue));
                    }
                    sb.deleteCharAt(sb.length() - 2); // Удаляем лишнюю запятую после последней строки
                    sb.append("}\n");
                    writer.write(sb.toString());
                }
            }
        }
    }
}
