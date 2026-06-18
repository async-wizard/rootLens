package com.rootlens.ingestion;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=localhost:9999",
        "spring.kafka.admin.fail-fast=false"
})
class IngestionServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
