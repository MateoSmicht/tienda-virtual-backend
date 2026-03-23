package lumato.desarrolo.tiendavirtual.service;

import com.google.common.hash.Hashing;
import lumato.desarrolo.tiendavirtual.exception.EmailDuplicadoException;
import lumato.desarrolo.tiendavirtual.model.Usuario;
import lumato.desarrolo.tiendavirtual.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UsuarioServiceImp implements UsuarioService {

    @Autowired
    private UsuarioRepository repository;
    @Autowired
    private PasswordEncoder passwordEncoder; // Inyectamos BCrypt

    public Usuario getUser(Long id) {
        Optional<Usuario> user = repository.findById(id);
        if (user.isPresent()) {
            return user.get();
        }
        return null;
    }

    public List<Usuario> getAllUsers() {
        List<Usuario> list = new ArrayList<>();

        Iterable<Usuario> users = repository.findAll();
        for (Usuario user:users) {
            list.add(user);
        }
        return list;
    }

    public void removeUser(Long id) {
        repository.deleteById(id);
    }



    public void addUser(Usuario user) {
        if (repository.findByEmail(user.getEmail()).isPresent()) {
            throw new EmailDuplicadoException("El email " + user.getEmail() + " ya está registrado en otra cuenta.");
        }

        // Encriptación profesional con BCrypt
        String hashPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(hashPassword);

        repository.save(user);
    }

    public void updateUser(Long id, Usuario updateUser) {
        updateUser.setId(id);
        repository.save(updateUser);
    }


  public List<Usuario> searchUser(String termino) {
      return repository.buscarPorTermino(termino);}
}