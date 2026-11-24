package cl.pchardware.gateway.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Sesión iniciada: access token (JWT) + refresh token para renovarlo")
public class LoginResponse {
    @Schema(description = "JWT para la cabecera Authorization: Bearer <token>")
    private String token;
    @Schema(example = "Bearer")
    private String tokenType;
    @Schema(description = "Segundos de vida del token", example = "3600")
    private Long expiresIn;
    @Schema(description = "Token opaco para POST /api/v1/auth/refresh (un solo uso)")
    private String refreshToken;
    private Long id;
    private String email;
    private String nombre;
    private String apellido;
    private String rut;
    @Schema(example = "CLIENTE")
    private String rol;
}
