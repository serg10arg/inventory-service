package com.resilient.inventoryservice.service;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.resilient.inventoryservice.domain.Product;
import com.resilient.inventoryservice.domain.QProduct;
import com.resilient.inventoryservice.repository.ProductRepository;
import com.resilient.inventoryservice.service.dto.ProductSummary;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final ProductRepository productRepository;
    private final JPAQueryFactory queryFactory;

    /**
     * LECTURA EFICIENTE
     * No usamos productRepositroy.findAll() porque traeria entidades pesadas.
     * Usasmos QueryDSL para proyectar directo al Record ProductSummary
     */
    @Transactional(readOnly = true)
    @CircuitBreaker(name = "inventoryDatabase", fallbackMethod = "fallbackSearch")
    public List<ProductSummary> searchProducts(String searchTerm) {
        QProduct p = QProduct.product;

        // [TOP TIER] El SQL generado SELECT solo pedirá sku, name, price y stock.
        // No traerá 'version', 'audit', ni relaciones Lazy.
        return queryFactory
                .select(Projections.constructor(ProductSummary.class,
                        p.sku,
                        p.name,
                        p.price,
                        p.stock.gt(0)
                ))
                .from(p)
                .where(p.name.containsIgnoreCase(searchTerm))
                .fetch();
    }

    @Transactional
    @Retry(name = "inventoryRetry") // Si falla por OptimisticLock, reintenta 3 veces
    public void decreaseStock(String sku, int quantity) {
        log.info("Intentando reducir stock para SKU: {}", sku);

        Product product = productRepository.findOne(QProduct.product.sku.eq(sku))
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));

        // [TOP TIER] La lógica de negocio vive en la Entidad (DDD), no aquí.
        product.removeStock(quantity);

        // Al salir del método, Hibernate compara la @Version.
        // Si cambió, lanza OptimisticLockingFailureException y Resilience4j reintenta.
        productRepository.save(product);
    }

    // --- FALLBACKS ---

    // Si la BD cae (Circuit Breaker abierto), devolvemos una lista vacía o caché
    // en lugar de un error 500 al usuario.
    public List<ProductSummary> fallbackSearch(String term, Throwable t) {
        log.error("Base de datos no disponible. Retornando respuesta degradada.", t);
        return List.of(); // O retornar datos de una caché Redis si existiera
    }

}
