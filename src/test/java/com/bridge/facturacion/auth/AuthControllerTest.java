package com.bridge.facturacion.auth;

import com.bridge.facturacion.usuario.Usuario;
import com.bridge.facturacion.usuario.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
// Boot 4 modularizado: la anotación se mudó de
// org.springframework.boot.test.autoconfigure.web.servlet (Boot 3)
// al módulo propio de webmvc-test.
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Boot 4: @SpringBootTest ya no trae MockMvc solo; hace falta @AutoConfigureMockMvc.
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    private static final String EMAIL = "operador@test.local";
    private static final String PASSWORD = "clave-de-prueba";

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private ObjectMapper objectMapper;

    @BeforeEach
    void crearOperadorDePrueba() {
        if (!usuarioRepository.existsByEmail(EMAIL)) {
            usuarioRepository.save(Usuario.of(EMAIL, passwordEncoder.encode(PASSWORD)));
        }
    }

    private String loginJson(String email, String password) {
        return """
                {"email":"%s","password":"%s"}""".formatted(email, password);
    }

    @Test
    void loginConCredencialesCorrectas_devuelve200YGuardaLaSesion() throws Exception {
        mockMvc.perform(post("/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL))
                // MockMvc no emite la cookie JSESSIONID (eso lo hace el servidor real);
                // la prueba equivalente es que el contexto quedo guardado en la sesion.
                // Esto valida ademas el saveContext(...) de AuthService.
                .andExpect(request().sessionAttribute(
                        HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                        notNullValue()));
    }

    @Test
    void loginConPasswordIncorrecta_devuelve401() throws Exception {
        mockMvc.perform(post("/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(EMAIL, "password-incorrecta")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenciales inválidas"));
    }

    @Test
    void loginConEmailInexistente_devuelveExactamenteElMismoCuerpoQuePasswordIncorrecta() throws Exception {
        String cuerpoPasswordMal = mockMvc.perform(post("/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(EMAIL, "password-incorrecta")))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        String cuerpoEmailInexistente = mockMvc.perform(post("/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("noexiste@test.local", "cualquiera")))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        // Si estos cuerpos difieren en algo (salvo el timestamp), hay enumeracion
        // de usuarios: un atacante podria descubrir que emails existen.
        assertEquals(sinTimestamp(cuerpoPasswordMal), sinTimestamp(cuerpoEmailInexistente));
    }

    private ObjectNode sinTimestamp(String json) throws Exception {
        ObjectNode node = (ObjectNode) objectMapper.readTree(json);
        node.remove("timestamp");
        return node;
    }

    @Test
    void loginConEmailMalFormado_devuelve400() throws Exception {
        mockMvc.perform(post("/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("esto-no-es-un-email", PASSWORD)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void alumnosSinAutenticar_devuelve401() throws Exception {
        mockMvc.perform(get("/alumnos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = EMAIL)
    void meAutenticado_devuelve200ConElEmail() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL));
    }
}
