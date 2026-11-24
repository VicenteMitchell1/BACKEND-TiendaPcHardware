package cl.pchardware.boleta.service;

import cl.pchardware.boleta.dto.CuponRequest;
import cl.pchardware.boleta.dto.CuponResponse;
import cl.pchardware.boleta.dto.UsuarioActual;
import cl.pchardware.boleta.exception.BadRequestException;
import cl.pchardware.boleta.exception.ConflictException;
import cl.pchardware.boleta.exception.ForbiddenException;
import cl.pchardware.boleta.exception.ResourceNotFoundException;
import cl.pchardware.boleta.model.Cupon;
import cl.pchardware.boleta.repository.CuponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

/** CRUD de cupones (sólo ADMIN) y validación de un código para el carrito y la compra. */
@Service
@RequiredArgsConstructor
public class CuponService {

    private final CuponRepository repo;

    @Transactional(readOnly = true)
    public List<CuponResponse> findAll(UsuarioActual usuario) {
        exigirAdmin(usuario);
        return repo.findAllByOrderByCodigoAsc().stream().map(CuponResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public CuponResponse findById(Long id, UsuarioActual usuario) {
        exigirAdmin(usuario);
        return CuponResponse.de(buscar(id));
    }

    /** Devuelve el cupón si existe y está activo; si no, 404 con un mensaje apto para mostrar al cliente. */
    @Transactional(readOnly = true)
    public CuponResponse validar(String codigo) {
        return CuponResponse.de(buscarActivo(codigo)
                .orElseThrow(() -> new ResourceNotFoundException("Código de descuento no válido o vencido.")));
    }

    /** Usado al emitir una boleta: un código inválido es un error de la solicitud (400). */
    @Transactional(readOnly = true)
    public Cupon obtenerParaCompra(String codigo) {
        return buscarActivo(codigo)
                .orElseThrow(() -> new BadRequestException("Código de descuento no válido o vencido."));
    }

    @Transactional
    public CuponResponse create(CuponRequest req, UsuarioActual usuario) {
        exigirAdmin(usuario);
        String codigo = normalizar(req.getCodigo());
        if (repo.existsByCodigo(codigo)) throw new ConflictException("Ya existe un cupón con el código " + codigo + ".");
        Cupon c = Cupon.builder()
                .codigo(codigo)
                .porcentaje(req.getPorcentaje())
                .descripcion(req.getDescripcion())
                .activo(req.getActivo() == null || req.getActivo())
                .build();
        return CuponResponse.de(repo.save(c));
    }

    @Transactional
    public CuponResponse update(Long id, CuponRequest req, UsuarioActual usuario) {
        exigirAdmin(usuario);
        Cupon c = buscar(id);
        String codigo = normalizar(req.getCodigo());
        if (repo.existsByCodigoAndIdNot(codigo, id)) throw new ConflictException("Ya existe un cupón con el código " + codigo + ".");
        c.setCodigo(codigo);
        c.setPorcentaje(req.getPorcentaje());
        c.setDescripcion(req.getDescripcion());
        if (req.getActivo() != null) c.setActivo(req.getActivo());
        return CuponResponse.de(repo.save(c));
    }

    /** Las boletas guardan el código y el monto descontado, por lo que borrar un cupón no altera ventas pasadas. */
    @Transactional
    public void delete(Long id, UsuarioActual usuario) {
        exigirAdmin(usuario);
        repo.delete(buscar(id));
    }

    private java.util.Optional<Cupon> buscarActivo(String codigo) {
        if (!StringUtils.hasText(codigo)) return java.util.Optional.empty();
        return repo.findByCodigo(normalizar(codigo)).filter(c -> Boolean.TRUE.equals(c.getActivo()));
    }

    private Cupon buscar(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Cupón no encontrado con ID: " + id));
    }

    static String normalizar(String codigo) {
        return codigo == null ? null : codigo.trim().toUpperCase(Locale.ROOT);
    }

    private static void exigirAdmin(UsuarioActual usuario) {
        if (!usuario.esAdmin()) throw new ForbiddenException("Operación exclusiva del administrador.");
    }
}
