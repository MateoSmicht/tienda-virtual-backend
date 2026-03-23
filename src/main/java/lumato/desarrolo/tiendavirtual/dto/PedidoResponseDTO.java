package lumato.desarrolo.tiendavirtual.dto;

import lombok.Data;

import java.time.LocalDateTime;
@Data
public class PedidoResponseDTO {

    private Long idPedido;
    private String estado;
    private Double total;
    private String metodoPago;
    private LocalDateTime fecha;
    private String mensaje;
    private String linkDePago;



    public PedidoResponseDTO() {}

}