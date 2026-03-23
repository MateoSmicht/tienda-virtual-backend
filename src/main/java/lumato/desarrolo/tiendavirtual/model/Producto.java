package lumato.desarrolo.tiendavirtual.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.antlr.v4.runtime.misc.NotNull;

@Entity
@Table(name = "productos")
@Data
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    @NotBlank(message = "El código de barras es obligatorio")
    private String codigoBarra;

    @NotBlank(message = "El nombre no puede estar vacío")
    private String nombre;
    private String descripcion;
    private String imagenUrl;
    @NotNull
    @DecimalMin(value = "0.1", message = "El precio debe ser mayor a 0")
    private Double precio;

    private Boolean controlarStock = false;
    private Integer stock;
    private Double ppp;

    private Boolean disponible = true;
    private Boolean esOferta = false;
    private Double precioOferta;

    @ManyToOne
    @JoinColumn(name = "subcategoria_id", nullable = false)
    private Subcategoria subcategoria;

}