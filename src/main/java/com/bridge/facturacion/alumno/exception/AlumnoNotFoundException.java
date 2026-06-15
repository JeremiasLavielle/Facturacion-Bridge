package com.bridge.facturacion.alumno.exception;

import com.bridge.facturacion.common.exception.ResourceNotFoundException;

public class AlumnoNotFoundException extends ResourceNotFoundException {
    public AlumnoNotFoundException(Long id) {
        super("Alumno no encontrado: " + id);
    }
}
