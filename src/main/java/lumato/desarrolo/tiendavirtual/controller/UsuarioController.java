package lumato.desarrolo.tiendavirtual.controller;

import lumato.desarrolo.tiendavirtual.dto.CambiarPasswordDTO;
import lumato.desarrolo.tiendavirtual.dto.EditarPerfilDTO;
import lumato.desarrolo.tiendavirtual.model.Usuario;
import lumato.desarrolo.tiendavirtual.service.UsuarioService;
import lumato.desarrolo.tiendavirtual.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class UsuarioController {

    @Autowired
    private UsuarioService service;

    @GetMapping("/user/{id}") // Traer un cliente especifico
    public Usuario getUser(@PathVariable Long id) {
        return service.getUser(id);
    }

    @GetMapping("/user") // Traer todos los clientes
    public List<Usuario> getAllUsers() {
        return service.getAllUsers();
    }

    @DeleteMapping("/user/{id}") // Eliminar un cliente
    public void removeUser(@PathVariable Long id) {
        service.removeUser(id);
    }

    @PostMapping("/register") // Agregar cliente
    public void register(@RequestBody Usuario user) {
        service.addUser(user);
    }

    @PutMapping("/user/{id}") // Modificar cliente
    public void updateUser(@PathVariable Long id,
                           @RequestBody Usuario updateUser) {
        service.updateUser(id, updateUser);
    }
    @GetMapping("/buscar")
    public ResponseEntity<List<Usuario>> buscarClientes(@RequestParam String termino) {
        return ResponseEntity.ok(service.searchUser(termino));
    }

    @PutMapping("user/me/password")
    public ResponseEntity<?> cambiarMiPassword(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody CambiarPasswordDTO dto) {
        try {
            // 1. Extraemos el token del encabezado (le sacamos el "Bearer ")
            String token = authHeader.replace("Bearer ", "");

            // 2. Usamos TU utilidad para sacar el ID del usuario
            String userIdStr = JwtUtil.getUserIdByToken(token);
            Long userId = Long.parseLong(userIdStr);

            // 3. Cambiamos la contraseña
            service.cambiarPassword(userId, dto.getPasswordActual(), dto.getPasswordNueva());

            return ResponseEntity.ok(Map.of("mensaje", "Contraseña actualizada con éxito"));

        } catch (RuntimeException e) {
            // Si la clave vieja está mal, tiramos error 400
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            // Si el token es inválido o expiró
            return ResponseEntity.status(401).body(Map.of("error", "Token inválido o expirado"));
        }
    }


    @GetMapping("user/me")
    public ResponseEntity<?> obtenerMiPerfil(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        Long userId = Long.parseLong(JwtUtil.getUserIdByToken(token));

        Usuario usuario = service.getUser(userId);
        return ResponseEntity.ok(usuario);
    }

    @PutMapping("user/me")
    public ResponseEntity<?> actualizarMiPerfil(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody EditarPerfilDTO dto) {
        try {
            String token = authHeader.replace("Bearer ", "");
            Long userId = Long.parseLong(JwtUtil.getUserIdByToken(token));

            service.actualizarPerfil(userId, dto);

            return ResponseEntity.ok(Map.of("mensaje", "Perfil actualizado correctamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

