package com.bridge.facturacion.arca;

import com.bridge.facturacion.EmisoresDePrueba;
import com.bridge.facturacion.emisor.Emisor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ArcaClientTest {

    private static final String ACTION_ULTIMO = ArcaClient.NS + "FECompUltimoAutorizado";
    private static final String ACTION_CAE = ArcaClient.NS + "FECAESolicitar";
    private static final String ACTION_CONSULTAR = ArcaClient.NS + "FECompConsultar";

    private SoapClient soapClient;
    private ArcaClient arcaClient;
    private Emisor emisor;

    @BeforeEach
    void setUp() {
        ArcaProperties properties = new ArcaProperties(
                "certs", "http://test/wsaa", "http://test/wsfe",
                Ambiente.HOMOLOGACION, 15, 45);
        emisor = EmisoresDePrueba.emisor(1L, "20111111112", 1);
        ArcaAuthService authService = mock(ArcaAuthService.class);
        when(authService.getCredenciales(any(Emisor.class)))
                .thenReturn(new Credenciales("T", "S", Instant.now().plusSeconds(43200)));

        soapClient = spy(new SoapClient(properties));
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

        assertEquals(41, arcaClient.ultimoComprobanteAutorizado(emisor));
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
                emisor, 96, 12345678L, new BigDecimal("15000"), LocalDate.of(2026, 7, 1), 5);

        assertTrue(resultado.aprobada());
        assertEquals(42, resultado.numeroComprobante());
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

        arcaClient.solicitarCae(emisor, 96, 12345678L, new BigDecimal("15000"), LocalDate.of(2026, 7, 15), 5);

ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(soapClient).post(anyString(), eq(ACTION_CAE), captor.capture());
        String request = captor.getValue();

        assertTrue(request.contains("<ar:Cuit>20111111112</ar:Cuit>"));
        assertTrue(request.contains("<ar:PtoVta>1</ar:PtoVta>"));
        assertTrue(request.contains("<ar:CbteTipo>11</ar:CbteTipo>"));
        assertTrue(request.contains("<ar:CbteDesde>42</ar:CbteDesde>"));
        assertTrue(request.contains("<ar:ImpTotal>15000.00</ar:ImpTotal>"));
        assertTrue(request.contains("<ar:ImpIVA>0</ar:ImpIVA>"));
        assertTrue(request.contains("<ar:FchServDesde>20260701</ar:FchServDesde>"));
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
                emisor, 96, 12345678L, new BigDecimal("15000"), LocalDate.of(2026, 7, 1), 5);

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
                () -> arcaClient.solicitarCae(emisor, 96, 12345678L, new BigDecimal("15000"),
                        LocalDate.of(2026, 7, 1), 5));
        assertTrue(ex.getMessage().contains("600"));
    }

    @Test
    void consultarUltimoEmitido_devuelveElDetalleDelComprobante() {
        stubUltimoAutorizado(42);
        String xml = envolver("""
                <FECompConsultarResponse xmlns="http://ar.gov.afip.dif.FEV1/">
                    <FECompConsultarResult><ResultGet>
                        <Concepto>2</Concepto>
                        <DocTipo>96</DocTipo><DocNro>12345678</DocNro>
                        <CbteDesde>42</CbteDesde><CbteHasta>42</CbteHasta>
                        <ImpTotal>15000.00</ImpTotal>
                        <FchServDesde>20260501</FchServDesde>
                        <FchServHasta>20260531</FchServHasta>
                        <CodAutorizacion>75123456789012</CodAutorizacion>
                        <FchVto>20260718</FchVto>
                    </ResultGet></FECompConsultarResult>
                </FECompConsultarResponse>
                """);
        doReturn(soapClient.parse(xml))
                .when(soapClient).post(anyString(), eq(ACTION_CONSULTAR), anyString());

        ComprobanteEmitido ultimo = arcaClient.consultarUltimoEmitido(emisor);

        assertEquals(42, ultimo.numero());
        assertEquals(12345678L, ultimo.docNro());
        assertEquals(0, ultimo.importeTotal().compareTo(new BigDecimal("15000.00")));
        assertEquals(LocalDate.of(2026, 5, 1), ultimo.servicioDesde());
        assertEquals("75123456789012", ultimo.cae());
        assertEquals(LocalDate.of(2026, 7, 18), ultimo.vencimientoCae());
    }

