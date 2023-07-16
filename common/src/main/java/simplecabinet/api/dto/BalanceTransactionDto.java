package simplecabinet.api.dto;

public class BalanceTransactionDto {
    public final long id;
    public final long userId;
    public final long fromId;
    public final long toId;
    public final double fromCount;
    public final double toCount;
    public final boolean multicurrency;
    public final String comment;

    public BalanceTransactionDto(long id, long userId, long fromId, long toId, double fromCount, double toCount, boolean multicurrency, String comment) {
        this.id = id;
        this.userId = userId;
        this.fromId = fromId;
        this.toId = toId;
        this.fromCount = fromCount;
        this.toCount = toCount;
        this.multicurrency = multicurrency;
        this.comment = comment;
    }
}
