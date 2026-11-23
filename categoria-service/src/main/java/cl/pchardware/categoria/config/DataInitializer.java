package cl.pchardware.categoria.config;

import cl.pchardware.categoria.model.Categoria;
import cl.pchardware.categoria.repository.CategoriaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final CategoriaRepository categoriaRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (categoriaRepository.count() >= 6) return;

        if (categoriaRepository.count() > 0) {
            categoriaRepository.deleteAll();
        }

        categoriaRepository.saveAll(List.of(
                Categoria.builder().nombre("Tarjetas Gráficas").descripcion("Potencia gráfica para gaming extremo y renderizado 3D").icono("bi-gpu-card").activo(true).build(),
                Categoria.builder().nombre("Procesadores").descripcion("El cerebro de tu máquina, AMD Ryzen e Intel Core").icono("bi-cpu").activo(true).build(),
                Categoria.builder().nombre("Memorias RAM").descripcion("Multitarea y velocidad DDR4 y DDR5 de alto rendimiento").icono("bi-memory").activo(true).build(),
                Categoria.builder().nombre("Almacenamiento").descripcion("Discos SSD NVMe de alta velocidad para cargas ultrarrápidas").icono("bi-hdd").activo(true).build(),
                Categoria.builder().nombre("Computadores").descripcion("Equipos armados listos para jugar y trabajar sin límites").icono("bi-pc-display").activo(true).build(),
                Categoria.builder().nombre("Periféricos").descripcion("Audífonos, teclados y accesorios para tu estación de juego").icono("bi-headphones").activo(true).build()
        ));
        log.info("categoria-service: 6 categorias oficiales creadas.");
    }
}
