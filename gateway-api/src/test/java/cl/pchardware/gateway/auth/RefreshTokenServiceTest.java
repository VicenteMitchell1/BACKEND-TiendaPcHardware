package cl.pchardware.gateway.auth;

import cl.pchardware.gateway.security.JwtProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class RefreshTokenServiceTest {

    @Autowired private RefreshTokenService service;
    @Autowired private RefreshTokenRepository repo;
    @Autowired private JwtProperties props;

    @Test
    @DisplayName("Un refresh token válido se consume una sola vez (rotación)")
    void usoUnico() {
        String token = service.crear(10L);
        assertThat(service.consumir(token)).isEqualTo(10L);
        assertThatThrownBy(() -> service.consumir(token)).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("Reutilizar un token ya usado revoca TODAS las sesiones del usuario")
    void reutilizacionRevocaTodo() {
        String usado = service.crear(20L);
        String otraSesion = service.crear(20L);
        service.consumir(usado);

        assertThatThrownBy(() -> service.consumir(usado)).isInstanceOf(BadCredentialsException.class);
        assertThatThrownBy(() -> service.consumir(otraSesion)).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("Logout: un token revocado ya no sirve")
    void revocado() {
        String token = service.crear(30L);
        service.revocar(token);
        assertThatThrownBy(() -> service.consumir(token)).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("Un token expirado es rechazado")
    void expirado() {
        String token = service.crear(40L);
        RefreshToken rt = repo.findByTokenHash(RefreshTokenService.hash(token)).orElseThrow();
        rt.setExpiraEn(Instant.now().minusSeconds(1));
        repo.save(rt);
        assertThatThrownBy(() -> service.consumir(token)).isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("expiró");
    }

    @Test
    @DisplayName("En la base sólo se guarda el hash SHA-256, nunca el token en claro")
    void soloHash() {
        String token = service.crear(50L);
        assertThat(repo.findAll()).noneMatch(rt -> rt.getTokenHash().equals(token));
        assertThat(repo.findByTokenHash(RefreshTokenService.hash(token))).isPresent();
        assertThat(props.getRefreshExpirationMs()).isPositive();
    }

    @Test
    @DisplayName("Un token inventado es rechazado")
    void inventado() {
        assertThatThrownBy(() -> service.consumir("token-que-no-existe")).isInstanceOf(BadCredentialsException.class);
    }
}
