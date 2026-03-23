package lumato.desarrolo.tiendavirtual.service;

import lumato.desarrolo.tiendavirtual.dto.*;
import lumato.desarrolo.tiendavirtual.repository.DetallePedidoRepository;
import lumato.desarrolo.tiendavirtual.repository.PedidoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class EstadisticasServiceImp implements EstadisticasService {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private DetallePedidoRepository detallePedidoRepository;

    // 1. CALCULAMOS LOS KPIs GLOBALES (Crecimiento)
    @Override
    public KpiDTO obtenerKpisGlobales() {
        KpiDTO kpi = new KpiDTO();

        // Fechas Meses
        LocalDateTime inicioMesActual = YearMonth.now().atDay(1).atStartOfDay();
        LocalDateTime inicioMesPasado = YearMonth.now().minusMonths(1).atDay(1).atStartOfDay();
        LocalDateTime finMesPasado = YearMonth.now().minusMonths(1).atEndOfMonth().atTime(23, 59, 59);

        // Fechas Días (Seguro para abarcar todo el día completo)
        LocalDateTime inicioHoy = LocalDate.now().atStartOfDay();
        LocalDateTime finHoy = LocalDate.now().atTime(23, 59, 59); // <-- Aseguramos el final del día

        LocalDateTime inicioAyer = LocalDate.now().minusDays(1).atStartOfDay();
        LocalDateTime finAyer = LocalDate.now().minusDays(1).atTime(23, 59, 59);

        // Consultas a BD usando finHoy en lugar de ahora()
        Double ingresosMesActual = pedidoRepository.sumIngresosEntreFechas(inicioMesActual, finHoy);
        Double ingresosMesPasadoTotal = pedidoRepository.sumIngresosEntreFechas(inicioMesPasado, finMesPasado);

        Long ventasHoyTotal = pedidoRepository.countPedidosEntreFechas(inicioHoy, finHoy);
        Long ventasAyerTotal = pedidoRepository.countPedidosEntreFechas(inicioAyer, finAyer);

        // Porcentajes
        double porcentajeMes = ingresosMesPasadoTotal == 0 ? (ingresosMesActual > 0 ? 100 : 0)
                : ((ingresosMesActual - ingresosMesPasadoTotal) / ingresosMesPasadoTotal) * 100;

        double porcentajeHoy = ventasAyerTotal == 0 ? (ventasHoyTotal > 0 ? 100 : 0)
                : ((double)(ventasHoyTotal - ventasAyerTotal) / ventasAyerTotal) * 100;

        kpi.setIngresosMes(ingresosMesActual);
        kpi.setPorcentajeMes(porcentajeMes);
        kpi.setVentasHoy(ventasHoyTotal);
        kpi.setPorcentajeHoy(porcentajeHoy);

        return kpi;
    }

    // 2. RANKING DE PRODUCTOS (Limitamos a 5 desde la BD)
    @Override
    public List<TopProductoDTO> obtenerTop5Productos(LocalDateTime inicio, LocalDateTime fin) {
        return detallePedidoRepository.findTopProductos(inicio, fin, PageRequest.of(0, 5));
    }

    // 3. STATS DE UN SOLO PRODUCTO
    @Override
    public ProductoStatDTO obtenerStatsProducto(Long productoId) {
        return detallePedidoRepository.findStatsByProductoId(productoId);
    }

    // 4. DATOS DEL GRÁFICO (Optimizado para no usar RAM)
    @Override
    public List<PuntoGraficoDTO> obtenerDatosGrafico(LocalDateTime inicio, LocalDateTime fin) {
        List<Object[]> ventas = pedidoRepository.findVentasParaGrafico(inicio, fin);
        List<PuntoGraficoDTO> grafico = new ArrayList<>();

        boolean esMismoDia = inicio.toLocalDate().isEqual(fin.toLocalDate());

        if (esMismoDia) {
            // Agrupamos por hora (0 a 23)
            double[] ventasPorHora = new double[24];
            for (Object[] v : ventas) {
                LocalDateTime fecha = (LocalDateTime) v[0];
                Double total = (Double) v[1];
                ventasPorHora[fecha.getHour()] += total;
            }
            for (int i = 0; i < 24; i++) {
                grafico.add(new PuntoGraficoDTO(i + ":00", ventasPorHora[i]));
            }
        } else {
            // Agrupamos por día
            Map<LocalDate, Double> ventasPorDia = new LinkedHashMap<>();
            LocalDate fechaActual = inicio.toLocalDate();
            LocalDate fechaFinal = fin.toLocalDate();

            // Inicializamos el mapa con ceros para que no falten días en el gráfico
            while (!fechaActual.isAfter(fechaFinal)) {
                ventasPorDia.put(fechaActual, 0.0);
                fechaActual = fechaActual.plusDays(1);
            }

            for (Object[] v : ventas) {
                LocalDate fecha = ((LocalDateTime) v[0]).toLocalDate();
                Double total = (Double) v[1];
                if (ventasPorDia.containsKey(fecha)) {
                    ventasPorDia.put(fecha, ventasPorDia.get(fecha) + total);
                }
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM", new Locale("es", "AR"));
            for (Map.Entry<LocalDate, Double> entry : ventasPorDia.entrySet()) {
                grafico.add(new PuntoGraficoDTO(entry.getKey().format(formatter), entry.getValue()));
            }
        }
        return grafico;
    }
    @Override
    public Optional<ProductoStatDTO> obtenerEstadisticasHistoricasDeProducto(Long id){
        return pedidoRepository.obtenerEstadisticasHistoricasPorProducto(id);
    }
}