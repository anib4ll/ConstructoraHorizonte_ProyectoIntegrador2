package cl.duoc.horizonte.controller;
import cl.duoc.horizonte.service.PlanningService;
import java.time.LocalDate;
import java.util.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController @RequestMapping("/api")
public class ExcelImportController {
 private final JdbcTemplate db; private final PlanningService planner;
 public ExcelImportController(JdbcTemplate db,PlanningService planner){this.db=db;this.planner=planner;}
 record Item(String code,String name,String type,String parent,String person,String description,LocalDate start,int days){}
 static ResponseStatusException fail(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
 static String val(Row row,int c,DataFormatter fmt){Cell cell=row.getCell(c);return cell==null?"":fmt.formatCellValue(cell).trim();}
 @PostMapping("/proyectos/{id}/edt/importar-excel") @Transactional
 public Map<String,Object> upload(@PathVariable long id,@RequestParam("archivo") MultipartFile file){
  if(file.isEmpty()||file.getOriginalFilename()==null||!file.getOriginalFilename().toLowerCase(Locale.ROOT).endsWith(".xlsx"))throw fail("Se requiere un archivo .xlsx");
  if(file.getSize()>5000000)throw fail("Archivo demasiado grande (máximo 5 MB)");
  if(db.queryForObject("SELECT COUNT(*) FROM proyectos WHERE id=?",Integer.class,id)==0)throw fail("Proyecto inexistente");
  List<Item> items=new ArrayList<>(); DataFormatter fmt=new DataFormatter();
  try(Workbook wb=new XSSFWorkbook(file.getInputStream())){
   Sheet sheet=wb.getSheetAt(0); Row head=sheet.getRow(0);
   if(head==null)throw fail("Faltan encabezados");
   String[] columns={"codigo","nombre","tipo","codigo_padre","responsable","descripcion","inicio_base","duracion"};
   for(int j=0;j<columns.length;j++)if(!val(head,j,fmt).equalsIgnoreCase(columns[j]))throw fail("Columna "+(j+1)+": se espera "+columns[j]);
   for(int r=1;r<=sheet.getLastRowNum();r++){
    Row row=sheet.getRow(r);if(row==null||val(row,0,fmt).isBlank())continue;
    String code=val(row,0,fmt),name=val(row,1,fmt),type=val(row,2,fmt).toUpperCase(Locale.ROOT),parent=val(row,3,fmt);
    if(code.length()>32||name.isBlank()||name.length()>220||!Set.of("ETAPA","ACTIVIDAD","SUBACTIVIDAD").contains(type))throw fail("Fila "+(r+1)+": código, nombre o tipo inválido");
    String person=val(row,4,fmt),desc=val(row,5,fmt);if(person.length()>120)throw fail("Responsable demasiado largo");
    LocalDate start;int days;
    try{Cell date=row.getCell(6);start=date!=null&&DateUtil.isCellDateFormatted(date)?date.getLocalDateTimeCellValue().toLocalDate():LocalDate.parse(val(row,6,fmt));days=Integer.parseInt(val(row,7,fmt));}
    catch(Exception e){throw fail("Fila "+(r+1)+": fecha AAAA-MM-DD y duración entera obligatorias");}
    if(days<1||days>999)throw fail("Fila "+(r+1)+": duración fuera de rango");
    items.add(new Item(code,name,type,parent,person,desc,start,days));if(items.size()>1000)throw fail("Máximo 1000 filas");
   }
  }catch(ResponseStatusException e){throw e;}catch(Exception e){throw fail("Excel inválido: "+e.getMessage());}
  if(items.isEmpty())throw fail("Excel sin datos");
  Map<String,Item> incoming=new HashMap<>();for(Item item:items)if(incoming.putIfAbsent(item.code(),item)!=null)throw fail("Código duplicado: "+item.code());
  Map<String,Long> ids=new HashMap<>();Map<String,String> types=new HashMap<>();
  for(var row:db.queryForList("SELECT codigo,id,tipo FROM edt_items WHERE proyecto_id=?",id)){String code=row.get("codigo").toString();ids.put(code,((Number)row.get("id")).longValue());types.put(code,row.get("tipo").toString());}
  for(Item item:items){
   if(ids.containsKey(item.code()))throw fail("Código ya existente: "+item.code()+". No se modificaron datos");
   String required=item.type().equals("ETAPA")?"":item.type().equals("ACTIVIDAD")?"ETAPA":"ACTIVIDAD";
   String parentType=incoming.containsKey(item.parent())?incoming.get(item.parent()).type():types.get(item.parent());
   if(required.isEmpty()?!item.parent().isEmpty():!required.equals(parentType))throw fail("Jerarquía inválida para "+item.code());
  }
  for(String type:List.of("ETAPA","ACTIVIDAD","SUBACTIVIDAD"))for(Item item:items)if(type.equals(item.type())){
   Long parent=item.parent().isBlank()?null:ids.get(item.parent());
   Long pk=db.queryForObject("INSERT INTO edt_items(proyecto_id,parent_id,codigo,nombre,descripcion,tipo,responsable,inicio_base,inicio,fin,duracion) VALUES(?,?,?,?,?,?,?,?,?,?,?) RETURNING id",Long.class,id,parent,item.code(),item.name(),item.description(),item.type(),item.person(),item.start(),item.start(),item.start(),item.days());
   ids.put(item.code(),pk);
  }
  planner.recalculate(id);
  return Map.of("importados",items.size(),"mensaje","Importación completada");
 }
}
