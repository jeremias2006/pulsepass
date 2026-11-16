package com.pulsepass.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Configuración de apoyo para el JWT.
 * {@code @EnableConfigurationProperties} registra {@link JwtProperties} como bean, de modo que
 * pueda inyectarse (p. ej. en {@link JwtService}) con los valores del properties ya enlazados.
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

    // Inyectable para poder fijar el tiempo en los tests de JwtService
    // Bean único de reloj en UTC; lo usan JwtService, AuthService y LocalAccountsSeeder en lugar de
    // llamar a now() directamente, así las pruebas pueden sustituirlo por un reloj fijo.
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}