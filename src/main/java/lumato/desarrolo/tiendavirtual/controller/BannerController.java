package lumato.desarrolo.tiendavirtual.controller;

import lumato.desarrolo.tiendavirtual.model.Banner;
import lumato.desarrolo.tiendavirtual.service.BannerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/banner")
@CrossOrigin(origins = "*")
public class BannerController {

    @Autowired
    private BannerService bannerService;

    // PÚBLICO: Cualquier usuario de la tienda puede verlo
    @GetMapping
    public ResponseEntity<?> obtenerBanner() {
        try {
            Banner banner = bannerService.obtenerBannerActivo();
            if (banner == null) {
                // Si no hay banner, devolvemos un 204 No Content
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok(banner);
        } catch (Exception e) {
            // Si algo explota, atajamos el error para que no rompa el CORS
            return ResponseEntity.internalServerError().body("Error al buscar el banner");
        }
    }

    // PROTEGIDO: Solo el admin puede modificarlo
    @PutMapping
    public ResponseEntity<Banner> actualizarBanner(@RequestBody Banner banner) {
        return ResponseEntity.ok(bannerService.actualizarBanner(banner));
    }
}