@Test
    void ultimoComprobanteAutorizado_tipo13_consultaLaNumeracionPropia() {
        stubUltimoAutorizado(7);

        arcaClient.ultimoComprobanteAutorizado(emisor, ArcaClient.NOTA_CREDITO_C);

ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(soapClient).post(anyString(), eq(ACTION_ULTIMO), captor.capture());
        assertTrue(captor.getValue().contains("<ar:CbteTipo>13</ar:CbteTipo>"));
    }

    @Test
    void solicitarCae_tipo13_armaElRequestConCbtesAsoc() {
        stubUltimoAutorizado(7);
        stubSolicitarCae("""
                <FeCabResp><Resultado>A</Resultado></FeCabResp>
                <FeDetResp><FECAEDetResponse>
                    <Resultado>A</Resultado><CAE>75123456789099</CAE><CAEFchVto>20260830</CAEFchVto>
                </FECAEDetResponse></FeDetResp>
                """);
        ComprobanteAsociado facturaOriginal = new ComprobanteAsociado(
                ArcaClient.FACTURA_C, 1, 42L, "20111111112", LocalDate.of(2026, 7, 4));

        ResultadoEmision resultado = arcaClient.solicitarCae(
                emisor, ArcaClient.NOTA_CREDITO_C, facturaOriginal,
                96, 12345678L, new BigDecimal("15000"), LocalDate.of(2026, 7, 1), 5);

        assertTrue(resultado.aprobada());
        assertEquals(8, resultado.numeroComprobante());

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(soapClient).post(anyString(), eq(ACTION_CAE), captor.capture());
        String request = captor.getValue();

        assertTrue(request.contains("<ar:CbteTipo>13</ar:CbteTipo>"));
        assertTrue(request.contains("<ar:CbtesAsoc>"));
        assertTrue(request.contains("<ar:Tipo>11</ar:Tipo>"));
        assertTrue(request.contains("<ar:Nro>42</ar:Nro>"));
        assertTrue(request.contains("<ar:Cuit>20111111112</ar:Cuit>"));
        assertTrue(request.contains("<ar:CbteFch>20260704</ar:CbteFch>"));
    }

    @Test
    void solicitarCae_tipo11_noIncluyeCbtesAsoc() {
        stubUltimoAutorizado(41);
        stubSolicitarCae("""
                <FeCabResp><Resultado>A</Resultado></FeCabResp>
                <FeDetResp><FECAEDetResponse>
                    <Resultado>A</Resultado><CAE>75123456789012</CAE><CAEFchVto>20260718</CAEFchVto>
                </FECAEDetResponse></FeDetResp>
                """);

        arcaClient.solicitarCae(emisor, 96, 12345678L, new BigDecimal("15000"),
                LocalDate.of(2026, 7, 1), 5);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(soapClient).post(anyString(), eq(ACTION_CAE), captor.capture());
        assertFalse(captor.getValue().contains("CbtesAsoc"));
    }

    @Test
    void consultarUltimoEmitido_devuelveNull_cuandoNoExisteComprobante() {

        stubUltimoAutorizado(42);
        String xml = envolver("""
                <FECompConsultarResponse xmlns="http://ar.gov.afip.dif.FEV1/">
                    <FECompConsultarResult>
                        <Errors><Err><Code>602</Code><Msg>No existe comprobante</Msg></Err></Errors>
                    </FECompConsultarResult>
                </FECompConsultarResponse>
                """);
        doReturn(soapClient.parse(xml))
                .when(soapClient).post(anyString(), eq(ACTION_CONSULTAR), anyString());

        assertNull(arcaClient.consultarUltimoEmitido(emisor));
    }
}
