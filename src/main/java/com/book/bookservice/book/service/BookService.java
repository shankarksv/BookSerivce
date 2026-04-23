package com.book.bookservice.book.service;

import com.book.bookservice.basket.entity.BasketStatus;
import com.book.bookservice.basket.repository.BasketItemRepository;
import com.book.bookservice.book.dto.BookResponse;
import com.book.bookservice.book.dto.CreateBookRequest;
import com.book.bookservice.book.dto.UpdateBookRequest;
import com.book.bookservice.book.entity.Book;
import com.book.bookservice.book.repository.BookRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class BookService {

    private static final BigDecimal DEFAULT_PRICE = new BigDecimal("50.00");

    private final BookRepository bookRepository;
    private final BasketItemRepository basketItemRepository;

    public BookService(BookRepository bookRepository, BasketItemRepository basketItemRepository) {
        this.bookRepository = bookRepository;
        this.basketItemRepository = basketItemRepository;
    }

    @Transactional(readOnly = true)
    public List<BookResponse> getAllBooks() {
        return bookRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public BookResponse getBookById(Long id) {
        return toResponse(getBookEntityById(id));
    }

    public BookResponse createBook(CreateBookRequest request) {
        Book book = new Book(
                request.title().trim(),
                request.author().trim(),
                request.year(),
                request.description(),
                DEFAULT_PRICE
        );
        return toResponse(bookRepository.save(book));
    }

    public BookResponse updateBook(Long id, UpdateBookRequest request) {
        Book book = getBookEntityById(id);
        book.setTitle(request.title().trim());
        book.setAuthor(request.author().trim());
        book.setYear(request.year());
        book.setDescription(request.description());
        return toResponse(bookRepository.save(book));
    }

    public void deleteBook(Long id) {
        if (!bookRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "book not found");
        }

        boolean existsInActiveBaskets = basketItemRepository.existsByBookIdAndBasketStatus(id, BasketStatus.ACTIVE);
        if (existsInActiveBaskets) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "book exists in active basket");
        }

        bookRepository.deleteById(id);
    }

    private Book getBookEntityById(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "book not found"));
    }

    private BookResponse toResponse(Book book) {
        return new BookResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getYear(),
                book.getDescription(),
                book.getPrice()
        );
    }
}
