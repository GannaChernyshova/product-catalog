package com.demo.testcontainers.product_catalog.integration;

import com.demo.testcontainers.product_catalog.domain.ProductRepository;
import com.demo.testcontainers.product_catalog.entity.Product;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.Random;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Testcontainers
@SpringBootTest
class ProductRepositoryTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:latest");

    @DynamicPropertySource
    static void setUp(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private ProductRepository repository;

    private static final Faker faker = new Faker();
    private static final Random random = new Random();


    @BeforeEach
    void setUpBeforeEach() {
        repository.deleteAll();
    }

    @Test
    void shouldSaveAndFindProduct() {
        //Prepare product data
        String code = faker.commerce().promotionCode();
        String name = faker.commerce().productName();
        BigDecimal price = BigDecimal.valueOf(random.nextDouble(10, 500));

        //Save product to DB
        repository.save(new Product(null, code, name, price));

        //Verify that product is saved in the DB with all expected fields
        assertThat(repository.findByCode(code)).isPresent();
        assertThat(repository.findByCode(code).get().getName()).isEqualTo(name);
        assertThat(repository.findByCode(code).get().getPrice()).isEqualTo(price);
    }

    @Test
    void shouldUpdateProductPrice() {
        //Prepare product data and save product to DB
        String code = faker.commerce().promotionCode();
        String name = faker.commerce().productName();
        BigDecimal price = BigDecimal.valueOf(random.nextDouble(10, 500));
        repository.save(new Product(null, code, name, price));

        //Update Product price
        repository.updateProductPrice(code, new BigDecimal("10.55"));

        //Verify that price is updated properly
        assertThat(repository.findByCode(code).get().getPrice()).isEqualTo(new BigDecimal("10.55"));
    }
}
