package com.book.bookservice.basket.controller;

import com.book.bookservice.basket.dto.AddBasketItemRequest;
import com.book.bookservice.basket.dto.BasketResponse;
import com.book.bookservice.basket.dto.UpdateBasketItemRequest;
import com.book.bookservice.basket.service.BasketService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/baskets")
public class BasketController {

    private final BasketService basketService;

    public BasketController(BasketService basketService) {
        this.basketService = basketService;
    }

    @PostMapping("/{userId}/add")
    public BasketResponse addBookToBasket(@PathVariable Long userId, @Valid @RequestBody AddBasketItemRequest request) {
        return basketService.addBookToBasket(userId, request);
    }

    @GetMapping("/{userId}")
    public BasketResponse getBasket(@PathVariable Long userId) {
        return basketService.getBasket(userId);
    }

    @PutMapping("/{userId}/items/{bookId}")
    public BasketResponse updateBasketItem(
            @PathVariable Long userId,
            @PathVariable Long bookId,
            @Valid @RequestBody UpdateBasketItemRequest request
    ) {
        return basketService.updateBasketItem(userId, bookId, request);
    }

    @DeleteMapping("/{userId}/items/{bookId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeBasketItem(@PathVariable Long userId, @PathVariable Long bookId) {
        basketService.removeBasketItem(userId, bookId);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearBasket(@PathVariable Long userId) {
        basketService.clearBasket(userId);
    }
}
