package io.clearstreet.swdn.risk;

import io.clearstreet.swdn.api.RiskApi;
import io.clearstreet.swdn.model.InstrumentType;
import io.clearstreet.swdn.model.Position;
import io.clearstreet.swdn.position.PositionManager;
import io.clearstreet.swdn.price.PriceRepository;
import io.clearstreet.swdn.refdata.ReferenceDataRepository;

public class RiskCalculator implements RiskApi {

    private final PositionManager positionManager;
    private final PriceRepository priceRepository;
    private final ReferenceDataRepository referenceDataRepository;

    public RiskCalculator(PositionManager positionManager, PriceRepository priceRepository,
                          ReferenceDataRepository referenceDataRepository) {
        this.positionManager = positionManager;
        this.priceRepository = priceRepository;
        this.referenceDataRepository = referenceDataRepository;
    }

    @Override
    public double calculateAccountPnl(String accountName) {
        double pnl = 0;
        for (Position position : positionManager.getPositionsForAccount(accountName)) {
            pnl += calculatePositionPnl(position);
        }
        return pnl;
    }

    @Override
    public double calculateMemberPnl(String memberName) {
        double pnl = 0;
        for (Position position : positionManager.getPositionsForMember(memberName)) {
            pnl += calculatePositionPnl(position);
        }
        return pnl;
    }

    public double calculateMemberMarketRisk(String memberName) {
        double totalUp = 0.0;
        double totalDown = 0.0;
        for (Position position : positionManager.getPositionsForMember(memberName)) {
            double price = priceRepository.getPrice(position.instrumentName()).orElse(0.0);
            double quantity = position.quantity();
            InstrumentType insType = referenceDataRepository.getInstrument(position.instrumentName())
                    .orElseThrow(() -> new RuntimeException("Instrument not found")).type();
            if (insType.equals(InstrumentType.OPTION)) {
                totalUp += 0.15 * price * quantity;
                totalDown += -0.10 * price * quantity;
            } else if (insType.equals(InstrumentType.STOCK)) {
                totalUp += 0.20 * price * quantity;
                totalDown += -0.20 * price * quantity;
            }
        }
        return Math.max(totalUp, totalDown);
    }

    @Override
    public double calculateMemberMargin(String memberName) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private double calculatePositionPnl(Position position) {
        double price = priceRepository.getPrice(position.instrumentName()).orElseThrow();
        return position.quantity() * price - position.initialValue();
    }
}
