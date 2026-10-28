package com.pulsepass.service;

import com.pulsepass.domain.TicketType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;


/**
 * Calcula el precio de un ticket a partir de su tipo ({@link TicketType}).
 *
 * <p>Es un {@code @Component} sin estado y sin acceso a base de datos. El precio NO lo envía el
 * cliente: el request solo indica el tipo y el servidor decide cuánto cuesta, así nadie puede
 * manipular lo que paga. Lo invoca {@code TicketServiceImpl.purchase} (ver BR-TICKET-009).</p>
 *
 * <p>Fórmula: {@code precio = BASE_PRICE x multiplicador del tipo}, redondeado a 2 decimales.</p>
 */
@Component
public class TicketPriceCalculator {

    // Precio base: lo que cuesta una entrada GENERAL. Los demás tipos son múltiplos de este valor.
    // Se construye con String ("50.00") y no con double para evitar errores de coma flotante.
    static final BigDecimal BASE_PRICE = new BigDecimal("50.00");

    /**
     * @param type tipo de ticket solicitado
     * @return precio final con 2 decimales (GENERAL 50.00, STUDENT 35.00, VIP 100.00, BACKSTAGE 150.00)
     */
    public BigDecimal calculate(TicketType type) {
        // "switch expression" sobre un enum: el compilador obliga a cubrir TODOS los tipos,
        // por eso no hay "default": si se agrega un TicketType nuevo, este código deja de compilar
        // hasta que se le asigne un multiplicador.
        BigDecimal multiplier = switch (type) {
            case GENERAL -> BigDecimal.ONE;                // x1.00 -> 50.00
            case STUDENT -> new BigDecimal("0.70");        // x0.70 -> 35.00 (30 % de descuento)
            case VIP -> new BigDecimal("2.00");            // x2.00 -> 100.00
            case BACKSTAGE -> new BigDecimal("3.00");      // x3.00 -> 150.00
        };
        // setScale(2, HALF_UP): deja exactamente 2 decimales y redondea .5 hacia arriba (como en dinero).
        return BASE_PRICE.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }
}