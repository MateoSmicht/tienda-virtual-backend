package lumato.desarrolo.tiendavirtual.controller;


import lumato.desarrolo.tiendavirtual.model.Categoria;
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
    public ResponseEntity<Categoria> actualizarCategoria(@PathVariable Long id, @RequestBody Categoria categoriaDetails) {
        // Primero buscamos la categoría que queremos editar
        Categoria categoriaExistente = categoriaService.obtenerPorId(id);

        // Le cambiamos el nombre por el que viene desde React
        categoriaExistente.setNombre(categoriaDetails.getNombre());

        // Guardamos los cambios y devolvemos el OK
        return ResponseEntity.ok(categoriaService.crearCategoria(categoriaExistente));
        // (Uso crearCategoria asumiendo que tu service usa el save() de JPA, que sirve tanto para crear como para actualizar)
    }


    // Ej: GET /api/categorias/1/subcategorias (Trae "Aerosoles", "Líquidos" de la categoría Limpieza)
    @GetMapping("/{categoriaId}/subcategorias")
    public ResponseEntity<List<Subcategoria>> listarSubcategorias(@PathVariable Long categoriaId) {
        return ResponseEntity.ok(categoriaService.obtenerSubcategoriasPorCategoria(categoriaId));
    }

    // Ej: POST /api/categorias/1/subcategorias (Agrega una nueva subcategoría a Limpieza)
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