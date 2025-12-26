package com.resilient.inventoryservice.repository;

import com.resilient.inventoryservice.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;


@Repository
public interface ProductRepository extends
        JpaRepository<Product, Long>,
        QuerydslPredicateExecutor<Product> { // <--- Habilita la magia de QueryDSL

    // Spring Data JPA estándar para búsquedas simples
    boolean existsBySku(String sku);

}
