package com.bridge.facturacion.arca;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

import java.time.OffsetDateTime;

@Service
public class ArcaAuthService {

    private static final Logger log = LoggerFactory.getLogger(ArcaAuthService.class);

    private final ArcaProperties properties;
    private final SoapClient soapClient;

    private volatile Credenciales cache;

    public ArcaAuthService(ArcaProperties properties, SoapClient soapClient) {
        this.properties = properties;
        this.soapClient = soapClient;
    }

    public synchronized Credenciales getCredenciales() {
        if (cache != null && cache.vigente()) {
            return cache;
        }
        log.info("Solicitando nuevo ticket de acceso a WSAA ({})", properties.ambiente());
        cache = login();
        log.info("Ticket WSAA obtenido, valido hasta {}", cache.expiration());
        return cache;
    }

    private Credenciales login() {
        String tra = TraBuilder.build();
        String cms = CmsSigner.signBase64(tra, properties.certificado(), properties.clavePrivada());

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

        // loginCmsReturn trae un XML (loginTicketResponse) escapado como texto:
        // hay que extraerlo y parsearlo de nuevo.
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
}
