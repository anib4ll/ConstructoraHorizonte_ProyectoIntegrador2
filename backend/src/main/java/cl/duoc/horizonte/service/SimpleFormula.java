package cl.duoc.horizonte.service;

import java.util.Map;

/** Evaluador limitado a números, variables y + - * / ( ). No ejecuta código. */
public class SimpleFormula {
    private final String expr;
    private final Map<String,Double> vars;
    private int index=0;
    public SimpleFormula(String expr,Map<String,Double> vars){this.expr=expr;this.vars=vars;}
    public double evaluate(){
        double value=expression();skip();
        if(index<expr.length()) throw new IllegalArgumentException("Fórmula inválida cerca de: "+expr.substring(index));
        if(!Double.isFinite(value)) throw new IllegalArgumentException("Resultado de KPI no finito");
        return value;
    }
    private double expression(){
        double result=term();
        while(true){skip();if(consume('+'))result+=term();else if(consume('-'))result-=term();else break;}
        return result;
    }
    private double term(){
        double result=factor();
        while(true){skip();
            if(consume('*'))result*=factor();
            else if(consume('/')){double denominator=factor(); result=denominator==0?0:result/denominator;}
            else break;
        }
        return result;
    }
    private double factor(){
        skip();
        if(consume('+'))return factor();
        if(consume('-'))return -factor();
        if(consume('(')){double value=expression();skip();if(!consume(')'))throw new IllegalArgumentException("Falta cerrar paréntesis");return value;}
        int start=index;
        while(index<expr.length()&&(Character.isLetterOrDigit(expr.charAt(index))||expr.charAt(index)=='_'||expr.charAt(index)=='.'))index++;
        if(start==index)throw new IllegalArgumentException("Fórmula inválida: "+expr);
        String token=expr.substring(start,index);
        if(Character.isDigit(token.charAt(0))){try{return Double.parseDouble(token);}catch(NumberFormatException e){throw new IllegalArgumentException("Número inválido: "+token);}}
        if(!vars.containsKey(token))throw new IllegalArgumentException("Variable de KPI desconocida: "+token);
        return vars.get(token);
    }
    private boolean consume(char c){if(index<expr.length()&&expr.charAt(index)==c){index++;return true;}return false;}
    private void skip(){while(index<expr.length()&&Character.isWhitespace(expr.charAt(index)))index++;}
}
