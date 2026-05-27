package com.bridge.facturacion.alumno.exception;

public class AlumnoDuplicadoException extends RuntimeException {
    public AlumnoDuplicadoException(String dni) {
        super("Alumno ya creado: " + dni);
    }
}
