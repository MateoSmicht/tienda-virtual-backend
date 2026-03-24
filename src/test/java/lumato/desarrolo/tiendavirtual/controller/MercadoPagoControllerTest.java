package lumato.desarrolo.tiendavirtual.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import lumato.desarrolo.tiendavirtual.service.MercadoPagoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.HashMap;
import java.util.Map;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false) // Apagamos la seguridad para el webhook
class MercadoPagoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    // Mockeamos el servicio para no pegarle de verdad a la API de Mercado Pago ni a tu BD
    @MockitoBean
    private MercadoPagoService mercadoPagoService;

    // TEST 1: Mercado Pago manda el ID en la URL
    @Test
    void recibirNotificacion_ConIdEnParametrosUrl_DebeLlamarAlServicio_Y_Retornar200() throws Exception {
        // Arrange: Le decimos al mock que no haga nada cuando lo llamen (porque devuelve void)
        doNothing().when(mercadoPagoService).procesarNotificacion(987654L);

        // Act & Assert
        mockMvc.perform(post("/api/pagos/notificacion?data.id=987654")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()); // Siempre debe devolver 200 para que MP no se trabe

        // Verify: Esta es la magia de Mockito. Comprobamos que tu controller realmente
        // haya extraído el 987654 de la URL y se lo haya pasado al servicio.
        verify(mercadoPagoService).procesarNotificacion(987654L);
    }

    // ==========================================
    // TEST 2: Mercado Pago manda el ID en el Body (JSON)
    // ==========================================
    @Test
    void recibirNotificacion_ConIdEnElBody_DebeLlamarAlServicio_Y_Retornar200() throws Exception {
        // Arrange
        doNothing().when(mercadoPagoService).procesarNotificacion(555555L);

        // Armamos el JSON tramposo que suele mandar Mercado Pago
        Map<String, Object> data = new HashMap<>();
        data.put("id", "555555");
        Map<String, Object> payload = new HashMap<>();
        payload.put("data", data);

        String jsonPayload = objectMapper.writeValueAsString(payload);

        // Act & Assert
        mockMvc.perform(post("/api/pagos/notificacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk());

        // Verify: Verificamos que tu controller supo bucear en el JSON y sacar el 555555
        verify(mercadoPagoService).procesarNotificacion(555555L);
    }
}