package com.bridge.facturacion.alumno;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "alumnos")
@Getter@Setter
@NoArgsConstructor

public class Alumno {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long  id;
    private String nombre;
    private String dni;
    @Enumerated(EnumType.STRING)
    private CondicionIva condicionIva;
}
