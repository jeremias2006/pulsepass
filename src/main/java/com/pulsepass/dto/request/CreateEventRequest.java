package com.pulsepass.dto.request;

import com.pulsepass.domain.EventCategory;

import java.time.LocalDate;

public record CreateEventRequest(
        String eventCode,
        String name,
        String description,
        EventCategory category,
        LocalDate eventDate,
        Integer minimumAge,
        String venueCode
) {
}
