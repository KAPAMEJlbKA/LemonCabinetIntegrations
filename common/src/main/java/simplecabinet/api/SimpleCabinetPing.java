package simplecabinet.api;

public class SimpleCabinetPing {
    public static class PingRequest {
        private int online;
        private int maxOnline;

        public PingRequest(int online, int maxOnline) {
            this.online = online;
            this.maxOnline = maxOnline;
        }

        public int getCurrentPlayerCount() {
            return online;
        }

        public void setCurrentPlayerCount(int online) {
            this.online = online;
        }

        public int getMaxPlayerCount() {
            return maxOnline;
        }

        public void setMaxPlayerCount(int maxOnline) {
            this.maxOnline = maxOnline;
        }
    }
}
