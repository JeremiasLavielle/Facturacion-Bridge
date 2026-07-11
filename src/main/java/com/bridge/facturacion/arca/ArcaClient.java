package com.bridge.facturacion.arca;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Cliente de WSFEv1. Emite Factura C (comprobante tipo 11, el unico que
 * emite un Monotributista) por servicios (concepto 2).
 *
 * Secuencia de emision:
 *   1. FECompUltimoAutorizado -> ultimo numero autorizado en el punto de venta
 *   2. FECAESolicitar con numero = ultimo + 1
 *   3. Parsear CAE / rechazo
 *
 * ARCA es la fuente de verdad de la numeracion: pedirla en cada emision
 * evita duplicados y huecos aunque la app se reinicie.
 */
@Service
public class ArcaClient {

    private static final Logger log = LoggerFactory.getLogger(ArcaClient.class);
    private static final DateTimeFormatter FECHA_ARCA = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int FACTURA_C = 11;
    private static final int CONCEPTO_SERVICIOS = 2;
    static final String NS = "http://ar.gov.afip.dif.FEV1/";

    private final ArcaProperties properties;
    private final ArcaAuthService authService;
    private final SoapClient soapClient;

    public ArcaClient(ArcaProperties properties, ArcaAuthService authService, SoapClient soapClient) {
        this.properties = properties;
        this.authService = authService;
        this.soapClient = soapClient;
    }

    /** Ultimo numero de comprobante autorizado (sirve tambien como "ping" de conectividad). */
    public long ultimoComprobanteAutorizado() {
        String body = """
                <ar:FECompUltimoAutorizado>
                    %s
                    <ar:PtoVta>%d</ar:PtoVta>
                    <ar:CbteTipo>%d</ar:CbteTipo>
                </ar:FECompUltimoAutorizado>
                """.formatted(authXml(), properties.puntoVenta(), FACTURA_C);

        Document doc = call("FECompUltimoAutorizado", body);
        throwIfErrors(doc);
        String nro = soapClient.firstText(doc, "CbteNro");
        if (nro == null) {
            throw new ArcaException("WSFE no devolvio CbteNro");
        }
        return Long.parseLong(nro);
    }

    /**
     * Solicita el CAE de una Factura C por servicios.
     *
     * @param docTipo              80=CUIT, 96=DNI, 99=sin identificar (consumidor final)
     * @param docNro               numero de documento (0 si docTipo=99)
     * @param importe              importe total (Factura C: total == neto, sin IVA discriminado)
     * @param periodo              mes facturado (se usa como rango de servicio)
     * @param condicionIvaReceptor codigo ARCA de la condicion IVA del receptor
     */
    public ResultadoEmision solicitarCae(int docTipo, long docNro, BigDecimal importe,
                                         LocalDate periodo, int condicionIvaReceptor) {

        long numero = ultimoComprobanteAutorizado() + 1;
        LocalDate hoy = LocalDate.now();
        LocalDate desde = periodo.withDayOfMonth(1);
        LocalDate hasta = periodo.withDayOfMonth(periodo.lengthOfMonth());
        String monto = importe.setScale(2, RoundingMode.HALF_UP).toPlainString();

        log.info("Solicitando CAE: cbte {} pv {} doc {}/{} importe {}",
                numero, properties.puntoVenta(), docTipo, docNro, monto);

        String body = """
                <ar:FECAESolicitar>
                    %s
                    <ar:FeCAEReq>
                        <ar:FeCabReq>
                            <ar:CantReg>1</ar:CantReg>
                            <ar:PtoVta>%d</ar:PtoVta>
                            <ar:CbteTipo>%d</ar:CbteTipo>
                        </ar:FeCabReq>
                        <ar:FeDetReq>
                            <ar:FECAEDetRequest>
                                <ar:Concepto>%d</ar:Concepto>
                                <ar:DocTipo>%d</ar:DocTipo>
                                <ar:DocNro>%d</ar:DocNro>
                                <ar:CbteDesde>%d</ar:CbteDesde>
                                <ar:CbteHasta>%d</ar:CbteHasta>
                                <ar:CbteFch>%s</ar:CbteFch>
                                <ar:ImpTotal>%s</ar:ImpTotal>
                                <ar:ImpTotConc>0</ar:ImpTotConc>
                                <ar:ImpNeto>%s</ar:ImpNeto>
                                <ar:ImpOpEx>0</ar:ImpOpEx>
                                <ar:ImpTrib>0</ar:ImpTrib>
                                <ar:ImpIVA>0</ar:ImpIVA>
                                <ar:FchServDesde>%s</ar:FchServDesde>
                                <ar:FchServHasta>%s</ar:FchServHasta>
                                <ar:FchVtoPago>%s</ar:FchVtoPago>
                                <ar:MonId>PES</ar:MonId>
                                <ar:MonCotiz>1</ar:MonCotiz>
                                <ar:CondicionIVAReceptorId>%d</ar:CondicionIVAReceptorId>
                            </ar:FECAEDetRequest>
                        </ar:FeDetReq>
                    </ar:FeCAEReq>
                </ar:FECAESolicitar>
                """.formatted(authXml(), properties.puntoVenta(), FACTURA_C,
                CONCEPTO_SERVICIOS, docTipo, docNro, numero, numero,
                FECHA_ARCA.format(hoy), monto, monto,
                FECHA_ARCA.format(desde), FECHA_ARCA.format(hasta), FECHA_ARCA.format(hoy),
                condicionIvaReceptor);

        Document doc = call("FECAESolicitar", body);
        throwIfErrors(doc);
        return parseResultado(doc, numero);
    }

