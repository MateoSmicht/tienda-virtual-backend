package lumato.desarrolo.tiendavirtual.controller;

import lumato.desarrolo.tiendavirtual.dto.AuthResponseDTO;
import lumato.desarrolo.tiendavirtual.dto.RequestLogin;
import lumato.desarrolo.tiendavirtual.model.Usuario;
import lumato.desarrolo.tiendavirtual.service.AuthService;
import lumato.desarrolo.tiendavirtual.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthService service;

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@RequestBody RequestLogin request) {
        // Si la contraseña está mal, el servicio lanza la excepción.
        Usuario user = service.login(request.getEmail(), request.getPassword());

        // Generamos el token
        String token = JwtUtil.generateToken(user);

        // Devolvemos un JSON con código HTTP 200 OK
        return ResponseEntity.ok(new AuthResponseDTO(token, user.getId(), user.getEmail()));
    }
}
