package lumato.desarrolo.tiendavirtual.dto;


import lombok.Data;
import lumato.desarrolo.tiendavirtual.model.enums.MetodoPago;

import java.util.List;
@Data
public class PedidoRequestDTO {
    private Long usuarioId;
    private MetodoPago metodoPago;
    private List<ItemCarritoDTO> items;
}
