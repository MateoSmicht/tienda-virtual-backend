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
            // 1. Creamos el cliente que se va a comunicar con MP
            PreferenceClient client = new PreferenceClient();

            // 2. Armamos el "ítem" a cobrar.
            // Para simplificar, le mandamos el total del pedido directamente.
            // 2. Armamos el "ítem" a cobrar.
            // 2. Armamos el "ítem" a cobrar
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

            // 3. Excluimos SOLO Efectivo y Cajeros (Dejamos Crédito, Débito y Cuenta)
            // --------------------------------------------------------
            List<PreferencePaymentTypeRequest> excludedPaymentTypes = new ArrayList<>();

            // Excluimos Efectivo (Pago Fácil / Rapipago)
            excludedPaymentTypes.add(PreferencePaymentTypeRequest.builder().id("ticket").build());
            // Excluimos Redes de Cajeros (Link / Banelco)
            excludedPaymentTypes.add(PreferencePaymentTypeRequest.builder().id("atm").build());

            PreferencePaymentMethodsRequest paymentMethods = PreferencePaymentMethodsRequest.builder()
                    .excludedPaymentTypes(excludedPaymentTypes)
                    .build();

            // --------------------------------------------------------
            // 4. Ensamblamos todo el pedido
            // --------------------------------------------------------
            PreferenceRequest request = PreferenceRequest.builder()
                    .items(items)
                    .paymentMethods(paymentMethods)
                    .notificationUrl(webhookUrl + "/api/pagos/notificacion")
                    .externalReference(pedido.getId().toString())
                    .build();

            // Forzamos el token directamente en la petición
            com.mercadopago.core.MPRequestOptions options = com.mercadopago.core.MPRequestOptions.builder()
                    .accessToken(com.mercadopago.MercadoPagoConfig.getAccessToken())
                    .build();

            // Pasamos el request Y las options
            Preference preference = client.create(request, options);

           return preference.getInitPoint();
           //return preference.getSandboxInitPoint(); // Este es el link de simulacro (Sandbox)
        } catch (MPApiException apiException) {
        System.out.println("❌ Error de la API de Mercado Pago:");
        System.out.println("Status Code: " + apiException.getApiResponse().getStatusCode());
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

                    // --- LA MAGIA DEL STOCK EMPIEZA ACÁ ---
                    System.out.println("📦 Descontando stock del pedido #" + idPedido);

                    // Recorremos los detalles del pedido (cada línea del carrito)
                    // (Asegurate de que tu clase Pedido tenga el getter para los detalles, ej: getDetalles())
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
                    // --- LA MAGIA DEL STOCK TERMINA ACÁ ---

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