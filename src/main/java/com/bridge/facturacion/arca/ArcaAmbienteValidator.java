package com.bridge.facturacion.arca;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Locale;

/**
 * Se niega a arrancar si {@code arca.ambiente} no coincide con las URLs
 * configuradas.
 *
 * <p>Antes de esto, {@code arca.ambiente} era decorativo: solo aparecía en una
 * línea de log. Lo que decide realmente contra qué servidor se factura son
 * {@code arca.url-wsaa} y {@code arca.url-wsfe}. Eso permitía el peor error
 * posible: dejar el ambiente en HOMOLOGACION, apuntar las URLs a producción, y
 * emitir comprobantes fiscales reales mientras el log dice "HOMOLOGACION".
 *
 * <p>Un comprobante emitido en producción no se puede borrar: solo se anula con
 * una nota de crédito, y ambos quedan en los registros de ARCA para siempre.
 * Por eso acá se falla al arrancar y no se avisa por log.
 */
@Component
public class ArcaAmbienteValidator {

    private static final Logger log = LoggerFactory.getLogger(ArcaAmbienteValidator.class);

    /**
     * La validación corre en el constructor a propósito: si falla, el bean no
     * se crea y Spring aborta el arranque. No hay forma de que la aplicación
     * quede en pie con una configuración incoherente.
     */
    public ArcaAmbienteValidator(ArcaProperties properties) {
        validar(properties);
    }

    static void validar(ArcaProperties properties) {
        Ambiente declarado = properties.ambiente();

        if (declarado == null) {
            throw new IllegalStateException(
                    "arca.ambiente no esta configurado. Defini ARCA_AMBIENTE "
                            + "en HOMOLOGACION o PRODUCCION.");
        }

        verificar("arca.url-wsaa", properties.urlWsaa(), declarado);
        verificar("arca.url-wsfe", properties.urlWsfe(), declarado);

        if (declarado == Ambiente.PRODUCCION) {
            log.warn("ARCA en PRODUCCION: cada comprobante emitido es fiscal, real "
                    + "y solo se puede anular con nota de credito");
        } else {
            log.info("ARCA en HOMOLOGACION: los comprobantes no tienen validez fiscal");
        }
    }

    private static void verificar(String propiedad, String url, Ambiente declarado) {
        Ambiente segunUrl = ambienteSegunUrl(url);

        if (segunUrl == null) {
            log.warn("{} apunta a un host local ({}): no se verifica el ambiente", propiedad, url);
            return;
        }

        if (segunUrl != declarado) {
            throw new IllegalStateException("""
                    Configuracion de ARCA incoherente.
                      %s = %s  -> parece %s
                      arca.ambiente          -> declara %s
                    Revisa ARCA_AMBIENTE, ARCA_URL_WSAA y ARCA_URL_WSFE en el .env.
                      Homologacion: https://wsaahomo.afip.gov.ar/ws/services/LoginCms
                                    https://wswhomo.afip.gov.ar/wsfev1/service.asmx
                      Produccion:   https://wsaa.afip.gov.ar/ws/services/LoginCms
                                    https://servicios1.afip.gov.ar/wsfev1/service.asmx"""
                    .formatted(propiedad, url, segunUrl, declarado));
        }
    }

    /**
     * Deduce el ambiente a partir del host de la URL.
     *
     * @return {@code null} si es un host local (tests con una ARCA falsa), donde
     * no hay nada que verificar.
     */
    static Ambiente ambienteSegunUrl(String url) {
        String host;
        try {
            host = URI.create(url.trim()).getHost();
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("URL de ARCA mal formada: " + url, e);
        }

        if (host == null) {
            throw new IllegalStateException("URL de ARCA sin host: " + url);
        }

        host = host.toLowerCase(Locale.ROOT);

        if (host.equals("localhost") || host.equals("127.0.0.1")
                || host.startsWith("host.docker")) {
            return null;
        }

        // Los endpoints de homologacion de ARCA llevan "homo" en el host
        // (wsaahomo.afip.gov.ar, wswhomo.afip.gov.ar). Cualquier otro host se
        // trata como PRODUCCION a proposito: si el dia de manana cambia el
        // dominio, el error se inclina a bloquear el arranque en vez de dejar
        // pasar comprobantes reales por accidente.
        return host.contains("homo") ? Ambiente.HOMOLOGACION : Ambiente.PRODUCCION;
    }
}
