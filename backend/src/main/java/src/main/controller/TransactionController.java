package src.main.controller;

import jakarta.validation.Valid;
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

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<TransactionAIResponse> createTransaction(@Valid @RequestBody TransactionRequest request) {
        log.debug("REST request to create Transaction : {}", request);
        TransactionAIResponse response = transactionService.createTransaction(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getTransactions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String categoryName) {
        log.debug("REST request to get Transactions with filter: dateFrom={}, dateTo={}, categoryName={}",
                 dateFrom, dateTo, categoryName);
        List<TransactionResponse> transactions = transactionService.getTransactions(dateFrom, dateTo, categoryName);
        return ResponseEntity.ok(transactions);
    }


    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getTransactionById(@PathVariable Integer id) {
        log.debug("REST request to get Transaction : {}", id);
        TransactionResponse transaction = transactionService.getTransactionById(id);
        return ResponseEntity.ok(transaction);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransactionResponse> updateTransaction(@PathVariable Integer id, @Valid @RequestBody TransactionRequest request) {
        log.debug("REST request to update Transaction : {}", request);
        TransactionResponse updatedTransaction = transactionService.updateTransaction(id, request);
        return ResponseEntity.ok(updatedTransaction);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Integer id) {
        log.debug("REST request to delete Transaction : {}", id);
        transactionService.deleteTransaction(id);
        return ResponseEntity.noContent().build();
    }

    // Комментируем или удаляем эндпоинт импорта
    /*
    @PostMapping("/import")
    public ResponseEntity<ImportResponse> importTransactions(@RequestParam("file") MultipartFile file) {
        log.debug("REST request to import Transactions from file : {}", file.getOriginalFilename());
        // Метод в сервисе сейчас заглушка
        ImportResponse response = transactionService.importTransactions(file);
        return ResponseEntity.ok(response);
    }
    */
}