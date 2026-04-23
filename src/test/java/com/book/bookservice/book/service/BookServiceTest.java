package com.book.bookservice.book.service;

import com.book.bookservice.basket.entity.BasketStatus;
import com.book.bookservice.basket.repository.BasketItemRepository;
import com.book.bookservice.book.dto.BookResponse;
import com.book.bookservice.book.dto.CreateBookRequest;
import com.book.bookservice.book.dto.UpdateBookRequest;
import com.book.bookservice.book.entity.Book;
import com.book.bookservice.book.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BasketItemRepository basketItemRepository;

    private BookService bookService;

    @BeforeEach
    void setUp() {
        bookService = new BookService(bookRepository, basketItemRepository);
    }

    @Test
    void shouldCreateBookWithDefaultPriceAndTrimmedValues() {
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> {
            Book saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 101L);
            return saved;
        });

        BookResponse response = bookService.createBook(new CreateBookRequest(
                "  Clean Code  ",
                "  Robert C. Martin  ",
                2008,
                "pragmatic coding"
        ));

        assertThat(response.id()).isEqualTo(101L);
        assertThat(response.title()).isEqualTo("Clean Code");
        assertThat(response.author()).isEqualTo("Robert C. Martin");
        assertThat(response.price()).isEqualByComparingTo("50.00");
    }

    @Test
    void shouldUpdateExistingBook() {
        Book existing = new Book("Old", "Author", 2001, "desc", new BigDecimal("50.00"));
        ReflectionTestUtils.setField(existing, "id", 5L);
        when(bookRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(bookRepository.save(existing)).thenReturn(existing);

        BookResponse response = bookService.updateBook(5L, new UpdateBookRequest(
                "  New Title ",
                " New Author ",
                2005,
                "new desc"
        ));

        assertThat(response.title()).isEqualTo("New Title");
        assertThat(response.author()).isEqualTo("New Author");
        assertThat(response.year()).isEqualTo(2005);
    }

    @Test
    void shouldRejectDeleteWhenBookInActiveBasket() {
        when(bookRepository.existsById(7L)).thenReturn(true);
        when(basketItemRepository.existsByBookIdAndBasketStatus(7L, BasketStatus.ACTIVE)).thenReturn(true);

        assertThatThrownBy(() -> bookService.deleteBook(7L))
                .isInstanceOf(ResponseStatusException.class)
                .matches(ex -> ((ResponseStatusException) ex).getStatusCode() == HttpStatus.CONFLICT);
    }

    @Test
    void shouldDeleteBookWhenNotInActiveBasket() {
        when(bookRepository.existsById(9L)).thenReturn(true);
        when(basketItemRepository.existsByBookIdAndBasketStatus(9L, BasketStatus.ACTIVE)).thenReturn(false);

        bookService.deleteBook(9L);

        verify(bookRepository).deleteById(9L);
    }
}
