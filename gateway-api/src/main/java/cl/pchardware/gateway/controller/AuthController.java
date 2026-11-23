package cl.pchardware.gateway.controller;

import cl.pchardware.gateway.client.UsuarioClient;
import cl.pchardware.gateway.dto.LoginRequest;
import cl.pchardware.gateway.dto.LoginResponse;
import cl.pchardware.gateway.dto.RegisterRequest;
import cl.pchardware.gateway.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticación", description = "Login y Registro de usuarios")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UsuarioClient usuarioClient;
    private final RestTemplate restTemplate;

    @Value("${services.usuario-url:http://localhost:8081}")
    private String usuarioUrl;

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión y obtener JWT")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail().trim().toLowerCase(), request.getPassword()
                    )
            );
        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("Credenciales incorrectas.");
        }

        var dto = usuarioClient.findByEmail(request.getEmail().trim().toLowerCase());
        if (dto == null) throw new BadCredentialsException("Usuario no encontrado.");

        String token = jwtTokenProvider.generarToken(
                dto.getEmail(), dto.getRol(),
                dto.getNombre() + (dto.getApellido() != null ? " " + dto.getApellido() : ""),
                dto.getId()
        );

        return ResponseEntity.ok(LoginResponse.builder()
                .token(token).tokenType("Bearer")
                .id(dto.getId()).email(dto.getEmail())
                .nombre(dto.getNombre()).apellido(dto.getApellido())
                .rut(dto.getRut()).rol(dto.getRol())
                .build());
    }

    @PostMapping("/register")
    @Operation(summary = "Registrar nuevo usuario (rol CLIENTE)")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        // Delegar al usuario-service
        LoginResponse resp = restTemplate.postForObject(
                usuarioUrl + "/interno/usuarios/register", request, LoginResponse.class
        );
        // Generar token propio
        if (resp != null) {
            var dto = usuarioClient.findByEmail(request.getEmail().trim().toLowerCase());
            if (dto != null) {
                String token = jwtTokenProvider.generarToken(
                        dto.getEmail(), dto.getRol(),
                        dto.getNombre() + (dto.getApellido() != null ? " " + dto.getApellido() : ""),
                        dto.getId()
                );
                return ResponseEntity.ok(LoginResponse.builder()
                        .token(token).tokenType("Bearer")
                        .id(dto.getId()).email(dto.getEmail())
                        .nombre(dto.getNombre()).apellido(dto.getApellido())
                        .rut(dto.getRut()).rol(dto.getRol())
                        .build());
            }
        }
        return ResponseEntity.internalServerError().build();
    }
}
