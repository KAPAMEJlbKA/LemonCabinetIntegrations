package simplecabinet.api.dto;

public class UserBalanceDto {
    public final long id;
    public final double balance;
    public final String currency;

    public UserBalanceDto(long id, double balance, String currency) {
        this.id = id;
        this.balance = balance;
        this.currency = currency;
    }
}
