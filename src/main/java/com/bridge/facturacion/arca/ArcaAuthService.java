package com.bridge.facturacion.arca;

import com.bridge.facturacion.emisor.Emisor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Autenticacion WSAA. Desde la Fase 7 el ticket se cachea POR CUIT:
 * cada emisor firma el TRA con SU certificado y obtiene su propio ticket,
 * sin pisar el de los demas.
 */
@Service
public class ArcaAuthService {

    private static final Logger log = LoggerFactory.getLogger(ArcaAuthService.class);

    private final ArcaProperties properties;
    private final SoapClient soapClient;

    private final Map<String, Credenciales> cachePorCuit = new ConcurrentHashMap<>();

    public ArcaAuthService(ArcaProperties properties, SoapClient soapClient) {
        this.properties = properties;
        this.soapClient = soapClient;
    }

    public synchronized Credenciales getCredenciales(Emisor emisor) {
        Credenciales cache = cachePorCuit.get(emisor.getCuit());
        if (cache != null && cache.vigente()) {
            return cache;
        }
        log.info("Solicitando ticket WSAA para CUIT {} ({})", emisor.getCuit(), properties.ambiente());
        Credenciales nuevas = login(emisor);
        cachePorCuit.put(emisor.getCuit(), nuevas);
        log.info("Ticket WSAA de CUIT {} obtenido, valido hasta {}", emisor.getCuit(), nuevas.expiration());
        return nuevas;
    }

    private Credenciales login(Emisor emisor) {
        String tra = TraBuilder.build();
        String cms = CmsSigner.signBase64(tra,
                rutaAbsoluta(emisor.getCertPath()),
                rutaAbsoluta(emisor.getKeyPath()));

        String envelope = """
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                                  xmlns:wsaa="http://wsaa.view.sua.dvadac.desein.afip.gov">
                    <soapenv:Body>
                        <wsaa:loginCms>
                            <wsaa:in0>%s</wsaa:in0>
                        </wsaa:loginCms>
                    </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(cms);

        Document response = soapClient.post(properties.urlWsaa(), "", envelope);

        String inner = soapClient.firstText(response, "loginCmsReturn");
        if (inner == null) {
            throw new ArcaException("WSAA no devolvio loginCmsReturn");
        }
        Document ticket = soapClient.parse(inner);

        String token = soapClient.firstText(ticket, "token");
        String sign = soapClient.firstText(ticket, "sign");
        String expiration = soapClient.firstText(ticket, "expirationTime");
        if (token == null || sign == null || expiration == null) {
            throw new ArcaException("Respuesta de WSAA incompleta (falta token/sign/expirationTime)");
        }
        return new Credenciales(token, sign, OffsetDateTime.parse(expiration).toInstant());
    }

    /** Los paths del emisor son relativos a arca.certs-dir. */
    private String rutaAbsoluta(String pathRelativo) {
        return Path.of(properties.certsDir()).resolve(pathRelativo).toString();
    }
}
