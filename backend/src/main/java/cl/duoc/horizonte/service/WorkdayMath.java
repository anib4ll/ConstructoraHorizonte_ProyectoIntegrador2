package cl.duoc.horizonte.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

/** Días hábiles lunes-viernes, con feriados del calendario del proyecto. */
public final class WorkdayMath {
    private WorkdayMath() { }
    public static boolean isWorkday(LocalDate date, Set<LocalDate> holidays) {
        return date.getDayOfWeek()!=DayOfWeek.SATURDAY && date.getDayOfWeek()!=DayOfWeek.SUNDAY && !holidays.contains(date);
    }
    public static LocalDate followingWorkday(LocalDate date, Set<LocalDate> holidays) {
        while(!isWorkday(date,holidays)) date=date.plusDays(1);
        return date;
    }
    public static LocalDate shift(LocalDate date, int workingDays, Set<LocalDate> holidays) {
        date=followingWorkday(date,holidays);
        if(workingDays==0) return date;
        int direction=Integer.signum(workingDays);
        for(int i=0;i<Math.abs(workingDays);i++) {
            do {date=date.plusDays(direction);} while(!isWorkday(date,holidays));
        }
        return date;
    }
    public static LocalDate end(LocalDate start,int duration,Set<LocalDate> holidays) {
        if(duration<1) throw new IllegalArgumentException("Duración mínima: 1 día hábil");
        return shift(start,duration-1,holidays);
    }
    public static int count(LocalDate start, LocalDate end,Set<LocalDate> holidays) {
        if(start.isAfter(end)) return 0;
        int n=0;
        for(LocalDate d=start;!d.isAfter(end);d=d.plusDays(1)) if(isWorkday(d,holidays)) n++;
        return n;
    }
}
