package cl.pchardware.categoria.service;

import cl.pchardware.categoria.dto.CategoriaRequest;
import cl.pchardware.categoria.dto.CategoriaResponse;
import cl.pchardware.categoria.exception.BadRequestException;
import cl.pchardware.categoria.exception.ResourceNotFoundException;
import cl.pchardware.categoria.model.Categoria;
import cl.pchardware.categoria.repository.CategoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository repo;

    @Transactional(readOnly = true)
    public List<CategoriaResponse> findAll() {
        return repo.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoriaResponse findById(Long id) {
        return toResponse(repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada: " + id)));
    }

    @Transactional
    public CategoriaResponse create(CategoriaRequest req) {
        if (repo.existsByNombre(req.getNombre())) {
            throw new BadRequestException("Ya existe una categoría con ese nombre: " + req.getNombre());
        }
        Categoria c = Categoria.builder()
                .nombre(req.getNombre().trim())
                .descripcion(req.getDescripcion())
                .icono(req.getIcono())
                .activo(req.getActivo() != null ? req.getActivo() : true)
                .build();
        return toResponse(repo.save(c));
    }

    @Transactional
    public CategoriaResponse update(Long id, CategoriaRequest req) {
        Categoria c = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada: " + id));
        c.setNombre(req.getNombre().trim());
        if (req.getDescripcion() != null) c.setDescripcion(req.getDescripcion());
        if (req.getIcono() != null) c.setIcono(req.getIcono());
        if (req.getActivo() != null) c.setActivo(req.getActivo());
        return toResponse(repo.save(c));
    }

    @Transactional
    public void delete(Long id) {
        if (!repo.existsById(id)) throw new ResourceNotFoundException("Categoría no encontrada: " + id);
        repo.deleteById(id);
    }

    private CategoriaResponse toResponse(Categoria c) {
        return CategoriaResponse.builder()
                .id(c.getId()).nombre(c.getNombre())
                .descripcion(c.getDescripcion()).icono(c.getIcono())
                .activo(c.getActivo()).build();
    }
}
