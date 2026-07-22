package com.bridge.facturacion.usuario;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "usuarios")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    private Usuario(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public static Usuario of(String email, String passwordHasheada) {
        return new Usuario(email, passwordHasheada);
    }
}
