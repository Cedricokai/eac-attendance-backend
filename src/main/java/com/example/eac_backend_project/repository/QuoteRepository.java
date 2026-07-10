package com.example.eac_backend_project.repository;


import com.example.eac_backend_project.entity.attendance.Quote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface QuoteRepository extends JpaRepository<Quote, Long> {

    Optional<Quote> findByQuoteNumber(String quoteNumber);

    List<Quote> findByCustomerNameContainingIgnoreCase(String customerName);

    List<Quote> findByQuoteDateBetween(LocalDate startDate, LocalDate endDate);

    List<Quote> findByStatus(Quote.QuoteStatus status);

    List<Quote> findByValidUntilBeforeAndStatusNot(LocalDate date, Quote.QuoteStatus status);
}