package cl.duoc.horizonte.controller;

import cl.duoc.horizonte.service.PlanningService;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import static cl.duoc.horizonte.controller.ApiSupport.*;

@RestController
@RequestMapping("/api")
public class PlanningController {
    private final JdbcTemplate db;
    private final PlanningService planner;
    public PlanningController(JdbcTemplate db,PlanningService planner){this.db=db;this.planner=planner;}
    @GetMapping("/dependencias")
    public List<Map<String,Object>> list(@RequestParam(defaultValue="1") long proyectoId){
        return db.queryForList("SELECT d.*,a.codigo AS codigo_predecesora,b.codigo AS codigo_sucesora FROM dependencias d JOIN edt_items a ON a.id=d.predecesora_id JOIN edt_items b ON b.id=d.sucesora_id WHERE d.proyecto_id=? ORDER BY d.id",proyectoId);
    }
    @PostMapping("/dependencias")
    public List<Map<String,Object>> add(@RequestBody Map<String,Object> body){
        long p=lng(body,"proyecto_id");
        planner.addDependency(p,lng(body,"predecesora_id"),lng(body,"sucesora_id"),required(body,"tipo"),integer(body,"desfase",0));
        return list(p);
    }
    @DeleteMapping("/dependencias/{id}") @Transactional
    public Map<String,Object> remove(@PathVariable long id){
        var d=one(db,"SELECT proyecto_id FROM dependencias WHERE id=?",id);
        db.update("DELETE FROM dependencias WHERE id=?",id);
        planner.recalculate(l(d.get("proyecto_id")));
        return Map.of("eliminado",id);
    }
    @GetMapping("/gantt")
    public Map<String,Object> gantt(@RequestParam(defaultValue="1") long proyectoId){
        return Map.of("items",db.queryForList("SELECT * FROM edt_items WHERE proyecto_id=? ORDER BY codigo",proyectoId),
            "dependencias",list(proyectoId),
            "hitos",db.queryForList("SELECT * FROM hitos WHERE proyecto_id=? ORDER BY fecha_comprometida",proyectoId));
    }
    @GetMapping("/calendario")
    public List<Map<String,Object>> holidays(@RequestParam(defaultValue="1") long proyectoId){
        return db.queryForList("SELECT * FROM dias_no_laborales WHERE proyecto_id=? ORDER BY fecha",proyectoId);
    }
    @PostMapping("/calendario") @Transactional
    public List<Map<String,Object>> addHoliday(@RequestBody Map<String,Object> body){
        long p=lng(body,"proyecto_id");
        db.update("INSERT INTO dias_no_laborales(proyecto_id,fecha,descripcion) VALUES(?,?,?)",p,date(body,"fecha",null),string(body,"descripcion"));
        planner.recalculate(p);
        return holidays(p);
    }
    @DeleteMapping("/calendario/{id}") @Transactional
    public Map<String,Object> deleteHoliday(@PathVariable long id){
        var row=one(db,"SELECT proyecto_id FROM dias_no_laborales WHERE id=?",id);
        db.update("DELETE FROM dias_no_laborales WHERE id=?",id);
        planner.recalculate(l(row.get("proyecto_id")));
        return Map.of("eliminado",id);
    }
}
