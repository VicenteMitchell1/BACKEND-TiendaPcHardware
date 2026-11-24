package cl.pchardware.categoria.service;

import cl.pchardware.categoria.client.ProductoClient;
import cl.pchardware.categoria.dto.CategoriaRequest;
import cl.pchardware.categoria.dto.CategoriaResponse;
import cl.pchardware.categoria.exception.ConflictException;
import cl.pchardware.categoria.exception.ResourceNotFoundException;
import cl.pchardware.categoria.model.Categoria;
import cl.pchardware.categoria.repository.CategoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository repo;
    private final ProductoClient productoClient;

    @Transactional(readOnly = true)
    public List<CategoriaResponse> findAll() {
        return repo.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CategoriaResponse findById(Long id) {
        return toResponse(buscar(id));
    }

    @Transactional
    public CategoriaResponse create(CategoriaRequest req) {
        String nombre = req.getNombre().trim();
        if (repo.existsByNombreIgnoreCase(nombre)) {
            throw new ConflictException("Ya existe una categoría con ese nombre: " + nombre);
        }
        Categoria c = Categoria.builder()
                .nombre(nombre)
                .descripcion(req.getDescripcion())
                .icono(req.getIcono())
                .activo(req.getActivo() == null || req.getActivo())
                .build();
        return toResponse(repo.save(c));
    }

    /**
     * Si cambia el nombre, se propaga a producto-service dentro de la misma transacción:
     * si la propagación falla se lanza una excepción y el cambio se revierte (no quedan nombres desincronizados).
     */
    @Transactional
    public CategoriaResponse update(Long id, CategoriaRequest req) {
        Categoria c = buscar(id);
        String nombre = req.getNombre().trim();
        boolean cambiaNombre = !nombre.equals(c.getNombre());
        if (cambiaNombre && repo.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new ConflictException("Ya existe una categoría con ese nombre: " + nombre);
        }
        c.setNombre(nombre);
        if (req.getDescripcion() != null) c.setDescripcion(req.getDescripcion());
        if (req.getIcono() != null) c.setIcono(req.getIcono());
        if (req.getActivo() != null) c.setActivo(req.getActivo());
        Categoria guardada = repo.saveAndFlush(c);
        if (cambiaNombre) productoClient.sincronizarNombre(id, nombre);
        return toResponse(guardada);
    }

    /** No se permite eliminar una categoría que todavía tiene productos (integridad CATEGORIA -> PRODUCTO). */
    @Transactional
    public void delete(Long id) {
        if (!repo.existsById(id)) throw new ResourceNotFoundException("Categoría no encontrada: " + id);
        long productos = productoClient.contarProductos(id);
        if (productos > 0) {
            throw new ConflictException("No se puede eliminar la categoría: tiene " + productos
                    + " producto(s) asociado(s). Reasígnelos o desactive la categoría.");
        }
        repo.deleteById(id);
    }

    private Categoria buscar(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada: " + id));
    }

    private CategoriaResponse toResponse(Categoria c) {
        return CategoriaResponse.builder()
                .id(c.getId()).nombre(c.getNombre())
                .descripcion(c.getDescripcion()).icono(c.getIcono())
                .activo(c.getActivo()).build();
    }
}
