package cl.pchardware.boleta.service;

import cl.pchardware.boleta.client.DetalleClient;
import cl.pchardware.boleta.client.ProductoClient;
import cl.pchardware.boleta.dto.BoletaRequest;
import cl.pchardware.boleta.dto.BoletaResponse;
import cl.pchardware.boleta.dto.DetalleBoletaDTO;
import cl.pchardware.boleta.exception.BadRequestException;
import cl.pchardware.boleta.exception.ResourceNotFoundException;
import cl.pchardware.boleta.model.Boleta;
import cl.pchardware.boleta.repository.BoletaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BoletaService {

    private final BoletaRepository boletaRepository;
    private final ProductoClient productoClient;
    private final DetalleClient detalleClient;

    @Transactional(readOnly = true)
    public List<BoletaResponse> findAll() {
        return boletaRepository.findAllByOrderByFechaEmisionDesc().stream()
                .map(this::enrichWithDetalles)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BoletaResponse findById(Long id) {
        Boleta b = boletaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Boleta no encontrada con ID: " + id));
        return enrichWithDetalles(b);
    }

    @Transactional(readOnly = true)
    public BoletaResponse findByNumero(String numero) {
        Boleta b = boletaRepository.findByNumeroBoleta(numero)
                .orElseThrow(() -> new ResourceNotFoundException("Boleta no encontrada con número: " + numero));
        return enrichWithDetalles(b);
    }

    @Transactional(readOnly = true)
    public List<BoletaResponse> findMisBoletas(String email, Long idUsuario) {
        List<Boleta> lista;
        if (idUsuario != null) {
            lista = boletaRepository.findByIdUsuarioOrderByFechaEmisionDesc(idUsuario);
        } else if (email != null && !email.isBlank()) {
            lista = boletaRepository.findByEmailClienteOrderByFechaEmisionDesc(email);
        } else {
            lista = List.of();
        }
        return lista.stream().map(this::enrichWithDetalles).collect(Collectors.toList());
    }

    @Transactional
    public BoletaResponse create(BoletaRequest request, String emailAuth, Long userIdAuth) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BadRequestException("La boleta debe incluir al menos un producto.");
        }

        // 1. Validar stock de todos los productos primero
        List<ProductoClient.ProductoDTO> productosValidados = new ArrayList<>();
        for (DetalleBoletaDTO item : request.getItems()) {
            ProductoClient.ProductoDTO prod = productoClient.findById(item.getIdProducto());
            if (prod.getStock() < item.getCantidad()) {
                throw new BadRequestException("Stock insuficiente para: " + prod.getNombre() + 
                        ". Disponibles: " + prod.getStock() + ", solicitados: " + item.getCantidad());
            }
            productosValidados.add(prod);
        }

        // 2. Descontar stock
        for (int i = 0; i < request.getItems().size(); i++) {
            DetalleBoletaDTO item = request.getItems().get(i);
            productoClient.updateStock(item.getIdProducto(), -item.getCantidad());
        }

        // 3. Crear cabecera Boleta
        String numeroBoleta = "BOL-" + (10000000 + new Random().nextInt(90000000));
        Long finalUserId = userIdAuth != null ? userIdAuth : request.getIdUsuario();
        String finalEmail = emailAuth != null && !emailAuth.isBlank() ? emailAuth : request.getEmailCliente();

        Boleta boleta = Boleta.builder()
                .numeroBoleta(numeroBoleta)
                .fechaEmision(LocalDateTime.now())
                .subtotal(request.getSubtotal() != null ? request.getSubtotal() : request.getTotal())
                .descuento(request.getDescuento() != null ? request.getDescuento() : 0)
                .total(request.getTotal())
                .metodoPago(request.getMetodoPago() != null ? request.getMetodoPago() : "Webpay Plus / Débito")
                .estado("PAGADA")
                .nombreCliente(request.getNombreCliente() != null ? request.getNombreCliente() : "Cliente General")
                .emailCliente(finalEmail != null ? finalEmail : "cliente@pchardware.cl")
                .rutCliente(request.getRutCliente() != null ? request.getRutCliente() : "")
                .direccionEnvio(request.getDireccionEnvio() != null ? request.getDireccionEnvio() : "Despacho a Domicilio")
                .comuna(request.getComuna() != null ? request.getComuna() : "Santiago")
                .region(request.getRegion() != null ? request.getRegion() : "Región Metropolitana de Santiago")
                .idUsuario(finalUserId)
                .build();

        Boleta guardada = boletaRepository.save(boleta);

        // 4. Construir items de detalle y guardarlos via detalle-service
        List<DetalleBoletaDTO> itemsToSave = new ArrayList<>();
        for (int i = 0; i < request.getItems().size(); i++) {
            DetalleBoletaDTO reqItem = request.getItems().get(i);
            ProductoClient.ProductoDTO prod = productosValidados.get(i);
            int precio = prod.getPrecio();
            int subtotalItem = precio * reqItem.getCantidad();

            itemsToSave.add(DetalleBoletaDTO.builder()
                    .idBoleta(guardada.getId())
                    .idProducto(prod.getId())
                    .nombreProducto(prod.getNombre())
                    .skuProducto(prod.getSku())
                    .cantidad(reqItem.getCantidad())
                    .precioUnitario(precio)
                    .subtotal(subtotalItem)
                    .build());
        }

        List<DetalleBoletaDTO> guardados = detalleClient.saveBatch(itemsToSave);

        return toResponse(guardada, guardados);
    }

    private BoletaResponse enrichWithDetalles(Boleta b) {
        List<DetalleBoletaDTO> detalles = detalleClient.findByBoletaId(b.getId());
        return toResponse(b, detalles);
    }

    private BoletaResponse toResponse(Boleta b, List<DetalleBoletaDTO> detalles) {
        return BoletaResponse.builder()
                .id(b.getId())
                .numeroBoleta(b.getNumeroBoleta())
                .fechaEmision(b.getFechaEmision())
                .subtotal(b.getSubtotal())
                .descuento(b.getDescuento())
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
