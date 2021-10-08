package simplecabinet.api;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.AtomicDouble;
import simplecabinet.api.dto.BalanceTransactionDto;
import simplecabinet.api.dto.UserBalanceDto;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

public class SimpleCabinetEconomy {
    private final SimpleCabinetAPI api;
    private final Cache<EconomyKey, EconomyValue> economyCache;

    public SimpleCabinetEconomy(SimpleCabinetAPI api) {
        this.api = api;
        this.economyCache = CacheBuilder.newBuilder()
                .build();
    }

    public double getBalance(UUID playerUUID, String currency) {
        EconomyKey key = new EconomyKey(playerUUID, currency);
        EconomyValue value = economyCache.getIfPresent(key);
        if(value == null) {
            UserBalanceDto balanceDto = fetchBalance(playerUUID, currency);
            if(balanceDto == null) {
                return -1;
            }
            value = new EconomyValue(balanceDto);
        } else if(!value.isRepresentative()) {
            UserBalanceDto balanceDto = fetchBalance(value.id);
            if(balanceDto == null) {
                return -1;
            }
            value = new EconomyValue(balanceDto);
        }
        economyCache.put(key, value);
        return value.balance.get();
    }

    public double getCachedBalance(UUID playerUUID, String currency) {
        EconomyKey key = new EconomyKey(playerUUID, currency);
        EconomyValue value = economyCache.getIfPresent(key);
        if(value == null) {
            return 0.0;
        }
        return value.balance.get();
    }

    public long getBalanceId(UUID playerUUID, String currency) {
        EconomyKey key = new EconomyKey(playerUUID, currency);
        EconomyValue value = economyCache.getIfPresent(key);
        if(value == null) {
            UserBalanceDto balanceDto = fetchBalance(playerUUID, currency);
            if(balanceDto == null) {
                return -1;
            }
            value = new EconomyValue(balanceDto);
            economyCache.put(key, value);
        }
        return value.id;
    }

    private UserBalanceDto fetchBalance(UUID playerUUID, String currency) {
        SimpleCabinetResponse<UserBalanceDto> result = api.adminGet(String.format("/admin/money/userbalance/uuid/%s/%s", playerUUID, currency), UserBalanceDto.class);
        return result.getOrThrow();
    }
    private UserBalanceDto fetchBalance(long balanceId) {
        SimpleCabinetResponse<UserBalanceDto> result = api.adminGet(String.format("/admin/money/userbalance/id/%s", balanceId), UserBalanceDto.class);
        return result.getOrThrow();
    }

    private boolean updateCachedBalance(long balanceId, double delta) {
        for(EconomyValue value : economyCache.asMap().values()) {
            if(value.id == balanceId) {
                value.balance.addAndGet(delta);
                return true;
            }
        }
        return false;
    }

    private boolean updateCachedBalance(UUID playerUUID, String currency, double delta) {
        EconomyKey key = new EconomyKey(playerUUID, currency);
        EconomyValue value = economyCache.getIfPresent(key);
        if(value == null) {
            return false;
        }
        value.balance.addAndGet(delta);
        return true;
    }

    private BalanceTransactionDto transferInternal(long fromBalanceId, long toBalanceId, double count, boolean self, String comment, boolean multicurrency) throws SimpleCabinetAPI.SimpleCabinetException {
        TransferMoneyRequest request = new TransferMoneyRequest(fromBalanceId, toBalanceId, count, self, comment, multicurrency);
        SimpleCabinetResponse<BalanceTransactionDto> result = api.adminPost("/admin/money/transfer", request, BalanceTransactionDto.class);
        return result.getOrThrow();
    }

    public BalanceTransactionDto transfer(long fromBalanceId, long toBalanceId, double count, boolean self, String comment, boolean multicurrency) throws SimpleCabinetAPI.SimpleCabinetException {
        BalanceTransactionDto dto = transferInternal(fromBalanceId, toBalanceId, count, self, comment, multicurrency);
        updateCachedBalance(fromBalanceId, dto.fromCount);
        updateCachedBalance(fromBalanceId, dto.toCount);
        return dto;
    }

    public BalanceTransactionDto transfer(UUID fromPlayerUUID, String fromCurrency, UUID toPlayerUUID, String toCurrency, double count, boolean self, String comment, boolean multicurrency) throws SimpleCabinetAPI.SimpleCabinetException {
        long fromBalanceId = getBalanceId(fromPlayerUUID, fromCurrency);
        if(fromBalanceId < 0) {
            throw new SimpleCabinetAPI.SimpleCabinetException("'From' balance not found");
        }
        long toBalanceId = getBalanceId(toPlayerUUID, toCurrency);
        if(toBalanceId < 0) {
            throw new SimpleCabinetAPI.SimpleCabinetException("'Target' balance not found");
        }
        BalanceTransactionDto dto = transferInternal(fromBalanceId, toBalanceId, count, self, comment, multicurrency);
        updateCachedBalance(fromPlayerUUID, fromCurrency, dto.fromCount);
        updateCachedBalance(toPlayerUUID, toCurrency, dto.toCount);
        return dto;
    }

