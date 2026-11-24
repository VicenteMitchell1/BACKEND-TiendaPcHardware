package cl.pchardware.usuario.config;

import cl.pchardware.usuario.model.Rol;
import cl.pchardware.usuario.model.Usuario;
import cl.pchardware.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder encoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (usuarioRepository.count() > 0) return;

        usuarioRepository.save(Usuario.builder().email("admin@duoc.cl").passwordHash(encoder.encode("admin")).nombre("Administrador").apellido("Sistema").rut("11111111-1").rol(Rol.ADMIN).activo(true).build());
        usuarioRepository.save(Usuario.builder().email("profesor@profesor.duoc.cl").passwordHash(encoder.encode("duoc")).nombre("Profesor").apellido("Evaluador").rut("22222222-2").rol(Rol.ADMIN).activo(true).build());
        usuarioRepository.save(Usuario.builder().email("vendedor@duoc.cl").passwordHash(encoder.encode("vendedor")).nombre("Carlos").apellido("Vendedor").rut("33333333-3").rol(Rol.VENDEDOR).activo(true).build());
        usuarioRepository.save(Usuario.builder().email("pancho@gmail.com").passwordHash(encoder.encode("gamer")).nombre("Panchito").apellido("Gamer").rut("18555444-3").rol(Rol.CLIENTE).activo(true).build());
        usuarioRepository.save(Usuario.builder().email("alumno@duoc.cl").passwordHash(encoder.encode("1234")).nombre("Alumno").apellido("Control3").rut("44444444-4").rol(Rol.CLIENTE).activo(true).build());

        log.info("usuario-service: 5 usuarios seed creados.");
    }
}
