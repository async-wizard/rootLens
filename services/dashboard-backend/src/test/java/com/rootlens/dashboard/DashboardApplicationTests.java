package com.rootlens.dashboard;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=6399",
        "spring.cache.type=none",
        "spring.kafka.bootstrap-servers=localhost:9999",
        "spring.kafka.listener.auto-startup=false"
})
class DashboardApplicationTests {

    @Test
    void contextLoads() {
    }
}
