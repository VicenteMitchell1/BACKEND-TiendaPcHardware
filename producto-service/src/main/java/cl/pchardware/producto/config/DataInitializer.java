package cl.pchardware.producto.config;

import cl.pchardware.producto.model.Producto;
import cl.pchardware.producto.repository.ProductoRepository;
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

    private final ProductoRepository productoRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (productoRepository.count() >= 19) return;

        // Si existen productos desactualizados o incompletos, re-inicializar con los 19 productos oficiales
        if (productoRepository.count() > 0) {
            productoRepository.deleteAll();
        }

        productoRepository.saveAll(List.of(
                Producto.builder().sku("GPU-RTX-4090FE").nombre("NVIDIA RTX 4090 Founders Edition").marca("NVIDIA").modelo("Founders Edition").descripcion("24GB GDDR6X, Ada Lovelace, DLSS 3.5, Ray Tracing Gen 3").precio(1599990).precioAnterior(1799990).stock(10).imagen("/static/img/productos/rtx-4090.jpg").esOferta(true).porcentajeDescuento(11).idCategoria(1L).nombreCategoria("Tarjetas Gráficas").activo(true).build(),
                Producto.builder().sku("CPU-AMD-7950X").nombre("AMD Ryzen 9 7950X").marca("AMD").modelo("7950X").descripcion("16 Cores, 32 Threads, hasta 5.7GHz Boost, Socket AM5").precio(650000).precioAnterior(720000).stock(15).imagen("/static/img/productos/ryzen-9.jpg").esOferta(true).porcentajeDescuento(10).idCategoria(2L).nombreCategoria("Procesadores").activo(true).build(),
                Producto.builder().sku("RAM-CORSAIR-32G").nombre("Memoria RAM Corsair Vengeance 32GB DDR5").marca("Corsair").modelo("Vengeance").descripcion("32GB (2x16GB) 6000MHz C36 Intel XMP / AMD EXPO").precio(180000).precioAnterior(199990).stock(50).imagen("/static/img/productos/ram-32gb.jpg").esOferta(false).idCategoria(3L).nombreCategoria("Memorias RAM").activo(true).build(),
                Producto.builder().sku("SSD-SAMSUNG-990P").nombre("SSD Samsung 990 PRO 2TB NVMe M.2").marca("Samsung").modelo("990 PRO").descripcion("PCIe 4.0, hasta 7450 MB/s lectura, memoria V-NAND").precio(220000).precioAnterior(259990).stock(30).imagen("/static/img/productos/ssd-2tb.jpg").esOferta(true).porcentajeDescuento(15).idCategoria(4L).nombreCategoria("Almacenamiento").activo(true).build(),
                Producto.builder().sku("PC-HP-OMEN-35L").nombre("PC Gamer HP Omen 35L").marca("HP").modelo("Omen 35L").descripcion("AMD Ryzen 7, 16GB DDR5, SSD 1TB, RTX 4060 Ti").precio(1219990).precioAnterior(1399990).stock(25).imagen("/static/img/productos/HP_OMEN_35L.jpg").esOferta(true).porcentajeDescuento(13).idCategoria(5L).nombreCategoria("Computadores").activo(true).build(),
                Producto.builder().sku("NB-MSI-KATANA-15").nombre("MSI Katana 15 HX Gaming Laptop").marca("MSI").modelo("Katana 15 HX B14WGK").descripcion("Intel Core i7-14650HX, 16GB RAM, SSD 1TB, RTX 4060 8GB").precio(1399900).precioAnterior(1549900).stock(20).imagen("/static/img/productos/MSI_Katana_15.jpg").esOferta(true).porcentajeDescuento(10).idCategoria(5L).nombreCategoria("Computadores").activo(true).build(),
                Producto.builder().sku("PC-XT3-R5500").nombre("PC Gamer Central XT3 Ryzen 5").marca("XT3").modelo("Ryzen 5 5500").descripcion("AMD Ryzen 5 5500, 16GB RAM, SSD M.2 512GB, RTX 3050 6GB").precio(769990).precioAnterior(849990).stock(35).imagen("/static/img/productos/PC_Gamer_Central_Gamer_XT3.jpg").esOferta(false).idCategoria(5L).nombreCategoria("Computadores").activo(true).build(),
                Producto.builder().sku("NB-ACER-NITRO-V15").nombre("Acer Notebook Gamer Nitro V15").marca("Acer").modelo("Nitro V15").descripcion("AMD Ryzen 5 7535HS, 16GB RAM, SSD 512GB, RTX 3050 6GB").precio(854990).precioAnterior(999990).stock(30).imagen("/static/img/productos/Acer_Notebook_Gamer_Nitro_V15.jpg").esOferta(true).porcentajeDescuento(15).idCategoria(5L).nombreCategoria("Computadores").activo(true).build(),
                Producto.builder().sku("PC-AMD-R75700G").nombre("PC Gamer AMD Ryzen 7 5700G").marca("AMD").modelo("Ryzen 7 5700G").descripcion("AMD Ryzen 7 5700G con Radeon Vega 8, 16GB RAM DDR4, SSD 500GB").precio(652990).precioAnterior(710000).stock(40).imagen("/static/img/productos/PC_Gamer_AMD_Ryzen_7_5700G.jpg").esOferta(false).idCategoria(5L).nombreCategoria("Computadores").activo(true).build(),
                Producto.builder().sku("PC-MSI-DRACO").nombre("PC Gamer Powered by MSI Draco").marca("MSI").modelo("Draco").descripcion("AMD Ryzen 5 5500, RTX 3050 6GB, 16GB RAM DDR4, Fuente 600W").precio(785000).precioAnterior(890000).stock(28).imagen("/static/img/productos/PC_Gamer_Powered_by_MSI_Draco.jpg").esOferta(true).porcentajeDescuento(12).idCategoria(5L).nombreCategoria("Computadores").activo(true).build(),
                Producto.builder().sku("PC-INTEL-I514400").nombre("PC Gamer Intel Core i5 14400").marca("Intel").modelo("Core i5-14400").descripcion("Intel i5 14400, RTX 3060 12GB, 16GB DDR5, SSD NVMe 1TB").precio(1170000).precioAnterior(1290000).stock(22).imagen("/static/img/productos/PC_Gamer_Intel_i5_14400.jpg").esOferta(false).idCategoria(5L).nombreCategoria("Computadores").activo(true).build(),
                Producto.builder().sku("NB-LENOVO-LEGION-5").nombre("Lenovo Legion Pro 5 Gen 9").marca("Lenovo").modelo("Legion Pro 5").descripcion("Intel Core i9-14900HX, RTX 4070 8GB, 32GB RAM DDR5, SSD 1TB 240Hz").precio(1999990).precioAnterior(2249990).stock(18).imagen("/static/img/productos/laptop_Lenovo_Legion_Pro_5.jpg").esOferta(true).porcentajeDescuento(11).idCategoria(5L).nombreCategoria("Computadores").activo(true).build(),
                Producto.builder().sku("NB-ASUS-ZEPHYRUS-16").nombre("Asus ROG Zephyrus Duo 16 Doble Pantalla").marca("ASUS").modelo("ROG Zephyrus Duo 16").descripcion("Pantalla 4K Mini-LED + Pantalla táctil ScreenPad Plus, 64GB RAM, SSD 2TB, RTX 4090").precio(3599990).precioAnterior(3899990).stock(8).imagen("/static/img/productos/Asus_ROG_Zephyr_us_Duo_16.jpg").esOferta(false).idCategoria(5L).nombreCategoria("Computadores").activo(true).build(),
                Producto.builder().sku("AUD-HP-DHE-8005").nombre("Auricular Gamer HP DHE-8005 Metal RGB").marca("HP").modelo("DHE-8005 Metal").descripcion("Diadema metálica autoajustable, drivers de 50mm, micrófono omnidireccional").precio(20490).precioAnterior(29990).stock(120).imagen("/static/img/productos/Auricular_Gamer_HP_DHE-8005.jpg").esOferta(true).porcentajeDescuento(32).idCategoria(6L).nombreCategoria("Periféricos").activo(true).build(),
                Producto.builder().sku("AUD-HP-DHE-8006").nombre("Audífonos Gamer HP DHE-8006 Surround 7.1").marca("HP").modelo("DHE-8006").descripcion("Conexión USB, audio posicional 7.1 envolvente, iluminación RGB gamer").precio(24990).precioAnterior(34990).stock(95).imagen("/static/img/productos/HP_DHE-8006.jpg").esOferta(true).porcentajeDescuento(28).idCategoria(6L).nombreCategoria("Periféricos").activo(true).build(),
                Producto.builder().sku("CPU-AMD-R55500").nombre("Procesador AMD Ryzen 5 5500").marca("AMD").modelo("Ryzen 5 5500").descripcion("6 núcleos / 12 hilos, 3.6GHz base / 4.2GHz boost, Socket AM4").precio(102990).precioAnterior(119990).stock(80).imagen("/static/img/productos/AMD_Ryzen_5_5500.jpg").esOferta(true).porcentajeDescuento(14).idCategoria(2L).nombreCategoria("Procesadores").activo(true).build(),
                Producto.builder().sku("GPU-NV-RTX3050").nombre("Tarjeta Gráfica GeForce RTX 3050 6GB").marca("NVIDIA").modelo("GeForce RTX 3050").descripcion("6GB GDDR6, arquitectura Ampere, Ray Tracing y DLSS").precio(209990).precioAnterior(245990).stock(45).imagen("/static/img/productos/GeoForce_RTX_3050_6GB.jpg").esOferta(true).porcentajeDescuento(15).idCategoria(1L).nombreCategoria("Tarjetas Gráficas").activo(true).build(),
                Producto.builder().sku("RAM-KINGSTON-16G").nombre("Memoria RAM DDR4 16GB (2x8GB) 3200MHz").marca("Kingston").modelo("DDR4 16GB").descripcion("Kit Dual Channel 2x8GB, disipador de aluminio negro").precio(47990).precioAnterior(55990).stock(110).imagen("/static/img/productos/RAM_DDR4_6GB_(2x8GB).jpg").esOferta(false).idCategoria(3L).nombreCategoria("Memorias RAM").activo(true).build(),
                Producto.builder().sku("SSD-KINGSTON-512G").nombre("Disco Sólido SSD M.2 NVMe 512GB PCIe Gen3").marca("Kingston").modelo("M.2 NVMe 512GB").descripcion("Lectura hasta 2100 MB/s, escritura hasta 1700 MB/s").precio(41990).precioAnterior(49990).stock(90).imagen("/static/img/productos/Disco_Sólido_SSD_M.2_NVMe.jpg").esOferta(true).porcentajeDescuento(16).idCategoria(4L).nombreCategoria("Almacenamiento").activo(true).build()
        ));
        log.info("producto-service: 19 productos oficiales creados con categorias e imagenes alineadas.");
    }
}
