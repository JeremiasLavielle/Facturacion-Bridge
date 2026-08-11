package com.bridge.facturacion.arca;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Deja escrito en la propia base de datos si es de HOMOLOGACION o PRODUCCION.
 *
 * <p>Sirve para dos cosas distintas:
 *
 * <ol>
 *   <li>Que los scripts SQL destructivos (seed/limpiar-facturas.sql y demas)
 *       puedan abortar solos al detectar PRODUCCION, sin depender de que quien
 *       los corre se acuerde de pasar una confirmacion.
 *   <li>Detectar que la aplicacion quedo apuntando a la base del ambiente
 *       equivocado. Si la base dice PRODUCCION y la app arranca como
 *       HOMOLOGACION, algo esta muy mal: o se emiten comprobantes de prueba
 *       sobre datos reales, o se pisan datos reales con datos de prueba. En ese
 *       caso se aborta el arranque.
 * </ol>
 *
 * <p>Corre como {@link ApplicationRunner} para garantizar que Flyway ya aplico
 * las migraciones y la tabla existe.
 */
@Component
public class RegistroDeAmbiente implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RegistroDeAmbiente.class);

    private final JdbcTemplate jdbc;
    private final ArcaProperties properties;

    public RegistroDeAmbiente(JdbcTemplate jdbc, ArcaProperties properties) {
        this.jdbc = jdbc;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        registrar();
    }

    void registrar() {
        Ambiente actual = properties.ambiente();

        List<String> filas = jdbc.queryForList(
                "SELECT ambiente FROM ambiente_bd WHERE id = 1", String.class);

        if (filas.isEmpty()) {
            jdbc.update("INSERT INTO ambiente_bd (id, ambiente) VALUES (1, ?)", actual.name());
            log.info("Base marcada como {}", actual);
            return;
        }

        String registrado = filas.get(0);

        if (!registrado.equals(actual.name())) {
            throw new IllegalStateException("""
                    Ambiente cruzado: la base esta marcada como %s pero la aplicacion
                    arranca como %s.

                    Esto normalmente significa que el .env quedo apuntando a la base
                    equivocada. Antes de tocar nada, revisa DB_URL y ARCA_AMBIENTE.

                    Si el cambio es intencional (por ejemplo, promover esta base a
                    produccion), actualiza la marca a mano:
                      UPDATE ambiente_bd SET ambiente = '%s', registrado_en = now() WHERE id = 1;"""
                    .formatted(registrado, actual, actual));
        }

        log.info("Base verificada: ambiente {}", actual);
    }
}
