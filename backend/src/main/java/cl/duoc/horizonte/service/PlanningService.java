package cl.duoc.horizonte.service;

import java.time.LocalDate;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static cl.duoc.horizonte.controller.ApiSupport.*;

/** Motor de precedencias. Calcula solo hojas y consolida padres para evitar doble cómputo. */
@Service
public class PlanningService {
    private final JdbcTemplate db;
    public PlanningService(JdbcTemplate db){this.db=db;}

    @Transactional
    public void recalculate(long projectId) {
        List<Map<String,Object>> all=db.queryForList("SELECT * FROM edt_items WHERE proyecto_id=? ORDER BY codigo",projectId);
        Set<LocalDate> holidays=new HashSet<>();
        for(var r:db.queryForList("SELECT fecha FROM dias_no_laborales WHERE proyecto_id=?",projectId)) holidays.add(ld(r.get("fecha")));
        Map<Long,Map<String,Object>> items=new LinkedHashMap<>();
        for(var r:all) if("SUBACTIVIDAD".equals(r.get("tipo"))) items.put(l(r.get("id")),r);
        List<Map<String,Object>> edges=db.queryForList("SELECT * FROM dependencias WHERE proyecto_id=?",projectId);
        Map<Long,List<Map<String,Object>>> incoming=new HashMap<>();
        Map<Long,List<Long>> outgoing=new HashMap<>();
        Map<Long,Integer> indegree=new HashMap<>();
        for(Long id:items.keySet()) indegree.put(id,0);
        for(var d:edges){
            long from=l(d.get("predecesora_id")),to=l(d.get("sucesora_id"));
            check(items.containsKey(from)&&items.containsKey(to),"Las dependencias deben vincular subactividades del mismo proyecto");
            incoming.computeIfAbsent(to,k->new ArrayList<>()).add(d);
            outgoing.computeIfAbsent(from,k->new ArrayList<>()).add(to);
            indegree.put(to,indegree.get(to)+1);
        }
        ArrayDeque<Long> queue=new ArrayDeque<>();
        for(var e:indegree.entrySet()) if(e.getValue()==0) queue.add(e.getKey());
        Map<Long,LocalDate> starts=new HashMap<>(), ends=new HashMap<>();
        int done=0;
        while(!queue.isEmpty()){
            long id=queue.removeFirst(); done++;
            var item=items.get(id);
            int duration=i(item.get("duracion"));
            LocalDate start=WorkdayMath.followingWorkday(ld(item.get("inicio_base")),holidays);
            for(var edge:incoming.getOrDefault(id,List.of())){
                long from=l(edge.get("predecesora_id"));
                String type=edge.get("tipo").toString();
                int lag=i(edge.get("desfase"));
                LocalDate minimum=switch(type){
                    case "FS" -> WorkdayMath.shift(ends.get(from),lag+1,holidays);
                    case "SS" -> WorkdayMath.shift(starts.get(from),lag,holidays);
                    case "FF" -> WorkdayMath.shift(WorkdayMath.shift(ends.get(from),lag,holidays),1-duration,holidays);
                    case "SF" -> WorkdayMath.shift(WorkdayMath.shift(starts.get(from),lag,holidays),1-duration,holidays);
                    default -> throw new IllegalArgumentException("Tipo de dependencia desconocido: "+type);
                };
                if(minimum.isAfter(start)) start=minimum;
            }
            LocalDate end=WorkdayMath.end(start,duration,holidays);
            starts.put(id,start); ends.put(id,end);
            db.update("UPDATE edt_items SET inicio=?,fin=? WHERE id=?",start,end,id);
            for(Long next:outgoing.getOrDefault(id,List.of())){
                int n=indegree.merge(next,-1,Integer::sum);
                if(n==0) queue.add(next);
            }
        }
        if(done!=items.size()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Dependencia circular rechazada");
        // Consolidación de etapa y actividad: agregación de sus hijos sin sumar dos veces.
        for(String type:List.of("ACTIVIDAD","ETAPA")){
            for(var parent:all) if(type.equals(parent.get("tipo"))){
                long id=l(parent.get("id"));
                var children=db.queryForList("SELECT inicio,fin,progreso FROM edt_items WHERE parent_id=?",id);
                if(children.isEmpty()) continue;
                LocalDate min=null,max=null; double progress=0;
                for(var c:children){
                    LocalDate a=ld(c.get("inicio")),b=ld(c.get("fin"));
                    if(min==null||a.isBefore(min))min=a;
                    if(max==null||b.isAfter(max))max=b;
                    progress+=d(c.get("progreso"));
                }
                db.update("UPDATE edt_items SET inicio=?,fin=?,duracion=?,progreso=? WHERE id=?", min,max,WorkdayMath.count(min,max,holidays),progress/children.size(),id);
            }
        }
        Double progress=db.queryForObject("SELECT COALESCE(AVG(progreso),0) FROM edt_items WHERE proyecto_id=? AND tipo='SUBACTIVIDAD'",Double.class,projectId);
        db.update("UPDATE proyectos SET avance=? WHERE id=?",progress,projectId);
    }

    @Transactional
    public void addDependency(long projectId,long from,long to,String type,int lag){
        check(Set.of("FS","SS","FF","SF").contains(type),"Relación debe ser FS, SS, FF o SF");
        check(from!=to,"Una actividad no puede depender de sí misma");
        var a=one(db,"SELECT proyecto_id,tipo FROM edt_items WHERE id=?",from);
        var b=one(db,"SELECT proyecto_id,tipo FROM edt_items WHERE id=?",to);
        check(l(a.get("proyecto_id"))==projectId&&l(b.get("proyecto_id"))==projectId,"Actividades deben pertenecer al proyecto");
        check("SUBACTIVIDAD".equals(a.get("tipo"))&&"SUBACTIVIDAD".equals(b.get("tipo")),"Las dependencias se aplican a subactividades");
        db.update("INSERT INTO dependencias(proyecto_id,predecesora_id,sucesora_id,tipo,desfase) VALUES(?,?,?,?,?)",projectId,from,to,type,lag);
        recalculate(projectId); // Si hay ciclo, @Transactional revierte la inserción.
    }
}
