package cl.pchardware.boleta;

import cl.pchardware.boleta.client.DetalleClient;
import cl.pchardware.boleta.client.ProductoClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Integración del controlador con la boleta semilla (id 1, dueño: usuario 4). */
@SpringBootTest
@AutoConfigureMockMvc
class BoletaApiIntegrationTest {

    private static final String KEY = "X-Internal-Token";
    private static final String VALOR = "test-internal-key";

    @Autowired private MockMvc mvc;
    @MockBean private ProductoClient productoClient;
    @MockBean private DetalleClient detalleClient;

    @Test
    @DisplayName("Sin clave interna -> 401, aunque se envíen cabeceras de usuario falsas")
    void sinClave() throws Exception {
        mvc.perform(get("/boletas/mis-boletas").header("X-User-Id", "4").header("X-User-Role", "ADMIN"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("CLIENTE dueño ve su boleta; otro CLIENTE recibe 403")
    void propiedad() throws Exception {
        when(detalleClient.findByBoletaId(anyLong())).thenReturn(List.of());
        mvc.perform(get("/boletas/1").header(KEY, VALOR).header("X-User-Id", "4").header("X-User-Role", "CLIENTE"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.numeroBoleta").value("BOL-18392044"));
        mvc.perform(get("/boletas/1").header(KEY, VALOR).header("X-User-Id", "5").header("X-User-Role", "CLIENTE"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Los parámetros de la URL no pueden suplantar la identidad (sólo cuentan las cabeceras)")
    void sinSuplantacionPorQuery() throws Exception {
        mvc.perform(get("/boletas/1").param("id", "4").param("rol", "ADMIN")
                        .header(KEY, VALOR).header("X-User-Id", "5").header("X-User-Role", "CLIENTE"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Compra sin items -> 400 con mensaje claro (antes 500)")
    void compraSinItems() throws Exception {
        mvc.perform(post("/boletas").header(KEY, VALOR).header("X-User-Id", "5").header("X-User-Role", "CLIENTE")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"items\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.items").value("La boleta debe incluir al menos un producto"));
    }

    @Test
    @DisplayName("Cupones: validar semilla (sin distinguir mayúsculas), inválido 404, CRUD sólo ADMIN")
    void cupones() throws Exception {
        mvc.perform(get("/cupones/validar/gamer10").header(KEY, VALOR).header("X-User-Id", "5").header("X-User-Role", "CLIENTE"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.porcentaje").value(10));
        mvc.perform(get("/cupones/validar/NOEXISTE").header(KEY, VALOR).header("X-User-Id", "5").header("X-User-Role", "CLIENTE"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/cupones").header(KEY, VALOR).header("X-User-Id", "3").header("X-User-Role", "VENDEDOR"))
                .andExpect(status().isForbidden());

        String admin = "1";
        mvc.perform(post("/cupones").header(KEY, VALOR).header("X-User-Id", admin).header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"codigo\":\"prueba-it\",\"porcentaje\":20}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.codigo").value("PRUEBA-IT"));
        mvc.perform(post("/cupones").header(KEY, VALOR).header("X-User-Id", admin).header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"codigo\":\"PRUEBA-IT\",\"porcentaje\":10}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/cupones").header(KEY, VALOR).header("X-User-Id", admin).header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"codigo\":\"X\",\"porcentaje\":95}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.codigo").exists())
                .andExpect(jsonPath("$.detalles.porcentaje").exists());
    }

    @Test
    @DisplayName("Compra con cupón inexistente -> 400 sin descontar stock")
    void compraCuponInvalido() throws Exception {
        mvc.perform(post("/boletas").header(KEY, VALOR).header("X-User-Id", "5").header("X-User-Role", "CLIENTE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigoCupon\":\"NOEXISTE\",\"items\":[{\"idProducto\":1,\"cantidad\":1}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Código de descuento no válido o vencido."));
        org.mockito.Mockito.verifyNoInteractions(productoClient);
    }

    @Test
    @DisplayName("Estado inválido al editar -> 400")
    void estadoInvalido() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/boletas/1")
                        .header(KEY, VALOR).header("X-User-Id", "1").header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"REGALADA\"}"))
                .andExpect(status().isBadRequest());
    }
}
