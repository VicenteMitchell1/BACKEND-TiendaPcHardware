package cl.pchardware.detalle;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class DetalleApiIntegrationTest {

    private static final String KEY = "X-Internal-Token";
    private static final String VALOR = "test-internal-key";

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("Sin clave interna -> 401")
    void sinClave() throws Exception {
        mvc.perform(get("/detalles")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("El subtotal lo calcula el servidor aunque el cliente envíe otro valor")
    void subtotalCalculado() throws Exception {
        mvc.perform(post("/detalles").header(KEY, VALOR).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idBoleta\":500,\"idProducto\":1,\"cantidad\":3,\"precioUnitario\":1000,\"subtotal\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subtotal").value(3000));
    }

    @Test
    @DisplayName("Lote con un elemento inválido -> 400 y no se guarda nada")
    void loteInvalido() throws Exception {
        mvc.perform(post("/detalles/batch").header(KEY, VALOR).contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"idBoleta\":600,\"idProducto\":1,\"cantidad\":1,\"precioUnitario\":10},"
                                + "{\"idBoleta\":600,\"idProducto\":2,\"cantidad\":0,\"precioUnitario\":10}]"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/detalles/boleta/600").header(KEY, VALOR))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Lote vacío -> 400")
    void loteVacio() throws Exception {
        mvc.perform(post("/detalles/batch").header(KEY, VALOR).contentType(MediaType.APPLICATION_JSON).content("[]"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Consulta en lote por varias boletas, edición con recálculo y borrado por boleta")
    void cicloCompleto() throws Exception {
        mvc.perform(post("/detalles/batch").header(KEY, VALOR).contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"idBoleta\":700,\"idProducto\":1,\"cantidad\":1,\"precioUnitario\":100},"
                                + "{\"idBoleta\":701,\"idProducto\":2,\"cantidad\":2,\"precioUnitario\":50}]"))
                .andExpect(status().isCreated());

        mvc.perform(get("/detalles/boletas").param("ids", "700,701").header(KEY, VALOR))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));

        mvc.perform(delete("/detalles/boleta/700").header(KEY, VALOR))
                .andExpect(status().isOk()).andExpect(jsonPath("$.eliminados").value(1));
        mvc.perform(get("/detalles/boleta/700").header(KEY, VALOR)).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("El detalle semilla coincide con el producto 3 del catálogo (RAM Corsair)")
    void semillaCoherente() throws Exception {
        mvc.perform(get("/detalles/1").header(KEY, VALOR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skuProducto").value("RAM-CORSAIR-32G"));
    }

    @Test
    @DisplayName("Actualizar un detalle recalcula el subtotal; detalle inexistente -> 404")
    void actualizar() throws Exception {
        String creado = mvc.perform(post("/detalles").header(KEY, VALOR).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idBoleta\":800,\"idProducto\":3,\"cantidad\":1,\"precioUnitario\":180000}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = creado.replaceAll(".*\"id\":(\\d+).*", "$1");

        mvc.perform(put("/detalles/" + id).header(KEY, VALOR).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idBoleta\":800,\"idProducto\":3,\"cantidad\":2,\"precioUnitario\":180000}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.subtotal").value(360000));
        mvc.perform(put("/detalles/99999").header(KEY, VALOR).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idBoleta\":1,\"idProducto\":3,\"cantidad\":1,\"precioUnitario\":1}"))
                .andExpect(status().isNotFound());
    }
}
