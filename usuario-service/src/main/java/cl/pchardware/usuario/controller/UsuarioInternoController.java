package cl.pchardware.usuario.controller;

import cl.pchardware.usuario.dto.CredencialesRequest;
import cl.pchardware.usuario.dto.RegisterRequest;
import cl.pchardware.usuario.dto.UsuarioInternalDTO;
import cl.pchardware.usuario.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Endpoints usados sólo por el API Gateway (protegidos por la clave interna). Nunca devuelven el hash de la contraseña. */
@RestController
@RequestMapping("/interno")
@Tag(name = "Interno (Gateway)", description = "Autenticación y registro invocados por el API Gateway")
@RequiredArgsConstructor
public class UsuarioInternoController {

    private final UsuarioService service;

    @PostMapping("/auth/verificar")
    @Operation(summary = "Verificar credenciales (200 = válidas, 401 = incorrectas, 403 = cuenta desactivada)")
    public UsuarioInternalDTO verificar(@Valid @RequestBody CredencialesRequest req) {
        return service.verificarCredenciales(req);
    }

    @GetMapping("/usuarios/{id}")
    @Operation(summary = "Obtener datos de sesión de un usuario por ID (usado al renovar el token)")
    public UsuarioInternalDTO findById(@PathVariable Long id) {
        return service.findInternalById(id);
    }

    @GetMapping("/usuarios/email/{email}")
    @Operation(summary = "Obtener datos de sesión de un usuario por email")
    public UsuarioInternalDTO findByEmail(@PathVariable String email) {
        return service.findInternalByEmail(email);
    }

    @PostMapping("/usuarios/register")
    @Operation(summary = "Registrar un usuario con rol CLIENTE")
    public ResponseEntity<UsuarioInternalDTO> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.register(req));
    }
}
