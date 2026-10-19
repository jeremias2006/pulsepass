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

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityFlowIntegrationTest {

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

    private TransactionTemplate tx;
    private String suffix;

    @BeforeEach
    void setUp() {
        tx = new TransactionTemplate(txManager);
        suffix = UUID.randomUUID().toString().substring(0, 8);
        if (venueRepository.findByCode("VEN-SEC-01").isEmpty()) {
            venueRepository.save(new Venue("VEN-SEC-01", "Venue Test", "Santa Marta", "Calle 1", 50000));
        }
    }

    // ------------------------------------------------------------ helpers
    private void register(String username, String email) throws Exception {
        mockMvc.perform(post("/api/users").contentType(APPLICATION_JSON).content("""
                {"username":"%s","email":"%s","firstName":"Test","lastName":"User",
                 "birthDate":"1995-05-20","password":"%s"}""".formatted(username, email, PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    private String login(String identifier, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON)
                        .content("""
                                {"identifier":"%s","password":"%s"}""".formatted(identifier, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.accessToken");
    }

    private void createPrivileged(String username, Role role) {
        tx.executeWithoutResult(s -> {
            User user = new User(username, username + "@pulsepass.test");
            user.assignProfile(new UserProfile("Priv", "Test", null, null, LocalDate.of(1990, 1, 1)));
            userRepository.save(user);
            credentialRepository.save(new UserCredential(
                    user, passwordEncoder.encode(PASSWORD), EnumSet.of(role), LocalDateTime.now()));
        });
    }

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

    private String buyTicket(String auth, String email, String eventCode) throws Exception {
        String body = mockMvc.perform(post("/api/tickets").header("Authorization", auth)
                        .contentType(APPLICATION_JSON).content("""
                                {"userEmail":"%s","eventCode":"%s","type":"GENERAL"}""".formatted(email, eventCode)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.ticketCode");
    }

    // ------------------------------------------------------------ tests
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