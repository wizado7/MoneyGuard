package src.main.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import src.main.dto.report.ReportResponse;
import src.main.service.ReportService;
import src.main.exception.BusinessException;
import src.main.exception.InvalidDataException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@Validated
@Slf4j
public class ReportController {

    private final ReportService reportService;

    @GetMapping
    public ResponseEntity<ReportResponse> getReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date_from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date_to,
            @RequestParam(required = false, defaultValue = "monthly") String type) {
        log.debug("REST запрос на получение отчета типа: {}", type);
        try {
            if (date_from != null && date_to != null && date_from.isAfter(date_to)) {
                throw new InvalidDataException("Начальная дата не может быть позже конечной даты")
                        .addError("date_from", "Начальная дата не может быть позже конечной даты");
            }
            
            if (!type.equals("daily") && !type.equals("weekly") && !type.equals("monthly") && !type.equals("yearly")) {
                throw new InvalidDataException("Некорректный тип отчета")
                        .addError("type", "Тип отчета должен быть одним из: daily, weekly, monthly, yearly");
            }
            
            return ResponseEntity.ok(reportService.getReport(date_from, date_to, type));
        } catch (InvalidDataException e) {
            throw e;
        } catch (Exception e) {
            log.error("Ошибка при получении отчета: {}", e.getMessage(), e);
            throw new BusinessException("Ошибка при получении отчета", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
} 