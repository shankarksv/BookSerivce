package com.book.bookservice.basket.service;

import com.book.bookservice.basket.dto.AddBasketItemRequest;
import com.book.bookservice.basket.dto.BasketResponse;
import com.book.bookservice.basket.dto.UpdateBasketItemRequest;
import com.book.bookservice.basket.entity.Basket;
import com.book.bookservice.basket.entity.BasketItem;
import com.book.bookservice.basket.entity.BasketStatus;
import com.book.bookservice.basket.repository.BasketItemRepository;
import com.book.bookservice.basket.repository.BasketRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BasketServiceTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock
    private BasketRepository basketRepository;

    @Mock
    private BasketItemRepository basketItemRepository;

    @Mock
    private BookRepository bookRepository;

    private BasketService basketService;

    @BeforeEach
    void setUp() {
        basketService = new BasketService(basketRepository, basketItemRepository, bookRepository);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("basketApiData")
    void basket_api_from_json(BasketCase c) {
        reset(basketRepository, basketItemRepository, bookRepository);
        switch (c.type()) {
            case "get" -> runGetCase(c);
            case "add" -> runAddCase(c);
            case "update" -> runUpdateCase(c);
            default -> throw new IllegalArgumentException("unsupported case type: " + c.type());
        }
    }

    private void runGetCase(BasketCase c) {
        if (Boolean.TRUE.equals(c.input().hasActiveBasket())) {
            Basket basket = basketForCase(c);
            when(basketRepository.findByUserIdAndStatus(c.userId(), BasketStatus.ACTIVE)).thenReturn(Optional.of(basket));
        } else {
            when(basketRepository.findByUserIdAndStatus(c.userId(), BasketStatus.ACTIVE)).thenReturn(Optional.empty());
        }

        BasketResponse response = basketService.getBasket(c.userId());
        assertThat(response.userId()).isEqualTo(c.userId());
        assertThat(response.status()).isEqualTo(BasketStatus.ACTIVE.name());
        assertThat(response.items()).hasSize(c.expected().itemsCount());
    }

    private void runAddCase(BasketCase c) {
        Basket basket = basketForCase(c);
        Book book = bookForCase(c.input().bookId(), c.input().bookTitle());
        when(basketRepository.findByUserIdAndStatus(c.userId(), BasketStatus.ACTIVE)).thenReturn(Optional.of(basket));
        when(bookRepository.findById(c.input().bookId())).thenReturn(Optional.of(book));
        when(basketItemRepository.findByBasketIdAndBookId(c.input().basketId(), c.input().bookId()))
                .thenReturn(Optional.ofNullable(existingItemForCase(c, basket, book)));

        BasketResponse response = basketService.addBookToBasket(
                c.userId(),
                new AddBasketItemRequest(c.input().bookId(), c.input().quantity()));

        assertThat(response.items()).hasSize(c.expected().itemsCount());
        assertThat(response.items().get(0).quantity()).isEqualTo(c.expected().firstItemQuantity());
        verify(basketRepository, times(1)).save(basket);
    }

    private void runUpdateCase(BasketCase c) {
        Basket basket = basketForCase(c);
        when(basketRepository.findByUserIdAndStatus(c.userId(), BasketStatus.ACTIVE)).thenReturn(Optional.of(basket));

        BasketItem existing = null;
        if (Boolean.TRUE.equals(c.input().itemExists())) {
            Book book = bookForCase(c.input().bookId(), c.input().bookTitle());
            existing = new BasketItem(basket, book, c.input().existingQuantity());
            basket.getItems().add(existing);
            when(basketItemRepository.findByBasketIdAndBookId(c.input().basketId(), c.input().bookId()))
                    .thenReturn(Optional.of(existing));
        } else {
            when(basketItemRepository.findByBasketIdAndBookId(c.input().basketId(), c.input().bookId()))
                    .thenReturn(Optional.empty());
        }

        if (c.expected().errorStatus() != null) {
            assertThatThrownBy(() -> basketService.updateBasketItem(
                    c.userId(),
                    c.input().bookId(),
                    new UpdateBasketItemRequest(c.input().quantity())))
                    .isInstanceOf(ResponseStatusException.class)
                    .matches(ex -> ((ResponseStatusException) ex).getStatusCode() == HttpStatus.valueOf(c.expected().errorStatus()));
            verify(basketRepository, never()).save(basket);
            return;
        }

        BasketResponse response = basketService.updateBasketItem(
                c.userId(),
                c.input().bookId(),
                new UpdateBasketItemRequest(c.input().quantity()));
        assertThat(response.items()).hasSize(c.expected().itemsCount());
        assertThat(response.items().get(0).quantity()).isEqualTo(c.expected().firstItemQuantity());
        verify(basketRepository, times(1)).save(basket);
        assertThat(existing).isNotNull();
    }

    private Basket basketForCase(BasketCase c) {
        Basket basket = new Basket(c.userId(), BasketStatus.ACTIVE);
        ReflectionTestUtils.setField(basket, "id", c.input().basketId());
        if (Boolean.TRUE.equals(c.input().hasActiveBasketItem())) {
            Book existingBook = bookForCase(c.input().bookId(), c.input().bookTitle());
            basket.getItems().add(new BasketItem(basket, existingBook, c.input().existingQuantity()));
        }
        return basket;
    }

    private Book bookForCase(Long bookId, String title) {
        Book book = new Book(title, "Test Author", 2020, "test description", new BigDecimal("50.00"));
        ReflectionTestUtils.setField(book, "id", bookId);
        return book;
    }

    private BasketItem existingItemForCase(BasketCase c, Basket basket, Book book) {
        if (!Boolean.TRUE.equals(c.input().itemExists())) {
            return null;
        }
        BasketItem existing = new BasketItem(basket, book, c.input().existingQuantity());
        basket.getItems().add(existing);
        return existing;
    }

    static Stream<BasketCase> basketApiData() {
        try (InputStream in = BasketServiceTest.class
                .getClassLoader()
                .getResourceAsStream("basket/basket-service-cases.json")) {
            if (in == null) {
                throw new IllegalStateException("missing test resource: basket/basket-service-cases.json");
            }
            List<BasketCase> cases = OBJECT_MAPPER.readValue(in, new TypeReference<>() {});
            return cases.stream();
        } catch (java.io.IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    record BasketCase(String name, String type, Long userId, BasketInput input, BasketExpected expected) {
        @Override
        public String toString() {
            return name;
        }
    }

    record BasketInput(
            Long basketId,
            Long bookId,
            String bookTitle,
            Integer quantity,
            Integer existingQuantity,
            Boolean hasActiveBasket,
            Boolean hasActiveBasketItem,
            Boolean itemExists
    ) {
    }

    record BasketExpected(Integer itemsCount, Integer firstItemQuantity, Integer errorStatus) {
    }
}
