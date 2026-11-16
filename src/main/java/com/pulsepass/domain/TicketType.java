package com.pulsepass.domain;

/**
 * Tipo de ticket. Se guarda como texto en {@code tickets.type} (CHECK chk_ticket_type en V1). El tipo determina
 * el precio: ver {@code TicketPriceCalculator} (GENERAL x1, STUDENT x0.70, VIP x2, BACKSTAGE x3 sobre el precio base).
 */
public enum TicketType {
    GENERAL,
    VIP,
    BACKSTAGE,
    STUDENT
}