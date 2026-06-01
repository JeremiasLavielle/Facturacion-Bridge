package com.bridge.facturacion.factura;

import com.bridge.facturacion.alumno.Alumno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface FacturaRepository extends JpaRepository<Factura, Long> {
    boolean existsByAlumnoAndPeriodo(Alumno alumno, LocalDate periodo);
    List<Factura> findByPeriodo(LocalDate periodo);
    List<Factura> findByAlumno(Alumno alumno);
}
