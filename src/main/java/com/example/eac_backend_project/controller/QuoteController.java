package com.example.eac_backend_project.controller;


import com.example.eac_backend_project.Service.QuoteService;
import com.example.eac_backend_project.entity.attendance.Quote;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/quotes")
@CrossOrigin(origins = "http://localhost:3000") // Adjust based on your React app URL
public class QuoteController {

    @Autowired
    private QuoteService quoteService;

    @GetMapping
    public ResponseEntity<List<Quote>> getAllQuotes() {
        List<Quote> quotes = quoteService.getAllQuotes();
        return ResponseEntity.ok(quotes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Quote> getQuoteById(@PathVariable Long id) {
        Quote quote = quoteService.getQuoteById(id);
        return ResponseEntity.ok(quote);
    }

    @GetMapping("/number/{quoteNumber}")
    public ResponseEntity<Quote> getQuoteByNumber(@PathVariable String quoteNumber) {
        Quote quote = quoteService.getQuoteByNumber(quoteNumber);
        return ResponseEntity.ok(quote);
    }

    @PostMapping
    public ResponseEntity<Quote> createQuote(@Valid @RequestBody Quote quote) {
        Quote createdQuote = quoteService.createQuote(quote);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdQuote);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Quote> updateQuote(@PathVariable Long id, @Valid @RequestBody Quote quote) {
        Quote updatedQuote = quoteService.updateQuote(id, quote);
        return ResponseEntity.ok(updatedQuote);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Boolean>> deleteQuote(@PathVariable Long id) {
        quoteService.deleteQuote(id);
        Map<String, Boolean> response = new HashMap<>();
        response.put("deleted", Boolean.TRUE);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<List<Quote>> searchQuotes(@RequestParam String customerName) {
        List<Quote> quotes = quoteService.searchQuotesByCustomerName(customerName);
        return ResponseEntity.ok(quotes);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Quote>> getQuotesByStatus(@PathVariable String status) {
        List<Quote> quotes = quoteService.getQuotesByStatus(status);
        return ResponseEntity.ok(quotes);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Quote> updateQuoteStatus(@PathVariable Long id, @RequestBody Map<String, String> statusMap) {
        String status = statusMap.get("status");
        Quote quote = quoteService.changeStatus(id, status);
        return ResponseEntity.ok(quote);
    }

    @PostMapping("/{id}/duplicate")
    public ResponseEntity<Quote> duplicateQuote(@PathVariable Long id) {
        Quote duplicatedQuote = quoteService.duplicateQuote(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(duplicatedQuote);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", ex.getMessage());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleEntityNotFound(EntityNotFoundException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
}