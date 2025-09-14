package io.clearstreet.swdn.position;

import io.clearstreet.swdn.api.PositionApi;
import io.clearstreet.swdn.api.TradeApi;
import io.clearstreet.swdn.model.*;
import io.clearstreet.swdn.refdata.ReferenceDataRepository;

import java.util.*;

public class PositionManager implements TradeApi, PositionApi {

    private final ReferenceDataRepository referenceDataManager;
    private static final List<Trade> trades = new ArrayList<>();

    public PositionManager(ReferenceDataRepository referenceDataManager) {
        this.referenceDataManager = referenceDataManager;
    }

    public static void clearTrades() {
        trades.clear();
    }

    @Override
    public boolean enterTrade(Trade trade) {
        if (trade.tradeType() == TradeType.REPLACE || trade.tradeType() == TradeType.CANCEL) {
            Optional<Trade> prevTrade = trades.stream().filter(t -> t.tradeId().equals(trade.tradeId())).findFirst();
            if (prevTrade.isPresent()) {
                trades.remove(prevTrade.get());
            } else
                return false;
        }
        if (trade.tradeType() == TradeType.NEW || trade.tradeType() == TradeType.REPLACE)
            trades.add(trade);
        return true;
    }

    @Override
    public List<Position> getPositionsForMember(String memberName) {
        Map<PositionKey, Position> positions = new HashMap<>();
        List<String> accounts = referenceDataManager.getMemberAccounts(memberName);
        for (Trade trade : trades) {
            if (accounts.contains(trade.accountName())) {
                PositionKey key = new PositionKey(memberName, trade.instrumentName());
                Position position = positions.get(key);
                if (position == null) {
                    position = new Position(memberName, trade.instrumentName(), 0, 0);
                }
                int tradeSide = TradeSide.BUY.equals(trade.side()) ? 1 : -1;
                positions.put(key, new Position(memberName, trade.instrumentName(),
                        position.quantity() + trade.quantity() * tradeSide,
                        position.initialValue() + trade.quantity() * trade.price() * tradeSide));
            }
        }
        return new ArrayList<>(positions.values());
    }

    @Override
    public List<Position> getPositionsForAccount(String accountName) {
        Map<PositionKey, Position> positions = new HashMap<>();
        for (Trade trade : trades) {
            if (trade.accountName().equals(accountName)) {
                PositionKey key = new PositionKey(trade.accountName(), trade.instrumentName());
                Position position = positions.get(key);
                if (position == null) {
                    position = new Position(trade.accountName(), trade.instrumentName(), 0, 0);
                }
                int tradeSide = TradeSide.BUY.equals(trade.side()) ? 1 : -1;
                positions.put(key, new Position(trade.accountName(), trade.instrumentName(),
                        position.quantity() + trade.quantity() * tradeSide,
                        position.initialValue() + trade.quantity() * trade.price() * tradeSide));
            }
        }
        return new ArrayList<>(positions.values());
    }

    // portfoloioName in Position could be an accountName or memberName
    private record PositionKey(String portfolioName, String instrumentName) {

    }
}
