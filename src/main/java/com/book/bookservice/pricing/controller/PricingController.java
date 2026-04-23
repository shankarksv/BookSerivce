package com.book.bookservice.pricing.controller;

import com.book.bookservice.pricing.dto.PricingRequest;
import com.book.bookservice.pricing.dto.PricingResponse;
import com.book.bookservice.pricing.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pricing")
@Tag(name = "Pricing APIs", description = "Pricing and invoice endpoints")
public class PricingController {

    private final InvoiceService invoiceService;

    public PricingController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @PostMapping("/calculate")
    @Operation(summary = "Calculate basket invoice")
    public PricingResponse calculate(@Valid @RequestBody PricingRequest request) {
        return invoiceService.calculate(request.userId());
    }
}
