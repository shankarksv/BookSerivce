package com.book.bookservice.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityRulesTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void adminShouldBeAbleToCreateBooks() throws Exception {
                mockMvc.perform(post("/api/books")
                        .with(httpBasic("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Domain-Driven Design\",\"author\":\"Eric Evans\",\"year\":2003,\"description\":\"DDD\",\"stockQuantity\":10}"))
                .andExpect(status().isCreated());
    }

    @Test
    void userShouldBeAbleToCreateBooksWhenOnlyAuthenticationIsEnabled() throws Exception {
                mockMvc.perform(post("/api/books")
                        .with(httpBasic("user", "user123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Refactoring\",\"author\":\"Martin Fowler\",\"year\":1999,\"description\":\"Refactoring\",\"stockQuantity\":10}"))
                .andExpect(status().isCreated());
    }
}
