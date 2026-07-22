package com.bridge.facturacion.auth;

import com.bridge.facturacion.auth.dto.LoginRequestDTO;
import com.bridge.facturacion.auth.dto.UsuarioResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    public UsuarioResponseDTO login(LoginRequestDTO dto,
                                    HttpServletRequest request,
                                    HttpServletResponse response) {

        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(dto.email(), dto.password()));

        // Desde Security 6, al autenticar a mano hay que guardar el contexto en la
        // sesion explicitamente. Sin saveContext(...) el login devuelve 200 pero el
        // siguiente pedido da 401 porque la sesion nunca se guardo.
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        return new UsuarioResponseDTO(authentication.getName());
    }
}
