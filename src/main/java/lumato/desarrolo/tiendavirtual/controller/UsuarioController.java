package lumato.desarrolo.tiendavirtual.controller;

import lumato.desarrolo.tiendavirtual.model.Usuario;
import lumato.desarrolo.tiendavirtual.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}

