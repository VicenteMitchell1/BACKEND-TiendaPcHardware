package cl.pchardware.usuario.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * Exige la cabecera {@value #HEADER} con la clave compartida entre los microservicios.
 * Así sólo el API Gateway (y los servicios internos entre sí) pueden invocar este servicio:
 * una petición directa desde fuera recibe 401, aunque el puerto quede expuesto.
 */
public class InternalApiKeyFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Internal-Token";
    private static final List<String> RUTAS_PUBLICAS = List.of("/v3/api-docs", "/swagger-ui");

    private final byte[] apiKey;

    public InternalApiKeyFilter(String apiKey) {
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("Falta la clave interna: defina la variable de entorno INTERNAL_API_KEY.");
        }
        this.apiKey = apiKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String ruta = request.getRequestURI();
        return RUTAS_PUBLICAS.stream().anyMatch(ruta::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String recibido = req.getHeader(HEADER);
        // Comparación en tiempo constante para no filtrar información por tiempos de respuesta.
        if (recibido != null && MessageDigest.isEqual(apiKey, recibido.getBytes(StandardCharsets.UTF_8))) {
            chain.doFilter(req, res);
            return;
        }
        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write("{\"status\":401,\"error\":\"Acceso restringido: este servicio sólo acepta peticiones del API Gateway.\"}");
    }
}
