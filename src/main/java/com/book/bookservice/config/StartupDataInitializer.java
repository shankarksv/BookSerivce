package com.book.bookservice.config;

import com.book.bookservice.book.entity.Book;
import com.book.bookservice.book.repository.BookRepository;
import com.book.bookservice.pricing.entity.DiscountConfig;
import com.book.bookservice.pricing.repository.DiscountConfigRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
public class StartupDataInitializer implements ApplicationRunner {

    private final BookRepository bookRepository;
    private final DiscountConfigRepository discountConfigRepository;

    public StartupDataInitializer(BookRepository bookRepository, DiscountConfigRepository discountConfigRepository) {
        this.bookRepository = bookRepository;
        this.discountConfigRepository = discountConfigRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedDiscountsIfEmpty();
        seedBooksIfEmpty();
    }

    private void seedDiscountsIfEmpty() {
        if (discountConfigRepository.count() > 0) {
            return;
        }

        discountConfigRepository.saveAll(List.of(
                new DiscountConfig(2, new BigDecimal("0.0500"), true),
                new DiscountConfig(3, new BigDecimal("0.1000"), true),
                new DiscountConfig(4, new BigDecimal("0.2000"), true),
                new DiscountConfig(5, new BigDecimal("0.2500"), true)
        ));
    }

    private void seedBooksIfEmpty() {
        if (bookRepository.count() > 0) {
            return;
        }

        BigDecimal price = new BigDecimal("50.00");
        int stockQuantity = 100;
        bookRepository.saveAll(List.of(
                new Book("Clean Code", "Robert C. Martin", 2008, "A handbook of agile software craftsmanship.", price, stockQuantity),
                new Book("Refactoring", "Martin Fowler", 1999, "Improving the design of existing code.", price, stockQuantity),
                new Book("Domain-Driven Design", "Eric Evans", 2003, "Tackling complexity in the heart of software.", price, stockQuantity),
                new Book("Test-Driven Development: By Example", "Kent Beck", 2002, "Practical guide to TDD workflow.", price, stockQuantity),
                new Book("The Pragmatic Programmer", "Andrew Hunt and David Thomas", 1999, "Classic tips and practices for software developers.", price, stockQuantity)
        ));
    }
}
