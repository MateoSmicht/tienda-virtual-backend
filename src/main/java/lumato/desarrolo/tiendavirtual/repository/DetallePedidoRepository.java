package lumato.desarrolo.tiendavirtual.repository;

import lumato.desarrolo.tiendavirtual.dto.ProductoStatDTO;
import lumato.desarrolo.tiendavirtual.dto.TopProductoDTO;
import lumato.desarrolo.tiendavirtual.model.DetallePedido;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DetallePedidoRepository extends JpaRepository<DetallePedido, Long> {


    @Query("SELECT new lumato.desarrolo.tiendavirtual.dto.TopProductoDTO(prod.id, prod.nombre, SUM(dp.cantidad), SUM(dp.cantidad * dp.precioUnitario)) " +
            "FROM DetallePedido dp JOIN dp.producto prod JOIN dp.pedido ped " +
            "WHERE ped.fechaPedido >= :inicio AND ped.fechaPedido <= :fin AND ped.estado != 'CANCELADO' " +
            "GROUP BY prod.id, prod.nombre ORDER BY SUM(dp.cantidad) DESC")
    List<TopProductoDTO> findTopProductos(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin, Pageable pageable);

    @Query("SELECT new lumato.desarrolo.tiendavirtual.dto.ProductoStatDTO(prod.nombre, SUM(dp.cantidad), SUM(dp.cantidad * dp.precioUnitario), MAX(ped.fechaPedido)) " +
            "FROM DetallePedido dp JOIN dp.producto prod JOIN dp.pedido ped " +
            "WHERE prod.id = :productoId AND ped.estado != 'CANCELADO' " +
            "GROUP BY prod.id, prod.nombre")
    ProductoStatDTO findStatsByProductoId(@Param("productoId") Long productoId);
}