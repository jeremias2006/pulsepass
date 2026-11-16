package com.pulsepass;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Configuración compartida de pruebas que declara un contenedor Docker con PostgreSQL REAL (no H2 ni un mock), para
 * probar las migraciones, las entidades y las consultas contra el mismo motor que se usa en producción.
 *
 * <p>{@code @TestConfiguration} solo existe en pruebas. {@code proxyBeanMethods = false} evita crear proxies
 * innecesarios para los métodos {@code @Bean}. Requiere Docker en ejecución; la primera vez descarga la imagen.</p>
 */
@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    // @ServiceConnection: Spring Boot detecta que este bean es un contenedor PostgreSQL y configura SOLO el DataSource
    // (url, usuario y contraseña) apuntando a él; no hace falta escribir esos datos en ningún .properties.
    // El contenedor se inicia junto con el contexto de Spring y Flyway aplica las migraciones en una base nueva vacía.
    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        // Imagen ligera (alpine) de PostgreSQL 16. Se descarga una sola vez y luego queda en caché local.
        return new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"));
    }

}
