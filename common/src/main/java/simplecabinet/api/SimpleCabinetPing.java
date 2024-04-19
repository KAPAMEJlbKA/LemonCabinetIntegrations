package simplecabinet.api;

import simplecabinet.api.dto.UserDto;

import java.util.List;

public class SimpleCabinetPing {
    public static class PingRequest {
        private int online;
        private int maxOnline;

        private List<String> users;


        public PingRequest(int online,int maxOnline, List<String> users) {
            this.online = online;
            this.maxOnline=maxOnline;
            this.users = users;
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
