package com.resilient.inventoryservice;

import com.resilient.inventoryservice.domain.Product;
import com.resilient.inventoryservice.repository.ProductRepository;
import com.resilient.inventoryservice.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
public class InventoryIntegrationTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
    }

    @Test
    @DisplayName("WAR GAME: Simulación de Flash Sale (Concurrencia + Retry)")
    void shouldHandleConcurrentUpdatesIdeally() throws ExecutionException, InterruptedException {

        // --- PREPARACIÓN ---
        // 1. Creamos el producto (Versión 0)

        Product ps5 = new Product("PS5-DIGITAL", "PlayStation 5", new BigDecimal("499.99"), 100);
        productRepository.save(ps5);

        System.out.println(">>> STOCK INICIAL: 100");

        // --- EL ATAQUE (2 Usuarios al mismo tiempo) ---
        CompletableFuture<Void> user1 = CompletableFuture.runAsync(() -> {
            System.out.println(">>> Hilo 1 Atacando....");
            inventoryService.decreaseStock("PS5-DIGITAL", 10);
        });

        CompletableFuture<Void> user2 = CompletableFuture.runAsync(() -> {
            System.out.println(">>> Hilo 2 Atacando....");
            inventoryService.decreaseStock("PS5-DIGITAL", 10);
        });

        // Esperamos a que terminen
        CompletableFuture.allOf(user1, user2).get();

        // --- VERIFICACIÓN ---
        Product result = productRepository.findAll().get(0);

        System.out.println(">>> STOCK FINAL: " + result.getStock());
        System.out.println(">>> VERSION FINAL: " + result.getVersion());

        // La magia: H2 también respeta @Version.
        // Si Resilience4j funciona, ambos hilos habrán tenido éxito.
        assertThat(result.getStock()).isEqualTo(80);
        assertThat(result.getVersion()).isEqualTo(2L);
    }
}
