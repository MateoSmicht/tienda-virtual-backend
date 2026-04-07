package lumato.desarrolo.tiendavirtual.service;


import lumato.desarrolo.tiendavirtual.model.DetallePedido;
import lumato.desarrolo.tiendavirtual.model.Pedido;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImp implements EmailService {

    @Autowired
    private JavaMailSender mailSender;

    // Traemos tu correo desde el application.properties
    @Value("${spring.mail.username}")
    private String miCorreo;

    @Override
    public void enviarAvisoDeNuevoPedido(Pedido pedido) {
        SimpleMailMessage mensaje = new SimpleMailMessage();

        mensaje.setFrom(miCorreo);
        // Te lo enviás a vos mismo para que te llegue la notificación
        mensaje.setTo(miCorreo);
        mensaje.setSubject("🚀 ¡NUEVO PEDIDO #" + pedido.getId() + " - Perfumería!");

        StringBuilder texto = new StringBuilder();
        texto.append("Hola, ¡entró una nueva compra en la web!\n\n");

        texto.append("DATOS DEL CLIENTE:\n");
        texto.append("- Nombre: ").append(pedido.getUsuario().getNombre()).append(" ").append(pedido.getUsuario().getApellido()).append("\n");
        texto.append("- Email: ").append(pedido.getUsuario().getEmail()).append("\n");
        texto.append("- Método de Pago: ").append(pedido.getMetodoPago().name()).append("\n\n");

        texto.append("DETALLE DE LA COMPRA:\n");
        for (DetallePedido detalle : pedido.getDetalles()) {
            texto.append(" > ").append(detalle.getCantidad()).append("x ")
                    .append(detalle.getProducto().getNombre())
                    .append(" ($").append(detalle.getPrecioUnitario()).append(" c/u)\n");
        }

        texto.append("\nTOTAL A COBRAR: $").append(pedido.getTotal()).append("\n");
        texto.append("\nPor favor, ingresá al Panel de Administración para gestionar este pedido.");

        mensaje.setText(texto.toString());

        mailSender.send(mensaje);
    }
}
