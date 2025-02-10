package com.demo.testcontainers.product_catalog.domain;

import com.demo.testcontainers.product_catalog.entity.Product;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;

import java.math.BigDecimal;
import java.util.Optional;

public interface ProductRepository extends MongoRepository<Product, String> {
    Optional<Product> findByCode(String code);

    @Query("{ 'code': ?0 }")
    @Update("{ '$set': { 'price': ?1 } }")
    void updateProductPrice(String productCode, BigDecimal price);
}
