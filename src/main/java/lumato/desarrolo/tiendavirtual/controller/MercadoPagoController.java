package lumato.desarrolo.tiendavirtual.controller;

import lumato.desarrolo.tiendavirtual.service.MercadoPagoService;
import lumato.desarrolo.tiendavirtual.service.PedidoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/pagos")
public class MercadoPagoController {

    @Autowired
    private PedidoService pedidoService;
    @Autowired
    private MercadoPagoService mercadoPagoService;
    @PostMapping("/notificacion")
    public ResponseEntity<Void> recibirNotificacion(
            @RequestBody(required = false) Map<String, Object> payload,
            @RequestParam Map<String, String> allParams) {

        // 1. Headers para saltar el bloqueo de ngrok
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.add("ngrok-skip-browser-warning", "true");

        try {
            System.out.println("📩 ¡LLEGÓ UNA NOTIFICACIÓN REAL!");
            System.out.println("Parámetros URL: " + allParams);
            System.out.println("Payload Body: " + payload);

            String paymentId = null;

            // Intentamos sacar el ID de todos los lugares posibles donde MP suele esconderlo
            if (allParams.containsKey("id")) paymentId = allParams.get("id");
            else if (allParams.containsKey("data.id")) paymentId = allParams.get("data.id");
            else if (payload != null && payload.containsKey("data")) {
                Map<String, Object> data = (Map<String, Object>) payload.get("data");
                paymentId = data.get("id").toString();
            }

            if (paymentId != null && !paymentId.equals("123456")) { // Ignoramos el test manual
                System.out.println("🔎 PROCESANDO PAGO REAL ID: " + paymentId);
                mercadoPagoService.procesarNotificacion(Long.parseLong(paymentId));
            }

        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
        }

        // Siempre devolvemos 200 para que MP no se trabe
        return ResponseEntity.ok().headers(headers).build();
    }
}