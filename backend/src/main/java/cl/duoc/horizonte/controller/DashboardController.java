package cl.duoc.horizonte.controller;

import cl.duoc.horizonte.service.CostService;
import cl.duoc.horizonte.service.KpiService;
import java.time.LocalDate;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import static cl.duoc.horizonte.controller.ApiSupport.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final JdbcTemplate db;private final CostService costs;private final KpiService kpis;
    public DashboardController(JdbcTemplate db,CostService costs,KpiService kpis){this.db=db;this.costs=costs;this.kpis=kpis;}
    @GetMapping("/proyecto")
    public Map<String,Object> project(@RequestParam(defaultValue="1") long proyectoId,
       @RequestParam(required=false) String etapa,@RequestParam(required=false) String responsable,
       @RequestParam(required=false) String desde,@RequestParam(required=false) String hasta){
        var project=one(db,"SELECT * FROM proyectos WHERE id=?",proyectoId);
        var items=db.queryForList("SELECT * FROM edt_items WHERE proyecto_id=? AND tipo='SUBACTIVIDAD'",proyectoId);
        if(etapa!=null&&!etapa.isBlank())items.removeIf(x->!x.get("codigo").toString().startsWith(etapa+"."));
        if(responsable!=null&&!responsable.isBlank())items.removeIf(x->!x.get("responsable").toString().toLowerCase().contains(responsable.toLowerCase()));
        if(desde!=null&&!desde.isBlank())items.removeIf(x->ld(x.get("fin")).isBefore(LocalDate.parse(desde)));
        if(hasta!=null&&!hasta.isBlank())items.removeIf(x->ld(x.get("inicio")).isAfter(LocalDate.parse(hasta)));
        var mil=db.queryForList("SELECT * FROM hitos WHERE proyecto_id=? ORDER BY fecha_comprometida",proyectoId);
        var cost=costs.summary(proyectoId);
        long delayed=items.stream().filter(x->d(x.get("progreso"))<100&&ld(x.get("fin")).isBefore(LocalDate.now())).count();
        double progress=items.stream().mapToDouble(x->d(x.get("progreso"))).average().orElse(0);
        return Map.of("proyecto",project,"actividades",items,"avanceFiltrado",progress,"atrasadas",delayed,
            "hitos",mil,"costos",cost,"kpis",kpis.refresh(proyectoId));
    }
    @GetMapping("/cambios")
    public Map<String,Object> changes(@RequestParam(defaultValue="1") long proyectoId,
        @RequestParam(required=false) String estado,@RequestParam(required=false) String riesgo,
        @RequestParam(required=false) String responsable){
        var changes=db.queryForList("SELECT * FROM cambios WHERE proyecto_id=?",proyectoId);
        if(estado!=null&&!estado.isBlank())changes.removeIf(c->!estado.equals(c.get("estado")));
        if(riesgo!=null&&!riesgo.isBlank())changes.removeIf(c->!riesgo.equals(c.get("riesgo")));
        if(responsable!=null&&!responsable.isBlank())changes.removeIf(c->!c.get("responsable").toString().toLowerCase().contains(responsable.toLowerCase()));
        Map<String,Long> states=new LinkedHashMap<>(),risks=new LinkedHashMap<>();
        for(var c:changes){states.merge(c.get("estado").toString(),1L,Long::sum);risks.merge(c.get("riesgo").toString(),1L,Long::sum);}
        var steps=db.queryForList("SELECT ec.* FROM etapas_cambio ec JOIN cambios c ON c.id=ec.cambio_id WHERE c.proyecto_id=?",proyectoId);
        long completed=steps.stream().filter(e->Boolean.TRUE.equals(e.get("completada"))).count();
        double est=changes.stream().mapToDouble(c->d(c.get("costo_estimado"))).sum();
        double real=changes.stream().mapToDouble(c->d(c.get("costo_real"))).sum();
        long late=changes.stream().filter(c->c.get("fecha_comprometida")!=null&&ld(c.get("fecha_comprometida")).isBefore(LocalDate.now())&&!Set.of("CERRADO","RECHAZADO").contains(c.get("estado"))).count();
        return Map.of("solicitudes",changes,"porEstado",states,"porRiesgo",risks,"etapasCompletadas",completed,
            "etapasTotales",steps.size(),"costoEstimado",est,"costoReal",real,"atrasadas",late,"kpis",kpis.refresh(proyectoId));
    }
}
