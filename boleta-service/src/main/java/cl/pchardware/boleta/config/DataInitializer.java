package cl.pchardware.boleta.config;

import cl.pchardware.boleta.model.Boleta;
import cl.pchardware.boleta.model.Cupon;
import cl.pchardware.boleta.repository.CuponRepository;
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
    private final CuponRepository cuponRepo;

    @Override
    @Transactional
    public void run(String... args) {
        sembrarCupones();
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

    private void sembrarCupones() {
        if (cuponRepo.count() > 0) return;
        cuponRepo.save(Cupon.builder().codigo("HARDWARE2026").porcentaje(15).descripcion("15% de descuento especial hardware 2026").activo(true).build());
        cuponRepo.save(Cupon.builder().codigo("GAMER10").porcentaje(10).descripcion("10% de descuento en periféricos y componentes").activo(true).build());
        cuponRepo.save(Cupon.builder().codigo("BIENVENIDO").porcentaje(5).descripcion("5% de descuento en tu primera compra").activo(true).build());
        log.info("boleta-service: 3 cupones seed creados.");
    }
}
