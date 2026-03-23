package lumato.desarrolo.tiendavirtual.service;

import lumato.desarrolo.tiendavirtual.model.Usuario;

public interface AuthService {
    Usuario login(String email, String password);
}
