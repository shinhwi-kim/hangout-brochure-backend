package com.hangoutbrochure.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Methods are ordered on purpose: Hibernate resolves the JDBC type of a query parameter from the
 * first bound value it sees and keeps it in the query plan cache, so a keyword-bearing request
 * executed first can hide a keyword-less request that would otherwise fail on its own.
 * The keyword-less cases therefore have to run first.
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class HangoutCategoryApiTests {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @Order(1)
    void returnsCategoriesWhenKeywordIsAbsent() throws Exception {
        mockMvc.perform(get("/api/hangout-categories").param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isNotEmpty());
    }

    @Test
    @Order(3)
    void returnsCategoriesWhenKeywordIsBlank() throws Exception {
        mockMvc.perform(get("/api/hangout-categories").param("size", "100").param("keyword", " "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isNotEmpty());
    }

    @Test
    @Order(4)
    void filtersCategoriesByKeywordIgnoringCase() throws Exception {
        String body = mockMvc.perform(get("/api/hangout-categories").param("size", "100").param("keyword", "MAR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<String> codes = JsonPath.read(body, "$.items[*].code");
        List<String> names = JsonPath.read(body, "$.items[*].name");
        for (int i = 0; i < codes.size(); i++) {
            assertThat(codes.get(i).toLowerCase() + names.get(i).toLowerCase()).contains("mar");
        }
    }

    @Test
    @Order(2)
    void paginatesWithCursorWhenKeywordIsAbsent() throws Exception {
        String firstPage = mockMvc.perform(get("/api/hangout-categories").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        int nextCursor = JsonPath.read(firstPage, "$.nextCursor");

        String secondPage = mockMvc.perform(get("/api/hangout-categories")
                        .param("size", "2")
                        .param("cursor", String.valueOf(nextCursor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<Integer> secondPageIds = JsonPath.read(secondPage, "$.items[*].id");
        assertThat(secondPageIds).allMatch(id -> id > nextCursor);
        assertThat(secondPageIds).isSorted();
    }

    @Test
    @Order(5)
    void paginatesWithCursorWhenKeywordIsPresent() throws Exception {
        String firstPage = mockMvc.perform(get("/api/hangout-categories").param("size", "1").param("keyword", "a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        int nextCursor = JsonPath.read(firstPage, "$.nextCursor");

        String secondPage = mockMvc.perform(get("/api/hangout-categories")
                        .param("size", "1")
                        .param("keyword", "a")
                        .param("cursor", String.valueOf(nextCursor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<Integer> secondPageIds = JsonPath.read(secondPage, "$.items[*].id");
        assertThat(secondPageIds).allMatch(id -> id > nextCursor);
    }
}
