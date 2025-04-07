package src.main.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import src.main.model.LimitPeriod;

/**
 * Утилитный класс для работы с датами
 */
public class DateUtil {

    /**
     * Возвращает первый день месяца для указанной даты
     */
    public static LocalDate getStartOfMonth(LocalDate date) {
        return date.withDayOfMonth(1);
    }

    /**
     * Возвращает последний день месяца для указанной даты
     */
    public static LocalDate getEndOfMonth(LocalDate date) {
        return date.with(TemporalAdjusters.lastDayOfMonth());
    }

    /**
     * Возвращает первый день недели (понедельник) для указанной даты
     */
    public static LocalDate getStartOfWeek(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    /**
     * Возвращает последний день недели (воскресенье) для указанной даты
     */
    public static LocalDate getEndOfWeek(LocalDate date) {
        return date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
    }

    /**
     * Возвращает первый день года для указанной даты
     */
    public static LocalDate getStartOfYear(LocalDate date) {
        return date.withDayOfYear(1);
    }

    /**
     * Возвращает последний день года для указанной даты
     */
    public static LocalDate getEndOfYear(LocalDate date) {
        return date.with(TemporalAdjusters.lastDayOfYear());
    }

    /**
     * Возвращает начало периода для указанного типа периода
     */
    public static LocalDate getStartOfPeriod(LimitPeriod period) {
        LocalDate today = LocalDate.now();
        
        switch (period) {
            case DAILY:
                return today;
            case WEEKLY:
                return getStartOfWeek(today);
            case MONTHLY:
                return getStartOfMonth(today);
            case YEARLY:
                return getStartOfYear(today);
            default:
                return today;
        }
    }
} 