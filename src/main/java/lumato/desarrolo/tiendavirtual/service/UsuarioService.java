package lumato.desarrolo.tiendavirtual.service;

import lumato.desarrolo.tiendavirtual.model.Usuario;

import java.util.List;

public interface UsuarioService {

    Usuario getUser(Long id);
    List<Usuario> getAllUsers();
    void removeUser(Long id);
    void addUser(Usuario user);
    void updateUser(Long id, Usuario updateUser);
    List<Usuario> searchUser(String termino);

}
