package com.book.bookservice.pricing.repository;

import com.book.bookservice.pricing.entity.DiscountConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiscountConfigRepository extends JpaRepository<DiscountConfig, Long> {

    List<DiscountConfig> findByActiveTrueOrderByGroupSizeAsc();
}
