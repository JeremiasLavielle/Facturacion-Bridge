package com.bridge.facturacion.notacredito;

import com.bridge.facturacion.factura.Factura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotaCreditoRepository extends JpaRepository<NotaCredito, Long> {
    Optional<NotaCredito> findByFactura(Factura factura);
}
