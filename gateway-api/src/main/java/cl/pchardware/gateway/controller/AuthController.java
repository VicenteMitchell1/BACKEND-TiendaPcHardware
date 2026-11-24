package cl.pchardware.gateway.controller;

import cl.pchardware.gateway.auth.RefreshTokenService;
import cl.pchardware.gateway.client.UsuarioClient;
import cl.pchardware.gateway.client.UsuarioClient.UsuarioSesion;
import cl.pchardware.gateway.dto.LoginRequest;
import cl.pchardware.gateway.dto.LoginResponse;
import cl.pchardware.gateway.dto.RefreshRequest;
import cl.pchardware.gateway.dto.RegisterRequest;
import cl.pchardware.gateway.dto.doc.ErrorDoc;
import cl.pchardware.gateway.security.JwtTokenProvider;
import cl.pchardware.gateway.security.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;

/**
 * Ciclo de vida de la sesión:
 * login/register -> access token (JWT, 1 h) + refresh token (7 días, un solo uso)
 * refresh       -> nuevo par de tokens (con rol y estado del usuario actualizados)
 * logout        -> revoca el refresh token
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticación", description = "Login, registro, renovación y cierre de sesión (JWT)")
@RequiredArgsConstructor
public class AuthController {

    private final UsuarioClient usuarioClient;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión y obtener JWT + refresh token")
    @ApiResponse(responseCode = "200", description = "Credenciales válidas")
    @ApiResponse(responseCode = "401", description = "Credenciales incorrectas", content = @Content(schema = @Schema(implementation = ErrorDoc.class)))
    @ApiResponse(responseCode = "403", description = "Cuenta desactivada", content = @Content(schema = @Schema(implementation = ErrorDoc.class)))
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        UsuarioSesion usuario = usuarioClient.verificarCredenciales(
                request.getEmail().trim().toLowerCase(Locale.ROOT), request.getPassword());
        return emitirSesion(usuario);
    }

    @PostMapping("/register")
    @Operation(summary = "Registrar nuevo usuario (siempre con rol CLIENTE) e iniciar sesión")
    @ApiResponse(responseCode = "201", description = "Usuario creado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(schema = @Schema(implementation = ErrorDoc.class)))
    @ApiResponse(responseCode = "409", description = "Email ya registrado", content = @Content(schema = @Schema(implementation = ErrorDoc.class)))
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        request.setEmail(request.getEmail().trim().toLowerCase(Locale.ROOT));
        return ResponseEntity.status(HttpStatus.CREATED).body(emitirSesion(usuarioClient.registrar(request)));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renovar la sesión: entrega un nuevo JWT y un nuevo refresh token (el anterior queda inválido)")
    @ApiResponse(responseCode = "200", description = "Sesión renovada")
    @ApiResponse(responseCode = "401", description = "Refresh token inválido, usado o expirado", content = @Content(schema = @Schema(implementation = ErrorDoc.class)))
    public LoginResponse refresh(@Valid @RequestBody RefreshRequest request) {
        Long idUsuario = refreshTokenService.consumir(request.getRefreshToken());
        UsuarioSesion usuario = usuarioClient.buscarPorId(idUsuario);
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new DisabledException("La cuenta está desactivada. Contacte al administrador.");
        }
        return emitirSesion(usuario);
    }

    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesión: revoca el refresh token")
    @ApiResponse(responseCode = "204", description = "Sesión cerrada")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        refreshTokenService.revocar(request.getRefreshToken());
        return ResponseEntity.noContent().build();
    }

    private LoginResponse emitirSesion(UsuarioSesion u) {
        String nombreCompleto = u.getNombre() + (u.getApellido() != null && !u.getApellido().isBlank() ? " " + u.getApellido() : "");
        String token = jwtTokenProvider.generarToken(new UsuarioAutenticado(u.getId(), u.getEmail(), u.getRol(), nombreCompleto));
        refreshTokenService.limpiarExpirados();
        return LoginResponse.builder()
                .token(token).tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationSeconds())
                .refreshToken(refreshTokenService.crear(u.getId()))
                .id(u.getId()).email(u.getEmail())
                .nombre(u.getNombre()).apellido(u.getApellido())
                .rut(u.getRut()).rol(u.getRol())
                .build();
    }
}
