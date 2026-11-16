package com.pulsepass.service.impl;

import com.pulsepass.dto.response.ArtistResponse;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.ArtistMapper;
import com.pulsepass.repository.ArtistRepository;
import com.pulsepass.service.ArtistService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Implementación de {@link ArtistService}. Todos sus métodos son de solo lectura. */
@Service
@Transactional(readOnly = true)
public class ArtistServiceImpl implements ArtistService {

    private final ArtistRepository artistRepository;
    private final ArtistMapper artistMapper;

    public ArtistServiceImpl(ArtistRepository artistRepository, ArtistMapper artistMapper) {
        this.artistRepository = artistRepository;
        this.artistMapper = artistMapper;
    }

    // Optional.map convierte la entidad a DTO si existe; si está vacío, orElseThrow lanza 404.
    @Override
    public ArtistResponse findById(Long id) {
        return artistRepository.findById(id)
                .map(artistMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Artist not found: " + id));
    }

    // IgnoreCase en el método del repositorio: "Solar Beat" y "solar beat" encuentran al mismo artista.
    @Override
    public ArtistResponse findByStageName(String stageName) {
        return artistRepository.findByStageNameIgnoreCase(stageName)
                .map(artistMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Artist not found: " + stageName));
    }

    // El nombre del método del repositorio ya expresa el filtro (active = true) y el orden (por stageName).
    @Override
    public List<ArtistResponse> findActiveArtists() {
        return artistRepository.findByActiveTrueOrderByStageNameAsc()
                .stream()
                .map(artistMapper::toResponse)
                .toList();
    }
}
