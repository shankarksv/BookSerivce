package com.book.bookservice.book.service;

import com.book.bookservice.basket.entity.BasketStatus;
import com.book.bookservice.basket.repository.BasketItemRepository;
import com.book.bookservice.book.dto.BookResponse;
import com.book.bookservice.book.dto.CreateBookRequest;
import com.book.bookservice.book.dto.UpdateBookRequest;
import com.book.bookservice.book.entity.Book;
import com.book.bookservice.book.repository.BookRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BasketItemRepository basketItemRepository;

    private BookService bookService;

    @BeforeEach
    void setUp() {
        bookService = new BookService(bookRepository, basketItemRepository);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("bookApiData")
    void book_api_from_json(BookCase c) {
        reset(bookRepository, basketItemRepository);
        switch (c.type()) {
            case "create" -> runCreateCase(c);
            case "update" -> runUpdateCase(c);
            case "delete" -> runDeleteCase(c);
            default -> throw new IllegalArgumentException("unsupported case type: " + c.type());
        }
    }

    private void runCreateCase(BookCase c) {
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> {
            Book saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", c.savedId());
            return saved;
        });

        BookResponse response = bookService.createBook(new CreateBookRequest(
                c.request().title(),
                c.request().author(),
                c.request().year(),
                c.request().description()));

        assertThat(response.id()).isEqualTo(c.savedId());
        assertThat(response.title()).isEqualTo(c.expected().title());
        assertThat(response.author()).isEqualTo(c.expected().author());
        assertThat(response.year()).isEqualTo(c.expected().year());
        assertThat(response.price()).isEqualByComparingTo(c.expected().price());
    }

    private void runUpdateCase(BookCase c) {
        Book existing = new Book(
                c.existing().title(),
                c.existing().author(),
                c.existing().year(),
                c.existing().description(),
                new BigDecimal(c.existing().price()));
        ReflectionTestUtils.setField(existing, "id", c.bookId());
        when(bookRepository.findById(c.bookId())).thenReturn(Optional.of(existing));
        when(bookRepository.save(existing)).thenReturn(existing);

        BookResponse response = bookService.updateBook(c.bookId(), new UpdateBookRequest(
                c.request().title(),
                c.request().author(),
                c.request().year(),
                c.request().description()));

        assertThat(response.id()).isEqualTo(c.bookId());
        assertThat(response.title()).isEqualTo(c.expected().title());
        assertThat(response.author()).isEqualTo(c.expected().author());
        assertThat(response.year()).isEqualTo(c.expected().year());
    }

    private void runDeleteCase(BookCase c) {
        when(bookRepository.existsById(c.bookId())).thenReturn(c.delete().existsInInventory());
        if (c.delete().existsInInventory()) {
            when(basketItemRepository.existsByBookIdAndBasketStatus(c.bookId(), BasketStatus.ACTIVE))
                    .thenReturn(c.delete().existsInActiveBasket());
        }

        if (c.expected().errorStatus() != null) {
            assertThatThrownBy(() -> bookService.deleteBook(c.bookId()))
                    .isInstanceOf(ResponseStatusException.class)
                    .matches(ex -> ((ResponseStatusException) ex).getStatusCode() == HttpStatus.valueOf(c.expected().errorStatus()));
        } else {
            bookService.deleteBook(c.bookId());
        }

        if (c.expected().deleteCallCount() != null && c.expected().deleteCallCount() > 0) {
            verify(bookRepository, times(c.expected().deleteCallCount())).deleteById(c.bookId());
        } else {
            verify(bookRepository, never()).deleteById(c.bookId());
        }
    }

    static Stream<BookCase> bookApiData() {
        try (InputStream in = BookServiceTest.class
                .getClassLoader()
                .getResourceAsStream("book/book-service-cases.json")) {
            if (in == null) {
                throw new IllegalStateException("missing test resource: book/book-service-cases.json");
            }
            List<BookCase> cases = OBJECT_MAPPER.readValue(in, new TypeReference<>() {});
            return cases.stream();
        } catch (java.io.IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    record BookCase(
            String name,
            String type,
            Long bookId,
            Long savedId,
            BookPayload request,
            BookPayload existing,
            DeletePayload delete,
            ExpectedPayload expected
    ) {
        @Override
        public String toString() {
            return name;
        }
    }

    record BookPayload(String title, String author, Integer year, String description, String price) {
    }

    record DeletePayload(Boolean existsInInventory, Boolean existsInActiveBasket) {
    }

    record ExpectedPayload(
            String title,
            String author,
            Integer year,
            String price,
            Integer errorStatus,
            Integer deleteCallCount
    ) {
    }
}
