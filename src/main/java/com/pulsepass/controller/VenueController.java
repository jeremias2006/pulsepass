package com.pulsepass.controller;

import com.pulsepass.dto.response.VenueResponse;
import com.pulsepass.service.VenueService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints REST de recintos (venues), solo lectura y públicos: {@code /api/venues}.
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
@RequestMapping("/api/venues")
public class VenueController {

    private final VenueService venueService;   // inyectado por constructor

    public VenueController(VenueService venueService) {
        this.venueService = venueService;
    }

    // GET /api/venues/active
    // Público. Lista los recintos activos.
    // (Spring da prioridad a la ruta literal sobre /{code})
    @GetMapping("/active")
    public ResponseEntity<List<VenueResponse>> findActiveVenues() {

        return ResponseEntity.ok(venueService.findActiveVenues());
    }

    // GET /api/venues/{code}
    // Público. Busca un recinto por su código; si no existe, 404.
    @GetMapping("/{code}")
    public ResponseEntity<VenueResponse> findByCode(
            @PathVariable String code) {

        return ResponseEntity.ok(venueService.findByCode(code));
    }
}
