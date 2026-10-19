package com.pulsepass.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

    // Inyectable para poder fijar el tiempo en los tests de JwtService
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}