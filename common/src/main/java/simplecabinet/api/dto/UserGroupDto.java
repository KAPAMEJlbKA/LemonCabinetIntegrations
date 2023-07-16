package simplecabinet.api.dto;

public class UserGroupDto {
    public final long id;
    public final String groupName;

    public UserGroupDto(long id, String groupName) {
        this.id = id;
        this.groupName = groupName;
    }
}
