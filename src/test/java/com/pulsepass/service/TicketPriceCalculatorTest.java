package com.pulsepass.service;

import com.pulsepass.domain.TicketType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TicketPriceCalculatorTest {

    private final TicketPriceCalculator calculator = new TicketPriceCalculator();

    @Test
    void generalIsBasePrice() {
        assertThat(calculator.calculate(TicketType.GENERAL)).isEqualByComparingTo("50.00");
    }

    @Test
    void studentHasDiscount() {
        assertThat(calculator.calculate(TicketType.STUDENT)).isEqualByComparingTo("35.00");
    }

    @Test
    void vipIsDoubleBase() {
        assertThat(calculator.calculate(TicketType.VIP)).isEqualByComparingTo("100.00");
    }

    @Test
    void backstageIsTripleBase() {
        assertThat(calculator.calculate(TicketType.BACKSTAGE)).isEqualByComparingTo("150.00");
    }

    @Test
    void neverReturnsNegativePrice() {
        for (TicketType type : TicketType.values()) {
            assertThat(calculator.calculate(type).signum()).isGreaterThanOrEqualTo(0);
        }
    }
}