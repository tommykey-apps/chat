package com.example.chat.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OpenApiSpecTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exportOpenApiSpec() throws Exception {
        var result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        Path outputDir = Path.of("build", "openapi");
        Files.createDirectories(outputDir);
        Files.writeString(outputDir.resolve("openapi.json"), json);
    }
}
