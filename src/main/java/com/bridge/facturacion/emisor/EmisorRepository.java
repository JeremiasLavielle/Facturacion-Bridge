package com.bridge.facturacion.emisor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmisorRepository extends JpaRepository<Emisor, Long> {
    List<Emisor> findByActivoTrueOrderById();
}
