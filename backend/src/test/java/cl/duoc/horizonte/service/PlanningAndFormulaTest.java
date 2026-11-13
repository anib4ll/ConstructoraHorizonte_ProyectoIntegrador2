package cl.duoc.horizonte.service;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlanningAndFormulaTest {
    @Test void calculaFinEnDiasHabiles() {
        Set<LocalDate> feriados=Set.of(LocalDate.of(2026,10,12));
        assertEquals(LocalDate.of(2026,10,13),
            WorkdayMath.end(LocalDate.of(2026,10,9),2,feriados));
    }
    @Test void permiteDesfaseNegativo() {
        assertEquals(LocalDate.of(2026,10,8),
            WorkdayMath.shift(LocalDate.of(2026,10,12),-2,Set.of()));
    }
    @Test void formulaConVariables() {
        double result=new SimpleFormula("100*(real-plan)/plan",
            Map.of("real",110.0,"plan",100.0)).evaluate();
        assertEquals(10.0,result,0.001);
    }
    @Test void rechazaVariableDesconocida() {
        assertThrows(IllegalArgumentException.class,
           ()->new SimpleFormula("variable_inexistente+10",Map.of()).evaluate());
    }
    @Test void evitaDivisionIndefinida() {
        assertEquals(0.0,new SimpleFormula("100/0",Map.of()).evaluate(),0.001);
    }
}
