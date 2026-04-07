package lumato.desarrolo.tiendavirtual.service;


import lumato.desarrolo.tiendavirtual.exception.CategoriaDuplicadaException;
import lumato.desarrolo.tiendavirtual.model.Categoria;
import lumato.desarrolo.tiendavirtual.model.Producto;
import lumato.desarrolo.tiendavirtual.model.Subcategoria;
import lumato.desarrolo.tiendavirtual.repository.CategoriaRepository;
import lumato.desarrolo.tiendavirtual.repository.SubcategoriaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;


@Service
public class CategoriaServiceImp implements CategoriaService {

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private SubcategoriaRepository subcategoriaRepository;

    // --- CATEGORÍAS PRINCIPALES ---

    @Override
    public List<Categoria> obtenerCategorias() {
        return categoriaRepository.findAll();
    }

    @Override
    public Categoria obtenerPorId(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se encontró la categoría con ID: " + id));
    }
    @Override
    public Categoria modificarCategoria(Long id, Categoria categoriaModificada) {
        Categoria categoriaExistente = categoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se encontró la categoría con ID: " + id));

        categoriaExistente.setNombre(categoriaModificada.getNombre());
        categoriaExistente.setIcono(categoriaModificada.getIcono());
        return categoriaRepository.save(categoriaExistente);
    }


    @Override
    public Categoria crearCategoria(Categoria categoria) {
        // 1. Limpiamos espacios basura al principio y al final
        String nombreLimpio = categoria.getNombre().trim();

        // 2. Verificamos si ya existe (ignorando mayúsculas/minúsculas)
        if (categoriaRepository.findByNombreIgnoreCase(nombreLimpio).isPresent()) {
            throw new CategoriaDuplicadaException("La categoría principal '" + nombreLimpio + "' ya existe.");
        }

        // 3. Guardamos con el nombre prolijo
        categoria.setNombre(nombreLimpio);
        return categoriaRepository.save(categoria);
    }

    @Override
    public void eliminarCategoria(Long id) {
        Categoria categoria = obtenerPorId(id);
        categoriaRepository.delete(categoria);
    }


    // --- SUBCATEGORÍAS ---

    @Override
    public List<Subcategoria> obtenerSubcategoriasPorCategoria(Long categoriaId) {
        // 1. Validamos que la categoría padre exista (si no existe, tira el 404)
        obtenerPorId(categoriaId);
        // 2. Le pedimos al repositorio de subcategorías que nos traiga la lista
        return subcategoriaRepository.findByCategoriaId(categoriaId);
    }

    @Override
    public Subcategoria crearSubcategoria(Long categoriaId, Subcategoria subcategoria) {
        // 1. Verificamos que la categoría padre exista
        Categoria categoriaPadre = obtenerPorId(categoriaId);

        String nombreLimpio = subcategoria.getNombre().trim();

        // 2. Verificamos que no haya un duplicado DENTRO de esa misma categoría
        if (subcategoriaRepository.findByNombreIgnoreCaseAndCategoriaId(nombreLimpio, categoriaId).isPresent()) {
            throw new CategoriaDuplicadaException("La subcategoría '" + nombreLimpio + "' ya existe dentro de " + categoriaPadre.getNombre());
        }

        // 3. Vinculamos la relación y guardamos
        subcategoria.setNombre(nombreLimpio);
        subcategoria.setCategoria(categoriaPadre); // IMPORTANTÍSIMO: Enganchar el hijo con el padre

        return subcategoriaRepository.save(subcategoria);
    }

    @Override
    public void eliminarSubcategoria(Long id) {
        subcategoriaRepository.deleteById(id);
    }
}