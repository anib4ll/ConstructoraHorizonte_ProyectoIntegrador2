package cl.duoc.horizonte.service;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static cl.duoc.horizonte.controller.ApiSupport.*;

@Service
public class KpiService {
    private final JdbcTemplate db;
    private final CostService costs;
    public KpiService(JdbcTemplate db,CostService costs){this.db=db;this.costs=costs;}

    /** Variables disponibles para que el usuario defina fórmulas. */
    public Map<String,Double> variables(long p){
        Map<String,Double> v=new LinkedHashMap<>();
        var project=one(db,"SELECT * FROM proyectos WHERE id=?",p);
        var leaves=db.queryForList("SELECT * FROM edt_items WHERE proyecto_id=? AND tipo='SUBACTIVIDAD'",p);
        var milestones=db.queryForList("SELECT * FROM hitos WHERE proyecto_id=?",p);
        var changes=db.queryForList("SELECT * FROM cambios WHERE proyecto_id=?",p);
        var stages=db.queryForList("SELECT ec.* FROM etapas_cambio ec JOIN cambios c ON c.id=ec.cambio_id WHERE c.proyecto_id=?",p);
        var ass=costs.assignments(p);
        var res=db.queryForList("SELECT * FROM recursos WHERE proyecto_id=?",p);
        var cost=costs.summary(p);
        LocalDate today=LocalDate.now();
        v.put("progreso",d(project.get("avance")));
        v.put("costo_plan",d(cost.get("plan")));
        v.put("costo_real",d(cost.get("real")));
        v.put("presupuesto",d(project.get("presupuesto")));
        v.put("hitos_total",(double)milestones.size());
        v.put("hitos_evaluados",(double)milestones.stream().filter(h->"CUMPLIDO".equals(h.get("estado"))||!ld(h.get("fecha_comprometida")).isAfter(today)).count());
        v.put("hitos_en_plazo",(double)milestones.stream().filter(h->"CUMPLIDO".equals(h.get("estado"))&&h.get("fecha_real")!=null&&!ld(h.get("fecha_real")).isAfter(ld(h.get("fecha_comprometida")))).count());
        v.put("actividades_total",(double)leaves.size());
        v.put("actividades_atrasadas",(double)leaves.stream().filter(a->d(a.get("progreso"))<100&&ld(a.get("fin")).isBefore(today)).count());
        v.put("actividades_completadas",(double)leaves.stream().filter(a->d(a.get("progreso"))>=100).count());
        v.put("actividades_cumplidas_plazo",(double)leaves.stream().filter(a->d(a.get("progreso"))>=100&&a.get("fin_real")!=null&&!ld(a.get("fin_real")).isAfter(ld(a.get("fin")))).count());
        v.put("horas_asignadas",ass.stream().filter(a->"PROFESIONAL".equals(a.get("tipo"))).mapToDouble(a->d(a.get("horas_semana"))*d(a.get("cantidad"))).sum());
        v.put("horas_disponibles",res.stream().filter(r->"PROFESIONAL".equals(r.get("tipo"))).mapToDouble(r->d(r.get("horas_disponibles"))).sum());
        v.put("integraciones_total",2.0);
        v.put("integraciones_validadas",(double)leaves.stream().filter(a->Set.of("1.3.1.1","1.3.2.1").contains(a.get("codigo"))&&d(a.get("progreso"))>=100).count());
        v.put("defectos_criticos",(double)leaves.stream().filter(a->"BLOQUEADA".equals(a.get("estado"))).count());
        v.put("cambios_total",(double)changes.size());
        v.put("cambios_cerrados",(double)changes.stream().filter(c->"CERRADO".equals(c.get("estado"))).count());
        v.put("cambios_aprobados",(double)changes.stream().filter(c->Set.of("APROBADO","EN_IMPLEMENTACION","CERRADO").contains(c.get("estado"))).count());
        v.put("cambios_aprobados_impl",(double)changes.stream().filter(c->"CERRADO".equals(c.get("estado"))).count());
        v.put("cambios_en_plazo",(double)changes.stream().filter(c->c.get("fecha_real")!=null&&c.get("fecha_comprometida")!=null&&!ld(c.get("fecha_real")).isAfter(ld(c.get("fecha_comprometida")))).count());
        v.put("cambios_riesgo_alto",(double)changes.stream().filter(c->Set.of("ALTO","CRITICO").contains(c.get("riesgo"))).count());
        v.put("etapas_total",(double)stages.size());
        v.put("etapas_cumplidas",(double)stages.stream().filter(s->Boolean.TRUE.equals(s.get("completada"))).count());
        v.put("costo_cambios_real",changes.stream().mapToDouble(c->d(c.get("costo_real"))).sum());
        v.put("costo_cambios_estimado",changes.stream().mapToDouble(c->d(c.get("costo_estimado"))).sum());
        v.put("cambios_atrasados",(double)changes.stream().filter(c->c.get("fecha_comprometida")!=null&&ld(c.get("fecha_comprometida")).isBefore(today)&&!Set.of("CERRADO","RECHAZADO").contains(c.get("estado"))).count());
        v.put("acciones_correctivas_total",(double)stages.stream().filter(s->i(s.get("orden"))==9).count());
        v.put("acciones_correctivas_cerradas",(double)stages.stream().filter(s->i(s.get("orden"))==9&&Boolean.TRUE.equals(s.get("completada"))).count());
        v.put("cambios_evaluados",(double)stages.stream().filter(s->i(s.get("orden"))==2&&Boolean.TRUE.equals(s.get("completada"))).count());
        v.put("dias_evaluacion_total",(double)stages.stream().filter(s->i(s.get("orden"))==2&&s.get("fecha")!=null).mapToLong(s->{
            var c=changes.stream().filter(x->l(x.get("id"))==l(s.get("cambio_id"))).findFirst().orElseThrow();
            return Math.max(0,ChronoUnit.DAYS.between(ld(c.get("fecha_solicitud")),ld(s.get("fecha"))));}).sum());
        v.put("cambios_implementados",(double)changes.stream().filter(c->"CERRADO".equals(c.get("estado"))).count());
        v.put("dias_impl_total",(double)changes.stream().filter(c->c.get("fecha_real")!=null).mapToLong(c->Math.max(0,ChronoUnit.DAYS.between(ld(c.get("fecha_solicitud")),ld(c.get("fecha_real"))))).sum());
        v.put("suma_adopcion",changes.stream().filter(c->c.get("adopcion")!=null).mapToDouble(c->d(c.get("adopcion"))).sum());
        v.put("cambios_con_adopcion",(double)changes.stream().filter(c->c.get("adopcion")!=null).count());
        v.put("suma_efectividad",changes.stream().filter(c->c.get("efectividad")!=null).mapToDouble(c->d(c.get("efectividad"))).sum());
        v.put("cambios_con_efectividad",(double)changes.stream().filter(c->c.get("efectividad")!=null).count());
        return v;
    }
    public String status(double value,String sense,double target,double warning){
        boolean good=sense.equals("MENOR")?value<=target:sense.equals("RANGO")?Math.abs(value-target)<=warning:value>=target;
        boolean caution=sense.equals("MENOR")?value<=warning:sense.equals("RANGO")?Math.abs(value-target)<=warning*2:value>=warning;
        return good?"VERDE":caution?"AMARILLO":"ROJO";
    }
    public void validate(String formula,long p) {new SimpleFormula(formula,variables(p)).evaluate();}
    @Transactional
    public List<Map<String,Object>> refresh(long p){
        Map<String,Double> vars=variables(p);
        var indicators=db.queryForList("SELECT * FROM kpis WHERE proyecto_id=? AND activo=true",p);
        for(var k:indicators){
            double value=new SimpleFormula(k.get("formula").toString(),vars).evaluate();
            String state=status(value,k.get("sentido").toString(),d(k.get("meta")),d(k.get("umbral")));
            value=Math.round(value*1000.0)/1000.0;
            db.update("UPDATE kpis SET valor=?,semaforo=? WHERE id=?",value,state,l(k.get("id")));
            var previous=db.queryForList("SELECT valor,semaforo,meta FROM mediciones_kpi WHERE kpi_id=? ORDER BY fecha DESC,id DESC LIMIT 1",l(k.get("id")));
            if(previous.isEmpty() || Math.abs(d(previous.get(0).get("valor"))-value)>0.0005 ||
                !state.equals(previous.get(0).get("semaforo")) ||
                Math.abs(d(previous.get(0).get("meta"))-d(k.get("meta")))>0.0005) {
                db.update("INSERT INTO mediciones_kpi(kpi_id,valor,semaforo,meta) VALUES(?,?,?,?)",l(k.get("id")),value,state,d(k.get("meta")));
            }
            k.put("valor",value);k.put("semaforo",state);
        }
        return indicators;
    }
}
