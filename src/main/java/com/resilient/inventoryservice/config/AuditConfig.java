package com.resilient.inventoryservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class AuditConfig {
    // Aquí podríamos configurar un AuditorAware para saber QUIÉN hizo el cambio
    // (ej. sacar el usuario de Spring Security), pero por ahora basta con esto
    // para las fechas.
}
