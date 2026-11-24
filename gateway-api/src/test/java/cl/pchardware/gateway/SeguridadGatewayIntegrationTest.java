package cl.pchardware.gateway;

import cl.pchardware.gateway.client.UsuarioClient;
import cl.pchardware.gateway.client.UsuarioClient.UsuarioSesion;
import cl.pchardware.gateway.proxy.ProxyService;
import cl.pchardware.gateway.security.JwtTokenProvider;
import cl.pchardware.gateway.security.UsuarioAutenticado;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.client.ResourceAccessException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Seguridad del gateway de punta a punta (filtro JWT + reglas por rol + flujo de sesión).
 * Los microservicios se simulan: aquí sólo se prueba quién puede llegar a ellos.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SeguridadGatewayIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private JwtTokenProvider jwt;
    @Autowired private ObjectMapper mapper;
    @MockBean private ProxyService proxy;
    @MockBean private UsuarioClient usuarioClient;

    @BeforeEach
    void simularMicroservicios() {
        when(proxy.get(anyString())).thenReturn(ResponseEntity.ok("[]"));
        when(proxy.forward(any(HttpMethod.class), anyString(), any())).thenReturn(ResponseEntity.ok("{}"));
    }

    private String token(String rol) {
        return jwt.generarToken(new UsuarioAutenticado(7L, rol.toLowerCase() + "@duoc.cl", rol, "Usuario " + rol));
    }

    private static UsuarioSesion sesion(long id, String rol, boolean activo) {
        UsuarioSesion u = new UsuarioSesion();
        u.setId(id);
        u.setEmail("user" + id + "@duoc.cl");
        u.setNombre("Usuario");
        u.setRol(rol);
        u.setActivo(activo);
        return u;
    }

    /** Matriz de permisos del Anexo 1 (ANONIMO = sin token). */
    @ParameterizedTest(name = "{2} {0} {1} -> {3}")
    @CsvSource({
            // Catálogo: lectura pública, escritura sólo ADMIN
            "GET,    /api/v1/productos,           ANONIMO,  200",
            "GET,    /api/v1/categorias/1,        ANONIMO,  200",
            "POST,   /api/v1/productos,           ANONIMO,  401",
            "POST,   /api/v1/productos,           CLIENTE,  403",
            "POST,   /api/v1/productos,           VENDEDOR, 403",
            "POST,   /api/v1/productos,           ADMIN,    200",
            "PUT,    /api/v1/categorias/1,        VENDEDOR, 403",
            "DELETE, /api/v1/categorias/1,        ADMIN,    200",
            // Usuarios: sólo ADMIN
            "GET,    /api/v1/usuarios,            ANONIMO,  401",
            "GET,    /api/v1/usuarios,            CLIENTE,  403",
            "GET,    /api/v1/usuarios,            VENDEDOR, 403",
            "GET,    /api/v1/usuarios,            ADMIN,    200",
            // Boletas: listado para el personal; el cliente sólo las suyas
            "GET,    /api/v1/boletas,             CLIENTE,  403",
            "GET,    /api/v1/boletas,             VENDEDOR, 200",
            "GET,    /api/v1/boletas/mis-boletas, CLIENTE,  200",
            "GET,    /api/v1/boletas/1,           CLIENTE,  200",
            "POST,   /api/v1/boletas,             ANONIMO,  401",
            "POST,   /api/v1/boletas,             CLIENTE,  200",
            "PUT,    /api/v1/boletas/1,           VENDEDOR, 403",
            "PUT,    /api/v1/boletas/1,           ADMIN,    200",
            "DELETE, /api/v1/boletas/1,           CLIENTE,  403",
            // Detalles: lectura personal, escritura ADMIN
            "GET,    /api/v1/detalles,            CLIENTE,  403",
            "GET,    /api/v1/detalles,            VENDEDOR, 200",
            "PUT,    /api/v1/detalles/1,          VENDEDOR, 403",
            "PUT,    /api/v1/detalles/1,          ADMIN,    200",
            // Cupones: validar con sesión; CRUD sólo ADMIN
            "GET,    /api/v1/cupones/validar/GAMER10, ANONIMO,  401",
            "GET,    /api/v1/cupones/validar/GAMER10, CLIENTE,  200",
            "GET,    /api/v1/cupones/validar/no%20valido, CLIENTE, 400",
            "GET,    /api/v1/cupones,             CLIENTE,  403",
            "GET,    /api/v1/cupones,             VENDEDOR, 403",
            "GET,    /api/v1/cupones,             ADMIN,    200",
            "POST,   /api/v1/cupones,             CLIENTE,  403",
            "DELETE, /api/v1/cupones/1,           ADMIN,    200",
            // Infraestructura
            "GET,    /h2-console,                 ANONIMO,  401",
            "GET,    /api/v1/no-existe,           ADMIN,    404",
    })
    void matrizDePermisos(String metodo, String ruta, String rol, int esperado) throws Exception {
        MockHttpServletRequestBuilder req = request(HttpMethod.valueOf(metodo), ruta)
                .contentType(MediaType.APPLICATION_JSON).content("{}");
        if (!"ANONIMO".equals(rol)) req.header("Authorization", "Bearer " + token(rol));
        mvc.perform(req).andExpect(status().is(esperado));
    }

    @Test
    @DisplayName("Token inválido o forjado -> 401 con mensaje JSON")
    void tokenInvalido() throws Exception {
        mvc.perform(get("/api/v1/usuarios").header("Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.e30.firma-falsa"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("Login: 200 con access + refresh; clave incorrecta 401; cuenta desactivada 403")
    void login() throws Exception {
        when(usuarioClient.verificarCredenciales("admin@duoc.cl", "admin")).thenReturn(sesion(1, "ADMIN", true));
        when(usuarioClient.verificarCredenciales("admin@duoc.cl", "mala")).thenThrow(new BadCredentialsException("Credenciales incorrectas."));
        when(usuarioClient.verificarCredenciales("baja@duoc.cl", "x")).thenThrow(new DisabledException("La cuenta está desactivada."));

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ADMIN@duoc.cl\",\"password\":\"admin\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.refreshToken").isString())
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.rol").value("ADMIN"));
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@duoc.cl\",\"password\":\"mala\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").value("Credenciales incorrectas."));
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"baja@duoc.cl\",\"password\":\"x\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Refresh rota el token; reusar el anterior -> 401; logout lo invalida")
    void cicloDeSesion() throws Exception {
        when(usuarioClient.verificarCredenciales(eq("cliente@duoc.cl"), anyString())).thenReturn(sesion(5, "CLIENTE", true));
        when(usuarioClient.buscarPorId(5L)).thenReturn(sesion(5, "CLIENTE", true));

        String refresh1 = campo(mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"cliente@duoc.cl\",\"password\":\"1234\"}")).andReturn().getResponse().getContentAsString(), "refreshToken");

        String respuesta = mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refresh1 + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.token").isString())
                .andReturn().getResponse().getContentAsString();
        String refresh2 = campo(respuesta, "refreshToken");

        mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refresh1 + "\"}"))
                .andExpect(status().isUnauthorized());
        // La reutilización revocó todas las sesiones del usuario, incluida la nueva
        mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refresh2 + "\"}"))
                .andExpect(status().isUnauthorized());

        String refresh3 = campo(mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"cliente@duoc.cl\",\"password\":\"1234\"}")).andReturn().getResponse().getContentAsString(), "refreshToken");
        mvc.perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refresh3 + "\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refresh3 + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Refresh de un usuario desactivado después del login -> 403")
    void refreshUsuarioDesactivado() throws Exception {
        when(usuarioClient.verificarCredenciales(eq("v@duoc.cl"), anyString())).thenReturn(sesion(8, "VENDEDOR", true));
        when(usuarioClient.buscarPorId(8L)).thenReturn(sesion(8, "VENDEDOR", false));
        String refresh = campo(mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"v@duoc.cl\",\"password\":\"x\"}")).andReturn().getResponse().getContentAsString(), "refreshToken");
        mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refresh + "\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Validación de login (email inválido) -> 400 con detalle")
    void loginInvalido() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"x\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.email").exists());
    }

    @Test
    @DisplayName("Microservicio caído -> 503 sin exponer detalles internos")
    void servicioCaido() throws Exception {
        when(proxy.get(anyString())).thenThrow(new ResourceAccessException("I/O error on GET request for http://interno:8083"));
        mvc.perform(get("/api/v1/productos"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("Servicio no disponible temporalmente. Intente más tarde."));
    }

    private String campo(String json, String nombre) throws Exception {
        JsonNode n = mapper.readTree(json);
        return n.get(nombre).asText();
    }
}
