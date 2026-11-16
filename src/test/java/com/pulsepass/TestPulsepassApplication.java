package com.pulsepass;

import org.springframework.boot.SpringApplication;

/**
 * Arranque de desarrollo: ejecuta la aplicación REAL pero conectada a un PostgreSQL desechable en Docker (el de
 * {@link TestcontainersConfiguration}), de modo que no hace falta tener PostgreSQL instalado. Se lanza con "run" desde
 * el IDE. Al detenerla, el contenedor y sus datos desaparecen. Sigue necesitando la variable de entorno
 * PULSEPASS_JWT_SECRET_BASE64, porque aquí no se activa el perfil "test".
 */
public class TestPulsepassApplication {

    public static void main(String[] args) {
        // from(PulsepassApplication::main) toma el arranque normal y with(...) le añade la configuración de Testcontainers.
        SpringApplication.from(PulsepassApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
