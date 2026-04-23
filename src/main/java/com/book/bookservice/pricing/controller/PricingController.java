package com.book.bookservice.pricing.controller;

import com.book.bookservice.pricing.dto.PricingRequest;
import com.book.bookservice.pricing.dto.PricingResponse;
import com.book.bookservice.pricing.service.InvoiceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pricing")
public class PricingController {

    private final InvoiceService invoiceService;

    public PricingController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @PostMapping("/calculate")
    public PricingResponse calculate(@Valid @RequestBody PricingRequest request) {
        return invoiceService.calculate(request.userId());
    }
}
