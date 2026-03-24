package lumato.desarrolo.tiendavirtual.service;


import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.core.MPRequestOptions;
import com.mercadopago.resources.payment.Payment;
import lumato.desarrolo.tiendavirtual.model.DetallePedido;
import lumato.desarrolo.tiendavirtual.model.Pedido;
import lumato.desarrolo.tiendavirtual.model.Producto;
import lumato.desarrolo.tiendavirtual.model.enums.EstadoPedido;
import lumato.desarrolo.tiendavirtual.repository.PedidoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MercadoPagoServiceImpTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private ProductoService productoService;

    @InjectMocks
    private MercadoPagoServiceImp mercadoPagoService;

    private Pedido pedidoFalso;
    private Producto productoFalso;
    private DetallePedido detalleFalso;

    @BeforeEach
    void setUp() {
        // Armamos el escenario completo del carrito
        productoFalso = new Producto();
        productoFalso.setId(100L);
        productoFalso.setNombre("Perfume de prueba");

        detalleFalso = new DetallePedido();
        detalleFalso.setProducto(productoFalso);
        detalleFalso.setCantidad(2);

        pedidoFalso = new Pedido();
        pedidoFalso.setId(1L);
        pedidoFalso.setEstado(EstadoPedido.PENDIENTE); // Aún no se pagó
        pedidoFalso.setDetalles(List.of(detalleFalso));
    }

    // ==========================================
    // TEST 1: Pago Aprobado Descuenta Stock
    // ==========================================
    @Test
    void procesarNotificacion_ConPagoAprobado_DebeCambiarEstadoYDescontarStock() throws Exception {
        // 1. Arrange (Preparar)
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedidoFalso));

        // Mockeamos la respuesta de Mercado Pago para que diga que está "approved"
        Payment mockPayment = mock(Payment.class);
        when(mockPayment.getStatus()).thenReturn("approved");
        when(mockPayment.getExternalReference()).thenReturn("1"); // ID del pedido

        // MAGIA NEGRA DE MOCKITO: Interceptamos el "new PaymentClient()"
        try (MockedConstruction<PaymentClient> mockedClient = mockConstruction(PaymentClient.class,
                (mock, context) -> {
                    when(mock.get(anyLong(), any(MPRequestOptions.class))).thenReturn(mockPayment);
                })) {

            // 2. Act (Ejecutar)
            mercadoPagoService.procesarNotificacion(123456789L);

            // 3. Assert (Verificar)
            // Verificamos que el estado del pedido haya cambiado
            assert(pedidoFalso.getEstado() == EstadoPedido.PAGADO);

            // Verificamos que se haya descontado el stock exacto (ID 100, Cantidad 2)
            verify(productoService, times(1)).descontarStock(100L, 2);

            // Verificamos que se haya guardado en la base de datos
            verify(pedidoRepository, times(1)).save(pedidoFalso);
        }
    }

    // ==========================================
    // TEST 2: Pago Ya Procesado (Evitar descontar el doble de stock)
    // ==========================================
    @Test
    void procesarNotificacion_ConPedidoYaPagado_NoDebeDescontarStockDeNuevo() throws Exception {
        // 1. Arrange: Cambiamos el estado a PAGADO desde el principio
        pedidoFalso.setEstado(EstadoPedido.PAGADO);
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedidoFalso));

        Payment mockPayment = mock(Payment.class);
        when(mockPayment.getStatus()).thenReturn("approved");
        when(mockPayment.getExternalReference()).thenReturn("1");

        try (MockedConstruction<PaymentClient> mockedClient = mockConstruction(PaymentClient.class,
                (mock, context) -> {
                    when(mock.get(anyLong(), any(MPRequestOptions.class))).thenReturn(mockPayment);
                })) {

            // 2. Act
            mercadoPagoService.procesarNotificacion(123456789L);

            // 3. Assert: Verificamos que el sistema detectó que ya estaba pagado y NO hizo nada
            verify(productoService, never()).descontarStock(anyLong(), anyInt());
            verify(pedidoRepository, never()).save(any(Pedido.class));
        }
    }
}