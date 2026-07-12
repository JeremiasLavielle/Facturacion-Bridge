package com.bridge.facturacion.arca;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Testea el flujo WSAA completo (TRA + firma real con un certificado de
 * prueba) cortando solo la salida a la red: el post() esta stubeado.
 * Lo importante aca es el CACHE: pedir dos tickets seguidos a WSAA real
 * es un error (rechaza el segundo), asi que el cache tiene que funcionar.
 */
class ArcaAuthServiceTest {

    @TempDir
    Path dir;

    private SoapClient soapClient;
    private ArcaAuthService authService;

    @BeforeEach
    void setUp() throws Exception {
        CertificadosDePrueba.Rutas rutas = CertificadosDePrueba.generarEn(dir);
        ArcaProperties properties = new ArcaProperties(
                "20111111112", 1, rutas.cert(), rutas.key(),
                "http://test/wsaa", "http://test/wsfe", Ambiente.HOMOLOGACION);

        // spy: objeto REAL (parse y firstText funcionan de verdad),
        // pero le stubeamos post() para no salir a internet.
        soapClient = spy(new SoapClient());
        authService = new ArcaAuthService(properties, soapClient);
    }

    private void stubRespuestaWsaa(OffsetDateTime expiration) {
        String ticket = """
                <loginTicketResponse>
                    <header><expirationTime>%s</expirationTime></header>
                    <credentials><token>TOKEN-TEST</token><sign>SIGN-TEST</sign></credentials>
                </loginTicketResponse>
                """.formatted(expiration);
        // WSAA devuelve el ticket ESCAPADO adentro de loginCmsReturn.
        String envelope = """
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                    <soap:Body>
                        <loginCmsResponse>
                            <loginCmsReturn>%s</loginCmsReturn>
                        </loginCmsResponse>
                    </soap:Body>
                </soap:Envelope>
                """.formatted(ticket.replace("<", "&lt;").replace(">", "&gt;"));
        doReturn(soapClient.parse(envelope))
                .when(soapClient).post(eq("http://test/wsaa"), eq(""), anyString());
    }

    @Test
    void getCredenciales_haceLoginYDevuelveTokenYSign() {
        stubRespuestaWsaa(OffsetDateTime.now().plusHours(12));

        Credenciales credenciales = authService.getCredenciales();

        assertEquals("TOKEN-TEST", credenciales.token());
        assertEquals("SIGN-TEST", credenciales.sign());
        assertTrue(credenciales.vigente());
    }

    @Test
    void getCredenciales_cachea_noVuelveALlamarAWsaa() {
        stubRespuestaWsaa(OffsetDateTime.now().plusHours(12));

        authService.getCredenciales();
        authService.getCredenciales();
        authService.getCredenciales();

        // Un solo login real, el resto salio del cache.
        verify(soapClient, times(1)).post(anyString(), anyString(), anyString());
    }

    @Test
    void getCredenciales_renueva_cuandoElTicketEstaPorVencer() {
        // Vence en 1 minuto: menor al margen de 5, se considera NO vigente.
        stubRespuestaWsaa(OffsetDateTime.now().plusMinutes(1));

        authService.getCredenciales();
        authService.getCredenciales();

        verify(soapClient, times(2)).post(anyString(), anyString(), anyString());
    }
}
