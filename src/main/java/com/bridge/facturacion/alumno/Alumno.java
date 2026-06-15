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

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false, unique = true)
    private String dni;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private CondicionIva condicionIva;
}
