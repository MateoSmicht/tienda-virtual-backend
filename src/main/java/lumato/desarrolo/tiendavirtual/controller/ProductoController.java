package lumato.desarrolo.tiendavirtual.controller;

import lumato.desarrolo.tiendavirtual.dto.IngresoStockDTO;
import lumato.desarrolo.tiendavirtual.dto.OfertaDTO;
import lumato.desarrolo.tiendavirtual.dto.ProductoStatsDTO;
import lumato.desarrolo.tiendavirtual.model.Producto;
import lumato.desarrolo.tiendavirtual.service.ProductoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/productos")
@CrossOrigin(origins = "*") // Permite que tu frontend HTML/JS se conecte sin bloqueos de seguridad CORS
public class ProductoController {

    @Autowired
    private ProductoService productoService;

    @GetMapping
    public ResponseEntity<Page<Producto>> obtenerProductos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "false") boolean todos,
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Long subcategoriaId, // <--- NUEVO
            @RequestParam(required = false) Integer stockMax) {

        Page<Producto> productos = productoService.obtenerProductosPaginadosYFiltrados(page, size, todos, busqueda, categoriaId, subcategoriaId, stockMax);
        return ResponseEntity.ok(productos);
    }

    // Endpoint: GET /api/productos/id
    @GetMapping("/{id}")
    public Producto buscarProducto(@PathVariable Long id) {
       return productoService.obtenerPorId(id);
    }

    // Endpoint: GET /api/productos/ofertas
    @GetMapping("/ofertas")
    public List<Producto> verOfertas() {
        return productoService.obtenerOfertas();
    }

    // Endpoint admin: PUT /api/productos/id/estado
    @PutMapping("/{id}/estado")
    public ResponseEntity<String> cambiarEstado(@PathVariable Long id, @RequestParam Boolean disponible) {

        // 1. Buscamos el producto en la base de datos (adaptalo a tu Service o Repository)
        Producto producto = productoService.obtenerPorId(id);
        // (Si usás repository directo sería: productoRepository.findById(id).get(); )

        // 2. Le cambiamos solo el estado
        producto.setDisponible(disponible);

        // 3. Lo volvemos a guardar
        productoService.guardarProducto(producto);

        return ResponseEntity.ok("Estado actualizado correctamente");
    }

    // Endpoint Admin: POST /api/productos
    @PostMapping
    public ResponseEntity<String> crearProducto(@RequestBody Producto producto) {
        productoService.guardarProducto(producto);
        return ResponseEntity.ok("Producto guardado correctamente");
    }

    // Endpoint Admin: DELETE /api/productos/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> borrarProducto(@PathVariable Long id) {
        productoService.eliminarProducto(id);
        return ResponseEntity.noContent().build(); // Devuelve un 204 No Content (éxito al borrar)
    }

    // Endpoint Admin: POST /api/productos/codigo/codigoBarra
    @GetMapping("/codigo/{codigoBarra}")
    public ResponseEntity<Producto> buscarProductoPorCodigo(@PathVariable String codigoBarra) {

        // El .trim() elimina cualquier espacio o salto de línea invisible
        String codigoLimpio = codigoBarra.trim();

        Producto producto = productoService.buscarPorCodigoBarra(codigoLimpio);

        if (producto != null) {
            return ResponseEntity.ok(producto);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    //Modoficar Producto
    @PutMapping("/{id}")
    public void modificarProducto(@PathVariable Long id,
                               @RequestBody Producto productoModificado) {
        productoService.modificarProducto(id, productoModificado);
    }


    // Endpoint Admin: PUT /api/productos/{id}/stock
    @PutMapping("/{id}/stock")
    public ResponseEntity<Producto> ingresarStock(@PathVariable Long id, @RequestBody IngresoStockDTO ingreso) {

        Producto productoActualizado = productoService.ingresarStock(id, ingreso.getCantidad(),
                ingreso.getPrecioCompra());

        if (productoActualizado != null) {
            return ResponseEntity.ok(productoActualizado);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // Endpoint Admin: PUT /api/productos/categoria/{categoriaId}/oferta
    @PutMapping("/categoria/{categoriaId}/oferta")
    public ResponseEntity<String> aplicarOfertaPorCategoria(
            @PathVariable Long categoriaId,
            @RequestBody OfertaDTO ofertaDTO) {

        if (ofertaDTO.getPorcentaje() == null) {
            return ResponseEntity.badRequest().body("Las ofertas masivas solo aceptan porcentaje.");
        }

        productoService.aplicarOfertaMasiva(categoriaId, ofertaDTO.getPorcentaje());

        return ResponseEntity.ok("Oferta masiva aplicada correctamente");
    }


    // Endpoint Admin: PUT /api/productos/{id}/oferta
    @PutMapping("/{id}/oferta")
    public ResponseEntity<Producto> aplicarOferta(
            @PathVariable Long id,
            @RequestBody OfertaDTO ofertaDTO) {

        Producto producto = productoService.aplicarOferta(id, ofertaDTO.getPorcentaje(), ofertaDTO.getPrecioFijo());

        if (producto != null) {
            return ResponseEntity.ok(producto);
        }
        return ResponseEntity.notFound().build();
    }

    // Endpoint Admin: DELETE /api/productos/{id}/oferta
    @DeleteMapping("/{id}/oferta")
    public ResponseEntity<Producto> quitarOferta(@PathVariable Long id) {
        Producto producto = productoService.quitarOferta(id);

        if (producto != null) {
            return ResponseEntity.ok(producto);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/ofertas/cantidad")
    public long cantProductosOferta(){
        return productoService.contProductosOferta();
    }
    // Endpoint: GET /api/productos/estadisticas
    @GetMapping("/estadisticas")
    public ResponseEntity<ProductoStatsDTO> obtenerEstadisticasInventario() {
        return ResponseEntity.ok(productoService.obtenerEstadisticasInventario());
    }



}