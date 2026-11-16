package com.pulsepass.service;

import com.pulsepass.dto.response.ArtistResponse;

import java.util.List;

/**
 * Contrato de consulta de artistas. Solo lectura y de acceso público.
 * La implementación es {@code ArtistServiceImpl}.
 */
public interface ArtistService {

    /** Busca un artista por su id; lanza 404 si no existe. */
    ArtistResponse findById(Long id);

    /** Busca un artista por su nombre artístico (sin distinguir mayúsculas); lanza 404 si no existe. */
    ArtistResponse findByStageName(String stageName);

    /** Lista los artistas activos ordenados por nombre artístico ascendente. */
    List<ArtistResponse> findActiveArtists();
}
