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

    /**
     * CUIT ficticios, propios de los tests. Los tests NO deben depender de los
     * emisores que siembran las migraciones: esos son datos fiscales reales que
     * cambian con la vida de las personas (altas, bajas, cambios de punto de
     * venta) y romperían el build cada vez que se tocan.
     */
    public static final String CUIT_TEST_UNO = "20111111112";
    public static final String CUIT_TEST_DOS = "27222222223";

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

        Path dir = Files.createTempDirectory("arca-certs-test");
        CertificadosDePrueba.generarParaCuit(dir, "20463447277");
        CertificadosDePrueba.generarParaCuit(dir, CUIT_TEST_UNO);
        CertificadosDePrueba.generarParaCuit(dir, CUIT_TEST_DOS);
        registry.add("arca.certs-dir", dir::toString);
    }
}
