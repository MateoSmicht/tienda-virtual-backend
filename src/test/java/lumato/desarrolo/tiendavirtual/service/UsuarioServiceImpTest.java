package lumato.desarrolo.tiendavirtual.service;

import lumato.desarrolo.tiendavirtual.exception.EmailDuplicadoException;
import lumato.desarrolo.tiendavirtual.model.Usuario;
import lumato.desarrolo.tiendavirtual.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Usamos Mockito puro. Es muchísimo más rápido porque no levanta Spring Boot.
@ExtendWith(MockitoExtension.class)
class UsuarioServiceImpTest {

    @Mock
    private UsuarioRepository repository; // Simulamos la base de datos

    @Mock
    private PasswordEncoder passwordEncoder; // Simulamos el encriptador

    @InjectMocks
    private UsuarioServiceImp usuarioService; // El servicio real que vamos a testear

    private Usuario usuarioFalso;

    @BeforeEach
    void setUp() {
        usuarioFalso = new Usuario();
        usuarioFalso.setId(1L);
        usuarioFalso.setNombre("Mateo");
        usuarioFalso.setEmail("mateo@lumato.com");
        usuarioFalso.setPassword("clave123"); // Clave en texto plano (como la manda React)
    }

    // ==========================================
    // TESTS DE CREAR USUARIO
    // ==========================================

    @Test
    void addUser_ConEmailNuevo_DebeEncriptarPassword_Y_Guardar() {
        // Arrange: Simulamos que el email no existe en la BD
        when(repository.findByEmail("mateo@lumato.com")).thenReturn(Optional.empty());
        // Simulamos que el encriptador hace su trabajo
        when(passwordEncoder.encode("clave123")).thenReturn("clave_encriptada_$$$");

        // Act: Llamamos al método
        usuarioService.addUser(usuarioFalso);

        // Assert: Verificamos que la contraseña se haya cambiado por la encriptada
        assertEquals("clave_encriptada_$$$", usuarioFalso.getPassword());
        // Verificamos que se haya llamado al repository.save() exactamente 1 vez
        verify(repository, times(1)).save(usuarioFalso);
    }

    @Test
    void addUser_ConEmailDuplicado_DebeLanzarExcepcion() {
        // Arrange: Simulamos que la BD ya tiene un usuario con ese email
        Usuario otroUsuario = new Usuario();
        otroUsuario.setEmail("mateo@lumato.com");
        when(repository.findByEmail("mateo@lumato.com")).thenReturn(Optional.of(otroUsuario));

        // Act & Assert: Verificamos que explote con nuestra excepción personalizada
        assertThrows(EmailDuplicadoException.class, () -> {
            usuarioService.addUser(usuarioFalso);
        });

        // Verificamos que NUNCA se haya llegado a guardar nada en la BD
        verify(repository, never()).save(any(Usuario.class));
    }

    // ==========================================
    // TESTS DE CAMBIAR CONTRASEÑA
    // ==========================================

    @Test
    void cambiarPassword_ConPasswordActualCorrecta_DebeGuardarNuevaClave() {
        // Arrange
        usuarioFalso.setPassword("clave_vieja_hash"); // Así estaría en la BD
        when(repository.findById(1L)).thenReturn(Optional.of(usuarioFalso));
        // Simulamos que el matches dice "sí, la clave actual coincide"
        when(passwordEncoder.matches("clave_vieja_plana", "clave_vieja_hash")).thenReturn(true);
        when(passwordEncoder.encode("clave_nueva")).thenReturn("clave_nueva_hash");

        // Act
        usuarioService.cambiarPassword(1L, "clave_vieja_plana", "clave_nueva");

        // Assert
        assertEquals("clave_nueva_hash", usuarioFalso.getPassword());
        verify(repository).save(usuarioFalso);
    }

    @Test
    void cambiarPassword_ConPasswordActualIncorrecta_DebeLanzarExcepcion() {
        // Arrange
        usuarioFalso.setPassword("clave_vieja_hash");
        when(repository.findById(1L)).thenReturn(Optional.of(usuarioFalso));
        // Simulamos que el matches dice "No, le pifiaste a la clave"
        when(passwordEncoder.matches("clave_equivocada", "clave_vieja_hash")).thenReturn(false);

        // Act & Assert
        RuntimeException excepcion = assertThrows(RuntimeException.class, () -> {
            usuarioService.cambiarPassword(1L, "clave_equivocada", "clave_nueva");
        });

        assertEquals("La contraseña actual es incorrecta", excepcion.getMessage());
        verify(repository, never()).save(any(Usuario.class));
    }
}