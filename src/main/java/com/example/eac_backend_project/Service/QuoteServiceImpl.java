package com.example.eac_backend_project.Service;


import com.example.eac_backend_project.entity.attendance.Quote;
import com.example.eac_backend_project.entity.attendance.QuoteItem;
import com.example.eac_backend_project.repository.QuoteRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class QuoteServiceImpl implements QuoteService {

    @Autowired
    private QuoteRepository quoteRepository;

    @Override
    public Quote createQuote(Quote quote) {
        if (quote.getQuoteNumber() == null || quote.getQuoteNumber().isEmpty()) {
            quote.setQuoteNumber(generateQuoteNumber());
        }

        quote.calculateTotals();
        quote.setStatus(Quote.QuoteStatus.DRAFT);
        quote.setCreatedAt(LocalDate.now());
        quote.setUpdatedAt(LocalDate.now());

        // Ensure items are properly linked
        if (quote.getItems() != null) {
            for (QuoteItem item : quote.getItems()) {
                item.calculateTotal();
            }
        }

        return quoteRepository.save(quote);
    }

    @Override
    public Quote updateQuote(Long id, Quote quoteDetails) {
        Quote existingQuote = quoteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Quote not found with id: " + id));

        // Update basic fields
        existingQuote.setCustomerName(quoteDetails.getCustomerName());
        existingQuote.setCustomerEmail(quoteDetails.getCustomerEmail());
        existingQuote.setCustomerPhone(quoteDetails.getCustomerPhone());
        existingQuote.setCustomerAddress(quoteDetails.getCustomerAddress());
        existingQuote.setQuoteDate(quoteDetails.getQuoteDate());
        existingQuote.setValidUntil(quoteDetails.getValidUntil());
        existingQuote.setNotes(quoteDetails.getNotes());
        existingQuote.setTerms(quoteDetails.getTerms());
        existingQuote.setTaxRate(quoteDetails.getTaxRate());
        existingQuote.setDiscount(quoteDetails.getDiscount());

        // Clear existing items and add new ones
        existingQuote.getItems().clear();
        if (quoteDetails.getItems() != null) {
            for (QuoteItem item : quoteDetails.getItems()) {
                existingQuote.addItem(item);
            }
        }

        // Recalculate totals
        existingQuote.calculateTotals();
        existingQuote.setUpdatedAt(LocalDate.now());

        return quoteRepository.save(existingQuote);
    }

    @Override
    public void deleteQuote(Long id) {
        Quote quote = quoteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Quote not found with id: " + id));
        quoteRepository.delete(quote);
    }

    @Override
    public Quote getQuoteById(Long id) {
        return quoteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Quote not found with id: " + id));
    }

    @Override
    public Quote getQuoteByNumber(String quoteNumber) {
        return quoteRepository.findByQuoteNumber(quoteNumber)
                .orElseThrow(() -> new EntityNotFoundException("Quote not found with number: " + quoteNumber));
    }

    @Override
    public List<Quote> getAllQuotes() {
        return quoteRepository.findAll();
    }

    @Override
    public List<Quote> searchQuotesByCustomerName(String customerName) {
        return quoteRepository.findByCustomerNameContainingIgnoreCase(customerName);
    }

    @Override
    public List<Quote> getQuotesByStatus(String status) {
        try {
            Quote.QuoteStatus quoteStatus = Quote.QuoteStatus.valueOf(status.toUpperCase());
            return quoteRepository.findByStatus(quoteStatus);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status: " + status);
        }
    }

    @Override
    public Quote changeStatus(Long id, String status) {
        Quote quote = getQuoteById(id);
        try {
            Quote.QuoteStatus newStatus = Quote.QuoteStatus.valueOf(status.toUpperCase());
            quote.setStatus(newStatus);
            quote.setUpdatedAt(LocalDate.now());
            return quoteRepository.save(quote);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status: " + status);
        }
    }

    @Override
    public Quote duplicateQuote(Long id) {
        Quote originalQuote = getQuoteById(id);
        Quote duplicatedQuote = new Quote();

        // Copy basic information
        duplicatedQuote.setCustomerName(originalQuote.getCustomerName());
        duplicatedQuote.setCustomerEmail(originalQuote.getCustomerEmail());
        duplicatedQuote.setCustomerPhone(originalQuote.getCustomerPhone());
        duplicatedQuote.setCustomerAddress(originalQuote.getCustomerAddress());
        duplicatedQuote.setQuoteDate(LocalDate.now());
        duplicatedQuote.setValidUntil(LocalDate.now().plusDays(30));
        duplicatedQuote.setNotes(originalQuote.getNotes());
        duplicatedQuote.setTerms(originalQuote.getTerms());
        duplicatedQuote.setTaxRate(originalQuote.getTaxRate());
        duplicatedQuote.setDiscount(originalQuote.getDiscount());

        // Generate new quote number
        duplicatedQuote.setQuoteNumber(generateQuoteNumber());

        // Duplicate items
        if (originalQuote.getItems() != null) {
            for (QuoteItem originalItem : originalQuote.getItems()) {
                QuoteItem duplicatedItem = new QuoteItem();
                duplicatedItem.setDescription(originalItem.getDescription());
                duplicatedItem.setQuantity(originalItem.getQuantity());
                duplicatedItem.setUnitPrice(originalItem.getUnitPrice());
                duplicatedItem.calculateTotal();
                duplicatedQuote.addItem(duplicatedItem);
            }
        }

        // Calculate totals
        duplicatedQuote.calculateTotals();
        duplicatedQuote.setStatus(Quote.QuoteStatus.DRAFT);

        return quoteRepository.save(duplicatedQuote);
    }

    private String generateQuoteNumber() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String random = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "QT-" + timestamp.substring(timestamp.length() - 8) + "-" + random;
    }
}