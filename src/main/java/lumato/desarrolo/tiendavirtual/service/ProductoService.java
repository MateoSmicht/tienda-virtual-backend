package lumato.desarrolo.tiendavirtual.service;


import jakarta.transaction.Transactional;
import lumato.desarrolo.tiendavirtual.dto.ProductoStatsDTO;
import lumato.desarrolo.tiendavirtual.model.Producto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductoService {
    Page<Producto> obtenerTodosAdmin(Pageable pageable);
    Page<Producto> obtenerCatalogoDisponible(Pageable pageable);
    List<Producto> obtenerOfertas();;
    Producto guardarProducto(Producto producto);
    Producto obtenerPorId(Long id); // Agregamos este que nos va a servirP
    void eliminarProducto(Long id); // Y este para el panel de admin
    Producto buscarPorCodigoBarra(String codigoBarra);
    void modificarProducto(Long id, Producto productoModificado);
    Producto ingresarStock(Long id, Integer cantidad, Double precioCompra);
    Producto descontarStock(Long id, Integer cantidadComprada);
    Producto aplicarOferta(Long id, Double porcentaje, Double precioFijo);
    Producto quitarOferta(Long id); // Fundamental para cuando termine la promo
    void aplicarOfertaMasiva(Long categoriaId, Double porcentaje);
    long contProductosOferta();
    ProductoStatsDTO obtenerEstadisticasInventario();
    Page<Producto> obtenerProductosPaginadosYFiltrados(int page, int size, boolean todos, String busqueda, Long categoriaId, Long subcategoriaId, Integer stockMax) ;

}