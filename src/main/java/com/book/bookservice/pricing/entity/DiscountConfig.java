package com.book.bookservice.pricing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "discount_config")
public class DiscountConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_size", nullable = false, unique = true)
    private int groupSize;

    @Column(name = "discount_percentage", nullable = false, precision = 5, scale = 4)
    private BigDecimal discountPercentage;

    @Column(nullable = false)
    private boolean active;

    protected DiscountConfig() {
    }

    public DiscountConfig(int groupSize, BigDecimal discountPercentage, boolean active) {
        this.groupSize = groupSize;
        this.discountPercentage = discountPercentage;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public int getGroupSize() {
        return groupSize;
    }

    public BigDecimal getDiscountPercentage() {
        return discountPercentage;
    }

    public boolean isActive() {
        return active;
    }
}
