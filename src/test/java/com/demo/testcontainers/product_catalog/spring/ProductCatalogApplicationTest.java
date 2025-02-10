package com.demo.testcontainers.product_catalog.spring;

import com.demo.testcontainers.product_catalog.entity.Product;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

class ProductCatalogApplicationTest extends BaseIntegrationConfig {
	@BeforeEach
	void setUp() {
		repository.deleteAll(); // Clean DB before each test

		Product product1 = new Product(null, "P101", "P101 name", new BigDecimal("20.00"));
		Product product2 = new Product(null, "P102", "P102 name", new BigDecimal("50.00"));

		repository.saveAll(List.of(product1, product2));
	}


	@Test
	void createProductSuccessfully() {
		String code = faker.commerce().promotionCode();
		String name = faker.commerce().productName();
		given()
				.contentType(ContentType.JSON)
				.body("""
                    {
                        "code": "%s",
                        "name": "%s",
                        "description": "Product %s description",
                        "price": 10.0
                    }
                  """.formatted(code, name, code))
				.when()
				.post("/products")
				.then()
				.statusCode(200) // Or use 201 if it returns "Created"
				.body("code", equalTo(code))
				.body("name", equalTo(name))
				.body("price", equalTo(10.0f)); // Use `f` to match JSON float type
	}

	@Test
	void retrieveProductSuccessfully() {
		// Retrieve Product and Validate
		given()
				.when()
				.get("/products")
				.then()
				.statusCode(200)
				.body("find { it.code == '%s' }.name".formatted("P101"), equalTo("P101 name"))
				.body("find { it.code == '%s' }.price".formatted("P102"), equalTo(50.00f));
	}

}
