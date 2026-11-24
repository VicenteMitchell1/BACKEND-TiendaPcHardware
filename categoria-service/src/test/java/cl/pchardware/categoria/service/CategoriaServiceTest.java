package cl.pchardware.categoria.service;

import cl.pchardware.categoria.client.ProductoClient;
import cl.pchardware.categoria.dto.CategoriaRequest;
import cl.pchardware.categoria.exception.ConflictException;
import cl.pchardware.categoria.exception.ResourceNotFoundException;
import cl.pchardware.categoria.exception.ServiceUnavailableException;
import cl.pchardware.categoria.model.Categoria;
import cl.pchardware.categoria.repository.CategoriaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CategoriaServiceTest {

    private CategoriaRepository repo;
    private ProductoClient productoClient;
    private CategoriaService service;

    @BeforeEach
    void setUp() {
        repo = mock(CategoriaRepository.class);
        productoClient = mock(ProductoClient.class);
        service = new CategoriaService(repo, productoClient);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(repo.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private static CategoriaRequest req(String nombre) {
        CategoriaRequest r = new CategoriaRequest();
        r.setNombre(nombre);
        return r;
    }

    private static Categoria cat(Long id, String nombre) {
        return Categoria.builder().id(id).nombre(nombre).activo(true).build();
    }

    @Test
    @DisplayName("Crear categoría con nombre repetido (sin importar mayúsculas) -> 409")
    void crearDuplicada() {
        when(repo.existsByNombreIgnoreCase("procesadores")).thenReturn(true);
        assertThatThrownBy(() -> service.create(req("procesadores"))).isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("Crear categoría queda activa por defecto")
    void crearActivaPorDefecto() {
        assertThat(service.create(req("  Fuentes de Poder ")).getNombre()).isEqualTo("Fuentes de Poder");
    }

    @Test
    @DisplayName("Renombrar una categoría propaga el nombre a sus productos")
    void renombrarPropaga() {
        when(repo.findById(1L)).thenReturn(Optional.of(cat(1L, "GPUs")));
        service.update(1L, req("Tarjetas Gráficas"));
        verify(productoClient).sincronizarNombre(1L, "Tarjetas Gráficas");
    }

    @Test
    @DisplayName("Editar sin cambiar el nombre no llama a producto-service")
    void editarSinRenombrar() {
        when(repo.findById(1L)).thenReturn(Optional.of(cat(1L, "GPUs")));
        CategoriaRequest r = req("GPUs");
        r.setDescripcion("nueva");
        service.update(1L, r);
        verifyNoInteractions(productoClient);
    }

    @Test
    @DisplayName("Si producto-service no responde, el renombrado falla (y la transacción se revierte)")
    void renombrarSinProductoService() {
        when(repo.findById(1L)).thenReturn(Optional.of(cat(1L, "GPUs")));
        doThrow(new ServiceUnavailableException("caído")).when(productoClient).sincronizarNombre(any(), any());
        assertThatThrownBy(() -> service.update(1L, req("Gráficas"))).isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    @DisplayName("No se puede eliminar una categoría con productos -> 409")
    void eliminarConProductos() {
        when(repo.existsById(1L)).thenReturn(true);
        when(productoClient.contarProductos(1L)).thenReturn(3L);
        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(ConflictException.class).hasMessageContaining("3 producto");
        verify(repo, never()).deleteById(any());
    }

    @Test
    @DisplayName("Categoría sin productos se elimina")
    void eliminarSinProductos() {
        when(repo.existsById(7L)).thenReturn(true);
        when(productoClient.contarProductos(7L)).thenReturn(0L);
        service.delete(7L);
        verify(repo).deleteById(7L);
    }

    @Test
    @DisplayName("Si no se puede verificar en producto-service, NO se elimina")
    void eliminarSinVerificacion() {
        when(repo.existsById(7L)).thenReturn(true);
        when(productoClient.contarProductos(7L)).thenThrow(new ServiceUnavailableException("caído"));
        assertThatThrownBy(() -> service.delete(7L)).isInstanceOf(ServiceUnavailableException.class);
        verify(repo, never()).deleteById(any());
    }

    @Test
    @DisplayName("Eliminar categoría inexistente -> 404")
    void eliminarInexistente() {
        assertThatThrownBy(() -> service.delete(99L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
