package com.bridge.facturacion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FlujoFacturacionE2ETest extends IntegracionTestBase {

    private static final String OPERADOR_EMAIL = "e2e@bridge.local";
    private static final String OPERADOR_PASSWORD = "clave-e2e";
    private static final String CAE = "75123456789012";
    private static final String CAE_NC = "75123456789099";

    // ---------- ARCA falsa: servidor HTTP local que habla SOAP ----------

    private static final HttpServer ARCA_FALSA;

    static {
        try {
            ARCA_FALSA = HttpServer.create(new InetSocketAddress(0), 0);
            ARCA_FALSA.createContext("/wsaa", exchange -> responder(exchange, respuestaWsaa()));
            ARCA_FALSA.createContext("/wsfe", exchange -> {
                String action = exchange.getRequestHeaders().getFirst("Soapaction");
                // La numeracion es por TIPO: factura C (11) va por 41,
                // nota de credito C (13) por 7 (numeracion propia).
                String cuerpo = new String(exchange.getRequestBody().readAllBytes(),
                        StandardCharsets.UTF_8);
                boolean esNotaCredito = cuerpo.contains("<ar:CbteTipo>13</ar:CbteTipo>");
                if (action != null && action.contains("FECompUltimoAutorizado")) {
                    responder(exchange, respuestaUltimoAutorizado(esNotaCredito ? 7 : 41));
                } else if (action != null && action.contains("FECAESolicitar")) {
                    responder(exchange, respuestaCaeAprobado(esNotaCredito ? CAE_NC : CAE));
                } else {
                    responder(exchange, "<sin-handler/>");
                }
            });
            ARCA_FALSA.start();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo levantar la ARCA falsa", e);
        }
    }

    private static void responder(HttpExchange exchange, String xml) throws IOException {
        exchange.getRequestBody().readAllBytes();
        byte[] bytes = xml.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/xml; charset=utf-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static String envolver(String contenido) {
        return """
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                    <soap:Body>%s</soap:Body>
                </soap:Envelope>
                """.formatted(contenido);
    }

    private static String respuestaWsaa() {
        String ticket = """
                <loginTicketResponse>
                    <header><expirationTime>%s</expirationTime></header>
                    <credentials><token>TOKEN-E2E</token><sign>SIGN-E2E</sign></credentials>
                </loginTicketResponse>
                """.formatted(OffsetDateTime.now().plusHours(12));
        return envolver("""
                <loginCmsResponse><loginCmsReturn>%s</loginCmsReturn></loginCmsResponse>"""
                .formatted(ticket.replace("<", "&lt;").replace(">", "&gt;")));
    }

    private static String respuestaUltimoAutorizado(long numero) {
        return envolver("""
                <FECompUltimoAutorizadoResponse xmlns="http://ar.gov.afip.dif.FEV1/">
                    <FECompUltimoAutorizadoResult>
                        <PtoVta>1</PtoVta><CbteTipo>11</CbteTipo><CbteNro>%d</CbteNro>
                    </FECompUltimoAutorizadoResult>
                </FECompUltimoAutorizadoResponse>
                """.formatted(numero));
    }

    private static String respuestaCaeAprobado(String cae) {
        return envolver("""
                <FECAESolicitarResponse xmlns="http://ar.gov.afip.dif.FEV1/">
                    <FECAESolicitarResult>
                        <FeCabResp><Resultado>A</Resultado></FeCabResp>
                        <FeDetResp><FECAEDetResponse>
                            <Resultado>A</Resultado>
                            <CAE>%s</CAE>
                            <CAEFchVto>20260818</CAEFchVto>
                        </FECAEDetResponse></FeDetResp>
                    </FECAESolicitarResult>
                </FECAESolicitarResponse>
                """.formatted(cae));
    }

    @DynamicPropertySource
    static void apuntarALaArcaFalsa(DynamicPropertyRegistry registry) {
        int puerto = ARCA_FALSA.getAddress().getPort();
        registry.add("arca.url-wsaa", () -> "http://localhost:" + puerto + "/wsaa");
        registry.add("arca.url-wsfe", () -> "http://localhost:" + puerto + "/wsfe");
        // Operador conocido, sin depender de variables de entorno de la maquina.
        registry.add("app.operador.email", () -> OPERADOR_EMAIL);
        registry.add("app.operador.password", () -> OPERADOR_PASSWORD);
    }

    // ---------- el flujo ----------

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void flujoCompleto_delLoginALaDescargaDelPdf() throws Exception {
        // 1. Login con el operador creado por UsuarioBootstrap al arrancar.
        MvcResult login = mockMvc.perform(post("/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}"""
                                .formatted(OPERADOR_EMAIL, OPERADOR_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession sesion = (MockHttpSession) login.getRequest().getSession(false);
        assertNotNull(sesion, "el login debe dejar una sesion creada");

        // 2. Los DOS emisores de la migracion V6 estan disponibles.
        mockMvc.perform(get("/emisores").session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].puntoVenta").value(1))
                .andExpect(jsonPath("$[1].puntoVenta").value(2));

        // 3. Alta de alumnos (uno para cada emisor).
        MvcResult alumno = mockMvc.perform(post("/alumnos").session(sesion).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Alumna E2E","dni":"30123456","condicionIva":"CONSUMIDOR_FINAL"}"""))
                .andExpect(status().isCreated())
                .andReturn();
        long alumnoId = idDe(alumno);

        MvcResult alumno2 = mockMvc.perform(post("/alumnos").session(sesion).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Alumno E2E Dos","dni":"30123457","condicionIva":"CONSUMIDOR_FINAL"}"""))
                .andExpect(status().isCreated())
                .andReturn();
        long alumno2Id = idDe(alumno2);

        // 4. Alta de facturas del periodo julio 2026: una CON CADA EMISOR.
        MvcResult factura = mockMvc.perform(post("/facturas").session(sesion).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"alumnoId":%d,"emisorId":1,"monto":15000.00,"periodo":"2026-07-01"}"""
                                .formatted(alumnoId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.emisor.puntoVenta").value(1))
                .andReturn();
        long facturaId = idDe(factura);

        MvcResult factura2 = mockMvc.perform(post("/facturas").session(sesion).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"alumnoId":%d,"emisorId":2,"monto":18000.00,"periodo":"2026-07-01"}"""
                                .formatted(alumno2Id)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.emisor.puntoVenta").value(2))
                .andReturn();
        long factura2Id = idDe(factura2);

        // 5. Emision: dispara WSAA (firma CMS real) + WSFE contra la ARCA falsa.
        mockMvc.perform(post("/facturas/{id}/emitir", facturaId).session(sesion).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EMITIDA"))
                .andExpect(jsonPath("$.cae").value(CAE))
                .andExpect(jsonPath("$.numeroComprobante").value(42)); // ultimo (41) + 1

        mockMvc.perform(post("/facturas/{id}/emitir", factura2Id).session(sesion).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EMITIDA"))
                .andExpect(jsonPath("$.emisor.puntoVenta").value(2));

        // 6. Descarga de los PDFs (cada uno con los datos de su emisor).
        byte[] pdf = mockMvc.perform(get("/facturas/{id}/pdf", facturaId).session(sesion))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"factura-0001-00000042.pdf\""))
                .andReturn().getResponse().getContentAsByteArray();

        assertTrue(pdf.length > 1000, "el PDF deberia tener contenido real");
        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));

        // El PDF del emisor 2 sale con SU punto de venta en el nombre.
        mockMvc.perform(get("/facturas/{id}/pdf", factura2Id).session(sesion))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"factura-0002-00000042.pdf\""));

        // 7. Nota de credito (Fase 8): anula la factura del emisor 1.
        //    Numeracion PROPIA del tipo 13: ultimo (7) + 1 = 8.
        MvcResult nc = mockMvc.perform(post("/facturas/{id}/nota-credito", facturaId)
                        .session(sesion).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motivo":"error en el monto facturado"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("EMITIDA"))
                .andExpect(jsonPath("$.cae").value(CAE_NC))
                .andExpect(jsonPath("$.numeroComprobante").value(8))
                .andExpect(jsonPath("$.facturaId").value((int) facturaId))
                .andReturn();
        long ncId = idDe(nc);

        // 8. La factura quedo ANULADA (conserva su CAE) y linkea a su NC.
        mockMvc.perform(get("/facturas/{id}", facturaId).session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ANULADA"))
                .andExpect(jsonPath("$.cae").value(CAE));
        mockMvc.perform(get("/facturas/{id}/nota-credito", facturaId).session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value((int) ncId));

        // 9. Una segunda NC sobre la misma factura se rechaza (409).
        mockMvc.perform(post("/facturas/{id}/nota-credito", facturaId)
                        .session(sesion).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motivo":"segundo intento"}"""))
                .andExpect(status().isConflict());

        // 10. PDF de la NC.
        byte[] pdfNc = mockMvc.perform(get("/notas-credito/{id}/pdf", ncId).session(sesion))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"nota-credito-0001-00000008.pdf\""))
                .andReturn().getResponse().getContentAsByteArray();

        assertTrue(pdfNc.length > 1000, "el PDF de la NC deberia tener contenido real");
        assertEquals("%PDF", new String(pdfNc, 0, 4, StandardCharsets.US_ASCII));
    }

    private long idDe(MvcResult resultado) throws Exception {
        JsonNode json = objectMapper.readTree(resultado.getResponse().getContentAsString());
        return json.get("id").asLong();
    }
}
