package lumato.desarrolo.tiendavirtual.repository;

import lumato.desarrolo.tiendavirtual.dto.ProductoStatDTO;
import lumato.desarrolo.tiendavirtual.model.Pedido;
import lumato.desarrolo.tiendavirtual.model.enums.EstadoPedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    // 1. Para que el cliente pueda ver su historial de compras en la web
    List<Pedido> findByUsuarioId(Long usuarioId);

    // 2. Para tu panel de administración: filtrar pedidos por estado
    // (Ej: Traer todos los pedidos "PENDIENTE" para armarlos)
    List<Pedido> findByEstado(EstadoPedido estado);

    // 3. Para las estadísticas del panel: Traer ventas en un rango de fechas
    // (Con esto vas a poder calcular cuánto recaudaste hoy o en todo el mes)
    List<Pedido> findByFechaPedidoBetween(LocalDateTime fechaInicio, LocalDateTime fechaFin);

    // Traer los pedidos de un cliente ordenados por el más reciente
    List<Pedido> findByUsuario_IdOrderByFechaPedidoDesc(Long usuarioId);

    @Query("SELECT p FROM Pedido p WHERE " +
            "(:estado IS NULL OR p.estado = :estado) AND " +
            "(cast(:fechaInicio as timestamp) IS NULL OR p.fechaPedido >= :fechaInicio) AND " +
            "(cast(:fechaFin as timestamp) IS NULL OR p.fechaPedido <= :fechaFin) " +
            "ORDER BY p.fechaPedido DESC")
    Page<Pedido> buscarPedidosConFiltros(
            @Param("estado") EstadoPedido estado,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin,
            Pageable pageable);

    // Suma la plata de un periodo
    @Query("SELECT COALESCE(SUM(p.total), 0) FROM Pedido p WHERE p.fechaPedido >= :inicio AND p.fechaPedido <= :fin AND p.estado != 'CANCELADO'")
    Double sumIngresosEntreFechas(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    // Cuenta la cantidad de ventas de un periodo
    @Query("SELECT COUNT(p) FROM Pedido p WHERE p.fechaPedido >= :inicio AND p.fechaPedido <= :fin AND p.estado != 'CANCELADO'")
    Long countPedidosEntreFechas(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    // Trae solo fechas y totales para armar el gráfico rápido sin saturar la RAM
    @Query("SELECT p.fechaPedido, p.total FROM Pedido p WHERE p.fechaPedido >= :inicio AND p.fechaPedido <= :fin AND p.estado != 'CANCELADO'")
    List<Object[]> findVentasParaGrafico(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);
    // Spring traduce esto automáticamente a: SELECT * FROM pedidos ORDER BY fecha_pedido DESC LIMIT 5
    List<Pedido> findTop5ByOrderByFechaPedidoDesc();

    @Query("SELECT new lumato.desarrolo.tiendavirtual.dto.ProductoStatDTO(" +
            "d.producto.nombre, " +
            "SUM(CAST(d.cantidad AS long)), " +
            "SUM(d.cantidad * d.precioUnitario), " +
            "MAX(d.pedido.fechaPedido)) " +
            "FROM DetallePedido d " +
            "WHERE d.producto.id = :productoId AND d.pedido.estado = lumato.desarrolo.tiendavirtual.model.enums.EstadoPedido.PAGADO " +
            "GROUP BY d.producto.id, d.producto.nombre")
    Optional<ProductoStatDTO> obtenerEstadisticasHistoricasPorProducto(@Param("productoId") Long productoId); // <--- ACA TAMBIÉN
}
