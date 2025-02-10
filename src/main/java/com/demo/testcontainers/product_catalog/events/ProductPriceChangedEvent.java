package com.demo.testcontainers.product_catalog.events;

import java.math.BigDecimal;

public record ProductPriceChangedEvent(String productCode, BigDecimal price) {}
