package simplecabinet.api.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class ServersDto {
    public final long id;
    public final String name;
    public final String displayName;
    public final int maxOnline;
    public final int online;
    public final List<UserDto> users;
    public final String updateDate;

    public ServersDto(long id, String name, String displayName, int maxOnline, int online, List<UserDto> users, String updateDate) {
        this.id = id;
        this.name = name;
        this.displayName = displayName;
        this.maxOnline = maxOnline;
        this.online = online;
        this.users = users;
        this.updateDate = updateDate;
    }

    public class PingResponseDto {
        private int online;
        private int maxOnline;

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
