package com.bridge.facturacion.factura.exception;

import com.bridge.facturacion.alumno.Alumno;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class FacturaDuplicadaException extends RuntimeException {
    public FacturaDuplicadaException(Alumno alumno, LocalDate periodo) {
        super("Ya existe una factura para el alumno "
                + alumno.getNombre()
                + " (DNI " + alumno.getDni() + ")"
                + " en el período " + periodo.format(DateTimeFormatter.ofPattern("MM/yyyy")));
    }
}
