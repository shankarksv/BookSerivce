package com.book.bookservice.basket.service;

import com.book.bookservice.basket.dto.AddBasketItemRequest;
import com.book.bookservice.basket.dto.BasketItemResponse;
import com.book.bookservice.basket.dto.BasketResponse;
import com.book.bookservice.basket.dto.UpdateBasketItemRequest;
import com.book.bookservice.basket.entity.Basket;
import com.book.bookservice.basket.entity.BasketItem;
import com.book.bookservice.basket.entity.BasketStatus;
import com.book.bookservice.basket.repository.BasketItemRepository;
import com.book.bookservice.basket.repository.BasketRepository;
import com.book.bookservice.book.entity.Book;
import com.book.bookservice.book.repository.BookRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class BasketService {

    private final BasketRepository basketRepository;
    private final BasketItemRepository basketItemRepository;
    private final BookRepository bookRepository;

    public BasketService(
            BasketRepository basketRepository,
            BasketItemRepository basketItemRepository,
            BookRepository bookRepository
    ) {
        this.basketRepository = basketRepository;
        this.basketItemRepository = basketItemRepository;
        this.bookRepository = bookRepository;
    }

    public BasketResponse addBookToBasket(Long userId, AddBasketItemRequest request) {
        Basket basket = getOrCreateActiveBasket(userId);
        Book book = getBookById(request.bookId());

        BasketItem item = basketItemRepository.findByBasketIdAndBookId(basket.getId(), request.bookId())
                .orElseGet(() -> {
                    BasketItem newItem = new BasketItem(basket, book, 0);
                    basket.getItems().add(newItem);
                    return newItem;
                });

        int targetQuantity = item.getQuantity() + request.quantity();
        validateStockAvailability(book, targetQuantity);
        item.setQuantity(targetQuantity);
        basketRepository.save(basket);
        return toResponse(basket);
    }

    @Transactional(readOnly = true)
    public BasketResponse getBasket(Long userId) {
        return basketRepository.findByUserIdAndStatus(userId, BasketStatus.ACTIVE)
                .map(this::toResponse)
                .orElseGet(() -> new BasketResponse(userId, BasketStatus.ACTIVE.name(), List.of()));
    }

    public BasketResponse updateBasketItem(Long userId, Long bookId, UpdateBasketItemRequest request) {
        Basket basket = getOrCreateActiveBasket(userId);
        BasketItem item = basketItemRepository.findByBasketIdAndBookId(basket.getId(), bookId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "basket item not found"));
        validateStockAvailability(item.getBook(), request.quantity());
        item.setQuantity(request.quantity());
        basketRepository.save(basket);
        return toResponse(basket);
    }

    public void removeBasketItem(Long userId, Long bookId) {
        Basket basket = getOrCreateActiveBasket(userId);
        BasketItem item = basketItemRepository.findByBasketIdAndBookId(basket.getId(), bookId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "basket item not found"));
        basket.getItems().remove(item);
        basketRepository.save(basket);
    }

    public void clearBasket(Long userId) {
        Basket basket = getOrCreateActiveBasket(userId);
        basket.getItems().clear();
        basket.setStatus(BasketStatus.CLEARED);
        basketRepository.save(basket);
    }

    private Basket getOrCreateActiveBasket(Long userId) {
        return basketRepository.findByUserIdAndStatus(userId, BasketStatus.ACTIVE)
                .orElseGet(() -> basketRepository.save(new Basket(userId, BasketStatus.ACTIVE)));
    }

    private Book getBookById(Long bookId) {
        return bookRepository.findById(bookId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "book not found"));
    }

    private void validateStockAvailability(Book book, int targetQuantity) {
        if (targetQuantity > book.getStockQuantity()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "requested quantity exceeds available stock for bookId=" + book.getId());
        }
    }

    private BasketResponse toResponse(Basket basket) {
        List<BasketItemResponse> items = new ArrayList<>(basket.getItems().size());
        for (BasketItem item : basket.getItems()) {
            items.add(new BasketItemResponse(
                    item.getBook().getId(),
                    item.getBook().getTitle(),
                    item.getQuantity()
            ));
        }
        return new BasketResponse(basket.getUserId(), basket.getStatus().name(), items);
    }
}
