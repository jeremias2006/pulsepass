package com.pulsepass;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada de PulsePass (API REST de eventos, artistas y tickets).
 *
 * <p>{@code @SpringBootApplication} reúne tres cosas: {@code @Configuration} (esta clase puede declarar beans),
 * {@code @EnableAutoConfiguration} (Spring Boot configura solo lo que detecta en el classpath: JPA, Flyway,
 * Security, MVC...) y {@code @ComponentScan}, que busca {@code @Component}, {@code @Service}, {@code @RestController}...
 * en este paquete ({@code com.pulsepass}) y en todos los subpaquetes. Por eso todas las clases del proyecto deben vivir
 * dentro de {@code com.pulsepass}.</p>
 */
@SpringBootApplication
public class PulsepassApplication {

    public static void main(String[] args) {
        // Arranca el contenedor de Spring (crea los beans, aplica las migraciones de Flyway) y levanta el servidor web
        // embebido (Tomcat por defecto). Como no hay server.port configurado, escucha en el puerto 8080.
        SpringApplication.run(PulsepassApplication.class, args);
    }

}
