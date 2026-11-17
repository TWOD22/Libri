package com.example.libri.util;

import com.example.libri.data.local.BookEntity;
import com.example.libri.data.local.ProgressLogEntity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Cálculos de la racha de lectura y de las estadísticas del mes.
 */
public final class StatsCalculator {

    private StatsCalculator() {
        // Clase de utilidades: no se instancia
    }

    /** Resultado de las estadísticas de un mes. */
    public static class MonthStats {
        public int booksFinished;
        public int pages;
        public int pagesPerDay;
        /** Días del mes (1 a 31) en los que se registró lectura. */
        public final Set<Integer> daysRead = new HashSet<>();
    }

    /** Conjunto de días (dayIndex) en los que el usuario registró avance. */
    public static Set<Long> readingDays(List<ProgressLogEntity> logs) {
        Set<Long> days = new HashSet<>();
        if (logs != null) {
            for (ProgressLogEntity log : logs) {
                if (log.delta > 0) {
                    days.add(DateUtils.dayIndex(log.date));
                }
            }
        }
        return days;
    }

    /**
     * Racha actual: días seguidos con lectura. Cuenta desde hoy; si hoy todavía no se ha leído,
     * cuenta desde ayer (así la racha no se pierde hasta que termine el día).
     */
    public static int currentStreak(Set<Long> days) {
        long today = DateUtils.today();
        long day = days.contains(today) ? today : today - 1;
        int streak = 0;
        while (days.contains(day)) {
            streak++;
            day--;
        }
        return streak;
    }

    /** true si hoy ya se registró avance (meta diaria cumplida). */
    public static boolean readToday(Set<Long> days) {
        return days.contains(DateUtils.today());
    }

    /** Estadísticas de un mes: libros terminados, páginas leídas y días de lectura. */
    public static MonthStats monthStats(int year, int month0,
                                        List<BookEntity> books, List<ProgressLogEntity> logs) {
        MonthStats stats = new MonthStats();
        if (books != null) {
            for (BookEntity book : books) {
                if (BookEntity.STATUS_DONE.equals(book.status) && book.finishDate > 0
                        && DateUtils.yearOf(book.finishDate) == year
                        && DateUtils.monthOf(book.finishDate) == month0) {
                    stats.booksFinished++;
                }
            }
        }
        if (logs != null) {
            for (ProgressLogEntity log : logs) {
                if (log.delta > 0 && DateUtils.yearOf(log.date) == year
                        && DateUtils.monthOf(log.date) == month0) {
                    stats.pages += log.pagesEquivalent;
                    stats.daysRead.add(DateUtils.dayOfMonthOf(log.date));
                }
            }
        }
        if (!stats.daysRead.isEmpty()) {
            stats.pagesPerDay = Math.round(stats.pages / (float) stats.daysRead.size());
        }
        return stats;
    }

    /** Cantidad de libros terminados en un año. */
    public static int booksFinishedInYear(int year, List<BookEntity> books) {
        int count = 0;
        if (books != null) {
            for (BookEntity book : books) {
                if (BookEntity.STATUS_DONE.equals(book.status) && book.finishDate > 0
                        && DateUtils.yearOf(book.finishDate) == year) {
                    count++;
                }
            }
        }
        return count;
    }
}
