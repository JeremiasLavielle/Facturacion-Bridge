package com.bridge.facturacion.auth;

import com.bridge.facturacion.auth.dto.LoginRequestDTO;
import com.bridge.facturacion.auth.dto.UsuarioResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<UsuarioResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto,
                                                    HttpServletRequest request,
                                                    HttpServletResponse response) {
        return ResponseEntity.ok(authService.login(dto, request, response));
    }

    @GetMapping("/me")
    public ResponseEntity<UsuarioResponseDTO> me(Authentication authentication) {
        return ResponseEntity.ok(new UsuarioResponseDTO(authentication.getName()));
    }
}
