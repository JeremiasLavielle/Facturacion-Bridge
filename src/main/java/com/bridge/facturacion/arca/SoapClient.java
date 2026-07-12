package com.bridge.facturacion.arca;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Cliente SOAP minimo: POST HTTP con el XML como texto y parseo de la
 * respuesta a un Document (arbol XML navegable).
 *
 * Decision de diseno: SOAP "a mano" en vez de stubs generados por WSDL —
 * mas transparente, mas facil de debuggear y sin fragilidad JAXB/JDK.
 */
@Component
public class SoapClient {

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    public Document post(String url, String soapAction, String xmlBody) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(45))
                    .header("Content-Type", "text/xml; charset=utf-8")
                    .header("SOAPAction", soapAction)
                    .POST(HttpRequest.BodyPublishers.ofString(xmlBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            Document doc = parse(response.body());

            // Un SOAP Fault puede venir con status 500: lo detectamos por contenido.
            String fault = firstText(doc, "faultstring");
            if (fault != null) {
                throw new ArcaException("SOAP Fault de ARCA: " + fault);
            }
            if (response.statusCode() != 200) {
                throw new ArcaException("ARCA respondio HTTP " + response.statusCode());
            }
            return doc;
        } catch (ArcaException e) {
            throw e;
        } catch (Exception e) {
            throw new ArcaException("Fallo la comunicacion con ARCA: " + e.getMessage(), e);
        }
    }

    /** Parsea un string XML con configuracion segura (sin entidades externas). */
    public Document parse(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setNamespaceAware(true);
            return factory.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new ArcaException("Respuesta XML invalida de ARCA", e);
        }
    }

    /** Texto del primer elemento con ese nombre (ignorando namespace), o null. */
    public String firstText(Document doc, String localName) {
        NodeList nodes = doc.getElementsByTagNameNS("*", localName);
        return nodes.getLength() > 0 ? nodes.item(0).getTextContent() : null;
    }
}
