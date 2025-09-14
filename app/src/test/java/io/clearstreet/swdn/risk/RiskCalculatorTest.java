package io.clearstreet.swdn.risk;

import static io.clearstreet.swdn.Fixtures.IBM_STOCK;
import static io.clearstreet.swdn.Fixtures.JP_MORGAN;
import static io.clearstreet.swdn.Fixtures.JP_MORGAN_ACCOUNT_1;
import static io.clearstreet.swdn.Fixtures.JP_MORGAN_POSITION_1;
import static io.clearstreet.swdn.Fixtures.JP_MORGAN_POSITION_2;
import static io.clearstreet.swdn.Fixtures.JP_MORGAN_POSITION_3;
import static io.clearstreet.swdn.Fixtures.TSLA_STOCK;

import io.clearstreet.swdn.model.Instrument;
import io.clearstreet.swdn.model.InstrumentType;
import io.clearstreet.swdn.model.Position;
import io.clearstreet.swdn.position.PositionManager;
import io.clearstreet.swdn.price.PriceRepository;
import io.clearstreet.swdn.refdata.ReferenceDataRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RiskCalculatorTest {

    public static final double DELTA = 0.0001;
    @Mock
    private PriceRepository priceRepository;
    @Mock
    private PositionManager positionManager;
    @Mock
    private ReferenceDataRepository referenceDataRepository;

    @Test
    void calculateAccountPnl() {
        // Given
        RiskCalculator riskCalculator = new RiskCalculator(positionManager, priceRepository,
                referenceDataRepository);
        Mockito.when(positionManager.getPositionsForAccount(JP_MORGAN_ACCOUNT_1.accountName()))
                .thenReturn(List.of((JP_MORGAN_POSITION_1)));
        Mockito.when(priceRepository.getPrice(IBM_STOCK.instrumentName()))
                .thenReturn(Optional.of(100.0));

        // When
        double accountPnl = riskCalculator.calculateAccountPnl(JP_MORGAN_ACCOUNT_1.accountName());

        // Then
        double expectedPnl =
                100.0 * JP_MORGAN_POSITION_1.quantity() - JP_MORGAN_POSITION_1.initialValue();
        Assertions.assertEquals(expectedPnl, accountPnl, DELTA);
    }

    @Test
    void calculateMemberPnl() {
        // Given
        RiskCalculator riskCalculator = new RiskCalculator(positionManager, priceRepository,
                referenceDataRepository);
        Mockito.when(positionManager.getPositionsForMember(JP_MORGAN.memberName()))
                .thenReturn(List.of(JP_MORGAN_POSITION_1, JP_MORGAN_POSITION_3));
        Mockito.when(priceRepository.getPrice(IBM_STOCK.instrumentName()))
                .thenReturn(Optional.of(100.0));
        Mockito.when(priceRepository.getPrice(TSLA_STOCK.instrumentName()))
                .thenReturn(Optional.of(100.0));

        // When
        double memberPnl = riskCalculator.calculateMemberPnl(JP_MORGAN.memberName());

        // Then
        double expectedPnl =
                100.0 * (JP_MORGAN_POSITION_1.quantity() + JP_MORGAN_POSITION_2.quantity()) - (
                        JP_MORGAN_POSITION_1.initialValue() + JP_MORGAN_POSITION_2.initialValue());
        Assertions.assertEquals(expectedPnl, memberPnl, DELTA);
    }

    @Test
    void calculateMemberMarketRisk() {

        RiskCalculator riskCalculator = new RiskCalculator(positionManager, priceRepository, referenceDataRepository);

        Position optionPosition = Mockito.mock(Position.class);
        Position stockPosition = Mockito.mock(Position.class);
        Mockito.when(optionPosition.instrumentName()).thenReturn("option1");
        Mockito.when(optionPosition.quantity()).thenReturn(-10.0); // Quantity can be negative on a position
        Mockito.when(stockPosition.instrumentName()).thenReturn("stock1");
        Mockito.when(stockPosition.quantity()).thenReturn(10.0);

        Instrument optionsInstrument = Mockito.mock(Instrument.class);
        Instrument stockInstrument = Mockito.mock(Instrument.class);
        Mockito.when(optionsInstrument.type()).thenReturn(InstrumentType.OPTION);
        Mockito.when(stockInstrument.type()).thenReturn(InstrumentType.STOCK);
        Mockito.when(referenceDataRepository.getInstrument("option1")).thenReturn(Optional.of(optionsInstrument));
        Mockito.when(referenceDataRepository.getInstrument("stock1")).thenReturn(Optional.of(stockInstrument));

        Mockito.when(positionManager.getPositionsForMember("member1"))
                .thenReturn(List.of(optionPosition, stockPosition));
        Mockito.when(priceRepository.getPrice("option1")).thenReturn(Optional.of(10.0));
        Mockito.when(priceRepository.getPrice("stock1")).thenReturn(Optional.of(10.0));

        // When
        double marketRisk = riskCalculator.calculateMemberMarketRisk("member1");

        // Then
        // Total UP = -15 + 20 = 5, Total DOWN = 10 + -20 = -10, Market risk = max(5, -10) = 5
        Assertions.assertEquals(5.0, marketRisk, DELTA);
    }
}