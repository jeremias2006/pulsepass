package com.pulsepass.integration;

import com.pulsepass.repository.UserCredentialRepository;
import com.pulsepass.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FR-SECAUTH-001 / NFR-SEC004 / PRD §17.3: el registro es atómico.
 * <p>
 * Se fuerza un fallo DESPUÉS de que UserService.register haya insertado el User (el ID es IDENTITY,
 * así que el INSERT ocurre de inmediato dentro de la transacción) y ANTES de guardar la credencial.
 * Si la transacción no fuera una sola, quedaría un User sin credencial en la base de datos.
 * <p>
 * Clase separada de SecurityFlowIntegrationTest: el spy altera el contexto de Spring y no debe
 * afectar a los demás tests.
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RegistrationAtomicityIntegrationTest {

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

    // @MockitoSpyBean envuelve el PasswordEncoder REAL de la aplicación en un espía de Mockito: se comporta normal salvo
    // cuando se le ordena fallar (doThrow). Por eso esta clase queda separada: el espía altera el contexto de Spring.
    @MockitoSpyBean PasswordEncoder passwordEncoder;

    // JSON de registro válido para el username dado (el email se deriva de él).
    private String body(String username) {
        return """
                {"username":"%s","email":"%s@mail.com","firstName":"Test","lastName":"User",
                 "birthDate":"1995-05-20","password":"%s"}""".formatted(username, username, PASSWORD);
    }

    // Secuencia: (1) se obliga a encode() a fallar, lo que ocurre DESPUÉS de insertar el User y ANTES de guardar la credencial;
    // (2) el registro responde 500; (3) no quedó nada persistido (ni usuario, ni perfil, ni credencial: se comparan los conteos);
    // (4) se restaura el espía y el reintento con el mismo username funciona, lo que demuestra que no quedó un usuario huérfano bloqueándolo.
    @Test
    void failureWhileSavingCredentialRollsBackTheUser() throws Exception {
        String username = "atomic" + UUID.randomUUID().toString().substring(0, 8);
        long credentialsBefore = credentialRepository.count();
        long usersBefore = userRepository.count();

        // El hash falla tras haber insertado el User dentro de la misma transacción
        Mockito.doThrow(new IllegalStateException("fallo simulado al hashear"))
                .when(passwordEncoder).encode(any(CharSequence.class));

        mockMvc.perform(post("/api/users").contentType(APPLICATION_JSON).content(body(username)))
                .andExpect(status().isInternalServerError());

        // Nada quedó persistido: ni usuario, ni perfil, ni credencial
        assertThat(userRepository.existsByUsername(username)).isFalse();
        assertThat(userRepository.existsByEmailIgnoreCase(username + "@mail.com")).isFalse();
        assertThat(userRepository.count()).isEqualTo(usersBefore);
        assertThat(credentialRepository.count()).isEqualTo(credentialsBefore);

        // Prueba de que no quedó un User huérfano bloqueando el username/email: el reintento funciona
        Mockito.reset(passwordEncoder);   // el spy vuelve a llamar al BCrypt real
        mockMvc.perform(post("/api/users").contentType(APPLICATION_JSON).content(body(username)))
                .andExpect(status().isCreated());

        assertThat(userRepository.existsByUsername(username)).isTrue();
        assertThat(credentialRepository.count()).isEqualTo(credentialsBefore + 1);
    }
}
