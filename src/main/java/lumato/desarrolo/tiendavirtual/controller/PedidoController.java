package lumato.desarrolo.tiendavirtual.controller;


import lumato.desarrolo.tiendavirtual.dto.ActualizarEstadoDTO;
import lumato.desarrolo.tiendavirtual.dto.NotificacionPedidoDTO;
import lumato.desarrolo.tiendavirtual.dto.PedidoRequestDTO;
import lumato.desarrolo.tiendavirtual.dto.PedidoResponseDTO;
import lumato.desarrolo.tiendavirtual.model.Pedido;
import lumato.desarrolo.tiendavirtual.model.enums.EstadoPedido;
import lumato.desarrolo.tiendavirtual.service.PedidoService;
import lumato.desarrolo.tiendavirtual.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pedidos")
@CrossOrigin(origins = "*")
public class PedidoController {

    @Autowired
    private PedidoService pedidoService;

    // Endpoint Web: POST /api/pedido
    @PostMapping
    public ResponseEntity<PedidoResponseDTO> realizarCompra(@RequestBody PedidoRequestDTO request) {
        try {
            PedidoResponseDTO response = pedidoService.procesarNuevaCompra(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (RuntimeException e) {
            System.out.println("Error al procesar pedido: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    public ResponseEntity<Page<Pedido>> verTodosLosPedidos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) EstadoPedido estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {

        // las convertimos a LocalDateTime (inicio del día y fin del día)
        LocalDateTime inicio = (fechaInicio != null) ? fechaInicio.atStartOfDay() : null;
        LocalDateTime fin = (fechaFin != null) ? fechaFin.atTime(23, 59, 59) : null;

        Page<Pedido> pedidos = pedidoService.obtenerPedidosPaginadosYFiltrados(page, size, estado, inicio, fin);

        return ResponseEntity.ok(pedidos);
    }

    // Endpoint Admin: GET /api/pedidos/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Pedido> verDetallePedido(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(pedidoService.obtenerPedidoPorId(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Endpoint Admin: PUT /api/pedidos/{id}/estado
    @PutMapping("/{id}/estado")
    public ResponseEntity<Pedido> cambiarEstado(@PathVariable Long id, @RequestBody ActualizarEstadoDTO dto) {
        try {
            Pedido pedidoActualizado = pedidoService.cambiarEstadoPedido(id, dto.getNuevoEstado());
            return ResponseEntity.ok(pedidoActualizado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Endpoint Admin: DELETE /api/pedidos/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarPedido(@PathVariable Long id) {
        pedidoService.eliminarPedido(id);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/recientes")
    public ResponseEntity<List<Pedido>> obtenerPedidosRecientes() {
        List<Pedido> ultimosPedidos = pedidoService.obtenerUltimos5Pedidos();
        return ResponseEntity.ok(ultimosPedidos);
    }

    @GetMapping("/notificaciones")
    public ResponseEntity<List<NotificacionPedidoDTO>> obtenerNotificaciones() {
        return ResponseEntity.ok(pedidoService.obtenerUltimasNotificaciones());
    }
    @GetMapping("/mis-pedidos")
    public ResponseEntity<?> obtenerMisPedidos(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            Long userId = Long.parseLong(JwtUtil.getUserIdByToken(token));

            List<Pedido> misPedidos = pedidoService.obtenerPedidosDeUsuario(userId);
            return ResponseEntity.ok(misPedidos);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "No autorizado o token inválido"));
        }
    }
}