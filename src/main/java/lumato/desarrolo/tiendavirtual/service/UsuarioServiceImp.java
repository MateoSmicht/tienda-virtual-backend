package lumato.desarrolo.tiendavirtual.service;

import lumato.desarrolo.tiendavirtual.dto.EditarPerfilDTO;
import lumato.desarrolo.tiendavirtual.exception.EmailDuplicadoException;
import lumato.desarrolo.tiendavirtual.model.Usuario;
import lumato.desarrolo.tiendavirtual.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UsuarioServiceImp implements UsuarioService {

    @Autowired
    private UsuarioRepository repository;
    @Autowired
    private PasswordEncoder passwordEncoder; // Inyectamos BCrypt
    @Override
    public Usuario getUser(Long id) {
        Optional<Usuario> user = repository.findById(id);
        if (user.isPresent()) {
            return user.get();
        }
        return null;
    }
    @Override
    public List<Usuario> getAllUsers() {
        List<Usuario> list = new ArrayList<>();

        Iterable<Usuario> users = repository.findAll();
        for (Usuario user:users) {
            list.add(user);
        }
        return list;
    }
    @Override
    public void removeUser(Long id) {
        repository.deleteById(id);
    }


    @Override
    public void addUser(Usuario user) {
        if (repository.findByEmail(user.getEmail()).isPresent()) {
            throw new EmailDuplicadoException("El email " + user.getEmail() + " ya está registrado en otra cuenta.");
        }

        // Encriptación profesional con BCrypt
        String hashPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(hashPassword);

        repository.save(user);
    }
    @Override
    public void updateUser(Long id, Usuario updateUser) {
        updateUser.setId(id);
        repository.save(updateUser);
    }

    @Override
    public List<Usuario> searchUser(String termino) {
        return repository.buscarPorTermino(termino);
    }
    @Override
    public void cambiarPassword(Long id, String passwordActual, String passwordNueva) {
        // 1. Buscamos al usuario por su ID
        Usuario usuario = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // 2. Verificamos que la clave actual sea correcta
        if (!passwordEncoder.matches(passwordActual, usuario.getPassword())) {
            throw new RuntimeException("La contraseña actual es incorrecta");
        }

        // 3. Encriptamos la nueva y la guardamos
        usuario.setPassword(passwordEncoder.encode(passwordNueva));
        repository.save(usuario);
    }

    @Override
    public void actualizarPerfil(Long id, EditarPerfilDTO dto) {
        Usuario usuario = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Si el usuario quiere cambiar su email, verificamos que el nuevo email no esté ocupado
        if (!usuario.getEmail().equals(dto.getEmail())) {
            if (repository.findByEmail(dto.getEmail()).isPresent()) {
                throw new RuntimeException("El email " + dto.getEmail() + " ya está en uso por otra cuenta.");
            }
            usuario.setEmail(dto.getEmail());
        }

        usuario.setNombre(dto.getNombre());
        usuario.setApellido(dto.getApellido());
        usuario.setTelefono(dto.getTelefono());

        repository.save(usuario);
    }

}