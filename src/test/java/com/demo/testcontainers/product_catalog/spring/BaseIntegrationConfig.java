package com.demo.testcontainers.product_catalog.spring;

import com.demo.testcontainers.product_catalog.domain.ProductRepository;
import io.restassured.RestAssured;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(
        // We have configured the RestAssured.port to the dynamic port of the application that is started by Spring
        // Boot.
        webEnvironment = RANDOM_PORT,
        // We have configured the spring.kafka.consumer.auto-offset-reset property to earliest to make sure that we read
        // all the messages from the beginning of the topic.
        properties = {"spring.kafka.consumer.auto-offset-reset=earliest"})
// We have reused the ContainersConfig class that we created to define all the required containers.
@Import(TestcontainersConfiguration.class)
public class BaseIntegrationConfig {
    static final Faker faker = new Faker();

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUpBase() {
        RestAssured.port = port;
    }

    @Autowired
    ProductRepository repository;

}
