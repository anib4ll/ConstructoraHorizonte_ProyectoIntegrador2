package cl.duoc.horizonte.controller;

import cl.duoc.horizonte.service.PlanningService;
import java.time.LocalDate;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import static cl.duoc.horizonte.controller.ApiSupport.*;

@RestController
@RequestMapping("/api/cambios")
public class ChangeController {
    public static final List<String> STAGES=List.of("Identificación","Evaluación","Análisis de riesgos","Planificación",
        "Comunicación","Implementación","Monitoreo","Comunicación de resultados","Acciones correctivas","Adopción cultural");
    private final JdbcTemplate db;private final PlanningService planning;
    public ChangeController(JdbcTemplate db,PlanningService planning){this.db=db;this.planning=planning;}
    @GetMapping
    public List<Map<String,Object>> all(@RequestParam(defaultValue="1") long proyectoId,
        @RequestParam(required=false) String estado,@RequestParam(required=false) String riesgo){
        var rows=db.queryForList("SELECT * FROM cambios WHERE proyecto_id=? ORDER BY codigo",proyectoId);
        return rows.stream().filter(x->estado==null||estado.isBlank()||estado.equals(x.get("estado")))
            .filter(x->riesgo==null||riesgo.isBlank()||riesgo.equals(x.get("riesgo"))).toList();
    }
    @GetMapping("/{id}")
    public Map<String,Object> oneChange(@PathVariable long id){return one(db,"SELECT * FROM cambios WHERE id=?",id);}
    @GetMapping("/{id}/etapas")
    public List<Map<String,Object>> stages(@PathVariable long id){oneChange(id);return db.queryForList("SELECT * FROM etapas_cambio WHERE cambio_id=? ORDER BY orden",id);}
    @GetMapping("/{id}/trazabilidad")
    public List<Map<String,Object>> baseline(@PathVariable long id){return db.queryForList("SELECT * FROM linea_base WHERE cambio_id=? ORDER BY fecha,id",id);}
    @PostMapping @Transactional
    public Map<String,Object> add(@RequestBody Map<String,Object> x){
        long project=lng(x,"proyecto_id");
        validateAffected(project,optionalLong(x,"item_id"),optionalLong(x,"asignacion_id"));
        check(num(x,"costo_estimado",0)>=0,"Costo estimado negativo");
        Long id=db.queryForObject("""
            INSERT INTO cambios(proyecto_id,codigo,solicitante,descripcion,justificacion,tipo,prioridad,riesgo,impacto,responsable,
               estado,fecha_comprometida,costo_estimado,item_id,nueva_duracion,asignacion_id,nueva_cantidad)
            VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) RETURNING id
            """,Long.class,project,required(x,"codigo"),required(x,"solicitante"),required(x,"descripcion"),string(x,"justificacion"),
            required(x,"tipo"),x.getOrDefault("prioridad","MEDIA"),x.getOrDefault("riesgo","MEDIO"),string(x,"impacto"),
            string(x,"responsable"),"ABIERTO",date(x,"fecha_comprometida",null),num(x,"costo_estimado",0),
            optionalLong(x,"item_id"),x.get("nueva_duracion"),optionalLong(x,"asignacion_id"),x.get("nueva_cantidad"));
        for(int i=0;i<STAGES.size();i++) db.update("INSERT INTO etapas_cambio(cambio_id,orden,nombre) VALUES(?,?,?)",id,i+1,STAGES.get(i));
        return oneChange(id);
    }
    @PutMapping("/{id}")
    public Map<String,Object> edit(@PathVariable long id,@RequestBody Map<String,Object> x){
        var old=oneChange(id);
        check(Set.of("ABIERTO","EN_EVALUACION").contains(old.get("estado")),"Una solicitud aprobada o cerrada no se edita directamente");
        long project=l(old.get("proyecto_id"));
        Long item=x.containsKey("item_id")?optionalLong(x,"item_id"):(old.get("item_id")==null?null:l(old.get("item_id")));
        Long ass=x.containsKey("asignacion_id")?optionalLong(x,"asignacion_id"):(old.get("asignacion_id")==null?null:l(old.get("asignacion_id")));
        validateAffected(project,item,ass);
        String state=x.getOrDefault("estado",old.get("estado")).toString();
        check(Set.of("ABIERTO","EN_EVALUACION").contains(state),"Use aprobar o rechazar para resolver una solicitud");
        db.update("""
          UPDATE cambios SET descripcion=?,justificacion=?,prioridad=?,riesgo=?,impacto=?,responsable=?,estado=?,
            fecha_comprometida=?,costo_estimado=?,item_id=?,nueva_duracion=?,asignacion_id=?,nueva_cantidad=? WHERE id=?
          """,x.getOrDefault("descripcion",old.get("descripcion")),x.getOrDefault("justificacion",old.get("justificacion")),
          x.getOrDefault("prioridad",old.get("prioridad")),x.getOrDefault("riesgo",old.get("riesgo")),
          x.getOrDefault("impacto",old.get("impacto")),x.getOrDefault("responsable",old.get("responsable")),state,
          date(x,"fecha_comprometida",old.get("fecha_comprometida")==null?null:ld(old.get("fecha_comprometida"))),
          num(x,"costo_estimado",d(old.get("costo_estimado"))),item,x.getOrDefault("nueva_duracion",old.get("nueva_duracion")),
          ass,x.getOrDefault("nueva_cantidad",old.get("nueva_cantidad")),id);
        return oneChange(id);
    }
    @PostMapping("/{id}/aprobar") @Transactional
    public Map<String,Object> approve(@PathVariable long id){
        var ch=oneChange(id);
        check(Set.of("ABIERTO","EN_EVALUACION").contains(ch.get("estado")),"La solicitud ya fue resuelta");
        long p=l(ch.get("proyecto_id"));
        int modifications=0;
        if(ch.get("item_id")!=null && ch.get("nueva_duracion")!=null){
            long target=l(ch.get("item_id"));int newDur=i(ch.get("nueva_duracion"));
            check(newDur>=1&&newDur<=999,"Duración solicitada inválida");
            var old=one(db,"SELECT duracion,tipo,proyecto_id FROM edt_items WHERE id=?",target);
            check(l(old.get("proyecto_id"))==p&&"SUBACTIVIDAD".equals(old.get("tipo")),"La solicitud debe modificar una subactividad del proyecto");
            db.update("INSERT INTO linea_base(cambio_id,item_id,campo,antes,despues) VALUES(?,?,?,?,?)",id,target,"duracion",old.get("duracion").toString(),Integer.toString(newDur));
            db.update("UPDATE edt_items SET duracion=? WHERE id=?",newDur,target);
            modifications++;
        }
        if(ch.get("asignacion_id")!=null&&ch.get("nueva_cantidad")!=null){
            long aId=l(ch.get("asignacion_id"));double quantity=d(ch.get("nueva_cantidad"));
            check(quantity>=0,"Cantidad inválida");
            var old=one(db,"SELECT cantidad,item_id,proyecto_id FROM asignaciones WHERE id=?",aId);
            check(l(old.get("proyecto_id"))==p,"Asignación de otro proyecto");
            db.update("INSERT INTO linea_base(cambio_id,item_id,campo,antes,despues) VALUES(?,?,?,?,?)",id,l(old.get("item_id")),
                "asignacion:"+aId+":cantidad",old.get("cantidad").toString(),Double.toString(quantity));
            db.update("UPDATE asignaciones SET cantidad=? WHERE id=?",quantity,aId);
            modifications++;
        }
        check(modifications>0,"Defina una duración nueva o ajuste de asignación antes de aprobar");
        planning.recalculate(p);
        db.update("UPDATE cambios SET estado='APROBADO',resultado='Aprobado: planificación y recursos actualizados' WHERE id=?",id);
        db.update("UPDATE etapas_cambio SET completada=true,fecha=CURRENT_DATE,evidencia='Validado para aprobación' WHERE cambio_id=? AND orden<=3",id);
        return oneChange(id);
    }
    @PostMapping("/{id}/rechazar")
    public Map<String,Object> reject(@PathVariable long id,@RequestBody Map<String,Object> x){
        var ch=oneChange(id);check(Set.of("ABIERTO","EN_EVALUACION").contains(ch.get("estado")),"La solicitud ya fue resuelta");
        db.update("UPDATE cambios SET estado='RECHAZADO',resultado=? WHERE id=?",required(x,"resultado"),id);
        return oneChange(id);
    }
    @PutMapping("/{id}/etapas/{order}")
    public Map<String,Object> updateStage(@PathVariable long id,@PathVariable int order,@RequestBody Map<String,Object> x){
        check(order>=1&&order<=10,"Etapa fuera de rango");
        oneChange(id);
        boolean completed=Boolean.parseBoolean(Objects.toString(x.get("completada"),"true"));
        if(completed&&order>1){
            check(db.queryForObject("SELECT COUNT(*) FROM etapas_cambio WHERE cambio_id=? AND orden<? AND completada=false",Integer.class,id,order)==0,
                "Complete primero las etapas anteriores");
        }
        db.update("UPDATE etapas_cambio SET completada=?,fecha=?,evidencia=?,responsable=? WHERE cambio_id=? AND orden=?",completed,
            completed?LocalDate.now():null,string(x,"evidencia"),string(x,"responsable"),id,order);
        return one(db,"SELECT * FROM etapas_cambio WHERE cambio_id=? AND orden=?",id,order);
    }
    @PostMapping("/{id}/cerrar")
    public Map<String,Object> close(@PathVariable long id,@RequestBody Map<String,Object> x){
        var c=oneChange(id);
        check(Set.of("APROBADO","EN_IMPLEMENTACION").contains(c.get("estado")),"Solo puede cerrar cambios aprobados");
        check(db.queryForObject("SELECT COUNT(*) FROM etapas_cambio WHERE cambio_id=? AND completada=false",Integer.class,id)==0,"Complete las 10 etapas antes de cerrar");
        double adopcion=num(x,"adopcion",0),efectividad=num(x,"efectividad",0),cost=num(x,"costo_real",0);
        check(adopcion>=0&&adopcion<=100&&efectividad>=0&&efectividad<=100&&cost>=0,"Mediciones de cierre inválidas");
        db.update("UPDATE cambios SET estado='CERRADO',fecha_real=CURRENT_DATE,costo_real=?,adopcion=?,efectividad=?,resultado=? WHERE id=?",
            cost,adopcion,efectividad,required(x,"resultado"),id);
        return oneChange(id);
    }
    private void validateAffected(long p,Long item,Long assignment){
        if(item!=null){var it=one(db,"SELECT proyecto_id,tipo FROM edt_items WHERE id=?",item);
            check(l(it.get("proyecto_id"))==p&&"SUBACTIVIDAD".equals(it.get("tipo")),"El elemento debe ser una subactividad del proyecto");}
        if(assignment!=null){var a=one(db,"SELECT proyecto_id FROM asignaciones WHERE id=?",assignment);
            check(l(a.get("proyecto_id"))==p,"Asignación de otro proyecto");}
    }
}
