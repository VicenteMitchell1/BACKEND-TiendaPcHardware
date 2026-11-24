package cl.pchardware.usuario;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Pruebas de integración del API con la base H2 y los datos semilla. */
@SpringBootTest
@AutoConfigureMockMvc
class UsuarioApiIntegrationTest {

    private static final String KEY = "X-Internal-Token";
    private static final String VALOR = "test-internal-key";

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("Sin clave interna el servicio responde 401 (no se puede usar saltándose el gateway)")
    void sinClaveInterna() throws Exception {
        mvc.perform(get("/usuarios")).andExpect(status().isUnauthorized());
        mvc.perform(get("/interno/usuarios/email/admin@duoc.cl").header(KEY, "clave-falsa")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Swagger del servicio es accesible sin clave")
    void swaggerPublico() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Los endpoints internos nunca devuelven el hash de la contraseña")
    void sinHashEnRespuestas() throws Exception {
        mvc.perform(get("/interno/usuarios/email/admin@duoc.cl").header(KEY, VALOR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("ADMIN"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("Verificar credenciales: 200 válidas / 401 incorrectas")
    void verificarCredenciales() throws Exception {
        mvc.perform(post("/interno/auth/verificar").header(KEY, VALOR).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"vendedor@duoc.cl\",\"password\":\"vendedor\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.rol").value("VENDEDOR"));
        mvc.perform(post("/interno/auth/verificar").header(KEY, VALOR).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"vendedor@duoc.cl\",\"password\":\"mala\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").value("Credenciales incorrectas."));
    }

    @Test
    @DisplayName("Validaciones devuelven 400 con el detalle por campo (antes 500)")
    void validacion400() throws Exception {
        mvc.perform(post("/usuarios").header(KEY, VALOR).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"no-es-email\",\"nombre\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.email").exists())
                .andExpect(jsonPath("$.detalles.nombre").exists());
    }

    @Test
    @DisplayName("JSON mal formado -> 400; ID no numérico -> 400; ruta inexistente -> 404")
    void erroresDelCliente() throws Exception {
        mvc.perform(post("/usuarios").header(KEY, VALOR).contentType(MediaType.APPLICATION_JSON).content("{malo"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/usuarios/abc").header(KEY, VALOR)).andExpect(status().isBadRequest());
        mvc.perform(get("/no-existe").header(KEY, VALOR)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Registro con email duplicado -> 409")
    void registroDuplicado() throws Exception {
        mvc.perform(post("/interno/usuarios/register").header(KEY, VALOR).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@duoc.cl\",\"password\":\"1234\",\"nombre\":\"X\"}"))
                .andExpect(status().isConflict());
    }
}
