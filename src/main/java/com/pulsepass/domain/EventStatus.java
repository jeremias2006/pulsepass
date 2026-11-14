package com.pulsepass.domain;

/**
 * Estado del ciclo de vida de un evento. Se guarda como texto en {@code events.status} (CHECK chk_event_status en V1).
 *
 * <p>Flujo actual: DRAFT (al crear) -> PUBLISHED ({@code EventServiceImpl.publish}) -> SOLD_OUT (cuando se vende el
 * último cupo, en {@code TicketServiceImpl.purchase}). CANCELLED y FINISHED existen en el modelo y las reglas los
 * consideran (p. ej. BR-EVENT-011: no se agregan artistas), pero hoy ningún servicio asigna esos dos estados.</p>
 */
public enum EventStatus {
    DRAFT,
    PUBLISHED,
    SOLD_OUT,
    CANCELLED,
    FINISHED
}