package lumato.desarrolo.tiendavirtual.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lumato.desarrolo.tiendavirtual.utils.JwtUtil;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;

@Component
public class CustomAccessFilter implements Filter {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        // 1. Si es una solicitud CORS de Pre-flight (OPTIONS), la dejamos pasar directo
        if (request.getMethod().equalsIgnoreCase("OPTIONS")) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();


        if (path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs") ||
                path.equals("/api/auth/login") ||
                path.equals("/api/register") ||
                path.startsWith("/api/categorias") ||
                path.startsWith("/api/productos") ||
                path.startsWith("/api/pedidos") ||
                path.startsWith("/api/estadisticas") ||
                path.startsWith("/api/user")) {

            filterChain.doFilter(request, response);
            return;
        }

        // 3. Proceso estricto del Token JWT para las rutas protegidas
        try {
            String header = request.getHeader("Authorization");

            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7); // Sacamos la palabra "Bearer "
                String userId = JwtUtil.getUserIdByToken(token); // Tu método actual

                // Si el token es válido, le decimos a Spring Security que el usuario está autorizado
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
                SecurityContextHolder.getContext().setAuthentication(auth);

                filterChain.doFilter(request, response);
            } else {
                // No mandó el token o está mal formateado
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write("Acceso denegado: Token no proporcionado o invalido");
            }
        } catch (Exception e) {
            // El token expiró o está corrupto
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("Acceso denegado: Token expirado o corrupto");
        }
    }
}