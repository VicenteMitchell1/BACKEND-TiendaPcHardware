package cl.pchardware.categoria;

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

@SpringBootTest
@AutoConfigureMockMvc
class CategoriaApiIntegrationTest {

    private static final String KEY = "X-Internal-Token";
    private static final String VALOR = "test-internal-key";

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("Sin clave interna -> 401")
    void sinClave() throws Exception {
        mvc.perform(get("/categorias")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Con clave interna se listan las 6 categorías semilla")
    void listar() throws Exception {
        mvc.perform(get("/categorias").header(KEY, VALOR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6));
    }

    @Test
    @DisplayName("Nombre vacío -> 400 con detalle del campo")
    void validacion() throws Exception {
        mvc.perform(post("/categorias").header(KEY, VALOR).contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.nombre").value("El nombre de la categoría es obligatorio"));
    }

    @Test
    @DisplayName("Nombre duplicado -> 409")
    void duplicado() throws Exception {
        mvc.perform(post("/categorias").header(KEY, VALOR).contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"procesadores\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Categoría inexistente -> 404")
    void noEncontrada() throws Exception {
        mvc.perform(get("/categorias/9999").header(KEY, VALOR)).andExpect(status().isNotFound());
    }
}
