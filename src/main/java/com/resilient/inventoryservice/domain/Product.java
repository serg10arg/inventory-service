package com.resilient.inventoryservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Getter @Setter
@NoArgsConstructor
public class Product extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String sku;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer stock;

    public Product(String sku, String name, BigDecimal price, Integer stock) {
        this.sku = sku;
        this.name = name;
        this.price = price;
        this.stock = stock;
    }

    // --- LÓGICA DE DOMINIO (DDD) ---

    // Este método es crucial. En lugar de hacer getStock() -> setStock() desde fuera,
    // le pedimos a la entidad que haga el trabajo. Esto ayuda a encapsular reglas.
    public void removeStock(int quantity) {
        if (this.stock < quantity) {
            throw new IllegalArgumentException("Stock insuficiente para el product: " + this.name);
        }

        this.stock -= quantity;
    }

}
