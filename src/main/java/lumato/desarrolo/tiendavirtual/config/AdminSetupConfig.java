package lumato.desarrolo.tiendavirtual.config;


import lumato.desarrolo.tiendavirtual.model.Usuario;
import lumato.desarrolo.tiendavirtual.model.enums.Rol;
import lumato.desarrolo.tiendavirtual.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AdminSetupConfig implements CommandLineRunner {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {

        String emailAdmin = "admin@perfumery.com";

        Optional<Usuario> adminExistente = usuarioRepository.findByEmail(emailAdmin);

        // Si no existe, lo creamos
        if (adminExistente.isEmpty()) {
            Usuario admin = new Usuario();
            admin.setNombre("Dueño");
            admin.setApellido("Perfumery");
            admin.setEmail(emailAdmin);
            admin.setPassword(passwordEncoder.encode("admin1234")); // Clave inicial
            admin.setRol(Rol.ADMIN);

            usuarioRepository.save(admin);
            System.out.println("✅ Cuenta de Super Administrador creada exitosamente.");
        } else {
            System.out.println("⚡ La cuenta de Administrador ya existe. Omitiendo creación.");
        }
    }
}