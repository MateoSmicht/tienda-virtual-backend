package lumato.desarrolo.tiendavirtual.controller;


import lumato.desarrolo.tiendavirtual.model.Categoria;
import lumato.desarrolo.tiendavirtual.model.Producto;
import lumato.desarrolo.tiendavirtual.model.Subcategoria;
import lumato.desarrolo.tiendavirtual.service.CategoriaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categorias")
@CrossOrigin(origins = "*")
public class CategoriaController {

    @Autowired
    private CategoriaService categoriaService;


    @GetMapping
    public ResponseEntity<List<Categoria>> listarCategorias() {
        return ResponseEntity.ok(categoriaService.obtenerCategorias());
    }

    @GetMapping("/{id}")
    public Categoria buscarPorId(@PathVariable Long id) {
        return(categoriaService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<Categoria> crearCategoria(@RequestBody Categoria categoria) {
        return ResponseEntity.ok(categoriaService.crearCategoria(categoria));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarCategoria(@PathVariable Long id) {
        categoriaService.eliminarCategoria(id);
        return ResponseEntity.noContent().build();
    }
    @PutMapping("/{id}")
    public ResponseEntity<Categoria> modificarCategoria(@PathVariable Long id,
                                  @RequestBody Categoria categoriaModificado) {
        Categoria actualizada = categoriaService.modificarCategoria(id, categoriaModificado);
        return ResponseEntity.ok(actualizada);
    }




    @GetMapping("/{categoriaId}/subcategorias")
    public ResponseEntity<List<Subcategoria>> listarSubcategorias(@PathVariable Long categoriaId) {
        return ResponseEntity.ok(categoriaService.obtenerSubcategoriasPorCategoria(categoriaId));
    }


    @PostMapping("/{categoriaId}/subcategorias")
    public ResponseEntity<Subcategoria> crearSubcategoria(
            @PathVariable Long categoriaId,
            @RequestBody Subcategoria subcategoria) {
        try {
            return ResponseEntity.ok(categoriaService.crearSubcategoria(categoriaId, subcategoria));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/subcategorias/{id}")
    public ResponseEntity<Void> eliminarSubcategoria(@PathVariable Long id) {
        categoriaService.eliminarSubcategoria(id);
        return ResponseEntity.noContent().build();
    }
}