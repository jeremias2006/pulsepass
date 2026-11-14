package com.pulsepass.controller;

import com.pulsepass.dto.response.ArtistResponse;
import com.pulsepass.service.ArtistService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints REST de artistas (solo lectura, públicos): {@code /api/artists}.
 *
 * <p>Capa Controller: solo traduce entre HTTP y Java. Lee la petición (ruta, parámetros, cuerpo JSON),
 * delega TODO el trabajo en el servicio y devuelve un {@code ResponseEntity} con el código HTTP y el
 * cuerpo. No contiene reglas de negocio ni habla con los repositorios.</p>
 *
 * <p>Anotaciones habituales: {@code @RestController} (cada método devuelve datos que Spring serializa
 * a JSON), {@code @RequestMapping} (prefijo común de las rutas) y {@code @GetMapping} /
 * {@code @PostMapping} / {@code @PatchMapping} (verbo HTTP + ruta). Los errores no se manejan aquí:
 * las excepciones las traduce {@code GlobalExceptionHandler}.</p>
 */
@RestController
@RequestMapping("/api/artists")
public class ArtistController {

    // Único colaborador: el servicio. Inmutable (final) e inyectado por constructor.
    private final ArtistService artistService;

    public ArtistController(ArtistService artistService) {
        this.artistService = artistService;
    }

    // GET /api/artists/active
    // Público. Lista los artistas activos. Responde 200.
    @GetMapping("/active")
    public ResponseEntity<List<ArtistResponse>> findActiveArtists() {

        return ResponseEntity.ok(artistService.findActiveArtists());
    }

    // GET /api/artists/by-stage-name?stageName=Solar Beat
    // Público. @RequestParam lee ?stageName= de la URL y es obligatorio (si falta, 400).
    // Si no existe un artista con ese nombre, el servicio lanza ResourceNotFoundException -> 404.
    @GetMapping("/by-stage-name")
    public ResponseEntity<ArtistResponse> findByStageName(
            @RequestParam String stageName) {

        return ResponseEntity.ok(artistService.findByStageName(stageName));
    }

    // GET /api/artists/{id}
    // Público. @PathVariable toma {id} de la ruta y lo convierte a Long: si no es numérico
    // (p. ej. /api/artists/abc) responde 400. Las rutas literales (/active, /by-stage-name)
    // tienen prioridad sobre esta ruta con variable.
    @GetMapping("/{id}")
    public ResponseEntity<ArtistResponse> findById(
            @PathVariable Long id) {

        return ResponseEntity.ok(artistService.findById(id));
    }
}
