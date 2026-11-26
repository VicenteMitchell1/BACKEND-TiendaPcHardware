package cl.pchardware.boleta.service;

import cl.pchardware.boleta.client.DetalleClient;
import cl.pchardware.boleta.client.ProductoClient;
import cl.pchardware.boleta.client.ProductoClient.ItemStock;
import cl.pchardware.boleta.dto.BoletaRequest;
import cl.pchardware.boleta.dto.BoletaResponse;
import cl.pchardware.boleta.dto.BoletaUpdateRequest;
import cl.pchardware.boleta.dto.DetalleBoletaDTO;
import cl.pchardware.boleta.dto.UsuarioActual;
import cl.pchardware.boleta.exception.BadRequestException;
import cl.pchardware.boleta.exception.ConflictException;
import cl.pchardware.boleta.exception.ForbiddenException;
import cl.pchardware.boleta.exception.ResourceNotFoundException;
import cl.pchardware.boleta.exception.UnauthorizedException;
import cl.pchardware.boleta.model.Boleta;
import cl.pchardware.boleta.model.Cupon;
import cl.pchardware.boleta.repository.BoletaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class BoletaService {

    public static final String ANULADA = "ANULADA";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final BoletaRepository boletaRepository;
    private final ProductoClient productoClient;
    private final DetalleClient detalleClient;
    private final CuponService cuponService;

    // ------------------------------------------------------------------ consultas

    @Transactional(readOnly = true)
    public List<BoletaResponse> findAll(UsuarioActual usuario) {
        if (!usuario.esPersonal()) throw new ForbiddenException("Sólo ADMIN y VENDEDOR pueden listar todas las boletas.");
        return conDetalles(boletaRepository.findAllByOrderByFechaEmisionDesc());
    }

    @Transactional(readOnly = true)
    public BoletaResponse findById(Long id, UsuarioActual usuario) {
        Boleta b = buscar(id);
        verificarAcceso(b, usuario);
        return toResponse(b, detalleClient.findByBoletaId(b.getId()));
    }

    @Transactional(readOnly = true)
    public BoletaResponse findByNumero(String numero, UsuarioActual usuario) {
        Boleta b = boletaRepository.findByNumeroBoleta(numero)
                .orElseThrow(() -> new ResourceNotFoundException("Boleta no encontrada con número: " + numero));
        verificarAcceso(b, usuario);
        return toResponse(b, detalleClient.findByBoletaId(b.getId()));
    }

    @Transactional(readOnly = true)
    public List<BoletaResponse> findMisBoletas(UsuarioActual usuario) {
        exigirUsuario(usuario);
        return conDetalles(boletaRepository.findByIdUsuarioOrderByFechaEmisionDesc(usuario.id()));
    }

    // ------------------------------------------------------------------ compra

    /**
     * Emite una boleta. Flujo (patrón saga con compensación):
     * 1. producto-service descuenta el stock de todos los items en una transacción atómica y devuelve los precios.
     * 2. Los montos se calculan aquí con esos precios (se ignoran los montos enviados por el cliente).
     * 3. Se guarda la cabecera y luego los detalles en detalle-service.
     * Si el paso 3 falla, se repone el stock y la transacción local revierte la cabecera: no quedan
     * boletas sin detalle ni stock descontado sin venta.
     */
    @Transactional
    public BoletaResponse create(BoletaRequest request, UsuarioActual usuario) {
        exigirUsuario(usuario);
        // El destino y el cupón se validan ANTES de tocar el stock: un dato inválido no debe descontar nada.
        RegionesDespacho.Destino destino = RegionesDespacho.validar(
                valorO(request.getRegion(), RegionesDespacho.REGION_POR_DEFECTO),
                valorO(request.getComuna(), RegionesDespacho.COMUNA_POR_DEFECTO));
        Cupon cupon = StringUtils.hasText(request.getCodigoCupon())
                ? cuponService.obtenerParaCompra(request.getCodigoCupon()) : null;
        List<ItemStock> items = request.getItems().stream()
                .map(i -> new ItemStock(i.getIdProducto(), i.getCantidad())).toList();

        List<ProductoClient.ProductoDTO> productos = productoClient.descontarStock(items);
        try {
            long subtotal = 0;
            List<DetalleBoletaDTO> lineas = new ArrayList<>();
            for (int i = 0; i < items.size(); i++) {
                ProductoClient.ProductoDTO prod = productos.get(i);
                int cantidad = items.get(i).cantidad();
                long subtotalLinea = (long) prod.getPrecio() * cantidad;
                subtotal += subtotalLinea;
                lineas.add(DetalleBoletaDTO.builder()
                        .idProducto(prod.getId()).nombreProducto(prod.getNombre()).skuProducto(prod.getSku())
                        .cantidad(cantidad).precioUnitario(prod.getPrecio()).subtotal(Math.toIntExact(subtotalLinea))
                        .build());
            }
            int subtotalInt = Math.toIntExact(subtotal);
            int descuento = cupon == null ? 0 : calcularDescuento(subtotalInt, cupon.getPorcentaje());

            Boleta boleta = boletaRepository.saveAndFlush(Boleta.builder()
                    .numeroBoleta(generarNumeroBoleta())
                    .fechaEmision(LocalDateTime.now())
                    .subtotal(subtotalInt)
                    .descuento(descuento)
                    .codigoCupon(cupon == null ? null : cupon.getCodigo())
                    .total(subtotalInt - descuento)
                    .metodoPago(valorO(request.getMetodoPago(), "Webpay Plus / Débito"))
                    .estado("PAGADA")
                    .nombreCliente(valorO(request.getNombreCliente(), "Cliente General"))
                    .emailCliente(usuario.email())
                    .rutCliente(valorO(request.getRutCliente(), ""))
                    .direccionEnvio(valorO(request.getDireccionEnvio(), "Despacho a Domicilio"))
                    .comuna(destino.comuna())
                    .region(destino.region())
                    .idUsuario(usuario.id())
                    .build());

            lineas.forEach(l -> l.setIdBoleta(boleta.getId()));
            List<DetalleBoletaDTO> guardados = detalleClient.saveBatch(lineas);
            return toResponse(boleta, guardados);
        } catch (ArithmeticException e) {
            compensar(items);
            throw new BadRequestException("El monto de la compra excede el máximo permitido.");
        } catch (RuntimeException e) {
            compensar(items);
            throw e;
        }
    }

    // ------------------------------------------------------------------ administración

    /** Edición por ADMIN. Pasar a ANULADA repone el stock; una boleta anulada no se puede reactivar. */
    @Transactional
    public BoletaResponse update(Long id, BoletaUpdateRequest req, UsuarioActual usuario) {
        exigirAdmin(usuario);
        Boleta b = buscar(id);
        // Si se edita el destino, el par resultante (lo nuevo + lo que no cambia) debe ser coherente.
        RegionesDespacho.Destino destino = req.getRegion() == null && req.getComuna() == null ? null
                : RegionesDespacho.validar(
                        req.getRegion() != null ? req.getRegion() : b.getRegion(),
                        req.getComuna() != null ? req.getComuna() : b.getComuna());
        List<DetalleBoletaDTO> detalles = detalleClient.findByBoletaId(id);

        if (StringUtils.hasText(req.getEstado()) && !req.getEstado().equals(b.getEstado())) {
            if (ANULADA.equals(b.getEstado())) {
                throw new ConflictException("La boleta está anulada y no puede cambiar de estado.");
            }
            if (ANULADA.equals(req.getEstado())) {
                productoClient.reponerStock(detalles.stream()
                        .map(d -> new ItemStock(d.getIdProducto(), d.getCantidad())).toList());
            }
            b.setEstado(req.getEstado());
        }
        if (req.getMetodoPago() != null) b.setMetodoPago(req.getMetodoPago());
        if (req.getNombreCliente() != null) b.setNombreCliente(req.getNombreCliente());
        if (req.getRutCliente() != null) b.setRutCliente(req.getRutCliente());
        if (req.getDireccionEnvio() != null) b.setDireccionEnvio(req.getDireccionEnvio());
        if (destino != null) {
            b.setRegion(destino.region());
            b.setComuna(destino.comuna());
        }
        return toResponse(boletaRepository.save(b), detalles);
    }

    /** Sólo se pueden eliminar boletas ANULADAS (su stock ya fue repuesto): una venta vigente no se borra. */
    @Transactional
    public void delete(Long id, UsuarioActual usuario) {
        exigirAdmin(usuario);
        Boleta b = buscar(id);
        if (!ANULADA.equals(b.getEstado())) {
            throw new ConflictException("Sólo se pueden eliminar boletas ANULADAS. Anule la boleta primero (PUT con estado ANULADA).");
        }
        detalleClient.deleteByBoletaId(id);
        boletaRepository.delete(b);
    }

    // ------------------------------------------------------------------ auxiliares

    /** Descuento redondeado al peso más cercano (misma regla que muestra el carrito). */
    static int calcularDescuento(int subtotal, int porcentaje) {
        return (int) Math.round(subtotal * (porcentaje / 100.0));
    }

    private void compensar(List<ItemStock> items) {
        try {
            productoClient.reponerStock(items);
        } catch (RuntimeException ex) {
            // Caso extremo: ambos servicios fallaron. Se deja registro para reponer manualmente.
            log.error("COMPENSACIÓN FALLIDA: reponer manualmente el stock de {}", items, ex);
        }
    }

    private void verificarAcceso(Boleta b, UsuarioActual usuario) {
        if (usuario.esPersonal()) return;
        if (usuario.id() == null || !Objects.equals(b.getIdUsuario(), usuario.id())) {
            throw new ForbiddenException("No tiene permiso para ver esta boleta.");
        }
    }

    private static void exigirUsuario(UsuarioActual usuario) {
        if (usuario.id() == null) throw new UnauthorizedException("Usuario no identificado.");
    }

    private static void exigirAdmin(UsuarioActual usuario) {
        if (!usuario.esAdmin()) throw new ForbiddenException("Operación exclusiva del administrador.");
    }

    private Boleta buscar(Long id) {
        return boletaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Boleta no encontrada con ID: " + id));
    }

    private List<BoletaResponse> conDetalles(List<Boleta> boletas) {
        Map<Long, List<DetalleBoletaDTO>> porBoleta = detalleClient.findByBoletaIds(boletas.stream().map(Boleta::getId).toList());
        return boletas.stream().map(b -> toResponse(b, porBoleta.getOrDefault(b.getId(), List.of()))).toList();
    }

    private String generarNumeroBoleta() {
        for (int intento = 0; intento < 10; intento++) {
            String numero = "BOL-" + (10_000_000 + RANDOM.nextInt(90_000_000));
            if (!boletaRepository.existsByNumeroBoleta(numero)) return numero;
        }
        throw new IllegalStateException("No se pudo generar un número de boleta único.");
    }

    private static String valorO(String valor, String porDefecto) {
        return StringUtils.hasText(valor) ? valor.trim() : porDefecto;
    }

    private BoletaResponse toResponse(Boleta b, List<DetalleBoletaDTO> detalles) {
        return BoletaResponse.builder()
                .id(b.getId())
                .numeroBoleta(b.getNumeroBoleta())
                .fechaEmision(b.getFechaEmision())
                .subtotal(b.getSubtotal())
                .descuento(b.getDescuento())
                .codigoCupon(b.getCodigoCupon())
                .total(b.getTotal())
                .metodoPago(b.getMetodoPago())
                .estado(b.getEstado())
                .nombreCliente(b.getNombreCliente())
                .emailCliente(b.getEmailCliente())
                .rutCliente(b.getRutCliente())
                .direccionEnvio(b.getDireccionEnvio())
                .comuna(b.getComuna())
                .region(b.getRegion())
                .idUsuario(b.getIdUsuario())
                .detalles(detalles)
                .build();
    }
}
