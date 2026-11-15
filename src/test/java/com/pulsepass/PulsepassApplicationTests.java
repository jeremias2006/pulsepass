package com.pulsepass;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Prueba de humo: comprueba que TODA la aplicación arranca. Carga el contexto completo de Spring contra un PostgreSQL
 * en contenedor; si algo está mal (configuración faltante, un bean que no se puede crear, una migración que falla o una
 * entidad que no coincide con el esquema por ddl-auto=validate), la prueba falla.
 *
 * <ul>
 *   <li>{@code @SpringBootTest}: levanta el contexto completo.</li>
 *   <li>{@code @Import}: añade el contenedor de {@link TestcontainersConfiguration}.</li>
 *   <li>{@code @ActiveProfiles("test")}: activa application-test.properties (secreto JWT de pruebas).</li>
 * </ul>
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("test")
class PulsepassApplicationTests {

    @Test
    void contextLoads() {
        // Cuerpo vacío a propósito: la prueba pasa si el contexto se levanta sin errores.
    }

}
