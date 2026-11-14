package com.pulsepass.domain;

/**
 * Categoría de un evento. Se guarda como texto en {@code events.category}. La base restringe los valores con
 * un CHECK (chk_event_category, migración V1): agregar una categoría nueva exige también una migración nueva.
 */
public enum EventCategory {
    MUSIC,
    SPORTS,
    TECHNOLOGY,
    EDUCATION,
    CULTURE,
    ENTERTAINMENT
}