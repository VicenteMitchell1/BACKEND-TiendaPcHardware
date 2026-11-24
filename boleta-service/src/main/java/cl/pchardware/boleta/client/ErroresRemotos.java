package cl.pchardware.boleta.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.client.HttpStatusCodeException;

/** Extrae el mensaje "error" que devuelven los otros microservicios, para mostrarlo tal cual al usuario. */
final class ErroresRemotos {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ErroresRemotos() {}

    static String mensaje(HttpStatusCodeException e, String porDefecto) {
        try {
            JsonNode n = MAPPER.readTree(e.getResponseBodyAsString());
            if (n != null && n.hasNonNull("error")) return n.get("error").asText();
        } catch (Exception ignored) {
            // cuerpo no JSON: se usa el mensaje por defecto
        }
        return porDefecto;
    }
}
