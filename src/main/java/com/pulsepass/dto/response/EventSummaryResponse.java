package com.pulsepass.dto.response;

import com.pulsepass.domain.EventCategory;
import com.pulsepass.domain.EventStatus;

import java.time.LocalDate;

public record EventSummaryResponse(
        Long id,
        String eventCode,
        String name,
        EventCategory category,
        EventStatus status,
        LocalDate eventDate,
        String venueName
) {
}
