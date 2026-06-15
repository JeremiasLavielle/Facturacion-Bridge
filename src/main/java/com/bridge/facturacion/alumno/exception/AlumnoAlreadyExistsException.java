package com.bridge.facturacion.alumno.exception;

import com.bridge.facturacion.common.exception.DuplicateResourceException;

public class AlumnoAlreadyExistsException extends DuplicateResourceException {
    public AlumnoAlreadyExistsException(String dni) {
        super("Alumno ya creado: " + dni);
    }
}
