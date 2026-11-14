package com.pulsepass.domain;

/**
 * Estado de un ticket. Se guarda como texto en {@code tickets.status} (CHECK chk_ticket_status en V1).
 *
 * <p>PAID (al comprar) -> CANCELLED ({@code cancel}) o USED ({@code markAsUsed}). RESERVED existe en el modelo y
 * en la base, pero el flujo actual no lo usa: la compra crea el ticket directamente como PAID.</p>
 */
public enum TicketStatus {
    RESERVED,
    PAID,
    CANCELLED,
    USED
}