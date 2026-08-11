package com.bridge.facturacion.arca;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistroDeAmbienteTest {

    private static final String CONSULTA = "SELECT ambiente FROM ambiente_bd WHERE id = 1";
    private static final String INSERT = "INSERT INTO ambiente_bd (id, ambiente) VALUES (1, ?)";

    @Mock
    private JdbcTemplate jdbc;

    private RegistroDeAmbiente registro(Ambiente ambiente) {
        ArcaProperties propiedades = new ArcaProperties(
                "/arca",
                "https://wsaahomo.afip.gov.ar/ws/services/LoginCms",
                "https://wswhomo.afip.gov.ar/wsfev1/service.asmx",
                ambiente, 15, 45);
        return new RegistroDeAmbiente(jdbc, propiedades);
    }

    @Test
    void marcaLaBaseLaPrimeraVez() {
        when(jdbc.queryForList(CONSULTA, String.class)).thenReturn(List.of());

        registro(Ambiente.HOMOLOGACION).registrar();

        verify(jdbc).update(INSERT, "HOMOLOGACION");
    }

    @Test
    void noVuelveAEscribirSiYaCoincide() {
        when(jdbc.queryForList(CONSULTA, String.class)).thenReturn(List.of("HOMOLOGACION"));

        registro(Ambiente.HOMOLOGACION).registrar();

        verify(jdbc, never()).update(anyString(), eq("HOMOLOGACION"));
    }

    @Test
    void abortaSiLaBaseEsDeProduccionYLaAppArrancaComoHomologacion() {
        // El caso grave: emitir comprobantes de prueba sobre datos fiscales reales.
        when(jdbc.queryForList(CONSULTA, String.class)).thenReturn(List.of("PRODUCCION"));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> registro(Ambiente.HOMOLOGACION).registrar());

        assertTrue(error.getMessage().contains("Ambiente cruzado"), error.getMessage());
        verify(jdbc, never()).update(anyString(), anyString());
    }

    @Test
    void abortaSiLaBaseEsDeHomologacionYLaAppArrancaComoProduccion() {
        // El reflejo del anterior: pisar datos reales con una base de prueba.
        when(jdbc.queryForList(CONSULTA, String.class)).thenReturn(List.of("HOMOLOGACION"));

        assertThrows(IllegalStateException.class,
                () -> registro(Ambiente.PRODUCCION).registrar());
    }
}
