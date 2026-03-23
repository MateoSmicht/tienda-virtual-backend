package lumato.desarrolo.tiendavirtual.repository;

import lumato.desarrolo.tiendavirtual.model.Producto;
import lumato.desarrolo.tiendavirtual.model.Usuario;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends CrudRepository<Usuario, Long> {

    @Query("SELECT c FROM Usuario c WHERE email = :email AND password = :password")
    List<Usuario> findByEmailAndPassword(@Param("email") String email,
                                           @Param("password") String password);
    Optional<Usuario> findById(Long id);

    void deleteById(Long id);

    Optional<Usuario> findByEmail(String email);


    @Query("SELECT u FROM Usuario u WHERE " +
            "LOWER(u.nombre) LIKE LOWER(CONCAT('%', :termino, '%')) OR " +
            "LOWER(u.apellido) LIKE LOWER(CONCAT('%', :termino, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :termino, '%'))")
    List<Usuario> buscarPorTermino(@Param("termino") String termino);
}