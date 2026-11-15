package com.ieum.dodam.user.api.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "dodam.demo-showcase-only=true")
@AutoConfigureMockMvc
@Transactional
class ShowcaseLoginIntegrationTest {
    @Autowired private MockMvc mvc;

    @Test
    void allowsTheTwoNamedShowcaseAccountsWithoutACode() throws Exception {
        mvc.perform(post("/api/demo/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account\":\"MINJI\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("김민지"));
        mvc.perform(post("/api/demo/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account\":\"JIHUN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("박지훈"));
    }

    @Test
    void rejectsOtherDemoAccountsInShowcaseMode() throws Exception {
        for (String body : new String[] {"{\"account\":\"SOONJA\"}", "{\"role\":\"CAREGIVER\"}"}) {
            mvc.perform(post("/api/demo/login").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
    }
}
