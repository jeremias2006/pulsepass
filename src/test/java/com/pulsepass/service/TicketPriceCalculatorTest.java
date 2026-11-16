package com.pulsepass.service;

import com.pulsepass.domain.TicketType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba unitaria PURA de TicketPriceCalculator: sin Spring, sin mocks y sin base de datos. La calculadora no tiene estado ni
 * dependencias, así que se crea con new y la prueba corre en milisegundos.
 *
 * Se usa isEqualByComparingTo("50.00") porque compara los BigDecimal por valor numérico (50.0 y 50.00 son iguales), mientras
 * que equals también compara la escala.
 */
class TicketPriceCalculatorTest {

    private final TicketPriceCalculator calculator = new TicketPriceCalculator();

    // GENERAL cuesta el precio base: 50.00.
    @Test
    void generalIsBasePrice() {
        assertThat(calculator.calculate(TicketType.GENERAL)).isEqualByComparingTo("50.00");
    }

    // STUDENT = 70 % del precio base (30 % de descuento): 35.00.
    @Test
    void studentHasDiscount() {
        assertThat(calculator.calculate(TicketType.STUDENT)).isEqualByComparingTo("35.00");
    }

    // VIP = el doble del precio base: 100.00.
    @Test
    void vipIsDoubleBase() {
        assertThat(calculator.calculate(TicketType.VIP)).isEqualByComparingTo("100.00");
    }

    // BACKSTAGE = el triple del precio base: 150.00.
    @Test
    void backstageIsTripleBase() {
        assertThat(calculator.calculate(TicketType.BACKSTAGE)).isEqualByComparingTo("150.00");
    }

    // Respalda BR-TICKET-009. Recorre TODOS los valores de TicketType: si se agrega un tipo nuevo, también se comprueba.
    @Test
    void neverReturnsNegativePrice() {
        for (TicketType type : TicketType.values()) {
            assertThat(calculator.calculate(type).signum()).isGreaterThanOrEqualTo(0);
        }
    }
}