package simplecabinet.bukkit;

public class Config {
    public String url = "URL";
    public String token = "TOKEN";
    public EconomyConfig economy = new EconomyConfig();
    public boolean testOnStartup;
    public static class EconomyConfig {
        public boolean enabled = true;
        public boolean vault = true;
        public String defaultCurrency = "ECO";
        public String transactionComment = "Economy";
        public boolean bankEmulator = false;
    }
}
