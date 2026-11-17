package com.example.libri.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Utilidades para trabajar con fechas. Un "día" se identifica con un número entero
 * (cuántos días han pasado desde 1970 en la zona horaria del celular), así es fácil
 * comparar días y calcular rachas.
 */
public final class DateUtils {

    public static final long DAY_MS = 24L * 60L * 60L * 1000L;

    private static final Locale ES = Locale.forLanguageTag("es-CO");

    private DateUtils() {
        // Clase de utilidades: no se instancia
    }

    /** Número del día al que pertenece un instante (en la zona horaria local). */
    public static long dayIndex(long millis) {
        int offset = TimeZone.getDefault().getOffset(millis);
        return Math.floorDiv(millis + offset, DAY_MS);
    }

    /** Número del día de hoy. */
    public static long today() {
        return dayIndex(System.currentTimeMillis());
    }

    /** Instante que corresponde a "hace N días" (se usa para los libros de ejemplo). */
    public static long millisDaysAgo(int daysAgo) {
        return System.currentTimeMillis() - daysAgo * DAY_MS;
    }

    /** Fecha corta, por ejemplo 16/11/2026. */
    public static String formatDate(long millis) {
        return new SimpleDateFormat("dd/MM/yyyy", ES).format(new Date(millis));
    }

    /** Título del mes, por ejemplo "Noviembre 2026". */
    public static String monthTitle(int year, int month0) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(year, month0, 1);
        String text = new SimpleDateFormat("MMMM yyyy", ES).format(c.getTime());
        return text.substring(0, 1).toUpperCase(ES) + text.substring(1);
    }

    /** Cantidad de días que tiene el mes. */
    public static int daysInMonth(int year, int month0) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(year, month0, 1);
        return c.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    /** Cuántas casillas vacías hay antes del día 1 si la semana empieza el lunes. */
    public static int firstDayOffsetMonday(int year, int month0) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(year, month0, 1);
        int dayOfWeek = c.get(Calendar.DAY_OF_WEEK); // domingo = 1 ... sábado = 7
        return (dayOfWeek + 5) % 7;                  // lunes = 0 ... domingo = 6
    }

    /** Número de día (dayIndex) de una fecha concreta del calendario. */
    public static long dayIndexOf(int year, int month0, int day) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(year, month0, day, 12, 0, 0);
        return dayIndex(c.getTimeInMillis());
    }

    /** Año de un instante. */
    public static int yearOf(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        return c.get(Calendar.YEAR);
    }

    /** Mes (0 a 11) de un instante. */
    public static int monthOf(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        return c.get(Calendar.MONTH);
    }

    /** Día del mes (1 a 31) de un instante. */
    public static int dayOfMonthOf(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        return c.get(Calendar.DAY_OF_MONTH);
    }
}
