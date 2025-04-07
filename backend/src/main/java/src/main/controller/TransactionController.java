package src.main.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import src.main.dto.transaction.ImportResponse;
import src.main.dto.transaction.TransactionAIResponse;
import src.main.dto.transaction.TransactionRequest;
import src.main.dto.transaction.TransactionResponse;
import src.main.service.TransactionService;
import src.main.exception.BusinessException;
import src.main.exception.EntityNotFoundException;
import src.main.exception.InvalidDataException;
import src.main.exception.OperationNotAllowedException;
import src.main.exception.ConflictException;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
@Validated
@Slf4j
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getTransactions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date_from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date_to,
            @RequestParam(required = false) String category
    ) {
        log.debug("REST запрос на получение списка транзакций");
        try {
            if (date_from != null && date_to != null && date_from.isAfter(date_to)) {
                throw new InvalidDataException("Начальная дата не может быть позже конечной даты")
                        .addError("date_from", "Начальная дата не может быть позже конечной даты");
            }
            return ResponseEntity.ok(transactionService.getTransactions(date_from, date_to, category));
        } catch (InvalidDataException e) {
            throw e;
        } catch (Exception e) {
            log.error("Ошибка при получении списка транзакций: {}", e.getMessage(), e);
            throw new BusinessException("Ошибка при получении списка транзакций", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping
    public ResponseEntity<TransactionAIResponse> createTransaction(@RequestBody @Valid TransactionRequest request) {
        log.debug("REST запрос на создание новой транзакции");
        try {
            TransactionAIResponse response = transactionService.createTransaction(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (EntityNotFoundException | OperationNotAllowedException | InvalidDataException e) {
            throw e;
        } catch (Exception e) {
            log.error("Ошибка при создании транзакции: {}", e.getMessage(), e);
            throw new BusinessException("Ошибка при создании транзакции", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransactionResponse> updateTransaction(
            @PathVariable @Positive(message = "ID должен быть положительным числом") Long id,
            @RequestBody @Valid TransactionRequest request
    ) {
        log.debug("REST запрос на обновление транзакции с ID: {}", id);
        try {
            return ResponseEntity.ok(transactionService.updateTransaction(id, request));
        } catch (EntityNotFoundException | OperationNotAllowedException | InvalidDataException e) {
            throw e;
        } catch (Exception e) {
            log.error("Ошибка при обновлении транзакции с ID {}: {}", id, e.getMessage(), e);
            throw new BusinessException("Ошибка при обновлении транзакции", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(
            @PathVariable @Positive(message = "ID должен быть положительным числом") Long id
    ) {
        log.debug("REST запрос на удаление транзакции с ID: {}", id);
        try {
            transactionService.deleteTransaction(id);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException | OperationNotAllowedException e) {
            throw e;
        } catch (Exception e) {
            log.error("Ошибка при удалении транзакции с ID {}: {}", id, e.getMessage(), e);
            throw new BusinessException("Ошибка при удалении транзакции", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/import")
    public ResponseEntity<ImportResponse> importTransactions(@RequestParam("file") MultipartFile file) {
        log.debug("REST запрос на импорт транзакций из файла");
        try {
            if (file.isEmpty()) {
                throw new InvalidDataException("Файл не может быть пустым")
                        .addError("file", "Файл не может быть пустым");
            }
            
            String fileName = file.getOriginalFilename();
            if (fileName == null || !(fileName.endsWith(".csv") || fileName.endsWith(".xlsx") || fileName.endsWith(".xls"))) {
                throw new InvalidDataException("Неподдерживаемый формат файла")
                        .addError("file", "Поддерживаются только файлы CSV, XLS и XLSX");
            }
            
            return ResponseEntity.accepted().body(transactionService.importTransactions(file));
        } catch (InvalidDataException e) {
            throw e;
        } catch (Exception e) {
            log.error("Ошибка при импорте транзакций: {}", e.getMessage(), e);
            throw new BusinessException("Ошибка при импорте транзакций", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
} 