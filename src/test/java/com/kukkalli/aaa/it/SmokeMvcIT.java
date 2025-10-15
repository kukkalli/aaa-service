package com.kukkalli.aaa.it;

import com.kukkalli.aaa.testsupport.MariaDbContainerSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SmokeMvcIT extends MariaDbContainerSupport {
    @Autowired
    MockMvc mvc;

    @Test
    void swagger_is_public() throws Exception {
        mvc.perform(get("/swagger-ui.html")).andExpect(status().isOk());
    }
}