package cl.pchardware.gateway.security;

import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private static final UsuarioAutenticado ADMIN = new UsuarioAutenticado(1L, "admin@duoc.cl", "ADMIN", "Administrador Sistema");

    private static JwtTokenProvider proveedor(String secretoTexto, long expiracionMs) {
        JwtProperties p = new JwtProperties();
        p.setSecret(Base64.getEncoder().encodeToString(secretoTexto.getBytes()));
        p.setExpirationMs(expiracionMs);
        return new JwtTokenProvider(p);
    }

    private static final String SECRETO = "clave-de-pruebas-con-mas-de-32-bytes-de-largo!!";

    @Test
    @DisplayName("Un token generado se valida y conserva id, email, rol y nombre")
    void generarYValidar() {
        JwtTokenProvider jwt = proveedor(SECRETO, 60_000);
        assertThat(jwt.validar(jwt.generarToken(ADMIN))).contains(ADMIN);
    }

    @Test
    @DisplayName("Token alterado (firma inválida) es rechazado")
    void tokenAlterado() {
        JwtTokenProvider jwt = proveedor(SECRETO, 60_000);
        String token = jwt.generarToken(ADMIN);
        String alterado = token.substring(0, token.length() - 3) + (token.endsWith("AAA") ? "BBB" : "AAA");
        assertThat(jwt.validar(alterado)).isEmpty();
    }

    @Test
    @DisplayName("Token firmado con otra clave es rechazado (no se puede forjar un ADMIN)")
    void otraClave() {
        String forjado = proveedor("otra-clave-distinta-tambien-de-mas-de-32-bytes!!", 60_000).generarToken(ADMIN);
        assertThat(proveedor(SECRETO, 60_000).validar(forjado)).isEmpty();
    }

    @Test
    @DisplayName("Token expirado es rechazado")
    void expirado() {
        JwtTokenProvider jwt = proveedor(SECRETO, -1_000);
        assertThat(jwt.validar(jwt.generarToken(ADMIN))).isEmpty();
    }

    @Test
    @DisplayName("Texto que no es un JWT es rechazado")
    void basura() {
        assertThat(proveedor(SECRETO, 60_000).validar("no-es-un-jwt")).isEmpty();
    }

    @Test
    @DisplayName("Sin secreto configurado la aplicación no arranca")
    void sinSecreto() {
        assertThatThrownBy(() -> new JwtTokenProvider(new JwtProperties()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("JWT_SECRET");
    }

    @Test
    @DisplayName("Un secreto débil (< 256 bits) es rechazado al arrancar")
    void secretoDebil() {
        assertThatThrownBy(() -> proveedor("corta", 60_000)).isInstanceOf(WeakKeyException.class);
    }
}
