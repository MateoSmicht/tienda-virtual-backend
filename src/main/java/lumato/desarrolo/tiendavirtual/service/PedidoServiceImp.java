package lumato.desarrolo.tiendavirtual.service;

import lumato.desarrolo.tiendavirtual.dto.ItemCarritoDTO;
import lumato.desarrolo.tiendavirtual.dto.PedidoRequestDTO;
import lumato.desarrolo.tiendavirtual.dto.PedidoResponseDTO;
import lumato.desarrolo.tiendavirtual.exception.StockInsuficienteException;
import lumato.desarrolo.tiendavirtual.model.DetallePedido;
import lumato.desarrolo.tiendavirtual.model.Pedido;
import lumato.desarrolo.tiendavirtual.model.Producto;
import lumato.desarrolo.tiendavirtual.model.Usuario;
import lumato.desarrolo.tiendavirtual.model.enums.EstadoPedido;
import lumato.desarrolo.tiendavirtual.repository.PedidoRepository;
import lumato.desarrolo.tiendavirtual.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PedidoServiceImp implements PedidoService {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProductoService productoService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private MercadoPagoService mercadoPagoService;

    @Override
    @Transactional
    public PedidoResponseDTO procesarNuevaCompra(PedidoRequestDTO request) {

        // 1. Buscamos al usuario que está comprando
        Usuario usuario = usuarioRepository.findById(request.getUsuarioId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + request.getUsuarioId()));

        // 2. Preparamos el nuevo pedido (Cabecera)
        Pedido pedido = new Pedido();
        pedido.setUsuario(usuario);
        pedido.setMetodoPago(request.getMetodoPago());

        Double totalPedido = 0.0;

        // 3. Procesamos cada producto del carrito
        for (ItemCarritoDTO item : request.getItems()) {

            // SOLUCIÓN: Solo obtenemos el producto para leer sus datos, NO descontamos stock acá
            Producto productoActualizado = productoService.obtenerPorId(item.getProductoId());

            // VALIDACIÓN PREVENTIVA: Miramos si hay stock antes de dejarlo avanzar
            Boolean controla = (productoActualizado.getControlarStock() != null) ? productoActualizado.getControlarStock() : false;
            if (controla) {
                Integer stockActual = (productoActualizado.getStock() != null) ? productoActualizado.getStock() : 0;
                if (stockActual < item.getCantidad()) {
                    throw new StockInsuficienteException("No hay stock suficiente para crear el pedido de: " + productoActualizado.getNombre());
                }
            }

            // 4. Lógica de Precios: ¿Tiene oferta?
            Double precioACobrar = productoActualizado.getPrecio();
            if (productoActualizado.getEsOferta() && productoActualizado.getPrecioOferta() != null) {
                precioACobrar = productoActualizado.getPrecioOferta(); // Congelamos el precio rebajado
            }

            // 5. Armamos la línea de detalle (el ticket)
            DetallePedido detalle = new DetallePedido();
            detalle.setProducto(productoActualizado);
            detalle.setCantidad(item.getCantidad());
            detalle.setPrecioUnitario(precioACobrar); // ¡Precio congelado históricamente!

            // 6. Vinculamos el detalle al pedido y sumamos la plata
            pedido.addDetalle(detalle);
            totalPedido += (precioACobrar * item.getCantidad());
        }

        // 7. Seteamos el total final y guardamos en MySQL
        pedido.setTotal(totalPedido);
        Pedido pedidoGuardado = pedidoRepository.save(pedido);

        // 8. ARMAMOS LA RESPUESTA LIMPIA (El DTO)
        PedidoResponseDTO response = new PedidoResponseDTO();
        response.setIdPedido(pedidoGuardado.getId());
        response.setEstado(pedidoGuardado.getEstado().name());
        response.setTotal(pedidoGuardado.getTotal());
        response.setMetodoPago(pedidoGuardado.getMetodoPago().name());
        response.setFecha(pedidoGuardado.getFechaPedido());
        response.setMensaje("¡Gracias por tu compra " + usuario.getNombre() + "! Te enviaremos los detalles a " + usuario.getEmail());

        try {
            emailService.enviarAvisoDeNuevoPedido(pedidoGuardado);
        } catch (Exception e) {
            System.out.println("Error al enviar el correo: " + e.getMessage());
        }

        // Si el cliente eligió pagar con MP, le fabricamos el link en el momento
        if (pedidoGuardado.getMetodoPago().name().equals("MERCADO_PAGO")) {
            String link = mercadoPagoService.crearLinkDePago(pedidoGuardado);
            response.setLinkDePago(link);
        }

        return response;
    }

    public Page<Pedido> obtenerPedidosPaginadosYFiltrados(int page, int size, EstadoPedido estado, LocalDateTime inicio, LocalDateTime fin) {
        Pageable pageable = PageRequest.of(page, size);
        return pedidoRepository.buscarPedidosConFiltros(estado, inicio, fin, pageable);
    }

    @Override
    public List<Pedido> obtenerTodosLosPedidos() {
        return pedidoRepository.findAll();
    }

    @Override
    public Pedido obtenerPedidoPorId(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));
    }

    @Override
    @Transactional // Agregamos esto para que si falla el stock, no cambie el estado
    public Pedido cambiarEstadoPedido(Long id, EstadoPedido nuevoEstado) {
        // 1. Buscamos el pedido
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se encontró el pedido con ID: " + id));

        // 2. Verificamos si lo estamos pasando a PAGADO y antes NO estaba pagado
        if (nuevoEstado == EstadoPedido.PAGADO && pedido.getEstado() != EstadoPedido.PAGADO) {

            System.out.println("Pago manual detectado. Descontando stock del pedido #" + id);

            // Recorremos los productos y descontamos
            pedido.getDetalles().forEach(detalle -> {
                productoService.descontarStock(
                        detalle.getProducto().getId(),
                        detalle.getCantidad()
                );
            });
        }

        // Opcional: ¿Qué pasa si el cliente cancela o no te paga la transferencia?
        // Le devolvemos el stock a la estantería.
        if (nuevoEstado == EstadoPedido.CANCELADO && pedido.getEstado() == EstadoPedido.PAGADO) {
            System.out.println("🔄 Pedido cancelado. Devolviendo stock...");
            pedido.getDetalles().forEach(detalle -> {
                productoService.ingresarStock(
                        detalle.getProducto().getId(),
                        detalle.getCantidad(),
                        detalle.getProducto().getPpp() // Mantenemos el mismo valor
                );
            });
        }

        // 3. Seteamos el nuevo estado
        pedido.setEstado(nuevoEstado);

        // 4. Guardamos
        return pedidoRepository.save(pedido);
    }

    @Override
    public void eliminarPedido(Long id) {
        pedidoRepository.deleteById(id);
    }
    @Override
    public List<Pedido> obtenerPedidosPorRangoDeFechas(LocalDateTime inicio, LocalDateTime fin) {
        return pedidoRepository.findByFechaPedidoBetween(inicio, fin);
    }
    @Override
    public List<Pedido> obtenerUltimos5Pedidos() {
        return pedidoRepository.findTop5ByOrderByFechaPedidoDesc();
    }
}
