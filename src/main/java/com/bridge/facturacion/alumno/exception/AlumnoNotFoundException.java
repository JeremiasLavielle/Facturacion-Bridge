package com.bridge.facturacion.alumno.exception;

public class AlumnoNotFoundException extends RuntimeException {
    public AlumnoNotFoundException(Long id) {
        super("Alumno no encontrado: " + id);
    }
}
