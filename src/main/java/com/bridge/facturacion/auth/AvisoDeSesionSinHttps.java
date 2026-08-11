package com.bridge.facturacion.auth;

import com.bridge.facturacion.arca.Ambiente;
import com.bridge.facturacion.arca.ArcaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Avisa por log cuando la cookie de sesion puede viajar sin cifrar.
 *
 * <p>{@code COOKIE_SEGURA=false} existe para una sola situacion: la etapa
 * transitoria en la que el sistema se sirve por IP y todavia no hay dominio ni
 * certificado. Con {@code secure=true} el navegador descarta la cookie en HTTP y
 * el login "entra y vuelve a salir" enseguida.
 *
 * <p>El problema de los arreglos transitorios es que se vuelven permanentes.
 * Este aviso esta para que no pase inadvertido: en PRODUCCION significa que la
 * contrasena de quien factura viaja en texto plano.
 */
@Component
public class AvisoDeSesionSinHttps {

    private static final Logger log = LoggerFactory.getLogger(AvisoDeSesionSinHttps.class);

    public AvisoDeSesionSinHttps(
            ArcaProperties properties,
            @Value("${server.servlet.session.cookie.secure:true}") boolean cookieSegura) {

        if (cookieSegura) {
            return;
        }

        if (properties.ambiente() == Ambiente.PRODUCCION) {
            log.warn("""
                    COOKIE_SEGURA=false EN PRODUCCION: la sesion y la contrasena viajan sin
                    cifrar. Solo es aceptable mientras no haya dominio ni HTTPS. Apenas Caddy
                    tenga el certificado, volver a COOKIE_SEGURA=true y reiniciar.""");
        } else {
            log.warn("COOKIE_SEGURA=false: la cookie de sesion viaja sin cifrar");
        }
    }
}
