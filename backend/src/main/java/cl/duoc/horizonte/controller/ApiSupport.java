package cl.duoc.horizonte.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

public final class ApiSupport {
    private ApiSupport() { }
    public static String string(Map<String,Object> m, String key) { return Objects.toString(m.get(key), "").trim(); }
    public static String required(Map<String,Object> m, String key) {
        String v = string(m,key);
        if (v.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta el campo: " + key);
        return v;
    }
    public static long lng(Map<String,Object> m, String key) {
        Object value = m.get(key);
        if (value == null || value.toString().isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Falta "+key);
        try { return Long.parseLong(value.toString()); }
        catch (NumberFormatException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Número inválido: "+key); }
    }
    public static Long optionalLong(Map<String,Object> m, String key) {
        Object v=m.get(key); return v==null || v.toString().isBlank()?null:lng(m,key);
    }
    public static int integer(Map<String,Object> m, String key, int defaultValue) {
        try { return m.get(key)==null?defaultValue:Integer.parseInt(m.get(key).toString()); }
        catch (NumberFormatException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Entero inválido: "+key); }
    }
    public static double num(Map<String,Object> m,String key,double d) {
        try { return m.get(key)==null?d:Double.parseDouble(m.get(key).toString()); }
        catch(NumberFormatException e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Número inválido: "+key);}
    }
    public static LocalDate date(Map<String,Object> m,String key,LocalDate fallback) {
        try { return m.get(key)==null || m.get(key).toString().isBlank()?fallback:LocalDate.parse(m.get(key).toString()); }
        catch (Exception e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Fecha inválida ISO 8601: "+key); }
    }
    public static void check(boolean ok,String message) {
        if(!ok) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,message);
    }
    public static Map<String,Object> one(JdbcTemplate db,String sql,Object...args) {
        var rows=db.queryForList(sql,args);
        if(rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Registro no encontrado");
        return rows.get(0);
    }
    public static long id(JdbcTemplate db,String sequence) {
        return db.queryForObject("SELECT nextval('"+sequence+"')",Long.class);
    }
    public static double d(Object o) {return o==null?0:Double.parseDouble(o.toString());}
    public static int i(Object o) {return o==null?0:Integer.parseInt(o.toString());}
    public static long l(Object o) {return Long.parseLong(o.toString());}
    public static LocalDate ld(Object o) {return o instanceof LocalDate d?d:LocalDate.parse(o.toString());}
}

@RestControllerAdvice
class ApiErrors {
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<Map<String,String>> known(ResponseStatusException e){
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("error",Objects.toString(e.getReason(),"Error")));
    }
    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<Map<String,String>> database(DataAccessException e){
        return ResponseEntity.badRequest().body(Map.of("error","Operación rechazada por integridad de datos: "+Objects.toString(e.getMostSpecificCause().getMessage(),"error SQL")));
    }
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String,String>> invalid(IllegalArgumentException e){
        return ResponseEntity.badRequest().body(Map.of("error",Objects.toString(e.getMessage(),"Datos inválidos")));
    }
}
