package com.bridge.facturacion;

import com.bridge.facturacion.emisor.Emisor;

/**
 * Fabrica emisores de prueba con datos completos. Los paths de
 * certificado siguen la convencion real: {@code <cuit>/certificado.crt}
 * relativos a {@code arca.certs-dir}.
 */
public final class EmisoresDePrueba {

    private EmisoresDePrueba() {}

    public static Emisor emisor(Long id, String cuit, int puntoVenta) {
        Emisor emisor = new Emisor();
        emisor.setId(id);
        emisor.setCuit(cuit);
        emisor.setRazonSocial("Titular " + cuit);
        emisor.setNombreFantasia("Instituto Bridge");
        emisor.setDomicilio("Calle 1 - La Plata");
        emisor.setCondicionFiscal("Responsable Monotributo");
        emisor.setIngresosBrutos("Exento");
        emisor.setInicioActividades("01/01/2020");
        emisor.setPuntoVenta(puntoVenta);
        emisor.setCertPath(cuit + "/certificado.crt");
        emisor.setKeyPath(cuit + "/clave-privada.key");
        emisor.setActivo(true);
        return emisor;
    }
}
