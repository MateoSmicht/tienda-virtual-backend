package lumato.desarrolo.tiendavirtual.repository;


import lumato.desarrolo.tiendavirtual.model.Subcategoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubcategoriaRepository extends JpaRepository<Subcategoria, Long> {

    List<Subcategoria> findByCategoriaId(Long categoriaId);

    Optional<Subcategoria> findByNombreIgnoreCaseAndCategoriaId(String nombre, Long categoriaId);
}