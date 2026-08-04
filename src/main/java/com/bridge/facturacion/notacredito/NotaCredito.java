package com.bridge.facturacion.notacredito;

import com.bridge.facturacion.emisor.Emisor;
import com.bridge.facturacion.factura.Factura;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Nota de Credito C (tipo 13): la UNICA correccion valida de una factura
 * con CAE. Anulacion TOTAL (mismo monto), una sola por factura, del mismo
 * emisor que la factura original. Tiene numeracion propia en ARCA.
 */
@Entity
@Table(name = "notas_credito")
@Getter
@NoArgsConstructor
public class NotaCredito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "factura_id", nullable = false, unique = true)
    private Factura factura;

    @ManyToOne
    @JoinColumn(name = "emisor_id", nullable = false)
    private Emisor emisor;

    @Column(nullable = false)
    private BigDecimal monto;

    @Column(nullable = false)
    private String motivo;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EstadoNotaCredito estado;

    private LocalDateTime fechaEmision;
    private String cae;
    private LocalDate vencimientoCae;
    private Long numeroComprobante;
    private String mensajeError;

    /** El emisor y el monto salen SIEMPRE de la factura original. */
    public static NotaCredito pendiente(Factura factura, String motivo) {
        NotaCredito nc = new NotaCredito();
        nc.factura = factura;
        nc.emisor = factura.getEmisor();
        nc.monto = factura.getMonto();
        nc.motivo = motivo;
        nc.estado = EstadoNotaCredito.PENDIENTE;
        return nc;
    }

    public void marcarEmitida(String cae, LocalDate vencimientoCae, Long numeroComprobante) {
        this.estado = EstadoNotaCredito.EMITIDA;
        this.cae = cae;
        this.vencimientoCae = vencimientoCae;
        this.numeroComprobante = numeroComprobante;
        this.fechaEmision = LocalDateTime.now();
        this.mensajeError = null;
    }

    public void marcarError(String mensaje) {
        this.estado = EstadoNotaCredito.ERROR;
        this.mensajeError = mensaje;
    }
}
