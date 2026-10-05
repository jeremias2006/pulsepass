package com.pulsepass;

import com.pulsepass.domain.*;
import com.pulsepass.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest
@Transactional
class PersistenceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"));

    @Autowired
    private VenueRepository venueRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private ArtistRepository artistRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserProfileRepository userProfileRepository;
    @Autowired
    private TicketRepository ticketRepository;
    @Autowired
    private Environment environment;

    @PersistenceContext
    private EntityManager entityManager;

    private static final LocalDate HOY = LocalDate.now();

    private Venue marina;
    private Venue arenaNorte;
    private Event caribbeanFest;
    private Event solarNight;
    private Event borrador;
    private Event cancelado;
    private Artist solarBeat;
    private Artist neonWaves;
    private Artist caribbeanSound;
    private User andrea;
    private User carlos;
    private User laura;
    private User miguel;

    @BeforeEach
    void seedScenario() {
        solarBeat = artistRepository.findByStageName("Solar Beat").orElseThrow();
        neonWaves = artistRepository.findByStageName("Neon Waves").orElseThrow();
        caribbeanSound = artistRepository.findByStageName("Caribbean Sound").orElseThrow();

        marina = venueRepository.save(new Venue(
                "VEN-SMR-01", "Marina Convention Center", "Santa Marta", "Calle 22 #3-15", 5000));
        arenaNorte = venueRepository.save(new Venue(
                "VEN-BOG-01", "Arena Norte", "Bogota", "Av. 68 #80-20", 12000));

        caribbeanFest = new Event("CMF-2026", "Caribbean Music Fest 2026",
                "Festival de musica del Caribe", HOY.plusMonths(3),
                EventCategory.MUSIC, EventStatus.PUBLISHED, 18);
        marina.addEvent(caribbeanFest);
        caribbeanFest.addArtist(solarBeat);
        caribbeanFest.addArtist(neonWaves);
        caribbeanFest.addArtist(caribbeanSound);

        solarNight = new Event("SLR-2026", "Solar Night", "Show electronico",
                HOY.plusMonths(5), EventCategory.MUSIC, EventStatus.PUBLISHED, 18);
        arenaNorte.addEvent(solarNight);
        solarNight.addArtist(solarBeat);

        borrador = new Event("DRF-2026", "Evento en borrador", null,
                HOY.plusMonths(2), EventCategory.CULTURE, EventStatus.DRAFT, null);
        marina.addEvent(borrador);

        cancelado = new Event("CNL-2026", "Evento cancelado", null,
                HOY.plusMonths(1), EventCategory.SPORTS, EventStatus.CANCELLED, null);
        marina.addEvent(cancelado);

        eventRepository.saveAll(List.of(caribbeanFest, solarNight, borrador, cancelado));

        andrea = userRepository.save(new User("andrea", "andrea@pulsepass.com"));
        carlos = userRepository.save(new User("carlos", "carlos@pulsepass.com"));
        laura = userRepository.save(new User("laura", "laura@pulsepass.com"));
        miguel = userRepository.save(new User("miguel", "miguel@pulsepass.com"));

        andrea.assignProfile(new UserProfile("Andrea", "Gomez", "3001112233",
                "Santa Marta", LocalDate.of(1998, 5, 14)));
        userRepository.save(andrea);

        ticketRepository.saveAll(List.of(
                new Ticket("TCK-0001", TicketType.VIP, new BigDecimal("250000.00"),
                        TicketStatus.PAID, LocalDateTime.now(), andrea, caribbeanFest),
                new Ticket("TCK-0002", TicketType.GENERAL, new BigDecimal("120000.00"),
                        TicketStatus.PAID, LocalDateTime.now(), carlos, caribbeanFest),
                new Ticket("TCK-0003", TicketType.GENERAL, new BigDecimal("120000.00"),
                        TicketStatus.RESERVED, LocalDateTime.now(), laura, caribbeanFest),
                new Ticket("TCK-0004", TicketType.VIP, new BigDecimal("250000.00"),
                        TicketStatus.CANCELLED, LocalDateTime.now(), miguel, caribbeanFest)));

        flushAndClear();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private static <T> List<String> codes(List<T> items, Function<T, String> extractor) {
        return items.stream().map(extractor).sorted().toList();
    }

    // ---------------------------------------------------------------------
    // QT-001 / QT-002: Flyway y validación del esquema
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("QT-001: Flyway aplica V1, V2 y V3 sobre una base vacía")
    void flywayAplicaLasTresMigraciones() {
        @SuppressWarnings("unchecked")
        List<String> versiones = entityManager.createNativeQuery("""
                        SELECT version FROM flyway_schema_history
                        WHERE success = true AND version IS NOT NULL
                        ORDER BY installed_rank
                        """)
                .getResultList();

        assertEquals(List.of("1", "2", "3"), versiones);
    }

    @Test
    @DisplayName("QT-001: V2 dejó cargado el catálogo inicial de artistas")
    void v2InsertaElCatalogoDeArtistas() {
        List<String> nombres = codes(artistRepository.findAll(), Artist::getStageName);

        assertTrue(nombres.containsAll(List.of(
                        "Caribbean Sound", "Digital Pulse", "Neon Waves", "Ocean Drive", "Solar Beat")),
                "Faltan artistas de la migración V2: " + nombres);
    }

    @Test
    @DisplayName("QT-002: Hibernate valida el esquema, no lo crea ni lo actualiza")
    void hibernateSoloValidaElEsquema() {
        assertEquals("validate", environment.getProperty("spring.jpa.hibernate.ddl-auto"));
        assertTrue(venueRepository.count() > 0);
    }

    @Test
    @DisplayName("QT-001 / FR-EVT-006: la columna streaming_url de V3 existe y es persistible")
    void v3AgregaStreamingUrl() {
        Event evento = eventRepository.findByEventCode("CMF-2026").orElseThrow();
        evento.setStreamingUrl("https://stream.pulsepass.com/cmf-2026");
        eventRepository.saveAndFlush(evento);
        flushAndClear();

        assertEquals("https://stream.pulsepass.com/cmf-2026",
                eventRepository.findByEventCode("CMF-2026").orElseThrow().getStreamingUrl());
    }

    // ---------------------------------------------------------------------
    // QT-003: Venue 1:N Event
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("FR-VEN-001 / AC-001: el venue se recupera por código y su capacidad es mayor que cero")
    void venueSeRecuperaPorCodigo() {
        Venue venue = venueRepository.findByCode("VEN-SMR-01").orElseThrow();

        assertEquals("Marina Convention Center", venue.getName());
        assertEquals("Santa Marta", venue.getCity());
        assertTrue(venue.getCapacity() > 0);
    }

    @Test
    @DisplayName("QT-003: un venue agrupa varios eventos (lado 1:N)")
    void venueTieneVariosEventos() {
        Venue venue = venueRepository.findByCode("VEN-SMR-01").orElseThrow();

        assertEquals(List.of("CMF-2026", "CNL-2026", "DRF-2026"),
                codes(venue.getEvents(), Event::getEventCode));
    }

    @Test
    @DisplayName("QT-003 / AC-002: el evento se recupera por eventCode junto con su venue (lado N:1)")
    void eventoSeRecuperaConSuVenue() {
        Event evento = eventRepository.findByEventCode("CMF-2026").orElseThrow();

        assertEquals("Caribbean Music Fest 2026", evento.getName());
        assertEquals("VEN-SMR-01", evento.getVenue().getCode());
        assertEquals("Santa Marta", evento.getVenue().getCity());
    }

    // ---------------------------------------------------------------------
    // QT-004: User 1:1 UserProfile
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("QT-004: el usuario tiene exactamente un perfil y el perfil apunta de vuelta")
    void usuarioTienePerfilUnoAUno() {
        User user = userRepository.findByEmailIgnoreCase("andrea@pulsepass.com").orElseThrow();
        UserProfile perfil = user.getProfile();

        assertNotNull(perfil);
        assertEquals("Andrea", perfil.getFirstName());
        assertEquals("Santa Marta", perfil.getCity());
        assertEquals(user.getId(), perfil.getUser().getId());
    }

    @Test
    @DisplayName("QT-004 / AC-004: la FK UNIQUE impide asociar un segundo perfil al mismo usuario")
    void segundoPerfilParaElMismoUsuarioViolaLaRestriccion() {
        User user = userRepository.findByEmailIgnoreCase("andrea@pulsepass.com").orElseThrow();

        UserProfile perfilDuplicado = new UserProfile("Otra", "Persona", "3009998877",
                "Bogota", LocalDate.of(2000, 1, 1));
        perfilDuplicado.setUser(user);

        assertThrows(DataIntegrityViolationException.class,
                () -> userProfileRepository.saveAndFlush(perfilDuplicado));
    }

    // ---------------------------------------------------------------------
    // QT-005: Event N:M Artist
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("QT-005 / AC-003: el evento queda asociado a sus tres artistas")
    void eventoTieneTresArtistas() {
        Event evento = eventRepository.findByEventCode("CMF-2026").orElseThrow();

        assertEquals(List.of("Caribbean Sound", "Neon Waves", "Solar Beat"),
                codes(List.copyOf(evento.getArtists()), Artist::getStageName));
    }

    @Test
    @DisplayName("QT-005: la navegación inversa Artist -> Event también funciona")
    void artistaVeSusEventos() {
        Artist artista = artistRepository.findByStageName("Solar Beat").orElseThrow();

        assertEquals(List.of("CMF-2026", "SLR-2026"),
                codes(List.copyOf(artista.getEvents()), Event::getEventCode));
    }

    @Test
    @DisplayName("QT-005 / AC-003: agregar dos veces el mismo artista no duplica la asociación (nivel Java)")
    void agregarDosVecesElMismoArtistaNoDuplicaLaAsociacion() {
        Event evento = eventRepository.findByEventCode("CMF-2026").orElseThrow();
        Artist repetido = artistRepository.findByStageName("Solar Beat").orElseThrow();

        evento.addArtist(repetido);
        eventRepository.saveAndFlush(evento);
        flushAndClear();

        Number filas = (Number) entityManager
                .createNativeQuery("SELECT COUNT(*) FROM event_artists WHERE event_id = :id")
                .setParameter("id", evento.getId())
                .getSingleResult();

        assertEquals(3L, filas.longValue());
    }

    @Test
    @DisplayName("QT-009 / FR-ART-003: la PK compuesta de event_artists rechaza un par evento-artista duplicado")
    void pkCompuestaRechazaParEventoArtistaDuplicado() {
        assertThrows(jakarta.persistence.PersistenceException.class, () ->
                entityManager.createNativeQuery("""
                                INSERT INTO event_artists (event_id, artist_id) VALUES (:e, :a)
                                """)
                        .setParameter("e", caribbeanFest.getId())
                        .setParameter("a", solarBeat.getId())
                        .executeUpdate());
    }

    // ---------------------------------------------------------------------
    // QT-006: Ticket -> User y Ticket -> Event
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("QT-006: cada ticket navega hacia su usuario y su evento")
    void ticketNavegaHaciaUsuarioYEvento() {
        Ticket ticket = ticketRepository.findByUserEmail("andrea@pulsepass.com").get(0);

        assertEquals("TCK-0001", ticket.getTicketCode());
        assertEquals("andrea", ticket.getUser().getUsername());
        assertEquals("CMF-2026", ticket.getEvent().getEventCode());
        assertEquals(TicketType.VIP, ticket.getType());
        assertEquals(TicketStatus.PAID, ticket.getStatus());
        assertEquals(0, ticket.getPrice().compareTo(new BigDecimal("250000")));
    }

    @Test
    @DisplayName("QT-006: el evento ve sus cuatro tickets y el usuario ve los suyos")
    void eventoYUsuarioVenSusTickets() {
        Event evento = eventRepository.findByEventCode("CMF-2026").orElseThrow();
        assertEquals(4, evento.getTickets().size());

        User user = userRepository.findByEmailIgnoreCase("carlos@pulsepass.com").orElseThrow();
        assertEquals(List.of("TCK-0002"), codes(user.getTickets(), Ticket::getTicketCode));
    }

    @Test
    @DisplayName("BR-008 / FR-TKT-005: los enums se persisten por nombre, no por ordinal")
    void enumsSePersistenComoTexto() {
        Object status = entityManager.createNativeQuery("""
                        SELECT status FROM tickets WHERE ticket_code = 'TCK-0001'
                        """)
                .getSingleResult();

        assertEquals("PAID", status);
    }

    // ---------------------------------------------------------------------
    // QT-007: Query Methods (simples y navegados)
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("QT-007: Query Method simple — findByStageName (FR-ART-002)")
    void queryMethodSimpleArtista() {
        assertTrue(artistRepository.findByStageName("Neon Waves").isPresent());
        assertTrue(artistRepository.findByStageName("Artista Inexistente").isEmpty());
    }

    @Test
    @DisplayName("QT-007: Query Method simple — findByEmailIgnoreCase (sección 14)")
    void queryMethodIgnoreCase() {
        Optional<User> user = userRepository.findByEmailIgnoreCase("andrea@PULSEPASS.com");

        assertTrue(user.isPresent());
        assertEquals("andrea", user.get().getUsername());
    }

    @Test
    @DisplayName("QT-007 / AC-006: findByStatusOrderByEventDateAsc trae solo PUBLISHED y ordenados (FR-EVT-005)")
    void queryMethodEventosPublicadosOrdenados() {
        List<Event> publicados = eventRepository.findByStatusOrderByEventDateAsc(EventStatus.PUBLISHED);

        assertEquals(List.of("CMF-2026", "SLR-2026"),
                publicados.stream().map(Event::getEventCode).toList());
        assertTrue(publicados.stream().allMatch(e -> e.getStatus() == EventStatus.PUBLISHED),
                "No deben aparecer eventos DRAFT ni CANCELLED");
    }

    @Test
    @DisplayName("QT-007: Query Method navegado — eventos por código de venue (FR-VEN-004)")
    void queryMethodNavegadoEventosPorVenue() {
        assertEquals(List.of("CMF-2026", "CNL-2026", "DRF-2026"),
                codes(eventRepository.findByVenueCode("VEN-SMR-01"), Event::getEventCode));
        assertEquals(List.of("SLR-2026"),
                codes(eventRepository.findByVenueCode("VEN-BOG-01"), Event::getEventCode));
    }

    @Test
    @DisplayName("QT-007: Query Method navegado — tickets de un usuario, con y sin estado (FR-TKT-006)")
    void queryMethodNavegadoTicketsPorUsuario() {
        assertEquals(1, ticketRepository.findByUserEmail("andrea@pulsepass.com").size());
        assertTrue(ticketRepository
                .findByUserEmailAndStatus("laura@pulsepass.com", TicketStatus.PAID).isEmpty());
        assertEquals(List.of("TCK-0003"),
                codes(ticketRepository.findByUserEmailAndStatus("laura@pulsepass.com", TicketStatus.RESERVED),
                        Ticket::getTicketCode));
    }

    @Test
    @DisplayName("QT-007: Query Method navegado — tickets PAID de un evento (FR-TKT-007)")
    void queryMethodNavegadoTicketsPagadosDelEvento() {
        assertEquals(List.of("TCK-0001", "TCK-0002"),
                codes(ticketRepository.findByEventEventCodeAndStatus("CMF-2026", TicketStatus.PAID),
                        Ticket::getTicketCode));
    }

    @Test
    @DisplayName("QT-007: Query Method navegado — tickets de eventos futuros (FR-SRC-004)")
    void queryMethodTicketsDeEventosFuturos() {
        assertEquals(4, ticketRepository
                .findByEventEventDateAfterOrderByEventEventDateAsc(HOY).size());
        assertTrue(ticketRepository
                .findByEventEventDateAfterOrderByEventEventDateAsc(HOY.plusYears(2)).isEmpty());
    }

    // ---------------------------------------------------------------------
    // QT-008: JPQL (JOIN y COUNT)
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("QT-008 / AC-007: JPQL con JOIN — eventos por artista, sin duplicados (FR-SRC-001)")
    void jpqlEventosPorArtistaSinDuplicados() {
        List<Event> eventos = eventRepository.findByArtistStageName("Solar Beat");
        List<String> encontrados = codes(eventos, Event::getEventCode);

        assertEquals(List.of("CMF-2026", "SLR-2026"), encontrados);
        assertEquals(encontrados.size(), encontrados.stream().distinct().count());
    }

    @Test
    @DisplayName("QT-008: JPQL con dos asociaciones — eventos por ciudad + artista (FR-SRC-002)")
    void jpqlEventosPorCiudadYArtista() {
        assertEquals(List.of("CMF-2026"),
                codes(eventRepository.findByCityAndArtistStageName("Santa Marta", "Solar Beat"),
                        Event::getEventCode));

        assertEquals(List.of("SLR-2026"),
                codes(eventRepository.findByCityAndArtistStageName("Bogota", "Solar Beat"),
                        Event::getEventCode));

        assertTrue(eventRepository.findByCityAndArtistStageName("Santa Marta", "Ocean Drive").isEmpty());
    }

    @Test
    @DisplayName("QT-008: JPQL compleja — eventos recomendados, case-insensitive y parcial (FR-SRC-003)")
    void jpqlEventosRecomendados() {
        List<Event> recomendados = eventRepository.findRecommendedEvents(HOY, "santa marta", "solar");

        assertEquals(List.of("CMF-2026"), codes(recomendados, Event::getEventCode));
        assertTrue(recomendados.stream().allMatch(e -> e.getStatus() == EventStatus.PUBLISHED));

        assertTrue(eventRepository
                .findRecommendedEvents(HOY.plusYears(1), "santa marta", "solar").isEmpty());
    }

    @Test
    @DisplayName("QT-008 / FR-SRC-003: DISTINCT evita duplicar un evento cuando varios artistas coinciden")
    void jpqlRecomendadosNoDuplicaEventoConVariosArtistasCoincidentes() {
        // "a" está en Solar Beat, Neon Waves y Caribbean Sound: los tres artistas de CMF-2026
        List<Event> recomendados = eventRepository.findRecommendedEvents(HOY, "santa marta", "a");

        assertEquals(List.of("CMF-2026"), codes(recomendados, Event::getEventCode));
    }

    @Test
    @DisplayName("QT-008 / AC-008: JPQL con COUNT — solo los tickets PAID participan del conteo (FR-TKT-008)")
    void jpqlConteoDeTicketsPagados() {
        assertEquals(2L, ticketRepository.countPaidTicketsByEventCode("CMF-2026"));
        assertEquals(0L, ticketRepository.countPaidTicketsByEventCode("SLR-2026"));
    }

    // ---------------------------------------------------------------------
    // QT-009 / NFR-001: constraints reales en PostgreSQL
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("QT-009 / AC-005: ticketCode duplicado — PostgreSQL rechaza el segundo registro")
    void ticketCodeDuplicadoViolaUnique() {
        Event evento = eventRepository.findByEventCode("CMF-2026").orElseThrow();
        User user = userRepository.findByEmailIgnoreCase("laura@pulsepass.com").orElseThrow();

        Ticket duplicado = new Ticket("TCK-0001", TicketType.GENERAL, new BigDecimal("120000.00"),
                TicketStatus.RESERVED, LocalDateTime.now(), user, evento);

        assertThrows(DataIntegrityViolationException.class,
                () -> ticketRepository.saveAndFlush(duplicado));
    }

    @Test
    @DisplayName("QT-009 / FR-EVT-002: eventCode duplicado viola UNIQUE")
    void eventCodeDuplicadoViolaUnique() {
        Event duplicado = new Event("CMF-2026", "Otro evento con el mismo codigo", null,
                HOY.plusMonths(6), EventCategory.MUSIC, EventStatus.DRAFT, null);
        duplicado.setVenue(venueRepository.findByCode("VEN-SMR-01").orElseThrow());

        assertThrows(DataIntegrityViolationException.class,
                () -> eventRepository.saveAndFlush(duplicado));
    }

    @Test
    @DisplayName("QT-009 / FR-USR-002: username duplicado viola UNIQUE")
    void usuarioDuplicadoViolaUnique() {
        assertThrows(DataIntegrityViolationException.class,
                () -> userRepository.saveAndFlush(new User("andrea", "otro@pulsepass.com")));
    }

    @Test
    @DisplayName("NFR-001 / FR-VEN-003: CHECK capacity > 0 — PostgreSQL rechaza capacidad cero")
    void capacidadNoPositivaViolaCheck() {
        Venue invalido = new Venue("VEN-BAD-01", "Venue sin aforo", "Santa Marta", "N/A", 0);

        assertThrows(DataIntegrityViolationException.class,
                () -> venueRepository.saveAndFlush(invalido));
    }

    @Test
    @DisplayName("NFR-001 / FR-TKT-003: CHECK price >= 0 — PostgreSQL rechaza precios negativos")
    void precioNegativoViolaCheck() {
        Event evento = eventRepository.findByEventCode("CMF-2026").orElseThrow();
        User user = userRepository.findByEmailIgnoreCase("miguel@pulsepass.com").orElseThrow();

        Ticket invalido = new Ticket("TCK-9999", TicketType.STUDENT, new BigDecimal("-1.00"),
                TicketStatus.RESERVED, LocalDateTime.now(), user, evento);

        assertThrows(DataIntegrityViolationException.class,
                () -> ticketRepository.saveAndFlush(invalido));
    }

    @Test
    @DisplayName("NFR-001 / FR-EVT-003: PostgreSQL rechaza un status fuera del catálogo")
    void estadoInvalidoDeEventoViolaCheck() {
        Long venueId = venueRepository.findByCode("VEN-SMR-01").orElseThrow().getId();

        assertThrows(jakarta.persistence.PersistenceException.class, () ->
                entityManager.createNativeQuery("""
                                INSERT INTO events (event_code, name, event_date, category, status, venue_id)
                                VALUES ('BAD-1', 'Evento raro', CURRENT_DATE, 'MUSIC', 'BANANA', :venueId)
                                """)
                        .setParameter("venueId", venueId)
                        .executeUpdate());
    }
}