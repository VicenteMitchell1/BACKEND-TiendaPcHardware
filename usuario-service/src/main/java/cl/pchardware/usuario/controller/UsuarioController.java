package cl.pchardware.usuario.controller;

import cl.pchardware.usuario.dto.CambioPasswordRequest;
import cl.pchardware.usuario.dto.HistorialResponse;
import cl.pchardware.usuario.dto.PerfilRequest;
import cl.pchardware.usuario.dto.UsuarioActual;
import cl.pchardware.usuario.dto.UsuarioRequest;
import cl.pchardware.usuario.dto.UsuarioResponse;
import cl.pchardware.usuario.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * El gateway restringe /usuarios a ADMIN, salvo /usuarios/me, disponible para cualquier usuario con sesión.
 * {@link UsuarioActual} se arma con las cabeceras X-User-* que agrega el gateway (ver UsuarioActualResolver).
 */
@RestController
@RequestMapping("/usuarios")
@Tag(name = "Usuarios", description = "CRUD de usuarios (ADMIN), historial de cambios y perfil propio")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService service;

    @GetMapping
    @Operation(summary = "Listar todos los usuarios")
    public List<UsuarioResponse> findAll() { return service.findAll(); }

    @GetMapping("/historial")
    @Operation(summary = "Historial de cambios en las cuentas (más recientes primero; filtrable por usuario)")
    public List<HistorialResponse> historial(@RequestParam(required = false) Long idUsuario) {
        return service.historial(idUsuario);
    }

    @GetMapping("/me")
    @Operation(summary = "Datos de la cuenta con la que se inició sesión")
    public UsuarioResponse perfil(@Parameter(hidden = true) UsuarioActual actual) {
        return service.perfil(actual);
    }

    @PutMapping("/me")
    @Operation(summary = "Actualizar nombre, apellido y email de la propia cuenta")
    public UsuarioResponse actualizarPerfil(@Parameter(hidden = true) UsuarioActual actual,
                                            @Valid @RequestBody PerfilRequest req) {
        return service.actualizarPerfil(actual, req);
    }

    @PutMapping("/me/password")
    @Operation(summary = "Cambiar la propia contraseña (exige la contraseña actual)")
    public ResponseEntity<Void> cambiarPassword(@Parameter(hidden = true) UsuarioActual actual,
                                                @Valid @RequestBody CambioPasswordRequest req) {
        service.cambiarPassword(actual, req);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener usuario por ID")
    public UsuarioResponse findById(@PathVariable Long id) { return service.findById(id); }

    @PostMapping
    @Operation(summary = "Crear usuario")
    public ResponseEntity<UsuarioResponse> create(@Valid @RequestBody UsuarioRequest req,
                                                  @Parameter(hidden = true) UsuarioActual autor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req, autor));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar datos, rol o estado de un usuario (la contraseña no se puede cambiar aquí)")
    public UsuarioResponse update(@PathVariable Long id, @Valid @RequestBody UsuarioRequest req,
                                  @Parameter(hidden = true) UsuarioActual autor) {
        return service.update(id, req, autor);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar usuario")
    public ResponseEntity<Void> delete(@PathVariable Long id, @Parameter(hidden = true) UsuarioActual autor) {
        service.delete(id, autor);
        return ResponseEntity.noContent().build();
    }
}
