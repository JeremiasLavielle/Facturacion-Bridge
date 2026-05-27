package com.bridge.facturacion.alumno.dto;

import com.bridge.facturacion.alumno.CondicionIva;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AlumnoResponseDTO {
    private Long id;
    private String nombre;
    private String dni;
    private CondicionIva condicionIva;
}
