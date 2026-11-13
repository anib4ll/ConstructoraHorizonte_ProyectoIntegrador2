package cl.duoc.horizonte.controller;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import static cl.duoc.horizonte.controller.ApiSupport.*;

@RestController
@RequestMapping("/api/hitos")
public class MilestoneController {
    private final JdbcTemplate db;
    public MilestoneController(JdbcTemplate db){this.db=db;}
    @GetMapping
    public List<Map<String,Object>> list(@RequestParam(defaultValue="1") long proyectoId){
        var list=db.queryForList("SELECT * FROM hitos WHERE proyecto_id=? ORDER BY fecha_comprometida",proyectoId);
        for(var h:list) h.put("actividad_ids",db.queryForList("SELECT item_id FROM hito_actividades WHERE hito_id=?",l(h.get("id"))).stream().map(x->x.get("item_id")).toList());
        return list;
    }
    @PostMapping @Transactional
    public List<Map<String,Object>> add(@RequestBody Map<String,Object> x){
        long p=lng(x,"proyecto_id");
        Long id=db.queryForObject("INSERT INTO hitos(proyecto_id,codigo,descripcion,responsable,fecha_comprometida,estado,criterio,evidencia) VALUES(?,?,?,?,?,?,?,?) RETURNING id",Long.class,
            p,required(x,"codigo"),required(x,"descripcion"),string(x,"responsable"),date(x,"fecha_comprometida",null),
            x.getOrDefault("estado","PENDIENTE"),string(x,"criterio"),string(x,"evidencia"));
        link(id,p,x.get("actividad_ids"));
        return list(p);
    }
    @PutMapping("/{id}") @Transactional
    public Map<String,Object> edit(@PathVariable long id,@RequestBody Map<String,Object> x){
        var h=one(db,"SELECT * FROM hitos WHERE id=?",id);
        String status=x.getOrDefault("estado",h.get("estado")).toString();
        check(Set.of("PENDIENTE","EN_CURSO","CUMPLIDO","ATRASADO","RECHAZADO").contains(status),"Estado de hito inválido");
        db.update("UPDATE hitos SET descripcion=?,responsable=?,fecha_comprometida=?,fecha_real=?,estado=?,criterio=?,evidencia=? WHERE id=?",
            x.getOrDefault("descripcion",h.get("descripcion")),x.getOrDefault("responsable",h.get("responsable")),
            date(x,"fecha_comprometida",ld(h.get("fecha_comprometida"))),
            date(x,"fecha_real",h.get("fecha_real")==null?null:ld(h.get("fecha_real"))),status,
            x.getOrDefault("criterio",h.get("criterio")),x.getOrDefault("evidencia",h.get("evidencia")),id);
        if(x.containsKey("actividad_ids")){db.update("DELETE FROM hito_actividades WHERE hito_id=?",id);link(id,l(h.get("proyecto_id")),x.get("actividad_ids"));}
        return one(db,"SELECT * FROM hitos WHERE id=?",id);
    }
    @DeleteMapping("/{id}")
    public Map<String,Object> delete(@PathVariable long id){
        one(db,"SELECT id FROM hitos WHERE id=?",id);
        db.update("DELETE FROM hitos WHERE id=?",id);
        return Map.of("eliminado",id);
    }
    @PatchMapping("/{id}/evidencia")
    public Map<String,Object> evidence(@PathVariable long id,@RequestBody Map<String,Object> x){
        db.update("UPDATE hitos SET evidencia=? WHERE id=?",required(x,"evidencia"),id);
        return one(db,"SELECT * FROM hitos WHERE id=?",id);
    }
    private void link(long h,long p,Object ids){
        if(!(ids instanceof List<?> list))return;
        for(Object id:list){
            long item=Long.parseLong(id.toString());
            var row=one(db,"SELECT proyecto_id FROM edt_items WHERE id=?",item);
            check(l(row.get("proyecto_id"))==p,"Hito y actividad deben pertenecer al mismo proyecto");
            db.update("INSERT INTO hito_actividades(hito_id,item_id) VALUES(?,?)",h,item);
        }
    }
}