    private BalanceTransactionDto addMoneyInternal(long balanceId, double count, String comment) throws SimpleCabinetAPI.SimpleCabinetException {
        AddMoneyRequest request = new AddMoneyRequest(balanceId, count, comment);
        SimpleCabinetResponse<BalanceTransactionDto> result = api.adminPost("/admin/money/addmoney", request, BalanceTransactionDto.class);
        return result.getOrThrow();
    }

    public BalanceTransactionDto addMoney(UUID playerUUID, String currency, double count, String comment) throws SimpleCabinetAPI.SimpleCabinetException {
        long balanceId = getBalanceId(playerUUID, currency);
        if(balanceId < 0) {
            throw new SimpleCabinetAPI.SimpleCabinetException("'Target' balance not found");
        }
        BalanceTransactionDto dto = addMoneyInternal(balanceId, count, comment);
        updateCachedBalance(playerUUID, currency, dto.fromCount);
        return dto;
    }

    public BalanceTransactionDto addMoney(long balanceId, double count, String comment) throws SimpleCabinetAPI.SimpleCabinetException {
        BalanceTransactionDto dto = addMoneyInternal(balanceId, count, comment);
        updateCachedBalance(balanceId, dto.fromCount);
        return dto;
    }

    private BalanceTransactionDto removeMoneyInternal(long balanceId, double count, String comment) throws SimpleCabinetAPI.SimpleCabinetException {
        AddMoneyRequest request = new AddMoneyRequest(balanceId, count, comment);
        SimpleCabinetResponse<BalanceTransactionDto> result = api.adminPost("/admin/money/removemoney", request, BalanceTransactionDto.class);
        BalanceTransactionDto dto = result.getOrThrow();
        updateCachedBalance(balanceId, -dto.fromCount);
        return dto;
    }

    public BalanceTransactionDto removeMoney(UUID playerUUID, String currency, double count, String comment) throws SimpleCabinetAPI.SimpleCabinetException {
        long balanceId = getBalanceId(playerUUID, currency);
        if(balanceId < 0) {
            throw new SimpleCabinetAPI.SimpleCabinetException("'Target' balance not found");
        }
        BalanceTransactionDto dto = removeMoneyInternal(balanceId, count, comment);
        updateCachedBalance(playerUUID, currency, dto.fromCount);
        return dto;
    }

    public BalanceTransactionDto removeMoney(long balanceId, double count, String comment) throws SimpleCabinetAPI.SimpleCabinetException {
        BalanceTransactionDto dto = removeMoneyInternal(balanceId, count, comment);
        updateCachedBalance(balanceId, dto.fromCount);
        return dto;
    }

    public static class TransferMoneyRequest {
        public long fromBalanceId;
        public long toBalanceId;
        public double count;
        public boolean selfUser;
        public String comment;
        public boolean multicurrency;

        public TransferMoneyRequest(long fromBalanceId, long toBalanceId, double count, boolean selfUser, String comment, boolean multicurrency) {
            this.fromBalanceId = fromBalanceId;
            this.toBalanceId = toBalanceId;
            this.count = count;
            this.selfUser = selfUser;
            this.comment = comment;
            this.multicurrency = multicurrency;
        }
    }

    public static class AddMoneyRequest {
        public long balanceId;
        public double count;
        public String comment;

        public AddMoneyRequest(long balanceId, double count, String comment) {
            this.balanceId = balanceId;
            this.count = count;
            this.comment = comment;
        }
    }

    private static class EconomyValue {
        public final long id;
        public final String currency;
        public final AtomicDouble balance;
        private final transient AtomicLong lastUpdateTime;

        public EconomyValue(long id, String currency, double balance) {
            this.id = id;
            this.currency = currency;
            this.balance = new AtomicDouble(balance);
            this.lastUpdateTime = new AtomicLong(System.currentTimeMillis());
        }

        public EconomyValue(UserBalanceDto balanceDto) {
            this.id = balanceDto.id;
            this.currency = balanceDto.currency;
            this.balance = new AtomicDouble(balanceDto.balance);
            this.lastUpdateTime = new AtomicLong(System.currentTimeMillis());
        }

        public boolean isRepresentative() {
            if(balance.get() == 0) {
                return System.currentTimeMillis() - lastUpdateTime.get() < 4000;
            }
            return System.currentTimeMillis() - lastUpdateTime.get() < 3000;
        }

        public void update() {
            lastUpdateTime.set(System.currentTimeMillis());
        }
    }

    private static class EconomyKey {
        private final UUID playerUUID;
        private final String currency;

        public EconomyKey(UUID playerUUID, String currency) {
            Objects.requireNonNull(playerUUID);
            Objects.requireNonNull(currency);
            this.playerUUID = playerUUID;
            this.currency = currency;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            EconomyKey that = (EconomyKey) o;
            return playerUUID.equals(that.playerUUID) && currency.equals(that.currency);
        }

        @Override
        public int hashCode() {
            return Objects.hash(playerUUID, currency);
        }
    }
}
