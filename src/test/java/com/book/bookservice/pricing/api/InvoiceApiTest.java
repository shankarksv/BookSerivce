package com.book.bookservice.pricing.api;

import com.book.bookservice.basket.entity.Basket;
import com.book.bookservice.basket.entity.BasketItem;
import com.book.bookservice.basket.entity.BasketStatus;
import com.book.bookservice.basket.repository.BasketItemRepository;
import com.book.bookservice.basket.repository.BasketRepository;
import com.book.bookservice.book.entity.Book;
import com.book.bookservice.book.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class InvoiceApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BasketRepository basketRepository;

    @Autowired
    private BasketItemRepository basketItemRepository;

    @BeforeEach
    void setUpData() {
        basketItemRepository.deleteAll();
        basketRepository.deleteAll();
        bookRepository.deleteAll();

        Book book = bookRepository.save(new Book(
                "Refactoring",
                "Martin Fowler",
                1999,
                "Refactoring",
                new BigDecimal("50.00")
        ));

        Basket basket = new Basket(2L, BasketStatus.ACTIVE);
        BasketItem item = new BasketItem(basket, book, 2);
        basket.getItems().add(item);
        basketRepository.save(basket);
    }

    @Test
    void shouldCalculateInvoiceWithTotalsAndDiscountBreakdown() throws Exception {
        mockMvc.perform(post("/api/pricing/calculate")
                        .with(httpBasic("user", "user123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPrice").exists())
                .andExpect(jsonPath("$.discountBreakdown").isArray())
                .andExpect(jsonPath("$.invoice.userId").value(1));
    }

    @Test
    void shouldReturnInvoiceItemsInPricingResponse() throws Exception {
        mockMvc.perform(post("/api/pricing/calculate")
                        .with(httpBasic("user", "user123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoice.items").isArray())
                .andExpect(jsonPath("$.invoice.items[0].bookId").exists())
                .andExpect(jsonPath("$.invoice.items[0].quantity").exists());
    }
}
