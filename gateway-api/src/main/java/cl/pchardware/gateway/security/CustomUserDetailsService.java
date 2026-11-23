package cl.pchardware.gateway.security;

import cl.pchardware.gateway.client.UsuarioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioClient usuarioClient;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        var dto = usuarioClient.findByEmail(email);
        if (dto == null) throw new UsernameNotFoundException("Usuario no encontrado: " + email);
        return new User(
                dto.getEmail(),
                dto.getPasswordHash(),
                List.of(new SimpleGrantedAuthority("ROLE_" + dto.getRol()))
        );
    }
}
