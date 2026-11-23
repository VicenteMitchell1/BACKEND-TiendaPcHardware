package cl.pchardware.usuario.controller;

import cl.pchardware.usuario.dto.RegisterRequest;
import cl.pchardware.usuario.dto.UsuarioInternalDTO;
import cl.pchardware.usuario.dto.UsuarioResponse;
import cl.pchardware.usuario.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/interno/usuarios")
@RequiredArgsConstructor
public class UsuarioInternoController {

    private final UsuarioService service;

    @GetMapping("/email/{email}")
    public UsuarioInternalDTO findByEmail(@PathVariable String email) {
        return service.findByEmailInternal(email);
    }

    @PostMapping("/register")
    public UsuarioResponse register(@Valid @RequestBody RegisterRequest req) {
        return service.register(req);
    }
}
