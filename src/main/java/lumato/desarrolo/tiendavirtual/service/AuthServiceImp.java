package lumato.desarrolo.tiendavirtual.service;

import lumato.desarrolo.tiendavirtual.model.Usuario;
import lumato.desarrolo.tiendavirtual.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthServiceImp implements AuthService {

    @Autowired
    private UsuarioRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public Usuario login(String email, String password) {
        // 1. Buscamos el usuario por email
        Optional<Usuario> userOpt = userRepository.findByEmail(email);

        // 2. Si existe, verificamos que la contraseña cruda coincida con el hash de la BD
        if (userOpt.isPresent() && passwordEncoder.matches(password, userOpt.get().getPassword())) {
            return userOpt.get();
        }

        // 3. Si falla, tiramos una excepción estándar de Spring Security
        throw new BadCredentialsException("Email o contraseña incorrectos");
    }
}