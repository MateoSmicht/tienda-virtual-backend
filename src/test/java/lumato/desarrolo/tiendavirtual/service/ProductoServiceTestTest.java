package lumato.desarrolo.tiendavirtual.service;

import lumato.desarrolo.tiendavirtual.exception.StockInsuficienteException;
import lumato.desarrolo.tiendavirtual.model.Producto;
import lumato.desarrolo.tiendavirtual.repository.ProductoRepository;
import org.junit.function.ThrowingRunnable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

// Le decimos a JUnit que vamos a usar Mockito para simular cosas
@ExtendWith(MockitoExtension.class)
class ProductoServiceTestTest {

    @Mock // Simulamos la base de datos (NO va a tocar MySQL)
    private ProductoRepository productoRepository;

    @InjectMocks // Este es el servicio REAL que vamos a probar
    private ProductoServiceImp productoService;

    private Producto productoFalso;

    // Este método se ejecuta ANTES de cada @Test para preparar el terreno
    @BeforeEach
    void setUp() {
        productoFalso = new Producto();
        productoFalso.setId(1L);
        productoFalso.setNombre("Silla Gamer");
        productoFalso.setControlarStock(true);
        productoFalso.setStock(10);
        productoFalso.setPrecio(1000.0);
    }

    @Test
    void cuandoHayStock_descontarStock_RestaCorrectamente() {
        // 1. Arrange (Preparar): Le enseñamos al Mock qué hacer cuando lo llamen
        when(productoRepository.findById(1L)).thenReturn(Optional.of(productoFalso));
        when(productoRepository.save(any(Producto.class))).thenReturn(productoFalso);

        // 2. Act (Actuar): Ejecutamos el método real
        Producto resultado = productoService.descontarStock(1L, 3);

        // 3. Assert (Afirmar): Verificamos que la matemática no mienta
        assertEquals(7, resultado.getStock(), "El stock debería ser 7 (10 - 3)");

        // Verificamos que nuestro código haya llamado al "save" de la base de datos
        verify(productoRepository, times(1)).save(productoFalso);
    }

    @Test
    void cuandoNoHayStock_descontarStock_LanzaExcepcion() {
        // 1. Arrange: Preparamos la BD falsa
        when(productoRepository.findById(1L)).thenReturn(Optional.of(productoFalso));

        // 2 & 3. Act y Assert a la vez: Verificamos que ESTALLE con la excepción correcta
        assertThrows(StockInsuficienteException.class, (ThrowingRunnable) () -> {
            productoService.descontarStock(1L, 15); // Intentamos comprar 15, pero hay 10
        });

        // Verificamos que, como falló, NUNCA haya intentado guardar en BD
        verify(productoRepository, never()).save(any(Producto.class));
    }

    @Test
    void aplicarOferta_ConPrecioFijo_AplicaCorrectamente() {
        // 1. Arrange
        when(productoRepository.findById(1L)).thenReturn(Optional.of(productoFalso));
        when(productoRepository.save(any(Producto.class))).thenReturn(productoFalso);

        // 2. Act: Le decimos que en vez de $1000, ahora vale $800 fijos
        Producto resultado = productoService.aplicarOferta(1L, null, 800.0);

        // 3. Assert
        assertTrue(resultado.getEsOferta(), "La banderita de esOferta debería estar en true");
        assertEquals(800.0, resultado.getPrecioOferta(), "El precio de oferta debería ser 800.0");
        verify(productoRepository, times(1)).save(productoFalso);
    }

    @Test
    void aplicarOferta_ConPorcentaje_CalculaYRedondeaCorrectamente() {
        // 1. Arrange
        when(productoRepository.findById(1L)).thenReturn(Optional.of(productoFalso));
        when(productoRepository.save(any(Producto.class))).thenReturn(productoFalso);

        // 2. Act: Le aplicamos un 15% de descuento a los $1000 base
        Producto resultado = productoService.aplicarOferta(1L, 15.0, null);

//        // 3. Assert: 15% de 1000 es 150. El precio final debería ser 850.
        assertTrue(resultado.getEsOferta());
        assertEquals(850.0, resultado.getPrecioOferta(), "El cálculo del porcentaje falló");
    }

    @Test
    void aplicarOferta_SinPrecioBase_LanzaExcepcion() {
        // 1. Arrange: Le "borramos" el precio base al producto falso para forzar el error
        productoFalso.setPrecio(null);
        when(productoRepository.findById(1L)).thenReturn(Optional.of(productoFalso));

        // 2 & 3. Act y Assert
        RuntimeException excepcion = assertThrows(RuntimeException.class, (ThrowingRunnable) () -> {
            productoService.aplicarOferta(1L, 10.0, null);
        });

        // Verificamos que el mensaje de error sea exactamente el que programaste
        assertEquals("El producto no tiene un precio base para aplicar oferta.", excepcion.getMessage());

        // Verificamos que no se haya guardado basura en la base de datos
        verify(productoRepository, never()).save(any(Producto.class));
    }

}