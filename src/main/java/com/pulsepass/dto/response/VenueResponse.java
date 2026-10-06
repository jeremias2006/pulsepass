package com.pulsepass.dto.response;

public record VenueResponse(
        Long id,
        String code,
        String name,
        String city,
        Integer capacity,
        boolean active
) {
}
