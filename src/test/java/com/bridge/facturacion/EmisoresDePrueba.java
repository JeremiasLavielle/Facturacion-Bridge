package com.bridge.facturacion;

import com.bridge.facturacion.emisor.Emisor;

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
