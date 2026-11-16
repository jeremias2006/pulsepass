package com.pulsepass.service;

import com.pulsepass.dto.response.VenueResponse;

import java.util.List;

/**
 * Contrato de consulta de recintos (venues). Solo lectura y de acceso público.
 * La implementación es {@code VenueServiceImpl}.
 */
public interface VenueService {

    /** Busca un recinto por su código; lanza 404 si no existe. */
    VenueResponse findByCode(String code);

    /** Lista los recintos activos ordenados por nombre ascendente. */
    List<VenueResponse> findActiveVenues();
}
