package com.demo.testcontainers.product_catalog.api;

import com.demo.testcontainers.product_catalog.domain.ProductService;
import com.demo.testcontainers.product_catalog.entity.Product;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping
    public List<Product> getAllProducts() {
        return service.getAllProducts();
    }

    @PostMapping
    Product createProduct(@RequestBody Product product) {
        return service.createProduct(product);
    }
}

