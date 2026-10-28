package com.pulsepass.service.impl;

import com.pulsepass.dto.response.VenueResponse;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.VenueMapper;
import com.pulsepass.repository.VenueRepository;
import com.pulsepass.service.VenueService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Implementación de {@link VenueService}. Todos sus métodos son de solo lectura. */
@Service
@Transactional(readOnly = true)
public class VenueServiceImpl implements VenueService {

    private final VenueRepository venueRepository;
    private final VenueMapper venueMapper;

    public VenueServiceImpl(VenueRepository venueRepository, VenueMapper venueMapper) {
        this.venueRepository = venueRepository;
        this.venueMapper = venueMapper;
    }

    // Optional.map convierte la entidad a DTO si existe; si está vacío, orElseThrow lanza 404.
    @Override
    public VenueResponse findByCode(String code) {
        return venueRepository.findByCode(code)
                .map(venueMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found: " + code));
    }

    // El nombre del método del repositorio ya expresa el filtro (active = true) y el orden (por nombre).
    @Override
    public List<VenueResponse> findActiveVenues() {
        return venueRepository.findByActiveTrueOrderByNameAsc()
                .stream()
                .map(venueMapper::toResponse)
                .toList();
    }
}
