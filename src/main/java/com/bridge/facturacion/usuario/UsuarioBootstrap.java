package com.bridge.facturacion.usuario;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class UsuarioBootstrap implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.operador.email:}")
    private String email;

    @Value("${app.operador.password:}")
    private String password;

    @Override
    @Transactional
    public void run(String... args) {
        if (email.isBlank() || password.isBlank()) {
            log.warn("Operador no configurado (app.operador.email / app.operador.password). No se crea usuario.");
            return;
        }
        if (usuarioRepository.existsByEmail(email)) {
            log.info("Operador '{}' ya existe. No se crea.", email);
            return;
        }
        usuarioRepository.save(Usuario.of(email, passwordEncoder.encode(password)));
        log.info("Operador '{}' creado.", email);
    }
}
