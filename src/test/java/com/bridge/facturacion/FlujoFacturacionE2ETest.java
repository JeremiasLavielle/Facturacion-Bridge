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

    // ---------- ARCA falsa: servidor HTTP local que habla SOAP ----------

    private static final HttpServer ARCA_FALSA;

    static {
        try {
            ARCA_FALSA = HttpServer.create(new InetSocketAddress(0), 0);
            ARCA_FALSA.createContext("/wsaa", exchange -> responder(exchange, respuestaWsaa()));
            ARCA_FALSA.createContext("/wsfe", exchange -> {
                String action = exchange.getRequestHeaders().getFirst("Soapaction");
                if (action != null && action.contains("FECompUltimoAutorizado")) {
                    responder(exchange, respuestaUltimoAutorizado(41));
                } else if (action != null && action.contains("FECAESolicitar")) {
                    responder(exchange, respuestaCaeAprobado());
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

    private static String respuestaCaeAprobado() {
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
                """.formatted(CAE));
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

        // 2. Alta de alumno.
        MvcResult alumno = mockMvc.perform(post("/alumnos").session(sesion).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Alumna E2E","dni":"30123456","condicionIva":"CONSUMIDOR_FINAL"}"""))
                .andExpect(status().isCreated())
                .andReturn();
        long alumnoId = idDe(alumno);

        // 3. Alta de factura del periodo julio 2026.
        MvcResult factura = mockMvc.perform(post("/facturas").session(sesion).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"alumnoId":%d,"monto":15000.00,"periodo":"2026-07-01"}"""
                                .formatted(alumnoId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andReturn();
        long facturaId = idDe(factura);

        // 4. Emision: dispara WSAA (firma CMS real) + WSFE contra la ARCA falsa.
        mockMvc.perform(post("/facturas/{id}/emitir", facturaId).session(sesion).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EMITIDA"))
                .andExpect(jsonPath("$.cae").value(CAE))
                .andExpect(jsonPath("$.numeroComprobante").value(42)); // ultimo (41) + 1

        // 5. Descarga del PDF del comprobante.
        byte[] pdf = mockMvc.perform(get("/facturas/{id}/pdf", facturaId).session(sesion))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn().getResponse().getContentAsByteArray();

        assertTrue(pdf.length > 1000, "el PDF deberia tener contenido real");
        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));
    }

    private long idDe(MvcResult resultado) throws Exception {
        JsonNode json = objectMapper.readTree(resultado.getResponse().getContentAsString());
        return json.get("id").asLong();
    }
}
