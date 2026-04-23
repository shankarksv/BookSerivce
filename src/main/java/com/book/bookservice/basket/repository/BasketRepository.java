package com.book.bookservice.basket.repository;

import com.book.bookservice.basket.entity.Basket;
import com.book.bookservice.basket.entity.BasketStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BasketRepository extends JpaRepository<Basket, Long> {

    Optional<Basket> findByUserIdAndStatus(Long userId, BasketStatus status);
}