    // ---------- helpers ----------

    private String authXml() {
        Credenciales cred = authService.getCredenciales();
        return """
                <ar:Auth>
                    <ar:Token>%s</ar:Token>
                    <ar:Sign>%s</ar:Sign>
                    <ar:Cuit>%s</ar:Cuit>
                </ar:Auth>""".formatted(cred.token(), cred.sign(), properties.cuitEmisor());
    }

    private Document call(String metodo, String body) {
        String envelope = """
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                                  xmlns:ar="%s">
                    <soapenv:Body>
                        %s
                    </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(NS, body);
        return soapClient.post(properties.urlWsfe(), NS + metodo, envelope);
    }

    /** Errores estructurales (auth vencida, punto de venta invalido, etc.): bloque Errors. */
    private void throwIfErrors(Document doc) {
        List<String> errores = codigosYMensajes(doc, "Err");
        if (!errores.isEmpty()) {
            throw new ArcaException("WSFE devolvio errores: " + String.join(" | ", errores));
        }
    }

    private ResultadoEmision parseResultado(Document doc, long numero) {
        // Resultado: A (aprobada) o R (rechazada)
        String resultado = soapClient.firstText(doc, "Resultado");
        List<String> observaciones = codigosYMensajes(doc, "Obs");

        if ("A".equals(resultado)) {
            String cae = soapClient.firstText(doc, "CAE");
            String vto = soapClient.firstText(doc, "CAEFchVto");
            if (cae == null || cae.isBlank() || vto == null) {
                throw new ArcaException("ARCA aprobo pero no devolvio CAE/vencimiento");
            }
            return new ResultadoEmision(true, numero, cae,
                    LocalDate.parse(vto, FECHA_ARCA), observaciones);
        }
        return new ResultadoEmision(false, numero, null, null,
                observaciones.isEmpty() ? List.of("Rechazada sin observaciones") : observaciones);
    }

    /** Junta pares Code+Msg de los bloques Err u Obs. */
    private List<String> codigosYMensajes(Document doc, String tag) {
        List<String> result = new ArrayList<>();
        NodeList nodes = doc.getElementsByTagNameNS("*", tag);
        for (int i = 0; i < nodes.getLength(); i++) {
            Element e = (Element) nodes.item(i);
            result.add("[" + textOf(e, "Code") + "] " + textOf(e, "Msg"));
        }
        return result;
    }

    private String textOf(Element parent, String localName) {
        NodeList nodes = parent.getElementsByTagNameNS("*", localName);
        return nodes.getLength() > 0 ? nodes.item(0).getTextContent() : "";
    }
}
