package lumato.desarrolo.tiendavirtual.repository;


import lumato.desarrolo.tiendavirtual.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    Page<Producto> findByDisponibleTrue(Pageable pageable);

    // Trae las ofertas que además estén disponibles
    List<Producto> findByEsOfertaTrueAndDisponibleTrue();

     //Busqueda por codigo de barra
     Optional<Producto> findByCodigoBarra(String codigoBarra);

    @Query("SELECT p FROM Producto p WHERE " +
            "(:busqueda IS NULL OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR LOWER(p.codigoBarra) LIKE LOWER(CONCAT('%', :busqueda, '%'))) AND " +
            "(:categoriaId IS NULL OR p.subcategoria.categoria.id = :categoriaId) AND " +
            "(:subcategoriaId IS NULL OR p.subcategoria.id = :subcategoriaId) AND " + // <--- ESTA LÍNEA ES NUEVA
            "(:stockMax IS NULL OR p.stock <= :stockMax) AND " +
            "(:todos = true OR p.disponible = true)")
    Page<Producto> buscarConFiltrosDinamicos(
            @Param("busqueda") String busqueda,
            @Param("categoriaId") Long categoriaId,
            @Param("subcategoriaId") Long subcategoriaId,
            @Param("stockMax") Integer stockMax,
            @Param("todos") boolean todos,
            Pageable pageable);

    @Query("SELECT p FROM Producto p WHERE p.subcategoria.categoria.id = :idCategoria")
    List<Producto> buscarPorCategoriaMadre(@Param("idCategoria") Long idCategoria);

    long countByPrecioOfertaGreaterThan(Double precio);

    long countByDisponibleTrue();
    // Cuenta cuántos productos tienen stock <= 0
    long countByStockLessThanEqual(Integer stock);

    @Query("SELECT COALESCE(SUM(p.stock * p.ppp), 0) FROM Producto p WHERE p.stock > 0")
    Double calcularValorTotalInventario();

}