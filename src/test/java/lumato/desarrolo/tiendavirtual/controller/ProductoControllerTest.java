package lumato.desarrolo.tiendavirtual.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lumato.desarrolo.tiendavirtual.exception.CodigoBarraDuplicadoException;
import lumato.desarrolo.tiendavirtual.model.Producto;
import lumato.desarrolo.tiendavirtual.service.ProductoService;
import org.junit.Ignore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;


import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.test.context.bean.override.mockito.MockitoBean; // <-- ¡Este es el nuevo!
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Le decimos a Spring que SOLO levante el contexto web para este Controller
@WebMvcTest(ProductoController.class)
class ProductoControllerTest {

    @Autowired
    private MockMvc mockMvc; // Nuestra herramienta para simular peticiones HTTP (Postman invisible)

    @Autowired
    private ObjectMapper objectMapper; // Para convertir objetos Java a JSON y viceversa

    @MockitoBean
    private ProductoService productoService; // Mockeamos el servicio para no tocar la BD

    private Producto productoFalso;

    @BeforeEach
    void setUp() {
        productoFalso = new Producto();
        productoFalso.setId(1L);
        productoFalso.setNombre("Monitor 24 pulgadas");
        productoFalso.setCodigoBarra("123456789");
        productoFalso.setPrecio(150000.0);
    }

    @Test
    void crearProducto_ConDatosValidos_DebeRetornarStatus200() throws Exception {
        // 1. Arrange
        when(productoService.guardarProducto(any(Producto.class))).thenReturn(productoFalso);

        // Convertimos nuestro producto Java a un String JSON para mandarlo en el Body
        String productoJson = objectMapper.writeValueAsString(productoFalso);

        // 2 & 3. Act & Assert: Hacemos un POST
        mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productoJson)) // Le metemos el JSON en el cuerpo
                .andExpect(status().isOk()) // Dependiendo de tu controller, podría ser isCreated() (201)
                .andExpect(jsonPath("$.nombre").value("Monitor 24 pulgadas"));
    }

    @Test
    void crearProducto_ConCodigoDuplicado_DebeRetornarStatus409Conflict() throws Exception {
        // 1. Arrange: Simulamos que el Service lanza la excepción que armamos antes
        when(productoService.guardarProducto(any(Producto.class)))
                .thenThrow(new CodigoBarraDuplicadoException("El código ya existe"));

        String productoJson = objectMapper.writeValueAsString(productoFalso);

        // 2 & 3. Act & Assert: Hacemos el POST y esperamos que el GlobalExceptionHandler ataje el error
        mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productoJson))
                .andExpect(status().isConflict()); // Esperamos el 409 Conflict que le pusimos a la excepción!
    }
}