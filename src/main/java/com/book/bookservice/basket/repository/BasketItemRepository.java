package com.book.bookservice.basket.repository;

import com.book.bookservice.basket.entity.BasketItem;
import com.book.bookservice.basket.entity.BasketStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BasketItemRepository extends JpaRepository<BasketItem, Long> {

    Optional<BasketItem> findByBasketIdAndBookId(Long basketId, Long bookId);

    boolean existsByBookIdAndBasketStatus(Long bookId, BasketStatus status);
}
