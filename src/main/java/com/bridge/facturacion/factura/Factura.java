package com.bridge.facturacion.factura;

import com.bridge.facturacion.alumno.Alumno;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "facturas")
@Getter@Setter
@NoArgsConstructor

public class Factura {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "alumno_id", nullable = false)
    private Alumno alumno;
    @Column(nullable = false)
    private BigDecimal monto;
    @Column(nullable = false)
    private LocalDate periodo;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EstadoFactura estado;
    private LocalDateTime fechaEmision;
    private String cae;
    private LocalDate vencimientoCae;
    private String mensajeError;
}
