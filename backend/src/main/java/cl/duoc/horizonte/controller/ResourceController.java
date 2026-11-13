package cl.duoc.horizonte.controller;

import cl.duoc.horizonte.service.CostService;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import static cl.duoc.horizonte.controller.ApiSupport.*;

@RestController
@RequestMapping("/api")
public class ResourceController {
    private final JdbcTemplate db;
    private final CostService costs;
    public ResourceController(JdbcTemplate db,CostService costs){this.db=db;this.costs=costs;}
    @GetMapping("/recursos")
    public List<Map<String,Object>> resources(@RequestParam(defaultValue="1") long proyectoId){
        return db.queryForList("SELECT * FROM recursos WHERE proyecto_id=? ORDER BY codigo",proyectoId);
    }
    @PostMapping("/recursos")
    public Map<String,Object> addResource(@RequestBody Map<String,Object> x){
        long p=lng(x,"proyecto_id");
        String type=required(x,"tipo");
        check(Set.of("PROFESIONAL","TIPO_PROFESIONAL","LICENCIA","MATERIAL","EQUIPAMIENTO","OTRO").contains(type),"Tipo de recurso inválido");
        check(num(x,"tarifa_hora",0)>=0&&num(x,"costo_unidad",0)>=0,"Costos no pueden ser negativos");
        Long id=db.queryForObject("INSERT INTO recursos(proyecto_id,codigo,nombre,tipo,especialidad,tarifa_hora,horas_disponibles,costo_unidad,cantidad_disponible,unidad) VALUES(?,?,?,?,?,?,?,?,?,?) RETURNING id",Long.class,
            p,required(x,"codigo"),required(x,"nombre"),type,string(x,"especialidad"),num(x,"tarifa_hora",0),num(x,"horas_disponibles",40),num(x,"costo_unidad",0),num(x,"cantidad_disponible",1),string(x,"unidad"));
        return one(db,"SELECT * FROM recursos WHERE id=?",id);
    }
    @PutMapping("/recursos/{id}")
    public Map<String,Object> editResource(@PathVariable long id,@RequestBody Map<String,Object> x){
        var old=one(db,"SELECT * FROM recursos WHERE id=?",id);
        double tariff=num(x,"tarifa_hora",d(old.get("tarifa_hora")));
        double unit=num(x,"costo_unidad",d(old.get("costo_unidad")));
        check(tariff>=0&&unit>=0,"Tarifas negativas no permitidas");
        db.update("UPDATE recursos SET nombre=?,especialidad=?,tarifa_hora=?,horas_disponibles=?,costo_unidad=?,cantidad_disponible=? WHERE id=?",
            x.getOrDefault("nombre",old.get("nombre")),x.getOrDefault("especialidad",old.get("especialidad")),tariff,
            num(x,"horas_disponibles",d(old.get("horas_disponibles"))),unit,
            num(x,"cantidad_disponible",d(old.get("cantidad_disponible"))),id);
        return one(db,"SELECT * FROM recursos WHERE id=?",id);
    }
    @DeleteMapping("/recursos/{id}")
    public Map<String,Object> deleteResource(@PathVariable long id){
        one(db,"SELECT id FROM recursos WHERE id=?",id);
        check(db.queryForObject("SELECT COUNT(*) FROM asignaciones WHERE recurso_id=?",Integer.class,id)==0,
            "No se puede eliminar un recurso que tiene asignaciones. Retírelas primero.");
        db.update("DELETE FROM recursos WHERE id=?",id);
        return Map.of("eliminado",id);
    }
    @GetMapping("/asignaciones")
    public List<Map<String,Object>> assignments(@RequestParam(defaultValue="1") long proyectoId){return costs.assignments(proyectoId);}
    @PostMapping("/asignaciones")
    public List<Map<String,Object>> addAssignment(@RequestBody Map<String,Object> x){
        long project=lng(x,"proyecto_id"),item=lng(x,"item_id"),resource=lng(x,"recurso_id");
        var i=one(db,"SELECT proyecto_id,tipo FROM edt_items WHERE id=?",item);
        var r=one(db,"SELECT proyecto_id FROM recursos WHERE id=?",resource);
        check(l(i.get("proyecto_id"))==project&&l(r.get("proyecto_id"))==project,"Los recursos y actividades deben pertenecer al proyecto");
        check("SUBACTIVIDAD".equals(i.get("tipo")),"Asigne recursos a subactividades (hojas)");
        check(num(x,"cantidad",1)>=0&&num(x,"horas_semana",0)>=0&&num(x,"semanas",1)>=0,"Cantidades y horas negativas");
        db.update("INSERT INTO asignaciones(proyecto_id,item_id,recurso_id,cantidad,cantidad_real,horas_semana,horas_semana_real,semanas,semanas_reales) VALUES(?,?,?,?,?,?,?,?,?)",
            project,item,resource,num(x,"cantidad",1),num(x,"cantidad_real",0),num(x,"horas_semana",0),num(x,"horas_semana_real",0),num(x,"semanas",1),num(x,"semanas_reales",0));
        return assignments(project);
    }
    @PutMapping("/asignaciones/{id}")
    public Map<String,Object> editAssignment(@PathVariable long id,@RequestBody Map<String,Object> x){
        var old=one(db,"SELECT * FROM asignaciones WHERE id=?",id);
        double count=num(x,"cantidad",d(old.get("cantidad"))),actual=num(x,"cantidad_real",d(old.get("cantidad_real")));
        double hours=num(x,"horas_semana",d(old.get("horas_semana"))),actualHours=num(x,"horas_semana_real",d(old.get("horas_semana_real")));
        double weeks=num(x,"semanas",d(old.get("semanas"))),actualWeeks=num(x,"semanas_reales",d(old.get("semanas_reales")));
        check(count>=0&&actual>=0&&hours>=0&&actualHours>=0&&weeks>=0&&actualWeeks>=0,"Cantidades u horas negativas");
        db.update("UPDATE asignaciones SET cantidad=?,cantidad_real=?,horas_semana=?,horas_semana_real=?,semanas=?,semanas_reales=? WHERE id=?",
            count,actual,hours,actualHours,weeks,actualWeeks,id);
        return one(db,"SELECT * FROM asignaciones WHERE id=?",id);
    }
    @DeleteMapping("/asignaciones/{id}")
    public Map<String,Object> deleteAssignment(@PathVariable long id){
        check(db.queryForObject("SELECT COUNT(*) FROM cambios WHERE asignacion_id=?",Integer.class,id)==0,"Asignación asociada a una solicitud de cambio");
        db.update("DELETE FROM asignaciones WHERE id=?",id);
        return Map.of("eliminado",id);
    }
    @GetMapping("/costos")
    public Map<String,Object> summary(@RequestParam(defaultValue="1") long proyectoId){return costs.summary(proyectoId);}
}
