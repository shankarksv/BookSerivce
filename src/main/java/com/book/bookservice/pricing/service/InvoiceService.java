package com.book.bookservice.pricing.service;

import com.book.bookservice.basket.entity.Basket;
import com.book.bookservice.basket.entity.BasketItem;
import com.book.bookservice.basket.entity.BasketStatus;
import com.book.bookservice.basket.repository.BasketRepository;
import com.book.bookservice.pricing.dto.BasketItemLine;
import com.book.bookservice.pricing.dto.PricingResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class InvoiceService {

    private final BasketRepository basketRepository;
    private final PricingService pricingService;

    public InvoiceService(BasketRepository basketRepository, PricingService pricingService) {
        this.basketRepository = basketRepository;
        this.pricingService = pricingService;
    }

    public PricingResponse calculate(Long userId) {
        Basket basket = basketRepository.findByUserIdAndStatus(userId, BasketStatus.ACTIVE).orElse(null);
        if (basket == null || basket.getItems().isEmpty()) {
            return new PricingResponse(
                    BigDecimal.ZERO.setScale(2),
                    List.of(),
                    new PricingResponse.Invoice(userId, List.of())
            );
        }

        List<BasketItemLine> lines = new ArrayList<>(basket.getItems().size());
        List<PricingResponse.InvoiceItem> invoiceItems = new ArrayList<>(basket.getItems().size());

        for (BasketItem item : basket.getItems()) {
            lines.add(new BasketItemLine(item.getBook().getId(), item.getQuantity()));
            invoiceItems.add(new PricingResponse.InvoiceItem(
                    item.getBook().getId(),
                    item.getBook().getTitle(),
                    item.getQuantity()
            ));
        }

        BigDecimal total = pricingService.calculateTotal(lines);
        return new PricingResponse(
                total,
                List.of(),
                new PricingResponse.Invoice(userId, invoiceItems)
        );
    }
}
