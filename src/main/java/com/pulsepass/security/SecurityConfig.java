package com.pulsepass.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuración central de seguridad de la API REST.
 *
 * <p>Define tres cosas:</p>
 * <ol>
 *   <li>La cadena de filtros ({@code SecurityFilterChain}): qué rutas son públicas, cuáles exigen
 *       token y cuáles exigen un rol (primera capa de autorización, por URL).</li>
 *   <li>Los beans de autenticación: codificador de contraseñas (BCrypt) y {@code AuthenticationManager}.</li>
 *   <li>La política CORS: qué orígenes de navegador pueden llamar a la API.</li>
 * </ol>
 * <p>La segunda capa, por método, son las anotaciones {@code @PreAuthorize} de los servicios; se
 * activa con {@code @EnableMethodSecurity}. La API es <b>sin estado</b>: no hay sesiones, cada
 * petición se autentica por sí sola con su token JWT.</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // habilita @PreAuthorize en los Services
public class SecurityConfig {

    /**
     * Construye la cadena de filtros de seguridad. Spring la aplica a toda petición HTTP antes de llegar
     * a los controladores. Los parámetros los inyecta Spring (son beans del proyecto).
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            JwtService jwtService,
                                            CustomUserDetailsService userDetailsService,
                                            RestAuthenticationEntryPoint authEntryPoint,
                                            RestAccessDeniedHandler accessDeniedHandler) throws Exception {

        // new, NO bean (Decisión D4)
        // Si fuera un @Bean/@Component, Spring Boot lo registraría también como filtro de servlet global,
        // fuera de la cadena de seguridad. Al crearlo con new, solo participa donde lo colocamos abajo.
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtService, userDetailsService);

        http
                // Solo válido mientras la autenticación NO viaje en cookies (API Bearer)
                .csrf(AbstractHttpConfigurer::disable)
                // Activa CORS usando el bean corsConfigurationSource definido más abajo.
                .cors(Customizer.withDefaults())
                // STATELESS: el servidor no crea sesión HTTP ni recuerda nada entre peticiones;
                // cada petición debe traer su propio token.
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Se desactivan los mecanismos de login que no se usan: aquí la autenticación es solo por JWT.
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                // Quién responde los errores de seguridad: 401 (sin autenticar) y 403 (sin permiso), ambos en JSON.
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                // Reglas por URL. Se evalúan EN ORDEN y gana la PRIMERA que coincide; por eso las más
                // específicas van antes que las generales. En los patrones, "*" reemplaza un solo segmento
                // de la ruta (p. ej. el eventCode). permitAll = sin token; hasRole = exige ese rol.
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/error").permitAll()   // ruta interna de Spring Boot para mostrar errores

                        // --- Públicas de autenticación ---
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/users").permitAll()

                        // --- Privilegiadas (ANTES de las públicas/generales) ---
                        // Crear/publicar eventos y asignar artistas: ADMIN. Ver tickets pagados y marcar uso: STAFF o ADMIN.
                        // Comprar un ticket: USER.
                        .requestMatchers(HttpMethod.POST, "/api/events").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/events/*/publish").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/events/*/artists/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/events/*/tickets/paid").hasAnyRole("STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/tickets/*/use").hasAnyRole("STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/tickets").hasRole("USER")

                        // --- Públicas de lectura (rutas explícitas, sin /**) ---
                        .requestMatchers(HttpMethod.GET,
                                "/api/venues/active", "/api/venues/*",
                                "/api/artists/active", "/api/artists/by-stage-name", "/api/artists/*").permitAll()
                        // /api/events/{eventCode} y /by-artist son públicas A NIVEL URL;
                        // la ocultación de borradores la hace EventServiceImpl (Paso 14)
                        .requestMatchers(HttpMethod.GET,
                                "/api/events/published", "/api/events/by-artist", "/api/events/*").permitAll()

                        // --- Todo lo demás exige token válido ---
                        // authenticated() solo pide estar autenticado (cualquier rol). El control fino
                        // (¿es tu ticket? ¿eres tú?) lo hace luego @PreAuthorize en el servicio.
                        // (GET /api/tickets/**, PATCH /api/tickets/*/cancel, GET /api/users/by-*, GET /api/auth/me)
                        .anyRequest().authenticated())
                // Coloca nuestro filtro JWT antes del filtro estándar de usuario/contraseña, para que cuando
                // se evalúen las reglas de arriba la petición ya esté autenticada (si traía un token válido).
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // Codificador de contraseñas: BCrypt aplica un hash lento y con "sal" aleatoria, por lo que dos
    // contraseñas iguales producen hashes distintos y no se puede "revertir" el hash.
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();   // strength 10 (por defecto)
    }

    // Gestor de autenticación que usa AuthService.login. El DaoAuthenticationProvider obtiene al usuario con
    // CustomUserDetailsService, compara la contraseña con el hash usando el PasswordEncoder y rechaza
    // las cuentas deshabilitadas (User.active = false).
    @Bean
    AuthenticationManager authenticationManager(CustomUserDetailsService userDetailsService,
                                                PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    // CORS: regla del NAVEGADOR que decide si una página servida desde otro origen (p. ej. un front en
    // otro puerto) puede llamar a esta API. No protege contra herramientas como Postman o curl.
    @Bean
    CorsConfigurationSource corsConfigurationSource(
            // Lista de orígenes permitidos, tomada del properties (variable PULSEPASS_CORS_ORIGINS).
            // Vacía = ningún origen permitido.
            @Value("${pulsepass.security.cors.allowed-origins:}") List<String> allowedOrigins) {

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins.stream().filter(o -> !o.isBlank()).toList());
        // Métodos y cabeceras permitidos desde otros orígenes (OPTIONS es la consulta previa "preflight" del navegador).
        config.setAllowedMethods(List.of("GET", "POST", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setMaxAge(3600L);   // el navegador puede recordar el resultado del preflight durante 1 hora

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Esta política aplica solo a las rutas que empiezan por /api/
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}