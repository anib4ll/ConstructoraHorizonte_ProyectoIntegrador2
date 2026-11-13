package cl.duoc.horizonte.config;

import cl.duoc.horizonte.service.PlanningService;
import cl.duoc.horizonte.service.KpiService;
import java.time.LocalDate;
import java.util.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Datos ficticios del caso de evaluación: una sola vez, sin borrar modificaciones posteriores. */
@Component
public class SeedData implements CommandLineRunner {
    private final JdbcTemplate db;
    private final PlanningService planner;
    private final KpiService kpis;
    public SeedData(JdbcTemplate db, PlanningService planner, KpiService kpis){this.db=db;this.planner=planner;this.kpis=kpis;}
    @Override @Transactional
    public void run(String...args){
        if(db.queryForObject("SELECT COUNT(*) FROM proyectos",Integer.class)>0)return;
        Long p=db.queryForObject("INSERT INTO proyectos(codigo,nombre,descripcion,responsable,inicio,fin,presupuesto,estado) VALUES(?,?,?,?,?,?,?,?) RETURNING id",Long.class,
           "PR-001","Plataforma Integrada de Gestión Operacional Constructora Horizonte",
           "Piloto en tres obras: control de avance, materiales, calidad, documentos y conexión corporativa. Sin reemplazar ERP.",
           "Jefe de Proyecto",LocalDate.of(2026,10,5),LocalDate.of(2027,7,2),260000000,"EN_EJECUCION");
        Map<String,Long> itemIds=new HashMap<>();
        String[][] wbs={
            {"1.1","Control operacional de obras","ETAPA","Jefe de Proyecto"},
            {"1.1.1","Desarrollar módulo de seguimiento de avance","ACTIVIDAD","Frontend / Backend"},
            {"1.1.1.1","Registrar actividades y porcentaje de avance","SUBACTIVIDAD","Frontend"},
            {"1.1.1.2","Registrar bloqueos y dependencias operacionales","SUBACTIVIDAD","Backend"},
            {"1.1.2","Desarrollar módulo de materiales y recepción","ACTIVIDAD","Backend"},
            {"1.1.2.1","Registrar recepción, guía y cantidades","SUBACTIVIDAD","Backend"},
            {"1.1.2.2","Registrar utilización y trazabilidad del material","SUBACTIVIDAD","Backend"},
            {"1.1.3","Desarrollar módulo de subcontratos y estados de pago","ACTIVIDAD","Backend"},
            {"1.1.3.1","Registrar contrato, contratista y avance respaldado","SUBACTIVIDAD","Backend"},
            {"1.1.3.2","Registrar antecedentes y estado de pago","SUBACTIVIDAD","Backend"},
            {"1.2","Calidad, documentación y continuidad operacional","ETAPA","Jefe de Proyecto"},
            {"1.2.1","Desarrollar módulo de inspecciones y observaciones","ACTIVIDAD","Frontend / QA"},
            {"1.2.1.1","Registrar observaciones y evidencia fotográfica","SUBACTIVIDAD","Frontend"},
            {"1.2.1.2","Gestionar corrección, reinspección y cierre","SUBACTIVIDAD","QA"},
            {"1.2.2","Desarrollar gestión documental","ACTIVIDAD","Backend"},
            {"1.2.2.1","Cargar y versionar documentos de obra","SUBACTIVIDAD","Backend"},
            {"1.2.2.2","Identificar versión vigente y documentos obsoletos","SUBACTIVIDAD","Backend"},
            {"1.2.3","Implementar operación con conectividad intermitente","ACTIVIDAD","Arquitecto / Backend"},
            {"1.2.3.1","Registrar información temporalmente sin conexión","SUBACTIVIDAD","Frontend"},
            {"1.2.3.2","Sincronizar registros pendientes","SUBACTIVIDAD","Backend"},
            {"1.3","Integraciones y control ejecutivo","ETAPA","Arquitecto / Integración"},
            {"1.3.1","Implementar integración con ERP financiero","ACTIVIDAD","Arquitecto / Integración"},
            {"1.3.1.1","Consultar y conciliar información económica","SUBACTIVIDAD","Arquitecto / Integración"},
            {"1.3.2","Implementar integración con compras e identidad","ACTIVIDAD","Arquitecto / Backend"},
            {"1.3.2.1","Integrar información del sistema de compras","SUBACTIVIDAD","Backend"},
            {"1.3.2.2","Implementar autenticación, usuarios y roles","SUBACTIVIDAD","Backend"},
            {"1.3.3","Desarrollar dashboard ejecutivo","ACTIVIDAD","Frontend"},
            {"1.3.3.1","Consolidar indicadores operacionales","SUBACTIVIDAD","Frontend"},
            {"1.4","Piloto y puesta en operación","ETAPA","Jefe de Proyecto"},
            {"1.4.1","Configurar las tres obras piloto","ACTIVIDAD","Jefe de Proyecto / Backend"},
            {"1.4.1.1","Configurar obras y cargar datos iniciales","SUBACTIVIDAD","Backend"},
            {"1.4.2","Habilitar usuarios y capacitación","ACTIVIDAD","Jefe de Proyecto"},
            {"1.4.2.1","Crear accesos y capacitar usuarios clave","SUBACTIVIDAD","Jefe de Proyecto"},
            {"1.4.3","Ejecutar puesta en marcha y estabilización","ACTIVIDAD","QA / Integración"},
            {"1.4.3.1","Habilitar operación productiva en las tres obras","SUBACTIVIDAD","Integración"},
            {"1.4.3.2","Resolver incidencias críticas de inicio","SUBACTIVIDAD","QA"},
        };
        int leafIndex=0;
        for(String[] item:wbs){
            String code=item[0],type=item[2];
            String parentCode=code.contains(".")?code.substring(0,code.lastIndexOf('.')):"";
            Long parentId="ETAPA".equals(type)?null:itemIds.get(parentCode);
            LocalDate base;
            int duration;
            if("SUBACTIVIDAD".equals(type)){
                base=LocalDate.of(2026,10,5).plusDays(leafIndex*7L);
                duration=6+(leafIndex%5)*2;
                leafIndex++;
            }else{base=LocalDate.of(2026,10,5);duration=5;}
            Long id=db.queryForObject("INSERT INTO edt_items(proyecto_id,parent_id,codigo,nombre,descripcion,tipo,responsable,inicio_base,inicio,fin,duracion) VALUES(?,?,?,?,?,?,?,?,?,?,?) RETURNING id",Long.class,
              p,parentId,code,item[1],item[1],type,item[3],base,base,base,duration);
            itemIds.put(code,id);
        }
        // Una relación de cada tipo, sin ciclos. Las fechas se recalculan con días hábiles.
        dep(p,itemIds,"1.1.1.1","1.1.1.2","FS",0);
        dep(p,itemIds,"1.2.1.1","1.2.2.1","SS",2);
        dep(p,itemIds,"1.3.1.1","1.3.3.1","FF",0);
        dep(p,itemIds,"1.4.2.1","1.4.3.1","SF",1);
        // Calendario del proyecto.
        for(LocalDate holiday:List.of(LocalDate.of(2026,12,25),LocalDate.of(2027,1,1),LocalDate.of(2027,5,21)))
            db.update("INSERT INTO dias_no_laborales(proyecto_id,fecha,descripcion) VALUES(?,?,?)",p,holiday,"Día no laborable configurado");
        // Cinco profesionales y cuatro categorías adicionales, con tarifas de ejemplo.
        String[][] resources={
          {"P01","Jefe de Proyecto","PROFESIONAL","Gestión","36000","40","0","1"},
          {"P02","Desarrollador Backend","PROFESIONAL","Java / API","29000","40","0","1"},
          {"P03","Desarrollador Frontend","PROFESIONAL","Angular","27000","40","0","1"},
          {"P04","QA / Tester","PROFESIONAL","Pruebas","24500","40","0","1"},
          {"P05","Arquitecto Integración","PROFESIONAL","ERP / Integraciones","34000","40","0","1"},
          {"TP01","Perfil desarrollador de apoyo","TIPO_PROFESIONAL","Soporte","23500","40","0","2"},
          {"L01","Licencia de software","LICENCIA","Plataforma","0","0","180000","10"},
          {"M01","Material de capacitación","MATERIAL","Documentación","0","0","12000","100"},
          {"E01","Equipo de pruebas","EQUIPAMIENTO","Hardware","0","0","260000","3"},
          {"O01","Servicios de apoyo","OTRO","Operación","0","0","90000","10"}
        };
        Map<String,Long> resIds=new HashMap<>();
        for(String[] r:resources){
            Long id=db.queryForObject("INSERT INTO recursos(proyecto_id,codigo,nombre,tipo,especialidad,tarifa_hora,horas_disponibles,costo_unidad,cantidad_disponible,unidad) VALUES(?,?,?,?,?,?,?,?,?,?) RETURNING id",Long.class,
             p,r[0],r[1],r[2],r[3],Double.valueOf(r[4]),Double.valueOf(r[5]),Double.valueOf(r[6]),Double.valueOf(r[7]),"unidad");
            resIds.put(r[0],id);
        }
        int idx=0;
        Long demoAssignment=null;
        for(String[] w:wbs) if("SUBACTIVIDAD".equals(w[2])){
            String resCode="P0"+(idx%5+1);
            Long id=db.queryForObject("INSERT INTO asignaciones(proyecto_id,item_id,recurso_id,cantidad,cantidad_real,horas_semana,horas_semana_real,semanas,semanas_reales) VALUES(?,?,?,?,?,?,?,?,?) RETURNING id",Long.class,
              p,itemIds.get(w[0]),resIds.get(resCode),1,idx<4?1:0,20+(idx%3)*5,idx<4?12:0,2,idx<4?1:0);
            if(w[0].equals("1.1.2.1"))demoAssignment=id;
            if(idx%4==0) db.update("INSERT INTO asignaciones(proyecto_id,item_id,recurso_id,cantidad,cantidad_real,horas_semana,horas_semana_real,semanas,semanas_reales) VALUES(?,?,?,?,?,?,?,?,?)",
                 p,itemIds.get(w[0]),resIds.get(idx%8==0?"L01":"M01"),2,idx<4?1:0,0,0,1,0);
            idx++;
        }
        String[][] milestones={
          {"H01","Control operacional disponible","1.1.3.2","2026-12-11"},
          {"H02","Calidad, documentos y sincronización","1.2.3.2","2027-02-12"},
          {"H03","Integraciones corporativas validadas","1.3.2.2","2027-04-02"},
          {"H04","Dashboard ejecutivo validado","1.3.3.1","2027-04-23"},
          {"H05","Tres obras listas para piloto","1.4.1.1","2027-05-28"},
          {"H06","Piloto operativo aceptado","1.4.3.2","2027-07-02"}
        };
        for(int i=0;i<milestones.length;i++){
            var h=milestones[i];
            Long id=db.queryForObject("INSERT INTO hitos(proyecto_id,codigo,descripcion,responsable,fecha_comprometida,estado,criterio,evidencia) VALUES(?,?,?,?,?,?,?,?) RETURNING id",Long.class,
             p,h[0],h[1],"Jefe de Proyecto",LocalDate.parse(h[3]),"PENDIENTE","Evidencia de pruebas y aceptación del responsable","Pendiente de entrega");
            db.update("INSERT INTO hito_actividades(hito_id,item_id) VALUES(?,?)",id,itemIds.get(h[2]));
        }
        // 8 KPI de proyecto y 12 KPI de gestión del cambio. Sus fórmulas son editables en UI.
        String[][] indicators={
          {"KP01","Avance del proyecto","PROYECTO","progreso","MAYOR","100","80","%"},
          {"KP02","Cumplimiento de hitos","PROYECTO","100*hitos_en_plazo/hitos_evaluados","MAYOR","90","75","%"},
          {"KP03","Desviación de costos","PROYECTO","100*(costo_real-costo_plan)/costo_plan","MENOR","5","10","%"},
          {"KP04","Actividades atrasadas","PROYECTO","100*actividades_atrasadas/actividades_total","MENOR","10","20","%"},
          {"KP05","Utilización de profesionales","PROYECTO","100*horas_asignadas/horas_disponibles","RANGO","85","15","%"},
          {"KP06","Actividades en plazo","PROYECTO","100*actividades_cumplidas_plazo/actividades_completadas","MAYOR","90","75","%"},
          {"KP07","Integraciones validadas","PROYECTO","100*integraciones_validadas/integraciones_total","MAYOR","100","80","%"},
          {"KP08","Bloqueos críticos abiertos","PROYECTO","defectos_criticos","MENOR","0","2","casos"},
          {"KC01","Cambios completados","CAMBIO","100*cambios_cerrados/cambios_total","MAYOR","80","60","%"},
          {"KC02","Cambios cerrados en plazo","CAMBIO","100*cambios_en_plazo/cambios_total","MAYOR","90","70","%"},
          {"KC03","Cambios de riesgo alto","CAMBIO","100*cambios_riesgo_alto/cambios_total","MENOR","20","40","%"},
          {"KC04","Cumplimiento de etapas","CAMBIO","100*etapas_cumplidas/etapas_total","MAYOR","85","70","%"},
          {"KC05","Cumplimiento de presupuesto","CAMBIO","100*costo_cambios_real/costo_cambios_estimado","MENOR","100","110","%"},
          {"KC06","Tiempo de evaluación","CAMBIO","dias_evaluacion_total/cambios_evaluados","MENOR","5","10","días"},
          {"KC07","Tiempo de implementación","CAMBIO","dias_impl_total/cambios_implementados","MENOR","15","20","días"},
          {"KC08","Cambios aprobados implementados","CAMBIO","100*cambios_aprobados_impl/cambios_aprobados","MAYOR","90","75","%"},
          {"KC09","Cambios atrasados","CAMBIO","100*cambios_atrasados/cambios_total","MENOR","10","20","%"},
          {"KC10","Acciones correctivas cerradas","CAMBIO","100*acciones_correctivas_cerradas/acciones_correctivas_total","MAYOR","90","75","%"},
          {"KC11","Adopción del cambio","CAMBIO","suma_adopcion/cambios_con_adopcion","MAYOR","85","70","%"},
          {"KC12","Efectividad del cambio","CAMBIO","suma_efectividad/cambios_con_efectividad","MAYOR","80","65","%"}
        };
        for(var k:indicators)db.update("INSERT INTO kpis(proyecto_id,codigo,nombre,ambito,formula,variables,sentido,meta,umbral,unidad,objetivo) VALUES(?,?,?,?,?,?,?,?,?,?,?)",
            p,k[0],k[1],k[2],k[3],"Variables del sistema",k[4],Double.parseDouble(k[5]),Double.parseDouble(k[6]),k[7],"Monitorear "+k[1].toLowerCase());
        String[][] changes={
          {"CAM-01","Incorporar módulo integral de prevención de riesgos","ALCANCE","ALTA","ALTO","RECHAZADO"},
          {"CAM-02","Agregar una cuarta obra piloto","ALCANCE","ALTA","ALTO","ABIERTO"},
          {"CAM-03","Firma digital en recepción de materiales","FUNCIONAL","MEDIA","MEDIO","EN_EVALUACION"},
          {"CAM-04","Adaptar integración por cambio de API de compras","TECNOLOGIA","CRITICA","CRITICO","EN_EVALUACION"},
          {"CAM-05","Mejorar sincronización con conexión intermitente","TECNOLOGIA","ALTA","MEDIO","EN_IMPLEMENTACION"}
        };
        for(int i=0;i<changes.length;i++){
            var c=changes[i];
            Long item = null;
            if (i == 2) {
                item = itemIds.get("1.1.2.1");
            } else if (i == 3) {
                item = itemIds.get("1.3.2.1");
            }
            Integer nuevaDuracion = null;
            if (i == 2) {
                nuevaDuracion = 18;
            } else if (i == 3) {
                nuevaDuracion = 13;
            }
            Double nuevaCantidad = i == 2 ? Double.valueOf(2.0) : null;
            Long ass=i==2?demoAssignment:null;
            Long id=db.queryForObject("INSERT INTO cambios(proyecto_id,codigo,solicitante,fecha_solicitud,descripcion,justificacion,tipo,prioridad,riesgo,impacto,responsable,estado,fecha_comprometida,costo_estimado,item_id,nueva_duracion,asignacion_id,nueva_cantidad) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) RETURNING id",Long.class,
              p,c[0],"Coordinación de obra",LocalDate.of(2026,10,20).plusDays(i*3L),c[1],"Mejorar el resultado del piloto",c[2],c[3],c[4],
              "Revisar planificación, costos y aceptación","Jefe de Proyecto",c[5],LocalDate.of(2026,12,15).plusDays(i*6L),
              (i+1)*350000,item,nuevaDuracion,ass,nuevaCantidad);
            for(int stage=1;stage<=10;stage++){
                boolean completed=(i==0&&stage<=2)||(i==2&&stage<=2)||(i==4&&stage<=5);
                db.update("INSERT INTO etapas_cambio(cambio_id,orden,nombre,responsable,completada,fecha,evidencia) VALUES(?,?,?,?,?,?,?)",
                  id,stage,cl.duoc.horizonte.controller.ChangeController.STAGES.get(stage-1),"Jefe de Proyecto",completed,
                  completed?LocalDate.of(2026,10,22).plusDays(stage):null,completed?"Registro de avance":"");
            }
        }
        planner.recalculate(p);
        kpis.refresh(p);
    }
    private void dep(long p,Map<String,Long> ids,String from,String to,String type,int lag){
        db.update("INSERT INTO dependencias(proyecto_id,predecesora_id,sucesora_id,tipo,desfase) VALUES(?,?,?,?,?)",p,ids.get(from),ids.get(to),type,lag);
    }
}
