package lumato.desarrolo.tiendavirtual.service;

import lumato.desarrolo.tiendavirtual.dto.ItemCarritoDTO;
import lumato.desarrolo.tiendavirtual.dto.PedidoRequestDTO;
import lumato.desarrolo.tiendavirtual.dto.PedidoResponseDTO;
import lumato.desarrolo.tiendavirtual.exception.StockInsuficienteException;
import lumato.desarrolo.tiendavirtual.model.Pedido;
import lumato.desarrolo.tiendavirtual.model.Producto;
import lumato.desarrolo.tiendavirtual.model.Usuario;
import lumato.desarrolo.tiendavirtual.model.enums.EstadoPedido;
import lumato.desarrolo.tiendavirtual.model.enums.MetodoPago;
import lumato.desarrolo.tiendavirtual.repository.PedidoRepository;
import lumato.desarrolo.tiendavirtual.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PedidoServiceImpTest {

    // 1. Mockeamos TODAS las dependencias que usa el PedidoService
    @Mock private PedidoRepository pedidoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ProductoService productoService;
    @Mock private EmailService emailService;
    @Mock private MercadoPagoService mercadoPagoService;

    // 2. Inyectamos los mocks en el servicio real
    @InjectMocks
    private PedidoServiceImp pedidoService;

    private Usuario usuarioFalso;
    private Producto productoFalso;
    private PedidoRequestDTO requestFalso;

    @BeforeEach
    void setUp() {
        // Preparamos un usuario
        usuarioFalso = new Usuario();
        usuarioFalso.setId(1L);
        usuarioFalso.setNombre("Mateo");
        usuarioFalso.setEmail("mateo@test.com");

        // Preparamos un producto (Teclado a $5000 con 10 en stock)
        productoFalso = new Producto();
        productoFalso.setId(100L);
        productoFalso.setNombre("Teclado Mecánico");
        productoFalso.setPrecio(5000.0);
        productoFalso.setControlarStock(true);
        productoFalso.setStock(10);
        productoFalso.setEsOferta(false);

        // Preparamos el Carrito (DTO)
        requestFalso = new PedidoRequestDTO();
        requestFalso.setUsuarioId(1L);
        requestFalso.setMetodoPago(MetodoPago.MERCADO_PAGO);

        ItemCarritoDTO item = new ItemCarritoDTO();
        item.setProductoId(100L);
        item.setCantidad(2); // Queremos comprar 2 teclados

        requestFalso.setItems(List.of(item));
    }

    @Test
    void procesarNuevaCompra_ConStockSuficiente_CalculaTotalYGeneraLink() {
        // ARRANGE (Preparar)
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioFalso));
        when(productoService.obtenerPorId(100L)).thenReturn(productoFalso);

        // Simulamos que al guardar el pedido, MySQL le asigna el ID 50 y estado PENDIENTE
        Pedido pedidoGuardado = new Pedido();
        pedidoGuardado.setId(50L);
        pedidoGuardado.setEstado(EstadoPedido.PENDIENTE);
        pedidoGuardado.setMetodoPago(MetodoPago.MERCADO_PAGO);
        pedidoGuardado.setTotal(10000.0); // 2 teclados x 5000
        when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedidoGuardado);

        // Simulamos la respuesta de Mercado Pago
        when(mercadoPagoService.crearLinkDePago(any(Pedido.class))).thenReturn("https://mercadopago.com/link-falso");

        // ACT (Actuar)
        PedidoResponseDTO response = pedidoService.procesarNuevaCompra(requestFalso);

        // ASSERT (Afirmar)
        assertNotNull(response);
        assertEquals(10000.0, response.getTotal(), "El total debe ser 10000 (5000 * 2)");
        assertEquals("https://mercadopago.com/link-falso", response.getLinkDePago(), "Debe devolver el link de MP");

        // Verificamos que no se haya restado stock acá (como arreglamos antes)
        verify(productoService, never()).descontarStock(anyLong(), anyInt());
        // Verificamos que se haya intentado mandar el mail
        verify(emailService, times(1)).enviarAvisoDeNuevoPedido(any(Pedido.class));
    }

    @Test
    void procesarNuevaCompra_SinStockSuficiente_LanzaExcepcionYFrenaTodo() {
        // ARRANGE
        // El cliente quiere 20 teclados, pero solo hay 10
        requestFalso.getItems().get(0).setCantidad(20);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioFalso));
        when(productoService.obtenerPorId(100L)).thenReturn(productoFalso);

        // ACT & ASSERT
        StockInsuficienteException ex = assertThrows(StockInsuficienteException.class, () -> {
            pedidoService.procesarNuevaCompra(requestFalso);
        });

        assertTrue(ex.getMessage().contains("No hay stock suficiente"));

        // Verificamos que al fallar, NUNCA se guardó el pedido ni se generó link de pago
        verify(pedidoRepository, never()).save(any(Pedido.class));
        verify(mercadoPagoService, never()).crearLinkDePago(any(Pedido.class));
    }
}