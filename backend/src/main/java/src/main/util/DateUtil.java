package src.main.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
        LocalDate now = LocalDate.now();
        
        switch (period) {
            case DAILY:
                return now;
            case WEEKLY:
                return now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MONTHLY:
                return now.with(TemporalAdjusters.firstDayOfMonth());
            case YEARLY:
                return now.with(TemporalAdjusters.firstDayOfYear());
            default:
                throw new IllegalArgumentException("Неизвестный период: " + period);
        }
    }

    /**
     * Возвращает начало периода для указанного типа периода
     */
    public static LocalDateTime getStartDateFromPeriod(String period) {
        LocalDate now = LocalDate.now();
        switch (period.toUpperCase()) {
            case "ДЕНЬ":
                return now.atStartOfDay();
            case "НЕДЕЛЯ":
                return now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
            case "МЕСЯЦ":
                return now.with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay();
            case "ГОД":
                return now.with(TemporalAdjusters.firstDayOfYear()).atStartOfDay();
            default:
                // По умолчанию возвращаем начало текущего месяца или выбрасываем исключение
                return now.with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay();
        }
    }
} 