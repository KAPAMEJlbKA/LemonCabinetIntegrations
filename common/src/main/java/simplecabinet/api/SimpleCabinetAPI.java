package simplecabinet.api;

import com.google.gson.*;

import java.io.*;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

public class SimpleCabinetAPI {
    private final boolean DEBUG = false;
    private final Logger logger = Logger.getLogger("SimpleCabinetAPI");
    private final String baseUrl;
    private final String token;
    private final Gson gson;
    public <R> SimpleCabinetResponse<R> get(String url, Type typeOfResult) throws SimpleCabinetException {
        return request("GET", url, null, typeOfResult, null);
    }

    public <T,R> SimpleCabinetResponse<R> get(String url, T request, Type typeOfResult) throws SimpleCabinetException {
        return request("GET", url, request, typeOfResult, null);
    }

    public <R> SimpleCabinetResponse<R> adminGet(String url, Type typeOfResult) throws SimpleCabinetException {
        return request("GET", url, null, typeOfResult, token);
    }

    public <T,R> SimpleCabinetResponse<R> adminGet(String url, T request, Type typeOfResult) throws SimpleCabinetException {
        return request("GET", url, request, typeOfResult, token);
    }

    public <T,R> SimpleCabinetResponse<R> post(String url, T request, Type typeOfResult) throws SimpleCabinetException {
        return request("POST", url, request, typeOfResult, null);
    }

    public <T,R> SimpleCabinetResponse<R> adminPost(String url, T request, Type typeOfResult) throws SimpleCabinetException {
        return request("POST", url, request, typeOfResult, token);
    }

    public <T,R> SimpleCabinetResponse<R> put(String url, T request, Type typeOfResult) throws SimpleCabinetException {
        return request("PUT", url, request, typeOfResult, null);
    }

    public <T,R> SimpleCabinetResponse<R> adminPut(String url, T request, Type typeOfResult) throws SimpleCabinetException {
        return request("PUT", url, request, typeOfResult, token);
    }

    public <T,R> SimpleCabinetResponse<R> request(String method, String url, T request, Type typeOfResult, String token) throws SimpleCabinetException {
        try {
            URL requestUrl = new URL(baseUrl.concat(url));
            HttpURLConnection c = (HttpURLConnection) requestUrl.openConnection();
            c.setRequestMethod(method);
            c.setRequestProperty("Accept", "application/json");
            c.setRequestProperty("Content-Type", "application/json");
            if(token != null) {
                c.setRequestProperty("Authorization", "Bearer ".concat(token));
            }
            if(DEBUG) {
                logger.info(String.format("Request URI: %s %s", method, url));
            }
            c.setDoInput(true);
            if(request != null) {
                c.setDoOutput(true);
                if(DEBUG) {
                    logger.info(String.format("Request: %s", gson.toJson(request)));
                }
                try(OutputStreamWriter writer = new OutputStreamWriter(c.getOutputStream())) {
                    gson.toJson(request, writer);
                }
            }
            int code = c.getResponseCode();
            R data = null;
            String error = null;
            {
                InputStream stream;
                if(code < 200 || code >= 300) {
                    stream = c.getErrorStream();
                } else {
                    stream = c.getInputStream();
                }
                try(InputStreamReader reader = new InputStreamReader(stream)) {
                    JsonObject element = gson.fromJson(reader, JsonObject.class);
                    if(DEBUG) {
                        logger.info(String.format("Response: %s", element));
                    }
                    if(element != null) {
                        if(!element.has("error")) {
                            data = gson.fromJson(element, typeOfResult);
                        } else {
                            error = element.get("error").getAsString();
                        }
                    }
                }
            }
            return new SimpleCabinetResponse<>(data, code, error);
        } catch (IOException ex) {
            throw new SimpleCabinetException(ex);
        }
    }

    public SimpleCabinetAPI(String baseUrl, String token) {
        this.baseUrl = baseUrl;
        this.token = token;
        this.gson = new GsonBuilder().create();
    }
    public final class LocalDateTimeTypeAdapter implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
        private final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        @Override
        public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.format(FORMATTER));
        }

        @Override
        public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return LocalDateTime.parse(json.getAsString(), FORMATTER);
        }
    }

    public static class SimpleCabinetException extends RuntimeException {
        public SimpleCabinetException() {
        }

        public SimpleCabinetException(String s) {
            super(s);
        }

        public SimpleCabinetException(String s, Throwable throwable) {
            super(s, throwable);
        }

        public SimpleCabinetException(Throwable throwable) {
            super(throwable);
        }
    }

}
