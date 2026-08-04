package com.bridge.facturacion;

import com.bridge.facturacion.arca.CertificadosDePrueba;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootTest
@AutoConfigureMockMvc
public abstract class IntegracionTestBase {

    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:17-bookworm");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void propiedadesDeIntegracion(DynamicPropertyRegistry registry) throws Exception {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);

        // Fase 7: los certificados van en <certs-dir>/<cuit>/..., como los
        // referencia la tabla "emisores" (migracion V6). Ambos emisores de
        // homologacion comparten el CUIT 20463447277.
        Path dir = Files.createTempDirectory("arca-certs-test");
        CertificadosDePrueba.generarParaCuit(dir, "20463447277");
        registry.add("arca.certs-dir", dir::toString);
    }
}
