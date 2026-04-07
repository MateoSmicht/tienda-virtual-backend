package lumato.desarrolo.tiendavirtual.service;

import lumato.desarrolo.tiendavirtual.model.Pedido;
import lumato.desarrolo.tiendavirtual.repository.PedidoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

import static lumato.desarrolo.tiendavirtual.model.enums.EstadoPedido.CANCELADO;
import static lumato.desarrolo.tiendavirtual.model.enums.EstadoPedido.PENDIENTE;
import static lumato.desarrolo.tiendavirtual.model.enums.MetodoPago.MERCADO_PAGO;

@Service
public class PedidoCleanupService {

    @Autowired
    private PedidoRepository pedidoRepository;
    @Autowired
    private PedidoService service;

    // Se ejecuta automáticamente cada 5 minutos (5 * 60 * 1000 ms)
    @Scheduled(fixedRate = 300000)
    @Transactional
    public void cancelarPedidosHuerfanos() {
        // Calculamos la hora límite: hace 16 minutos porque el link se vence cada 15
        LocalDateTime limite = LocalDateTime.now().minusMinutes(16);

        // Buscamos pedidos "PENDIENTES" que sean de "MERCADO_PAGO" y sean más viejos que el límite
        List<Pedido> pedidosAbandonados = pedidoRepository
                .findByEstadoAndMetodoPagoAndFechaPedidoBefore(PENDIENTE, MERCADO_PAGO, limite);

        for (Pedido pedido : pedidosAbandonados) {
            service.cambiarEstadoPedido(pedido.getId(),CANCELADO);

            pedidoRepository.save(pedido);
            System.out.println("Pedido huérfano #" + pedido.getId() + " cancelado por timeout.");
        }
    }
}