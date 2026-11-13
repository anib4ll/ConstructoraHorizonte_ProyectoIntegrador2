package cl.duoc.horizonte.controller;

import cl.duoc.horizonte.service.PlanningService;
import java.time.LocalDate;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import static cl.duoc.horizonte.controller.ApiSupport.*;

@RestController
@RequestMapping("/api")
public class ProjectController {
    private final JdbcTemplate db;
    private final PlanningService planner;
    public ProjectController(JdbcTemplate db, PlanningService planner){this.db=db;this.planner=planner;}

    @GetMapping("/proyectos")
    public List<Map<String,Object>> projects(){return db.queryForList("SELECT * FROM proyectos ORDER BY id");}
    @GetMapping("/proyectos/{id}")
    public Map<String,Object> project(@PathVariable long id){return one(db,"SELECT * FROM proyectos WHERE id=?",id);}
    @PostMapping("/proyectos")
    public Map<String,Object> create(@RequestBody Map<String,Object> body){
        LocalDate start=date(body,"inicio",LocalDate.now());
        LocalDate end=date(body,"fin",start.plusMonths(9));
        check(!end.isBefore(start),"La fecha final debe ser posterior a la inicial");
        check(num(body,"presupuesto",0)>=0,"Presupuesto negativo");
        Long id=db.queryForObject("INSERT INTO proyectos(codigo,nombre,descripcion,responsable,inicio,fin,presupuesto) VALUES(?,?,?,?,?,?,?) RETURNING id",Long.class,
            required(body,"codigo"),required(body,"nombre"),string(body,"descripcion"),string(body,"responsable"),start,end,num(body,"presupuesto",0));
        return project(id);
    }
    @PutMapping("/proyectos/{id}")
    public Map<String,Object> update(@PathVariable long id,@RequestBody Map<String,Object> body){
        var old=project(id);
        LocalDate start=date(body,"inicio",ld(old.get("inicio"))),end=date(body,"fin",ld(old.get("fin")));
        check(!end.isBefore(start),"Fecha de término anterior al inicio");
        double budget=num(body,"presupuesto",d(old.get("presupuesto")));
        check(budget>=0,"El presupuesto no puede ser negativo");
        String state=body.containsKey("estado")?required(body,"estado"):old.get("estado").toString();
        check(Set.of("PLANIFICADO","EN_EJECUCION","SUSPENDIDO","TERMINADO").contains(state),"Estado inválido");
        db.update("UPDATE proyectos SET nombre=?,descripcion=?,responsable=?,inicio=?,fin=?,presupuesto=?,estado=? WHERE id=?",
            body.containsKey("nombre")?required(body,"nombre"):old.get("nombre"),
            body.getOrDefault("descripcion",old.get("descripcion")),
            body.getOrDefault("responsable",old.get("responsable")),start,end,budget,state,id);
        return project(id);
    }
    @GetMapping("/edt")
    public List<Map<String,Object>> edt(@RequestParam(defaultValue="1") long proyectoId){
        return db.queryForList("SELECT * FROM edt_items WHERE proyecto_id=? ORDER BY codigo",proyectoId);
    }
    @PostMapping("/edt") @Transactional
    public Map<String,Object> createItem(@RequestBody Map<String,Object> body){
        long projectId=lng(body,"proyecto_id"); project(projectId);
        String type=required(body,"tipo"); check(Set.of("ETAPA","ACTIVIDAD","SUBACTIVIDAD").contains(type),"Tipo EDT inválido");
        Long parent=optionalLong(body,"parent_id"); validateParent(projectId,parent,type);
        LocalDate base=date(body,"inicio_base",date(body,"inicio",LocalDate.now()));
        int duration=integer(body,"duracion",5);check(duration>0&&duration<1000,"Duración de 1 a 999 días");
        Long id=db.queryForObject("INSERT INTO edt_items(proyecto_id,parent_id,codigo,nombre,descripcion,tipo,responsable,inicio_base,inicio,fin,duracion) VALUES(?,?,?,?,?,?,?,?,?,?,?) RETURNING id",Long.class,
            projectId,parent,required(body,"codigo"),required(body,"nombre"),string(body,"descripcion"),type,string(body,"responsable"),base,base,base,duration);
        planner.recalculate(projectId);
        return item(id);
    }
    @PutMapping("/edt/{id}") @Transactional
    public Map<String,Object> updateItem(@PathVariable long id,@RequestBody Map<String,Object> body){
        var old=item(id); long projectId=l(old.get("proyecto_id"));
        String type=old.get("tipo").toString();
        LocalDate base=date(body,"inicio_base",ld(old.get("inicio_base")));
        int duration=integer(body,"duracion",i(old.get("duracion")));
        check(duration>0 && duration<1000,"Duración entre 1 y 999 días");
        if(!type.equals("SUBACTIVIDAD")){
            check(!body.containsKey("duracion")&&!body.containsKey("inicio_base"),"Las fechas de padres se consolidan automáticamente");
        }
        double progress=num(body,"progreso",d(old.get("progreso")));
        check(progress>=0&&progress<=100,"El avance debe estar entre 0 y 100");
        String state=body.containsKey("estado")?required(body,"estado"):old.get("estado").toString();
        check(Set.of("PENDIENTE","EN_CURSO","COMPLETADA","BLOQUEADA").contains(state),"Estado inválido");
        db.update("UPDATE edt_items SET nombre=?,descripcion=?,responsable=?,inicio_base=?,duracion=?,progreso=?,estado=?,inicio_real=?,fin_real=? WHERE id=?",
            body.getOrDefault("nombre",old.get("nombre")),body.getOrDefault("descripcion",old.get("descripcion")),
            body.getOrDefault("responsable",old.get("responsable")),base,duration,progress,state,
            date(body,"inicio_real",old.get("inicio_real")==null?null:ld(old.get("inicio_real"))),
            date(body,"fin_real",old.get("fin_real")==null?null:ld(old.get("fin_real"))),id);
        planner.recalculate(projectId);
        return item(id);
    }
    @PatchMapping("/edt/{id}/avance") @Transactional
    public Map<String,Object> progress(@PathVariable long id,@RequestBody Map<String,Object> body){
        var old=item(id);check("SUBACTIVIDAD".equals(old.get("tipo")),"El avance se ingresa en subactividades");
        double p=num(body,"progreso",d(old.get("progreso")));
        check(p>=0&&p<=100,"Avance entre 0 y 100");
        db.update("UPDATE edt_items SET progreso=?,estado=? WHERE id=?",p,p==100?"COMPLETADA":(p>0?"EN_CURSO":"PENDIENTE"),id);
        planner.recalculate(l(old.get("proyecto_id")));
        return item(id);
    }
    @DeleteMapping("/edt/{id}") @Transactional
    public Map<String,Object> delete(@PathVariable long id){
        var old=item(id);
        check(db.queryForObject("SELECT COUNT(*) FROM edt_items WHERE parent_id=?",Integer.class,id)==0,"No se puede eliminar un elemento con hijos");
        check(db.queryForObject("SELECT COUNT(*) FROM dependencias WHERE predecesora_id=? OR sucesora_id=?",Integer.class,id,id)==0,"Eliminar antes sus dependencias");
        check(db.queryForObject("SELECT COUNT(*) FROM asignaciones WHERE item_id=?",Integer.class,id)==0,"Eliminar antes sus asignaciones");
        check(db.queryForObject("SELECT COUNT(*) FROM hito_actividades WHERE item_id=?",Integer.class,id)==0,"Quitar antes la asociación con hitos");
        check(db.queryForObject("SELECT COUNT(*) FROM cambios WHERE item_id=?",Integer.class,id)==0,"El elemento está relacionado con una solicitud de cambio");
        db.update("DELETE FROM edt_items WHERE id=?",id);
        planner.recalculate(l(old.get("proyecto_id")));
        return Map.of("eliminado",id);
    }
    private Map<String,Object> item(long id){return one(db,"SELECT * FROM edt_items WHERE id=?",id);}
    private void validateParent(long projectId,Long parent,String type){
        if("ETAPA".equals(type)) {check(parent==null,"La etapa no tiene padre");return;}
        check(parent!=null,"Actividad y subactividad necesitan padre");
        var p=item(parent);
        check(l(p.get("proyecto_id"))==projectId,"El padre debe ser del mismo proyecto");
        check(("ACTIVIDAD".equals(type)&&"ETAPA".equals(p.get("tipo")))||
              ("SUBACTIVIDAD".equals(type)&&"ACTIVIDAD".equals(p.get("tipo"))),"Jerarquía inválida: ETAPA → ACTIVIDAD → SUBACTIVIDAD");
    }
}
