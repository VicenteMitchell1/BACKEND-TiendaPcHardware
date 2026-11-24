package cl.pchardware.gateway.security;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Control de acceso por rol (RBAC) según el Anexo 1:
 * - ADMIN: acceso total.
 * - VENDEDOR: ve productos, órdenes (boletas) y sus detalles; nada más.
 * - CLIENTE: tienda (catálogo público), comprar y ver SUS boletas.
 * 401 = falta token o es inválido/expiró (el frontend debe renovar o pedir login); 403 = rol sin permiso.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] PERSONAL = {"ADMIN", "VENDEDOR"};
    private static final String[] TODOS = {"ADMIN", "VENDEDOR", "CLIENTE"};

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(c -> c.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) ->
                        escribirError(res, HttpServletResponse.SC_UNAUTHORIZED, "No autenticado: token ausente, inválido o expirado."))
                .accessDeniedHandler((req, res, ex) ->
                        escribirError(res, HttpServletResponse.SC_FORBIDDEN, "Acceso denegado: su rol no tiene permiso para esta operación.")))
            .authorizeHttpRequests(auth -> auth
                // Públicos
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/register",
                        "/api/v1/auth/refresh", "/api/v1/auth/logout").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/productos/**", "/api/v1/categorias/**").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Administración (sólo ADMIN)
                .requestMatchers("/api/v1/usuarios/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/productos/**", "/api/v1/categorias/**").hasRole("ADMIN")
                // Boletas
                .requestMatchers(HttpMethod.GET, "/api/v1/boletas").hasAnyRole(PERSONAL)
                .requestMatchers(HttpMethod.GET, "/api/v1/boletas/mis-boletas").hasAnyRole(TODOS)
                .requestMatchers(HttpMethod.GET, "/api/v1/boletas/**").hasAnyRole(TODOS) // propiedad validada en boleta-service
                .requestMatchers(HttpMethod.POST, "/api/v1/boletas").hasAnyRole(TODOS)
                .requestMatchers("/api/v1/boletas/**").hasRole("ADMIN")
                // Cupones: cualquier usuario autenticado valida un código; el CRUD es sólo ADMIN
                .requestMatchers(HttpMethod.GET, "/api/v1/cupones/validar/**").hasAnyRole(TODOS)
                .requestMatchers("/api/v1/cupones/**").hasRole("ADMIN")
                // Detalles de boleta: lectura para el personal, escritura sólo ADMIN
                .requestMatchers(HttpMethod.GET, "/api/v1/detalles/**").hasAnyRole(PERSONAL)
                .requestMatchers("/api/v1/detalles/**").hasRole("ADMIN")
                .requestMatchers("/error").permitAll()
                // Cualquier otra ruta exige sesión (si no existe, el usuario autenticado recibe 404)
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** El filtro JWT sólo debe ejecutarse dentro de la cadena de Spring Security, no como filtro global adicional. */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registro = new FilterRegistrationBean<>(filter);
        registro.setEnabled(false);
        return registro;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("http://localhost:*", "http://127.0.0.1:*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With", "Origin"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private static void escribirError(HttpServletResponse res, int status, String mensaje) throws IOException {
        res.setStatus(status);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write("{\"timestamp\":\"" + LocalDateTime.now() + "\",\"status\":" + status
                + ",\"error\":\"" + mensaje + "\"}");
    }
}
