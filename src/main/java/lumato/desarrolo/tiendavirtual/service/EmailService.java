package lumato.desarrolo.tiendavirtual.service;


import lumato.desarrolo.tiendavirtual.model.Pedido;

public interface EmailService {
    void enviarAvisoDeNuevoPedido(Pedido pedido);
}