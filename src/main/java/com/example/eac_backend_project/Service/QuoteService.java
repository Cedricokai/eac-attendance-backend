package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.entity.attendance.Quote;

import java.util.List;

public interface QuoteService {

    Quote createQuote(Quote quote);

    Quote updateQuote(Long id, Quote quote);

    void deleteQuote(Long id);

    Quote getQuoteById(Long id);

    Quote getQuoteByNumber(String quoteNumber);

    List<Quote> getAllQuotes();

    List<Quote> searchQuotesByCustomerName(String customerName);

    List<Quote> getQuotesByStatus(String status);

    Quote changeStatus(Long id, String status);

    Quote duplicateQuote(Long id);
}