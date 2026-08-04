package com.bridge.facturacion.factura;

import com.bridge.facturacion.alumno.Alumno;
import com.bridge.facturacion.emisor.Emisor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "facturas")
@Getter
@NoArgsConstructor
public class Factura {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "alumno_id", nullable = false)
    private Alumno alumno;
    @ManyToOne
    @JoinColumn(name = "emisor_id", nullable = false)
    private Emisor emisor;
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
    private Long numeroComprobante;
    private String mensajeError;

    public static Factura pendiente(Alumno alumno, Emisor emisor, BigDecimal monto, LocalDate periodo) {
        Factura factura = new Factura();
        factura.alumno = alumno;
        factura.emisor = emisor;
        factura.monto = monto;
        factura.periodo = periodo;
        factura.estado = EstadoFactura.PENDIENTE;
        return factura;
    }

    public void marcarEmitida(String cae, LocalDate vencimientoCae, Long numeroComprobante) {
        this.estado = EstadoFactura.EMITIDA;
        this.cae = cae;
        this.vencimientoCae = vencimientoCae;
        this.numeroComprobante = numeroComprobante;
        this.fechaEmision = LocalDateTime.now();
        this.mensajeError = null;
    }

    public void marcarError(String mensaje) {
        this.estado = EstadoFactura.ERROR;
        this.mensajeError = mensaje;
    }

    /** La anula su nota de credito: conserva CAE y numero (el comprobante existe). */
    public void marcarAnulada() {
        this.estado = EstadoFactura.ANULADA;
    }
}
