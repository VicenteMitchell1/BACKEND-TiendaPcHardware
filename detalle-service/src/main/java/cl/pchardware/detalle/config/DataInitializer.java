package cl.pchardware.detalle.config;

import cl.pchardware.detalle.model.DetalleBoleta;
import cl.pchardware.detalle.repository.DetalleBoletaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final DetalleBoletaRepository repo;

    @Override
    @Transactional
    public void run(String... args) {
        if (repo.count() > 0) return;

        repo.save(DetalleBoleta.builder()
                .idBoleta(1L)
                .idProducto(3L)
                .nombreProducto("Memoria RAM Corsair Vengeance 32GB DDR5")
                .skuProducto("RAM-CORSAIR-32G")
                .cantidad(1)
                .precioUnitario(180000)
                .subtotal(180000)
                .build());

        log.info("detalle-service: 1 detalle seed creado para boleta 1.");
    }
}
