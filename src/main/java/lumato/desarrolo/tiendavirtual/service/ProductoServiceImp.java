package lumato.desarrolo.tiendavirtual.service;



import lumato.desarrolo.tiendavirtual.dto.ProductoStatsDTO;
import lumato.desarrolo.tiendavirtual.exception.CodigoBarraDuplicadoException;
import lumato.desarrolo.tiendavirtual.exception.ProductoNoEncontradoException;
import lumato.desarrolo.tiendavirtual.exception.StockInsuficienteException;
import lumato.desarrolo.tiendavirtual.model.Producto;
import lumato.desarrolo.tiendavirtual.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class ProductoServiceImp implements ProductoService {

    @Autowired
    private ProductoRepository productoRepository;

    @Override
    public List<Producto> obtenerOfertas() {
        return productoRepository.findByEsOfertaTrueAndDisponibleTrue();
    }

    @Override
    public Page<Producto> obtenerTodosAdmin(Pageable pageable) {
        return productoRepository.findAll(pageable);
    }

    @Override
    public Page<Producto> obtenerCatalogoDisponible(Pageable pageable) {
        return productoRepository.findByDisponibleTrue(pageable);
    }



    @Override
    public Producto guardarProducto(Producto producto) {
        // CORRECCIÓN DEL BUG: Verificamos que el código no exista EN OTRO producto distinto
        Optional<Producto> existente = productoRepository.findByCodigoBarra(producto.getCodigoBarra());
        if (existente.isPresent() && !existente.get().getId().equals(producto.getId())) {
            throw new CodigoBarraDuplicadoException("El código de barras " + producto.getCodigoBarra() + " ya está registrado.");
        }
        return productoRepository.save(producto);
    }
    @Override
    public void eliminarProducto(Long id) {
        // Validamos antes de borrar
        Producto producto = obtenerPorId(id);
        productoRepository.delete(producto);
    }

    @Override
    public Producto buscarPorCodigoBarra(String codigoBarra) {
        return productoRepository.findByCodigoBarra(codigoBarra)
                .orElseThrow(() -> new ProductoNoEncontradoException("No se encontró producto con código: " + codigoBarra));
    }

    @Override
    public Producto obtenerPorId(Long id) {
        // ESTA ES LA MAGIA: Si no lo encuentra, lanza el error y frena todo.
        return productoRepository.findById(id)
                .orElseThrow(() -> new ProductoNoEncontradoException("No se encontró el producto con ID: " + id));
    }

    @Override
    public void modificarProducto(Long id, Producto productoModificado) {
        Producto existente = obtenerPorId(id); // Valida que exista
        productoModificado.setId(existente.getId());
        guardarProducto(productoModificado); // Usa el guardarProducto corregido
    }

    @Override
    public Producto ingresarStock(Long id, Integer cantidad, Double precioCompra) {
        Producto producto = obtenerPorId(id); // Ya no hace falta el if(producto != null)
        Integer stockActual = (producto.getStock() != null) ? producto.getStock() : 0;
        Double pppActual = (producto.getPpp() != null) ? producto.getPpp() : 0.0;

        Double valorInventarioActual = stockActual * pppActual;
        Double valorNuevoIngreso = cantidad * precioCompra;

        Integer nuevoStock = stockActual + cantidad;
        Double nuevoPpp = (valorInventarioActual + valorNuevoIngreso) / nuevoStock;

        producto.setStock(nuevoStock);
        producto.setPpp(Math.round(nuevoPpp * 100.0) / 100.0);

        return productoRepository.save(producto); // Guardamos directo para no pasar por la validación de código de barras
    }

    @Override
    public Producto descontarStock(Long id, Integer cantidadComprada) {
        Producto producto = obtenerPorId(id);

        Boolean controla = (producto.getControlarStock() != null) ? producto.getControlarStock() : false;

        if (controla) {
            Integer stockActual = (producto.getStock() != null) ? producto.getStock() : 0;

            if (stockActual >= cantidadComprada) {
                producto.setStock(stockActual - cantidadComprada);
                return productoRepository.save(producto);
            } else {
                // ACÁ USAMOS NUESTRA NUEVA EXCEPCIÓN
                throw new StockInsuficienteException("Stock insuficiente para: " + producto.getNombre() + ". Stock actual: " + stockActual);
            }
            }
        return producto;
    }

    @Override
    public Producto aplicarOferta(Long id, Double porcentaje, Double precioFijo) {
        Producto producto = obtenerPorId(id);

        if (producto.getPrecio() != null) {
            if (precioFijo != null) {
                producto.setPrecioOferta(precioFijo);
            } else if (porcentaje != null) {
                Double descuento = producto.getPrecio() * (porcentaje / 100.0);
                producto.setPrecioOferta(Math.round((producto.getPrecio() - descuento) * 100.0) / 100.0);
            }
            producto.setEsOferta(true);
            return productoRepository.save(producto);
        }
        throw new RuntimeException("El producto no tiene un precio base para aplicar oferta.");
    }

    @Override
    public Producto quitarOferta(Long id) {
        Producto producto = obtenerPorId(id);
        producto.setEsOferta(false);
        producto.setPrecioOferta(null);
        return productoRepository.save(producto);
    }

    @Transactional
    @Override
    public void aplicarOfertaMasiva(Long categoriaId, Double porcentaje) {

        // 1. Buscamos todos los productos que pertenezcan a esa categoría madre
        // (Asegurate de tener este método en tu ProductoRepository)
        List<Producto> productos = productoRepository.buscarPorCategoriaMadre(categoriaId);

        // 2. Recorremos y reutilizamos tu método actual para aplicar la oferta a cada uno
        for (Producto producto : productos) {
            aplicarOferta(producto.getId(), porcentaje, null);
        }
    }

    @Override
    public long contProductosOferta(){
        return productoRepository.countByPrecioOfertaGreaterThan(0.0);
    }
    @Override
    public ProductoStatsDTO obtenerEstadisticasInventario() {
        long total = productoRepository.count();
        long activos = productoRepository.countByDisponibleTrue();
        long sinStock = productoRepository.countByStockLessThanEqual(0);
        Double valorInventario = productoRepository.calcularValorTotalInventario();

        return new ProductoStatsDTO(total, activos, sinStock, valorInventario);
    }

    @Override
    public Page<Producto> obtenerProductosPaginadosYFiltrados(int page, int size, boolean todos, String busqueda, Long categoriaId, Long subcategoriaId, Integer stockMax) {
        Pageable pageable = PageRequest.of(page, size);
        if (busqueda != null && busqueda.trim().isEmpty()) busqueda = null;

        return productoRepository.buscarConFiltrosDinamicos(busqueda, categoriaId, subcategoriaId, stockMax, todos, pageable);
    }



}
