package lumato.desarrolo.tiendavirtual.service;


import lumato.desarrolo.tiendavirtual.dto.NotificacionPedidoDTO;
import lumato.desarrolo.tiendavirtual.dto.PedidoRequestDTO;
import lumato.desarrolo.tiendavirtual.dto.PedidoResponseDTO;
import lumato.desarrolo.tiendavirtual.model.Pedido;
import lumato.desarrolo.tiendavirtual.model.enums.EstadoPedido;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;

public interface PedidoService {
    PedidoResponseDTO procesarNuevaCompra(PedidoRequestDTO request);
    List<Pedido> obtenerTodosLosPedidos();
    Pedido obtenerPedidoPorId(Long id);
    Pedido cambiarEstadoPedido(Long id, EstadoPedido nuevoEstado);
    void eliminarPedido(Long id);
    List<Pedido> obtenerPedidosPorRangoDeFechas(LocalDateTime inicio, LocalDateTime fin);
    List<Pedido> obtenerUltimos5Pedidos();
    Page<Pedido> obtenerPedidosPaginadosYFiltrados(int page, int size, EstadoPedido estado, LocalDateTime inicio, LocalDateTime fin);
    List<NotificacionPedidoDTO> obtenerUltimasNotificaciones();
}
