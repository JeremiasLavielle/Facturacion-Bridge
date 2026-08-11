package com.bridge.facturacion.arca;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;









class SoapClientTest {

    private static final String XML_OK = "<respuesta><dato>ok</dato></respuesta>";

    private static HttpServer server;
    private static String baseUrl;


    private static volatile String cuerpo;
    private static volatile int status;
    private static volatile long demoraMs;

    @BeforeAll
    static void levantarServidor() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/soap", exchange -> {
            try {
                exchange.getRequestBody().readAllBytes();
                if (demoraMs > 0) {
                    Thread.sleep(demoraMs);
                }
                byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "text/xml; charset=utf-8");
                exchange.sendResponseHeaders(status, bytes.length);
                exchange.getResponseBody().write(bytes);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });



        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        server.start();
        baseUrl = "http://localhost:" + server.getAddress().getPort() + "/soap";
    }

    @AfterAll
    static void apagarServidor() {
        server.stop(0);
    }

    @BeforeEach
    void resetearServidor() {
        cuerpo = XML_OK;
        status = 200;
        demoraMs = 0;
    }

    private SoapClient cliente() {

        ArcaProperties properties = new ArcaProperties(
                "certs", baseUrl, baseUrl, Ambiente.HOMOLOGACION, 1, 1);
        return new SoapClient(properties);
    }

    @Test
    void post_devuelveElDocumento_cuandoLaRespuestaEsValida() {
        Document doc = cliente().post(baseUrl, "accion", "<pedido/>");

        assertEquals("ok", cliente().firstText(doc, "dato"));
    }



    @Test
    void post_tiraComunicacion_cuandoElServidorNoRespondeATiempo() {
        demoraMs = 2000;

        ArcaException e = assertThrows(ArcaException.class,
                () -> cliente().post(baseUrl, "accion", "<pedido/>"));

        assertInstanceOf(ArcaComunicacionException.class, e);
    }

    @Test
    void post_tiraComunicacion_cuandoNoHayServidor() {

        ArcaException e = assertThrows(ArcaException.class,
                () -> cliente().post("http://localhost:1/soap", "accion", "<pedido/>"));

        assertInstanceOf(ArcaComunicacionException.class, e);
    }



    @Test
    void post_tiraDefinitiva_anteUnSoapFault() {
        status = 500;
        cuerpo = """
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                    <soap:Body><soap:Fault>
                        <faultstring>Token invalido</faultstring>
                    </soap:Fault></soap:Body>
                </soap:Envelope>
                """;

        ArcaException e = assertThrows(ArcaException.class,
                () -> cliente().post(baseUrl, "accion", "<pedido/>"));

        assertFalse(e instanceof ArcaComunicacionException,
                "un SOAP Fault es respuesta definitiva: no debe frenar el batch");
        assertTrue(e.getMessage().contains("Token invalido"));
    }

    @Test
    void post_tiraDefinitiva_anteHttpDistintoDe200SinFault() {
        status = 503;

        ArcaException e = assertThrows(ArcaException.class,
                () -> cliente().post(baseUrl, "accion", "<pedido/>"));

        assertFalse(e instanceof ArcaComunicacionException);
        assertTrue(e.getMessage().contains("503"));
    }

    @Test
    void post_tiraDefinitiva_anteXmlInvalido() {
        cuerpo = "esto no es xml";

        ArcaException e = assertThrows(ArcaException.class,
                () -> cliente().post(baseUrl, "accion", "<pedido/>"));

        assertFalse(e instanceof ArcaComunicacionException);
    }
}
