package com.pulsepass.integration;

import com.jayway.jsonpath.JsonPath;
import com.pulsepass.domain.Role;
import com.pulsepass.domain.User;
import com.pulsepass.domain.UserCredential;
import com.pulsepass.domain.UserProfile;
import com.pulsepass.domain.Venue;
import com.pulsepass.repository.UserCredentialRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.repository.VenueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de INTEGRACIÓN del flujo completo de seguridad: aplicación entera, base PostgreSQL real y JWT reales, sin dobles. Cada
 * prueba hace lo que haría un cliente: se registra, inicia sesión, obtiene un token y lo envía en la cabecera Authorization para
 * llamar a la API, y se comprueba qué le permite o le niega la seguridad.
 *
 * <ul>
 *   <li>{@code @AutoConfigureMockMvc}: ofrece un MockMvc conectado a la aplicación completa (con filtros de seguridad reales).</li>
 *   <li>Esta clase NO es {@code @Transactional}: cada llamada confirma sus cambios en la base, como en producción. Por eso cada
 *       prueba usa un sufijo aleatorio (UUID) en usernames y códigos de evento: así no chocan entre sí aunque compartan la base.</li>
 *   <li>Los usuarios ADMIN y STAFF se crean directamente en la base ({@code createPrivileged}), porque por la API solo se
 *       puede obtener el rol USER.</li>
 *   <li>Política D6: pedir un recurso inexistente que pertenecería a otro usuario responde 403 igual que uno existente, para no revelar qué existe.</li>
 * </ul>
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityFlowIntegrationTest {

    // Contenedor Docker con PostgreSQL REAL, compartido por todas las pruebas de esta clase (static). @Container lo
    // arranca y lo detiene; @ServiceConnection configura el DataSource de Spring para que apunte a él, sin escribir
    // url ni credenciales. Flyway aplica las migraciones en esa base vacía. Requiere Docker en ejecución.
    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"));

    private static final String PASSWORD = "UnaFraseSeguraLarga2026!";   // ficticia, solo tests

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired UserCredentialRepository credentialRepository;
    @Autowired VenueRepository venueRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired PlatformTransactionManager txManager;

    // Permite ejecutar un bloque de código dentro de una transacción propia (se usa para escribir directamente en la base).
    private TransactionTemplate tx;
    // Sufijo aleatorio distinto en cada prueba: evita colisiones de username, email y códigos al compartir la base.
    private String suffix;

    // Antes de cada prueba: nueva plantilla de transacción, sufijo aleatorio y, si no existe, el recinto VEN-SEC-01 que usan los eventos.
    @BeforeEach
    void setUp() {
        tx = new TransactionTemplate(txManager);
        suffix = UUID.randomUUID().toString().substring(0, 8);
        if (venueRepository.findByCode("VEN-SEC-01").isEmpty()) {
            venueRepository.save(new Venue("VEN-SEC-01", "Venue Test", "Santa Marta", "Calle 1", 50000));
        }
    }

    // ------------------------------------------------------------ helpers
    // Registra un usuario por la API (POST /api/users) y comprueba que la respuesta no trae contraseña ni hash.
    private void register(String username, String email) throws Exception {
        mockMvc.perform(post("/api/users").contentType(APPLICATION_JSON).content("""
                {"username":"%s","email":"%s","firstName":"Test","lastName":"User",
                 "birthDate":"1995-05-20","password":"%s"}""".formatted(username, email, PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    // Inicia sesión y devuelve el valor listo para la cabecera Authorization: "Bearer <token>". JsonPath.read extrae el token del JSON.
    private String login(String identifier, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON)
                        .content("""
                                {"identifier":"%s","password":"%s"}""".formatted(identifier, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.accessToken");
    }

    // Crea un usuario con un rol privilegiado (ADMIN o STAFF) escribiendo directo en la base dentro de una transacción.
    private void createPrivileged(String username, Role role) {
        tx.executeWithoutResult(s -> {
            User user = new User(username, username + "@pulsepass.test");
            user.assignProfile(new UserProfile("Priv", "Test", null, null, LocalDate.of(1990, 1, 1)));
            userRepository.save(user);
            credentialRepository.save(new UserCredential(
                    user, passwordEncoder.encode(PASSWORD), EnumSet.of(role), LocalDateTime.now()));
        });
    }

    // Como ADMIN, crea un evento futuro (hoy + 30 días) y, si se indica, lo publica.
    private void createEvent(String adminAuth, String code, boolean publish) throws Exception {
        mockMvc.perform(post("/api/events").header("Authorization", adminAuth)
                        .contentType(APPLICATION_JSON).content("""
                                {"eventCode":"%s","name":"Festival","description":"d","category":"MUSIC",
                                 "eventDate":"%s","minimumAge":0,"venueCode":"VEN-SEC-01"}"""
                                .formatted(code, LocalDate.now().plusDays(30))))
                .andExpect(status().isCreated());
        if (publish) {
            mockMvc.perform(patch("/api/events/" + code + "/publish").header("Authorization", adminAuth))
                    .andExpect(status().isOk());
        }
    }

    // Compra un ticket GENERAL por la API y devuelve su código.
    private String buyTicket(String auth, String email, String eventCode) throws Exception {
        String body = mockMvc.perform(post("/api/tickets").header("Authorization", auth)
                        .contentType(APPLICATION_JSON).content("""
                                {"userEmail":"%s","eventCode":"%s","type":"GENERAL"}""".formatted(email, eventCode)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.ticketCode");
    }

    // ------------------------------------------------------------ tests
    // Flujo feliz: registro -> login (token Bearer, 900 s) -> GET /me con ese token. También se puede iniciar sesión con el email en vez del username.
    @Test
    void loginReturnsBearerTokenValidFor900SecondsAndMeWorks() throws Exception {
        String andrea = "andrea" + suffix;
        register(andrea, andrea + "@mail.com");

        mockMvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON).content("""
                        {"identifier":"%s","password":"%s"}""".formatted(andrea, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900));

        mockMvc.perform(get("/api/auth/me").header("Authorization", login(andrea + "@mail.com", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(andrea))
                .andExpect(jsonPath("$.roles[0]").value("USER"));
    }

    // Contraseña incorrecta y usuario inexistente dan el MISMO mensaje 401 (no se puede averiguar qué usuarios existen) y la respuesta no filtra trazas ni nombres de excepciones.
    @Test
    void wrongPasswordAndUnknownUserReturnTheSameGeneric401() throws Exception {
        String andrea = "andrea" + suffix;
        register(andrea, andrea + "@mail.com");

        String wrongPassword = mockMvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON)
                        .content("""
                                {"identifier":"%s","password":"incorrecta-123456"}""".formatted(andrea)))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
        String unknownUser = mockMvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON)
                        .content("""
                                {"identifier":"nadie%s","password":"incorrecta-123456"}""".formatted(suffix)))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        assertThat(JsonPath.<String>read(wrongPassword, "$.message"))
                .isEqualTo(JsonPath.<String>read(unknownUser, "$.message"));
        assertThat(wrongPassword).doesNotContain("stackTrace", "BadCredentials");
    }

    // FR-SECTKT-01: Andrea intenta comprar un ticket a nombre de Carlos y recibe 403; luego se comprueba que Carlos no recibió ningún ticket.
    @Test
    void userCannotBuyForAnotherUser() throws Exception {
        String admin = "admin" + suffix, andrea = "andrea" + suffix, carlos = "carlos" + suffix;
        createPrivileged(admin, Role.ADMIN);
        register(andrea, andrea + "@mail.com");
        register(carlos, carlos + "@mail.com");
        String adminAuth = login(admin, PASSWORD);
        createEvent(adminAuth, "EV-" + suffix, true);

        mockMvc.perform(post("/api/tickets").header("Authorization", login(andrea, PASSWORD))
                        .contentType(APPLICATION_JSON).content("""
                                {"userEmail":"%s@mail.com","eventCode":"EV-%s","type":"GENERAL"}"""
                                .formatted(carlos, suffix)))
                .andExpect(status().isForbidden());

        // y Carlos no recibió ningún ticket
        mockMvc.perform(get("/api/tickets/by-user").param("email", carlos + "@mail.com")
                        .header("Authorization", login(carlos, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // Un tercero recibe 403 al leer el ticket ajeno, listar los tickets de otro, ver su usuario o cancelar su ticket. Un código
    // inexistente también da 403 (política D6). El dueño sí puede leerlo y cancelarlo (queda CANCELLED).
    @Test
    void userCannotReadOrCancelForeignTicketButOwnerCan() throws Exception {
        String admin = "admin" + suffix, andrea = "andrea" + suffix, carlos = "carlos" + suffix;
        createPrivileged(admin, Role.ADMIN);
        register(andrea, andrea + "@mail.com");
        register(carlos, carlos + "@mail.com");
        createEvent(login(admin, PASSWORD), "EV-" + suffix, true);

        String carlosAuth = login(carlos, PASSWORD);
        String andreaAuth = login(andrea, PASSWORD);
        String ticketCode = buyTicket(carlosAuth, carlos + "@mail.com", "EV-" + suffix);

        mockMvc.perform(get("/api/tickets/" + ticketCode).header("Authorization", andreaAuth))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/tickets/by-user").param("email", carlos + "@mail.com")
                        .header("Authorization", andreaAuth))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/users/by-email").param("email", carlos + "@mail.com")
                        .header("Authorization", andreaAuth))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/tickets/" + ticketCode + "/cancel").header("Authorization", andreaAuth))
                .andExpect(status().isForbidden());
        // ticket inexistente → también 403 (política D6)
        mockMvc.perform(get("/api/tickets/TKT-NOEXISTE").header("Authorization", andreaAuth))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/tickets/" + ticketCode).header("Authorization", carlosAuth))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/tickets/" + ticketCode + "/cancel").header("Authorization", carlosAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    // Un USER no puede publicar un evento (403) pero ADMIN sí. Un USER no puede marcar un ticket como usado (403) pero STAFF sí (queda USED); usarlo de nuevo da 409 por regla de negocio.
    @Test
    void onlyAdminPublishesAndOnlyStaffOrAdminUseTickets() throws Exception {
        String admin = "admin" + suffix, laura = "laura" + suffix, andrea = "andrea" + suffix;
        createPrivileged(admin, Role.ADMIN);
        createPrivileged(laura, Role.STAFF);
        register(andrea, andrea + "@mail.com");
        String adminAuth = login(admin, PASSWORD);
        String andreaAuth = login(andrea, PASSWORD);

        createEvent(adminAuth, "EV-" + suffix, false);
        mockMvc.perform(patch("/api/events/EV-" + suffix + "/publish").header("Authorization", andreaAuth))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/events/EV-" + suffix + "/publish").header("Authorization", adminAuth))
                .andExpect(status().isOk());

        String ticketCode = buyTicket(andreaAuth, andrea + "@mail.com", "EV-" + suffix);
        mockMvc.perform(patch("/api/tickets/" + ticketCode + "/use").header("Authorization", andreaAuth))
                .andExpect(status().isForbidden());

        String lauraAuth = login(laura, PASSWORD);
        mockMvc.perform(patch("/api/tickets/" + ticketCode + "/use").header("Authorization", lauraAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("USED"));
        mockMvc.perform(patch("/api/tickets/" + ticketCode + "/use").header("Authorization", lauraAuth))
                .andExpect(status().isConflict());   // regla de negocio heredada
    }

    // Un evento DRAFT: anónimo recibe 404 (como si no existiera), ADMIN lo ve (200) y no aparece en la lista pública de publicados.
    @Test
    void draftEventsAreHiddenFromAnonymousButVisibleToAdmin() throws Exception {
        String admin = "admin" + suffix;
        createPrivileged(admin, Role.ADMIN);
        String adminAuth = login(admin, PASSWORD);
        createEvent(adminAuth, "DR-" + suffix, false);

        mockMvc.perform(get("/api/events/DR-" + suffix)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/events/DR-" + suffix).header("Authorization", adminAuth))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/events/published"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.eventCode=='DR-%s')]".formatted(suffix)).isEmpty());
    }

    // Tres casos de token inaceptable, todos 401: token con la firma alterada (se cambian sus últimos 5 caracteres), basura tras "Bearer" y el token enviado
    // como parámetro de la URL (solo se acepta por la cabecera Authorization).
    @Test
    void tamperedAndMalformedTokensGet401() throws Exception {
        String andrea = "andrea" + suffix;
        register(andrea, andrea + "@mail.com");
        String valid = login(andrea, PASSWORD);
        String tampered = valid.substring(0, valid.length() - 5) + "AAAAA";

        mockMvc.perform(get("/api/auth/me").header("Authorization", tampered))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer basura"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/auth/me").param("access_token", valid.substring(7)))   // query string no vale
                .andExpect(status().isUnauthorized());
    }

    // Tras desactivar al usuario en la base, el MISMO token (aún vigente) deja de funcionar (401) y el login también falla: el filtro consulta el estado actual en cada petición.
    @Test
    void deactivatedUserLosesAccessWithAStillValidToken() throws Exception {
        String andrea = "andrea" + suffix;
        register(andrea, andrea + "@mail.com");
        String auth = login(andrea, PASSWORD);
        mockMvc.perform(get("/api/auth/me").header("Authorization", auth)).andExpect(status().isOk());

        tx.executeWithoutResult(s -> userRepository.findByUsername(andrea).orElseThrow().deactivate());

        mockMvc.perform(get("/api/auth/me").header("Authorization", auth))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON).content("""
                        {"identifier":"%s","password":"%s"}""".formatted(andrea, PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    // Un registro que intenta colarse como ADMIN (enviando "roles" y "role" en el JSON) se crea, pero esos campos se ignoran y el usuario solo tiene el rol USER.
    @Test
    void registrationCannotAssignPrivilegedRoles() throws Exception {
        String hacker = "hacker" + suffix;
        mockMvc.perform(post("/api/users").contentType(APPLICATION_JSON).content("""
                        {"username":"%s","email":"%s@mail.com","firstName":"H","lastName":"X",
                         "birthDate":"1995-05-20","password":"%s","roles":["ADMIN"],"role":"ADMIN"}"""
                        .formatted(hacker, hacker, PASSWORD)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/auth/me").header("Authorization", login(hacker, PASSWORD)))
                .andExpect(jsonPath("$.roles.length()").value(1))
                .andExpect(jsonPath("$.roles[0]").value("USER"));
    }

    // T-SEC-015: un tercero recibe 403 sin datos del usuario (también si el usuario no existe: política D6). El propio usuario recibe 200 y ADMIN puede ver datos ajenos; un anónimo recibe 401.
    @Test
    void userCannotReadForeignUserByUsernameButOwnerAndAdminCan() throws Exception {
        String admin = "admin" + suffix, andrea = "andrea" + suffix, carlos = "carlos" + suffix;
        createPrivileged(admin, Role.ADMIN);
        register(andrea, andrea + "@mail.com");
        register(carlos, carlos + "@mail.com");
        String andreaAuth = login(andrea, PASSWORD);
        String adminAuth = login(admin, PASSWORD);

        // T-SEC-015: tercero => 403, sin datos del usuario
        mockMvc.perform(get("/api/users/by-username").param("username", carlos)
                        .header("Authorization", andreaAuth))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.username").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist());

        // usuario inexistente => también 403 (política D6: no revelar existencia)
        mockMvc.perform(get("/api/users/by-username").param("username", "nadie" + suffix)
                        .header("Authorization", andreaAuth))
                .andExpect(status().isForbidden());

        // propio => 200 (por ID persistente)
        mockMvc.perform(get("/api/users/by-username").param("username", andrea)
                        .header("Authorization", andreaAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(andrea));
        mockMvc.perform(get("/api/users/by-email").param("email", andrea + "@mail.com")
                        .header("Authorization", andreaAuth))
                .andExpect(status().isOk());

        // ADMIN autorizado expresamente sobre datos ajenos
        mockMvc.perform(get("/api/users/by-username").param("username", carlos)
                        .header("Authorization", adminAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(carlos));
        mockMvc.perform(get("/api/users/by-email").param("email", carlos + "@mail.com")
                        .header("Authorization", adminAuth))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/tickets/by-user").param("email", carlos + "@mail.com")
                        .header("Authorization", adminAuth))
                .andExpect(status().isOk());

        // anónimo => 401
        mockMvc.perform(get("/api/users/by-username").param("username", carlos))
                .andExpect(status().isUnauthorized());
    }
}