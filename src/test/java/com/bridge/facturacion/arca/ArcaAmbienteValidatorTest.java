package com.bridge.facturacion.arca;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArcaAmbienteValidatorTest {

    private static final String WSAA_HOMO = "https://wsaahomo.afip.gov.ar/ws/services/LoginCms";
    private static final String WSFE_HOMO = "https://wswhomo.afip.gov.ar/wsfev1/service.asmx";
    private static final String WSAA_PROD = "https://wsaa.afip.gov.ar/ws/services/LoginCms";
    private static final String WSFE_PROD = "https://servicios1.afip.gov.ar/wsfev1/service.asmx";

    private ArcaProperties propiedades(String wsaa, String wsfe, Ambiente ambiente) {
        return new ArcaProperties("/arca", wsaa, wsfe, ambiente, 15, 45);
    }

    private void validar(ArcaProperties propiedades) {
        // El constructor valida: si la config es incoherente, el bean no se crea
        // y Spring aborta el arranque.
        new ArcaAmbienteValidator(propiedades);
    }

    @Test
    void reconoceLosHostDeHomologacionPorElHomoEnElNombre() {
        assertEquals(Ambiente.HOMOLOGACION, ArcaAmbienteValidator.ambienteSegunUrl(WSAA_HOMO));
        assertEquals(Ambiente.HOMOLOGACION, ArcaAmbienteValidator.ambienteSegunUrl(WSFE_HOMO));
    }

    @Test
    void reconoceLosHostDeProduccion() {
        assertEquals(Ambiente.PRODUCCION, ArcaAmbienteValidator.ambienteSegunUrl(WSAA_PROD));
        assertEquals(Ambiente.PRODUCCION, ArcaAmbienteValidator.ambienteSegunUrl(WSFE_PROD));
    }

    @Test
    void unHostDesconocidoSeAsumeProduccion() {
        // Sesgo deliberado: ante la duda, bloquear el arranque antes que dejar
        // pasar comprobantes reales creyendo que son de prueba.
        assertEquals(Ambiente.PRODUCCION,
                ArcaAmbienteValidator.ambienteSegunUrl("https://wsaa.arca.gob.ar/ws/services/LoginCms"));
    }

    @Test
    void elHostLocalNoSeVerifica() {
        assertNull(ArcaAmbienteValidator.ambienteSegunUrl("http://localhost:8080/wsaa"));
        assertNull(ArcaAmbienteValidator.ambienteSegunUrl("http://127.0.0.1:9999/wsfe"));
    }

    @Test
    void aceptaLaConfiguracionCoherenteDeHomologacion() {
        assertDoesNotThrow(() ->
                validar(propiedades(WSAA_HOMO, WSFE_HOMO, Ambiente.HOMOLOGACION)));
    }

    @Test
    void aceptaLaConfiguracionCoherenteDeProduccion() {
        assertDoesNotThrow(() ->
                validar(propiedades(WSAA_PROD, WSFE_PROD, Ambiente.PRODUCCION)));
    }

    @Test
    void rechazaUrlsDeProduccionDeclaradasComoHomologacion() {
        // El caso peligroso: emitir comprobantes fiscales reales creyendo que
        // se esta probando.
        IllegalStateException error = assertThrows(IllegalStateException.class, () ->
                validar(propiedades(WSAA_PROD, WSFE_PROD, Ambiente.HOMOLOGACION)));

        assertTrue(error.getMessage().contains("incoherente"), error.getMessage());
    }

    @Test
    void rechazaUrlsDeHomologacionDeclaradasComoProduccion() {
        assertThrows(IllegalStateException.class, () ->
                validar(propiedades(WSAA_HOMO, WSFE_HOMO, Ambiente.PRODUCCION)));
    }

    @Test
    void rechazaLaMezclaDeAmbientesEntreLasDosUrls() {
        assertThrows(IllegalStateException.class, () ->
                validar(propiedades(WSAA_HOMO, WSFE_PROD, Ambiente.HOMOLOGACION)));
    }

    @Test
    void rechazaElAmbienteSinConfigurar() {
        IllegalStateException error = assertThrows(IllegalStateException.class, () ->
                validar(propiedades(WSAA_HOMO, WSFE_HOMO, null)));

        assertTrue(error.getMessage().contains("ARCA_AMBIENTE"), error.getMessage());
    }

    @Test
    void toleraLaArcaFalsaDeLosTests() {
        assertDoesNotThrow(() -> validar(propiedades(
                "http://localhost:1234/wsaa", "http://localhost:1234/wsfe", Ambiente.HOMOLOGACION)));
    }
}
