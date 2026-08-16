package com.t4kash.api;

import com.t4kash.api.network.entity.Publicacion;
import com.t4kash.api.network.repository.PublicacionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class ApiPostgresqlIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRESQL =
            new PostgreSQLContainer<>("postgres:17-alpine")
                    .withDatabaseName("t4kash")
                    .withUsername("t4kash")
                    .withPassword("t4kash")
                    .withInitScript("schema-postgresql.sql");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRESQL::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRESQL::getUsername);
        registry.add("spring.datasource.password", POSTGRESQL::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PublicacionRepository publicationRepository;

    @Test
    void bootsWithTheProductionSchemaAndExposesHealth() throws Exception {
        Integer tableCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_type = 'BASE TABLE'
                """,
                Integer.class
        );

        assertThat(tableCount).isEqualTo(52);
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void marketplaceListsRemainBounded() throws Exception {
        mockMvc.perform(get("/api/tasks")
                        .queryParam("page", "0")
                        .queryParam("size", "5000"))
                .andExpect(status().isOk());
    }

    @Test
    void networkFeedAppliesVisibilityHidingAndBlocking() {
        jdbcTemplate.update(
                """
                INSERT INTO usuarios (
                    id_usuario, nombre_usuario, nombre, apellido, correo,
                    password_hash, estado_usuario, id_universidad
                ) VALUES
                    (901, 'lector.network', 'Lector', 'Network',
                     'lector.network@example.com', 'hash', 'ACTIVO', 1),
                    (902, 'autor.network', 'Autor', 'Network',
                     'autor.network@example.com', 'hash', 'ACTIVO', 1)
                """
        );
        jdbcTemplate.update(
                """
                INSERT INTO conexiones_usuarios (
                    id_usuario_solicitante, id_usuario_receptor, estado_conexion
                ) VALUES (901, 902, 'ACEPTADA')
                """
        );
        jdbcTemplate.update(
                """
                INSERT INTO publicaciones (
                    id_publicacion, id_usuario, contenido,
                    tipo_publicacion, visibilidad
                ) VALUES
                    (911, 902, 'Publica', 'TEXTO', 'PUBLICA'),
                    (912, 902, 'Universidad', 'TEXTO', 'UNIVERSIDAD'),
                    (913, 902, 'Conexion', 'TEXTO', 'CONEXIONES')
                """
        );

        List<Publicacion> visible = publicationRepository.findFeed(
                901,
                "PARA_TI",
                PageRequest.of(0, 20)
        );
        assertThat(visible).hasSize(3);

        jdbcTemplate.update(
                """
                INSERT INTO publicaciones_ocultas (id_publicacion, id_usuario)
                VALUES (911, 901)
                """
        );
        assertThat(publicationRepository.findFeed(
                901,
                "PARA_TI",
                PageRequest.of(0, 20)
        )).hasSize(2);

        jdbcTemplate.update(
                """
                INSERT INTO bloqueos_usuarios (
                    id_usuario_bloquea,
                    id_usuario_bloqueado
                ) VALUES (901, 902)
                """
        );
        assertThat(publicationRepository.findFeed(
                901,
                "PARA_TI",
                PageRequest.of(0, 20)
        )).isEmpty();
    }
}
