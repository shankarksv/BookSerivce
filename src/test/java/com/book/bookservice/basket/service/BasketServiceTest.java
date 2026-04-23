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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BasketServiceTest {

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

    @Test
    void shouldReturnEmptyActiveBasketWhenUserHasNoBasket() {
        when(basketRepository.findByUserIdAndStatus(1L, BasketStatus.ACTIVE)).thenReturn(Optional.empty());

        BasketResponse response = basketService.getBasket(1L);

        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo(BasketStatus.ACTIVE.name());
        assertThat(response.items()).isEmpty();
    }

    @Test
    void shouldAddNewBookToBasket() {
        Basket basket = new Basket(2L, BasketStatus.ACTIVE);
        ReflectionTestUtils.setField(basket, "id", 20L);
        Book book = new Book("DDD", "Evans", 2003, "domain design", new BigDecimal("50.00"));
        ReflectionTestUtils.setField(book, "id", 3L);

        when(basketRepository.findByUserIdAndStatus(2L, BasketStatus.ACTIVE)).thenReturn(Optional.of(basket));
        when(bookRepository.findById(3L)).thenReturn(Optional.of(book));
        when(basketItemRepository.findByBasketIdAndBookId(20L, 3L)).thenReturn(Optional.empty());

        BasketResponse response = basketService.addBookToBasket(2L, new AddBasketItemRequest(3L, 2));

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).quantity()).isEqualTo(2);
        verify(basketRepository).save(basket);
    }

    @Test
    void shouldIncrementQuantityWhenBookAlreadyExistsInBasket() {
        Basket basket = new Basket(4L, BasketStatus.ACTIVE);
        ReflectionTestUtils.setField(basket, "id", 40L);
        Book book = new Book("Refactoring", "Fowler", 1999, "code refactoring", new BigDecimal("50.00"));
        ReflectionTestUtils.setField(book, "id", 8L);
        BasketItem existing = new BasketItem(basket, book, 1);
        basket.getItems().add(existing);

        when(basketRepository.findByUserIdAndStatus(4L, BasketStatus.ACTIVE)).thenReturn(Optional.of(basket));
        when(bookRepository.findById(8L)).thenReturn(Optional.of(book));
        when(basketItemRepository.findByBasketIdAndBookId(40L, 8L)).thenReturn(Optional.of(existing));

        BasketResponse response = basketService.addBookToBasket(4L, new AddBasketItemRequest(8L, 3));

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).quantity()).isEqualTo(4);
    }

    @Test
    void shouldUpdateBasketItemQuantity() {
        Basket basket = new Basket(6L, BasketStatus.ACTIVE);
        ReflectionTestUtils.setField(basket, "id", 60L);
        Book book = new Book("XP", "Beck", 2000, "xp", new BigDecimal("50.00"));
        ReflectionTestUtils.setField(book, "id", 12L);
        BasketItem existing = new BasketItem(basket, book, 2);
        basket.getItems().add(existing);

        when(basketRepository.findByUserIdAndStatus(6L, BasketStatus.ACTIVE)).thenReturn(Optional.of(basket));
        when(basketItemRepository.findByBasketIdAndBookId(60L, 12L)).thenReturn(Optional.of(existing));

        BasketResponse response = basketService.updateBasketItem(6L, 12L, new UpdateBasketItemRequest(5));

        assertThat(response.items().get(0).quantity()).isEqualTo(5);
    }

    @Test
    void shouldThrowNotFoundWhenUpdatingMissingBasketItem() {
        Basket basket = new Basket(7L, BasketStatus.ACTIVE);
        ReflectionTestUtils.setField(basket, "id", 70L);
        when(basketRepository.findByUserIdAndStatus(7L, BasketStatus.ACTIVE)).thenReturn(Optional.of(basket));
        when(basketItemRepository.findByBasketIdAndBookId(70L, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> basketService.updateBasketItem(7L, 99L, new UpdateBasketItemRequest(1)))
                .isInstanceOf(ResponseStatusException.class)
                .matches(ex -> ((ResponseStatusException) ex).getStatusCode() == HttpStatus.NOT_FOUND);
    }
}
