package com.bridge.facturacion.arca;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Testea el armado del request y el parseo de respuestas de WSFE usando
 * XMLs con la misma forma que devuelve ARCA. La red esta stubeada.
 */
class ArcaClientTest {

    private static final String ACTION_ULTIMO = ArcaClient.NS + "FECompUltimoAutorizado";
    private static final String ACTION_CAE = ArcaClient.NS + "FECAESolicitar";

    private SoapClient soapClient;
    private ArcaClient arcaClient;

    @BeforeEach
    void setUp() {
        ArcaProperties properties = new ArcaProperties(
                "20111111112", 1, "cert", "key",
                "http://test/wsaa", "http://test/wsfe", Ambiente.HOMOLOGACION);
        ArcaAuthService authService = mock(ArcaAuthService.class);
        when(authService.getCredenciales())
                .thenReturn(new Credenciales("T", "S", Instant.now().plusSeconds(43200)));

        soapClient = spy(new SoapClient());
        arcaClient = new ArcaClient(properties, authService, soapClient);
    }

    private String envolver(String contenido) {
        return """
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                    <soap:Body>%s</soap:Body>
                </soap:Envelope>
                """.formatted(contenido);
    }

    private void stubUltimoAutorizado(long numero) {
        String xml = envolver("""
                <FECompUltimoAutorizadoResponse xmlns="http://ar.gov.afip.dif.FEV1/">
                    <FECompUltimoAutorizadoResult>
                        <PtoVta>1</PtoVta><CbteTipo>11</CbteTipo><CbteNro>%d</CbteNro>
                    </FECompUltimoAutorizadoResult>
                </FECompUltimoAutorizadoResponse>
                """.formatted(numero));
        doReturn(soapClient.parse(xml))
                .when(soapClient).post(anyString(), eq(ACTION_ULTIMO), anyString());
    }

    private void stubSolicitarCae(String cuerpoResult) {
        String xml = envolver("""
                <FECAESolicitarResponse xmlns="http://ar.gov.afip.dif.FEV1/">
                    <FECAESolicitarResult>%s</FECAESolicitarResult>
                </FECAESolicitarResponse>
                """.formatted(cuerpoResult));
        doReturn(soapClient.parse(xml))
                .when(soapClient).post(anyString(), eq(ACTION_CAE), anyString());
    }

    @Test
    void ultimoComprobanteAutorizado_devuelveElNumero() {
        stubUltimoAutorizado(41);

        assertEquals(41, arcaClient.ultimoComprobanteAutorizado());
    }

    @Test
    void solicitarCae_devuelveAprobada_conElNumeroSiguiente() {
        stubUltimoAutorizado(41);
        stubSolicitarCae("""
                <FeCabResp><Resultado>A</Resultado></FeCabResp>
                <FeDetResp><FECAEDetResponse>
                    <Resultado>A</Resultado>
                    <CAE>75123456789012</CAE>
                    <CAEFchVto>20260718</CAEFchVto>
                </FECAEDetResponse></FeDetResp>
                """);

        ResultadoEmision resultado = arcaClient.solicitarCae(
                96, 12345678L, new BigDecimal("15000"), LocalDate.of(2026, 7, 1), 5);

        assertTrue(resultado.aprobada());
        assertEquals(42, resultado.numeroComprobante()); // ultimo (41) + 1
        assertEquals("75123456789012", resultado.cae());
        assertEquals(LocalDate.of(2026, 7, 18), resultado.vencimientoCae());
    }

    @Test
    void solicitarCae_armaElRequestConLosDatosFiscalesCorrectos() {
        stubUltimoAutorizado(41);
        stubSolicitarCae("""
                <FeCabResp><Resultado>A</Resultado></FeCabResp>
                <FeDetResp><FECAEDetResponse>
                    <Resultado>A</Resultado><CAE>75123456789012</CAE><CAEFchVto>20260718</CAEFchVto>
                </FECAEDetResponse></FeDetResp>
                """);

        arcaClient.solicitarCae(96, 12345678L, new BigDecimal("15000"), LocalDate.of(2026, 7, 15), 5);

        // Capturamos el XML que se le mando a ARCA y verificamos lo critico.
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(soapClient).post(anyString(), eq(ACTION_CAE), captor.capture());
        String request = captor.getValue();

        assertTrue(request.contains("<ar:CbteTipo>11</ar:CbteTipo>"));           // Factura C
        assertTrue(request.contains("<ar:CbteDesde>42</ar:CbteDesde>"));         // numeracion de ARCA + 1
        assertTrue(request.contains("<ar:ImpTotal>15000.00</ar:ImpTotal>"));     // 2 decimales
        assertTrue(request.contains("<ar:ImpIVA>0</ar:ImpIVA>"));                // C: sin IVA discriminado
        assertTrue(request.contains("<ar:FchServDesde>20260701</ar:FchServDesde>")); // mes completo
        assertTrue(request.contains("<ar:FchServHasta>20260731</ar:FchServHasta>"));
        assertTrue(request.contains("<ar:CondicionIVAReceptorId>5</ar:CondicionIVAReceptorId>"));
    }

    @Test
    void solicitarCae_devuelveRechazo_conLosMotivos() {
        stubUltimoAutorizado(41);
        stubSolicitarCae("""
                <FeCabResp><Resultado>R</Resultado></FeCabResp>
                <FeDetResp><FECAEDetResponse>
                    <Resultado>R</Resultado>
                    <Observaciones><Obs>
                        <Code>10048</Code><Msg>Campo DocNro invalido</Msg>
                    </Obs></Observaciones>
                </FECAEDetResponse></FeDetResp>
                """);

        ResultadoEmision resultado = arcaClient.solicitarCae(
                96, 12345678L, new BigDecimal("15000"), LocalDate.of(2026, 7, 1), 5);

        assertFalse(resultado.aprobada());
        assertNull(resultado.cae());
        assertEquals(1, resultado.mensajes().size());
        assertTrue(resultado.mensajes().get(0).contains("10048"));
    }

    @Test
    void solicitarCae_tiraArcaException_cuandoWsfeDevuelveErrores() {
        stubUltimoAutorizado(41);
        stubSolicitarCae("""
                <Errors><Err>
                    <Code>600</Code><Msg>Token invalido</Msg>
                </Err></Errors>
                """);

        ArcaException ex = assertThrows(ArcaException.class,
                () -> arcaClient.solicitarCae(96, 12345678L, new BigDecimal("15000"),
                        LocalDate.of(2026, 7, 1), 5));
        assertTrue(ex.getMessage().contains("600"));
    }
}
