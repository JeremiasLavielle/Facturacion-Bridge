package com.bridge.facturacion.emisor;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
