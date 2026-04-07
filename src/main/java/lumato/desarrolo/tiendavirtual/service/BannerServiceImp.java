package lumato.desarrolo.tiendavirtual.service;

import lumato.desarrolo.tiendavirtual.model.Banner;
import lumato.desarrolo.tiendavirtual.repository.BannerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BannerServiceImp implements BannerService {

    @Autowired
    private BannerRepository bannerRepository;

    @Override
    public Banner obtenerBannerActivo() {
        return bannerRepository.findAll().stream().findFirst()
                .orElseGet(() -> {
                    Banner defaultBanner = new Banner();
                    defaultBanner.setEtiqueta("Oferta de Temporada");
                    defaultBanner.setTitulo("Elegancia en cada Gota.");
                    defaultBanner.setSubtitulo("Hasta 40% de descuento en fragancias premium y artículos de cuidado personal.");
                    defaultBanner.setImagenUrl("https://images.unsplash.com/photo-1594035910387-fea47794261f?q=80&w=1000&auto=format&fit=crop");
                    defaultBanner.setProductoDestacado("Maison de Luxe No. 5");
                    defaultBanner.setVariante("banner");
                    return bannerRepository.save(defaultBanner);
                });
    }

    @Override
    public Banner actualizarBanner(Banner datosNuevos) {
        // Buscamos el banner existente (o creamos el default)
        Banner bannerActual = obtenerBannerActivo();

        // Pisamos los datos
        bannerActual.setEtiqueta(datosNuevos.getEtiqueta());
        bannerActual.setTitulo(datosNuevos.getTitulo());
        bannerActual.setSubtitulo(datosNuevos.getSubtitulo());
        bannerActual.setImagenUrl(datosNuevos.getImagenUrl());
        bannerActual.setProductoDestacado(datosNuevos.getProductoDestacado());
        bannerActual.setVariante(datosNuevos.getVariante());
        // Guardamos y devolvemos
        return bannerRepository.save(bannerActual);
    }
}