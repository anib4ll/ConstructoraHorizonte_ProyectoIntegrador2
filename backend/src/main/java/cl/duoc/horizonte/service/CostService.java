package cl.duoc.horizonte.service;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.time.DayOfWeek;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import static cl.duoc.horizonte.controller.ApiSupport.*;

@Service
public class CostService {
    private final JdbcTemplate db;
    public CostService(JdbcTemplate db){this.db=db;}

    public double planned(Map<String,Object> a){
        if("PROFESIONAL".equals(a.get("tipo")) || "TIPO_PROFESIONAL".equals(a.get("tipo")))
            return d(a.get("horas_semana"))*d(a.get("semanas"))*d(a.get("tarifa_hora"))*d(a.get("cantidad"));
        return d(a.get("cantidad"))*d(a.get("costo_unidad"));
    }
    public double actual(Map<String,Object> a){
        if("PROFESIONAL".equals(a.get("tipo")) || "TIPO_PROFESIONAL".equals(a.get("tipo")))
            return d(a.get("horas_semana_real"))*d(a.get("semanas_reales"))*d(a.get("tarifa_hora"))*d(a.get("cantidad_real"));
        return d(a.get("cantidad_real"))*d(a.get("costo_unidad"));
    }
    public List<Map<String,Object>> assignments(long projectId){
        var rows=db.queryForList("""
            SELECT a.*,r.nombre AS recurso,r.tipo,r.tarifa_hora,r.costo_unidad,r.horas_disponibles,
                   e.nombre AS actividad,e.codigo AS codigo_actividad,e.inicio,e.fin
            FROM asignaciones a JOIN recursos r ON r.id=a.recurso_id
            JOIN edt_items e ON e.id=a.item_id WHERE a.proyecto_id=? ORDER BY a.id
            """,projectId);
        for(var row:rows){row.put("costo_plan",planned(row));row.put("costo_real",actual(row));}
        return rows;
    }
    public Map<String,Object> summary(long projectId){
        var assignments=assignments(projectId);
        var items=db.queryForList("SELECT id,parent_id,codigo,nombre,tipo FROM edt_items WHERE proyecto_id=? ORDER BY codigo",projectId);
        Map<Long,Map<String,Object>> byId=new HashMap<>();
        for(var e:items){e.put("costo_plan",0.0);e.put("costo_real",0.0);byId.put(l(e.get("id")),e);}
        for(var a:assignments){
            var e=byId.get(l(a.get("item_id")));
            if(e!=null){e.put("costo_plan",d(e.get("costo_plan"))+d(a.get("costo_plan")));
                e.put("costo_real",d(e.get("costo_real"))+d(a.get("costo_real")));}
        }
        for(String type:List.of("ACTIVIDAD","ETAPA")){
            for(var child:items){
                String ct=child.get("tipo").toString();
                if((type.equals("ACTIVIDAD") && !ct.equals("SUBACTIVIDAD")) ||
                   (type.equals("ETAPA") && !ct.equals("ACTIVIDAD"))) continue;
                if(child.get("parent_id")==null)continue;
                var parent=byId.get(l(child.get("parent_id")));
                if(parent!=null){ parent.put("costo_plan",d(parent.get("costo_plan"))+d(child.get("costo_plan")));
                    parent.put("costo_real",d(parent.get("costo_real"))+d(child.get("costo_real"))); }
            }
        }
        double plan=items.stream().filter(e->"ETAPA".equals(e.get("tipo"))).mapToDouble(e->d(e.get("costo_plan"))).sum();
        double real=items.stream().filter(e->"ETAPA".equals(e.get("tipo"))).mapToDouble(e->d(e.get("costo_real"))).sum();
        return Map.of("plan",plan,"real",real,"porItem",items,"alertas",overallocations(projectId));
    }
    public List<Map<String,Object>> overallocations(long projectId){
        var rows=assignments(projectId);
        Map<String,Double> usage=new LinkedHashMap<>(),capacities=new HashMap<>();
        for(var row:rows){
            if(!"PROFESIONAL".equals(row.get("tipo")))continue;
            LocalDate first=ld(row.get("inicio")),last=ld(row.get("fin"));
            LocalDate week=first.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            while(!week.isAfter(last)){
                String key=row.get("recurso_id")+"_"+week;
                usage.merge(key,d(row.get("horas_semana"))*d(row.get("cantidad")),Double::sum);
                capacities.put(key,d(row.get("horas_disponibles")));
                week=week.plusWeeks(1);
            }
        }
        List<Map<String,Object>> warnings=new ArrayList<>();
        for(var e:usage.entrySet()) if(e.getValue()>capacities.get(e.getKey())+0.0001){
            String[] parts=e.getKey().split("_");
            warnings.add(Map.of("recursoId",Long.parseLong(parts[0]),"semana",parts[1],
                "horasAsignadas",e.getValue(),"horasDisponibles",capacities.get(e.getKey())));
        }
        return warnings;
    }
}
