package com.bridge.facturacion.alumno.dto;

import com.bridge.facturacion.alumno.CondicionIva;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AlumnoRequestDTO {
    @NotBlank @Size(min = 4, max = 100)
    private String nombre;
    @NotBlank @Pattern(regexp = "\\d{7,8}") // obliga a que dni sean entre 7 y 8 dígitos
    private String dni;
    @NotNull
    private CondicionIva condicionIva;
}
