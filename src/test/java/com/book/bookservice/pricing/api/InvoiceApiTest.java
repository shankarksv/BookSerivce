package com.book.bookservice.pricing.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class InvoiceApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "USER")
    void shouldCalculateInvoiceWithTotalsAndDiscountBreakdown() throws Exception {
        mockMvc.perform(post("/api/pricing/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPrice").exists())
                .andExpect(jsonPath("$.discountBreakdown").isArray())
                .andExpect(jsonPath("$.invoice.userId").value(1));
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldReturnInvoiceItemsInPricingResponse() throws Exception {
        mockMvc.perform(post("/api/pricing/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoice.items").isArray())
                .andExpect(jsonPath("$.invoice.items[0].bookId").exists())
                .andExpect(jsonPath("$.invoice.items[0].quantity").exists());
    }
}
