package com.bridge.facturacion.factura.exception;

import com.bridge.facturacion.alumno.Alumno;
import com.bridge.facturacion.common.exception.DuplicateResourceException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class FacturaAlreadyExistsException extends DuplicateResourceException {
    public FacturaAlreadyExistsException(Alumno alumno, LocalDate periodo) {
        super("Ya existe una factura para el alumno "
                + alumno.getNombre()
                + " (DNI " + alumno.getDni() + ")"
                + " en el período " + periodo.format(DateTimeFormatter.ofPattern("MM/yyyy")));
    }
}
