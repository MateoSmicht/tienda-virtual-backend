package lumato.desarrolo.tiendavirtual.service;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.*;
import com.mercadopago.core.MPRequestOptions;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import lumato.desarrolo.tiendavirtual.model.Pedido;
import lumato.desarrolo.tiendavirtual.model.enums.EstadoPedido;
import lumato.desarrolo.tiendavirtual.repository.PedidoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class MercadoPagoServiceImp implements MercadoPagoService {
    @Value("${mercado.pago.webhook.url}")
    private String webhookUrl;
    @Autowired
    PedidoRepository pedidoRepository;
    @Autowired
    private ProductoService productoService;
    @Override
    public String crearLinkDePago(Pedido pedido) {
        try {
            // 1. Cliente MP
            PreferenceClient client = new PreferenceClient();

            // 2. Ítem (Tu lógica original)
            List<PreferenceItemRequest> items = new ArrayList<>();
            PreferenceItemRequest item = PreferenceItemRequest.builder()
                    .id("PEDIDO-" + pedido.getId())
                    .title("Compra en Perfumería - Pedido #" + pedido.getId())
                    .description("Artículos de perfumería y limpieza")
                    .categoryId("home")
                    .quantity(1)
                    .unitPrice(new BigDecimal(pedido.getTotal().toString()))
                    .currencyId("ARS")
                    .build();
            items.add(item);

            // 3. Métodos de pago (Tu lógica original)
            List<PreferencePaymentTypeRequest> excludedPaymentTypes = new ArrayList<>();
            excludedPaymentTypes.add(PreferencePaymentTypeRequest.builder().id("ticket").build());
            excludedPaymentTypes.add(PreferencePaymentTypeRequest.builder().id("atm").build());

            PreferencePaymentMethodsRequest paymentMethods = PreferencePaymentMethodsRequest.builder()
                    .excludedPaymentTypes(excludedPaymentTypes)
                    .build();


            // 3.5 NUEVO: Configuración de retorno a React
            PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                    .success("http://localhost:5173/pago/exito")     // <--- A donde vuelve si sale OK
                    .pending("http://localhost:5173/pago/pendiente") // <--- A donde vuelve si queda pendiente
                    .failure("http://localhost:5173/pago/fallo")     // <--- A donde vuelve si falla o cancela
                    .build();

            // 4. Ensamblamos el pedido

            OffsetDateTime ahora = OffsetDateTime.now();
            OffsetDateTime vencimiento = ahora.plusMinutes(15);
            PreferenceRequest request = PreferenceRequest.builder()
                    .items(items)
                    .paymentMethods(paymentMethods)
                    .notificationUrl(webhookUrl + "/api/pagos/notificacion")
                    .externalReference(pedido.getId().toString())
                    //.backUrls(backUrls)
                    //.autoReturn("approved")
                    .expires(true)
                    .expirationDateFrom(ahora)
                    .expirationDateTo(vencimiento)
                    .build();

            // 5. Token y Creación
            com.mercadopago.core.MPRequestOptions options = com.mercadopago.core.MPRequestOptions.builder()
                    .accessToken(com.mercadopago.MercadoPagoConfig.getAccessToken())
                    .build();

            Preference preference = client.create(request, options);

            return preference.getInitPoint();

        } catch (MPApiException apiException) {
            System.out.println("❌ Error de la API de Mercado Pago:");
            System.out.println("Detalle exacto: " + apiException.getApiResponse().getContent());
            return null;
        } catch (MPException e) {
            System.out.println("❌ Error interno de la librería: " + e.getMessage());
            return null;
        }
    }

    public void procesarNotificacion(Long paymentId) {
        try {
            PaymentClient client = new PaymentClient();
            MPRequestOptions options = MPRequestOptions.builder()
                    .accessToken(MercadoPagoConfig.getAccessToken())
                    .build();

            Payment payment = client.get(paymentId, options);

            // Verificamos que el pago esté aprobado
            if ("approved".equals(payment.getStatus())) {
                Long idPedido = Long.parseLong(payment.getExternalReference());

                // Buscamos el pedido
                Pedido pedido = pedidoRepository.findById(idPedido)
                        .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

                // Evitamos procesar dos veces el mismo pedido si Mercado Pago manda doble aviso
                if (pedido.getEstado() != EstadoPedido.PAGADO) {

                    pedido.setEstado(EstadoPedido.PAGADO);

                    System.out.println("📦 Descontando stock del pedido #" + idPedido);


                    pedido.getDetalles().forEach(detalle -> {
                        try {
                            productoService.descontarStock(
                                    detalle.getProducto().getId(),
                                    detalle.getCantidad()
                            );
                            System.out.println("✅ Stock descontado: " + detalle.getProducto().getNombre() + " (-" + detalle.getCantidad() + ")");
                        } catch (Exception e) {
                            // Si justo no hay stock (condición de carrera), lo anotamos pero el pago ya entró
                            System.err.println("⚠️ Alerta de stock para " + detalle.getProducto().getNombre() + ": " + e.getMessage());
                        }
                    });

                    // Finalmente guardamos el pedido con su nuevo estado
                    pedidoRepository.save(pedido);
                    System.out.println("✅ Base de Datos actualizada: Pedido #" + idPedido + " -> PAGADO");
                } else {
                    System.out.println("ℹ️ El pedido #" + idPedido + " ya estaba marcado como PAGADO.");
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Error al procesar: " + e.getMessage());
        }
    }
}