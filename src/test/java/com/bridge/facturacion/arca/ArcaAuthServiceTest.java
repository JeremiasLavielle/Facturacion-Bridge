package com.bridge.facturacion.arca;

import com.bridge.facturacion.EmisoresDePrueba;
import com.bridge.facturacion.emisor.Emisor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ArcaAuthServiceTest {

    private static final String CUIT_UNO = "20111111112";
    private static final String CUIT_DOS = "20222222223";

    @TempDir
    Path dir;

    private SoapClient soapClient;
    private ArcaAuthService authService;
    private Emisor emisorUno;
    private Emisor emisorDos;

    @BeforeEach
    void setUp() throws Exception {

        CertificadosDePrueba.generarParaCuit(dir, CUIT_UNO);
        CertificadosDePrueba.generarParaCuit(dir, CUIT_DOS);
        emisorUno = EmisoresDePrueba.emisor(1L, CUIT_UNO, 1);
        emisorDos = EmisoresDePrueba.emisor(2L, CUIT_DOS, 1);

        ArcaProperties properties = new ArcaProperties(
                dir.toString(), "http://test/wsaa", "http://test/wsfe",
                Ambiente.HOMOLOGACION, 15, 45);

soapClient = spy(new SoapClient(properties));
        authService = new ArcaAuthService(properties, soapClient);
    }

    private void stubRespuestaWsaa(OffsetDateTime expiration) {
        String ticket = """
                <loginTicketResponse>
                    <header><expirationTime>%s</expirationTime></header>
                    <credentials><token>TOKEN-TEST</token><sign>SIGN-TEST</sign></credentials>
                </loginTicketResponse>
                """.formatted(expiration);

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

        Credenciales credenciales = authService.getCredenciales(emisorUno);

        assertEquals("TOKEN-TEST", credenciales.token());
        assertEquals("SIGN-TEST", credenciales.sign());
        assertTrue(credenciales.vigente());
    }

    @Test
    void getCredenciales_cachea_noVuelveALlamarAWsaa() {
        stubRespuestaWsaa(OffsetDateTime.now().plusHours(12));

        authService.getCredenciales(emisorUno);
        authService.getCredenciales(emisorUno);
        authService.getCredenciales(emisorUno);

        verify(soapClient, times(1)).post(anyString(), anyString(), anyString());
    }

    @Test
    void getCredenciales_renueva_cuandoElTicketEstaPorVencer() {
        stubRespuestaWsaa(OffsetDateTime.now().plusMinutes(1));

        authService.getCredenciales(emisorUno);
        authService.getCredenciales(emisorUno);

        verify(soapClient, times(2)).post(anyString(), anyString(), anyString());
    }

    @Test
    void getCredenciales_unTicketPorCuit_noSePisanEntreSi() {
        stubRespuestaWsaa(OffsetDateTime.now().plusHours(12));

authService.getCredenciales(emisorUno);
        authService.getCredenciales(emisorDos);
        authService.getCredenciales(emisorUno);
        authService.getCredenciales(emisorDos);

        verify(soapClient, times(2)).post(anyString(), anyString(), anyString());
    }
}
