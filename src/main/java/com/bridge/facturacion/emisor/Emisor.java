package com.bridge.facturacion.emisor;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Un emisor de comprobantes: CUIT + punto de venta + certificado ARCA +
 * datos fiscales que van en la cabecera/pie del PDF.
 *
 * <p>Los paths de certificado y clave son RELATIVOS a {@code arca.certs-dir}
 * (ej. "20463447277/certificado.crt"), asi la misma fila sirve en dev,
 * en el contenedor y en los tests.</p>
 */
@Entity
@Table(name = "emisores")
@Getter
@Setter
@NoArgsConstructor
public class Emisor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String cuit;

    @Column(nullable = false)
    private String razonSocial;

    @Column(nullable = false)
    private String nombreFantasia;

    @Column(nullable = false)
    private String domicilio;

    @Column(nullable = false)
    private String condicionFiscal;

    @Column(nullable = false)
    private String ingresosBrutos;

    @Column(nullable = false)
    private String inicioActividades;

    @Column(nullable = false)
    private int puntoVenta;

    @Column(nullable = false)
    private String certPath;

    @Column(nullable = false)
    private String keyPath;

    @Column(nullable = false)
    private boolean activo = true;
}
