package simplecabinet.api.dto;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class UserDto {
    public final long id;
    public final String username;
    public final UUID uuid;
    public final Gender gender;
    public final String status;
    public final List<UserGroupDto> groups;
    public final UserTexture skin;
    public final UserTexture cloak;

    public UserDto(long id, String username, UUID uuid, Gender gender, String status, List<UserGroupDto> groups, UserTexture skin, UserTexture cloak) {
        this.id = id;
        this.username = username;
        this.uuid = uuid;
        this.gender = gender;
        this.status = status;
        this.groups = groups;
        this.skin = skin;
        this.cloak = cloak;
    }

    public static class UserTexture {
        public final String url;
        public final String digest;
        public final Map<String, String> metadata;

        public UserTexture(String url, String digest, Map<String, String> metadata) {
            this.url = url;
            this.digest = digest;
            this.metadata = metadata;
        }
    }

    public enum Gender {
        FEMALE,
        MALE
    }
}
