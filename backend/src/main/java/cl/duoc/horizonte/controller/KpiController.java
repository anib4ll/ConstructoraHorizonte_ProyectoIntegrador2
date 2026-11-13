package cl.duoc.horizonte.controller;

import cl.duoc.horizonte.service.KpiService;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import static cl.duoc.horizonte.controller.ApiSupport.*;

@RestController
@RequestMapping("/api/kpis")
public class KpiController {
    private final JdbcTemplate db;private final KpiService service;
    public KpiController(JdbcTemplate db,KpiService service){this.db=db;this.service=service;}
    @GetMapping
    public List<Map<String,Object>> all(@RequestParam(defaultValue="1") long proyectoId){
        service.refresh(proyectoId);
        return db.queryForList("SELECT * FROM kpis WHERE proyecto_id=? ORDER BY codigo",proyectoId);
    }
    @GetMapping("/variables")
    public Map<String,Double> vars(@RequestParam(defaultValue="1") long proyectoId){return service.variables(proyectoId);}
    @PostMapping("/evaluar")
    public List<Map<String,Object>> evaluate(@RequestParam(defaultValue="1") long proyectoId){return service.refresh(proyectoId);}
    @GetMapping("/{id}/historial")
    public List<Map<String,Object>> history(@PathVariable long id){return db.queryForList("SELECT * FROM mediciones_kpi WHERE kpi_id=? ORDER BY fecha DESC LIMIT 100",id);}
    @PostMapping
    public List<Map<String,Object>> add(@RequestBody Map<String,Object> x){
        long p=lng(x,"proyecto_id");String formula=required(x,"formula"); service.validate(formula,p);
        String sense=required(x,"sentido");check(Set.of("MAYOR","MENOR","RANGO").contains(sense),"Sentido inválido");
        db.update("INSERT INTO kpis(proyecto_id,codigo,nombre,objetivo,ambito,formula,variables,unidad,sentido,meta,umbral,periodicidad,fuente,responsable) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
            p,required(x,"codigo"),required(x,"nombre"),string(x,"objetivo"),required(x,"ambito"),formula,string(x,"variables"),
            x.getOrDefault("unidad","%"),sense,num(x,"meta",90),num(x,"umbral",75),x.getOrDefault("periodicidad","Semanal"),
            x.getOrDefault("fuente","Sistema"),x.getOrDefault("responsable","Jefe de Proyecto"));
        service.refresh(p);return all(p);
    }
    @PutMapping("/{id}")
    public Map<String,Object> edit(@PathVariable long id,@RequestBody Map<String,Object> x){
        var k=one(db,"SELECT * FROM kpis WHERE id=?",id);long p=l(k.get("proyecto_id"));
        String formula=x.getOrDefault("formula",k.get("formula")).toString();service.validate(formula,p);
        String sense=x.getOrDefault("sentido",k.get("sentido")).toString();check(Set.of("MAYOR","MENOR","RANGO").contains(sense),"Sentido inválido");
        db.update("UPDATE kpis SET nombre=?,objetivo=?,formula=?,variables=?,unidad=?,sentido=?,meta=?,umbral=?,periodicidad=?,responsable=?,activo=? WHERE id=?",
            x.getOrDefault("nombre",k.get("nombre")),x.getOrDefault("objetivo",k.get("objetivo")),formula,
            x.getOrDefault("variables",k.get("variables")),x.getOrDefault("unidad",k.get("unidad")),sense,
            num(x,"meta",d(k.get("meta"))),num(x,"umbral",d(k.get("umbral"))),x.getOrDefault("periodicidad",k.get("periodicidad")),
            x.getOrDefault("responsable",k.get("responsable")),x.getOrDefault("activo",k.get("activo")),id);
        service.refresh(p);
        return one(db,"SELECT * FROM kpis WHERE id=?",id);
    }
}
