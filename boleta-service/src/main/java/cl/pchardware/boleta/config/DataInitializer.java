package cl.pchardware.boleta.config;

import cl.pchardware.boleta.model.Boleta;
import cl.pchardware.boleta.repository.BoletaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final BoletaRepository repo;

    @Override
    @Transactional
    public void run(String... args) {
        if (repo.count() > 0) return;

        repo.save(Boleta.builder()
                .numeroBoleta("BOL-18392044")
                .fechaEmision(LocalDateTime.now().minusDays(1))
                .subtotal(180000)
                .descuento(0)
                .total(180000)
                .metodoPago("Webpay Plus / Débito")
                .estado("PAGADA")
                .nombreCliente("Panchito Gamer")
                .emailCliente("pancho@gmail.com")
                .rutCliente("18555444-3")
                .direccionEnvio("Av. Providencia 1234, Depto 402")
                .comuna("Providencia")
                .region("Región Metropolitana de Santiago")
                .idUsuario(4L)
                .build());

        log.info("boleta-service: 1 boleta seed creada (BOL-18392044).");
    }
}
