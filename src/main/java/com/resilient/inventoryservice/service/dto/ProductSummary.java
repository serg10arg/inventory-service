package com.resilient.inventoryservice.service.dto;

import java.math.BigDecimal;

// Proyección ligera: Solo lo que el frontend necesita ver en una lista.
public record ProductSummary(
        String sku,
        String name,
        BigDecimal price,
        boolean inStock) {
}
