package com.demo.testcontainers.product_catalog.integration;

import com.demo.testcontainers.product_catalog.domain.ProductRepository;
import com.demo.testcontainers.product_catalog.entity.Product;
import com.demo.testcontainers.product_catalog.events.ProductPriceChangedEvent;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;
import java.util.Random;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;

@Testcontainers
@SpringBootTest
@TestPropertySource(
        properties = {
                // Make sure that we read all the messages from the beginning of the topic.
                "spring.kafka.consumer.auto-offset-reset=earliest"
        }
)
class ProductEventsTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:latest");
    @Container
    static final KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("apache/kafka-native:3.8.0")
    );

    @Autowired
    private ProductRepository repository;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    private static final Faker faker = new Faker();
    private static final Random random = new Random();

    @DynamicPropertySource
    static void setUp(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @BeforeEach
    void setUpBeforeEach() {
        repository.deleteAll();
    }

    @Test
    void shouldHandleProductPriceChangedEvent() {
        String code = faker.commerce().promotionCode();
        String name = faker.commerce().productName();
        BigDecimal price = BigDecimal.valueOf(random.nextDouble(10, 500));
        BigDecimal newPrice = new BigDecimal("14.50");
        repository.save(new Product(null, code, name, price));


        ProductPriceChangedEvent event = new ProductPriceChangedEvent(code, newPrice);

        kafkaTemplate.send("product-price-changes", event.productCode(), event);

        await()
                .pollInterval(Duration.ofSeconds(3))
                .atMost(10, SECONDS)
                .untilAsserted(() -> {
                    Optional<Product> optionalProduct = repository.findByCode(
                            code
                    );
                    assertThat(optionalProduct).isPresent();
                    assertThat(optionalProduct.get().getCode()).isEqualTo(code);
                    assertThat(optionalProduct.get().getPrice())
                            .isEqualTo(newPrice);
                });
    }

    @Test
    void shouldHandleMultiplePriceUpdates() {
        String code = faker.commerce().promotionCode();
        String name = faker.commerce().productName();
        BigDecimal initialPrice = new BigDecimal("30.00");

        repository.save(new Product(null, code, name, initialPrice));

        BigDecimal firstPriceUpdate = new BigDecimal("40.00");
        BigDecimal secondPriceUpdate = new BigDecimal("50.00");

        kafkaTemplate.send("product-price-changes", code, new ProductPriceChangedEvent(code, firstPriceUpdate));
        kafkaTemplate.send("product-price-changes", code, new ProductPriceChangedEvent(code, secondPriceUpdate));

        await()
                .pollInterval(Duration.ofSeconds(3))
                .atMost(10, SECONDS)
                .untilAsserted(() -> {
                    Optional<Product> optionalProduct = repository.findByCode(code);
                    assertThat(optionalProduct).isPresent();
                    assertThat(optionalProduct.get().getPrice()).isEqualTo(secondPriceUpdate); // Last event should win
                });
    }

}
