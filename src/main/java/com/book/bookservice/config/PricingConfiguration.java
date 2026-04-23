package com.book.bookservice.config;

import com.book.bookservice.pricing.service.DiscountPolicyProvider;
import com.book.bookservice.pricing.service.PricingService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PricingConfiguration {

    @Bean
    public PricingService pricingService(DiscountPolicyProvider discountPolicyProvider) {
        return new PricingService(discountPolicyProvider);
    }
}
