package me.windy.demo.order.core.application.config

import io.micronaut.context.annotation.Factory
import jakarta.inject.Singleton
import me.windy.demo.order.core.application.service.PlaceOrderService
import me.windy.demo.order.core.port.outgoing.DomainEventPublisher
import me.windy.demo.order.core.port.outgoing.OrderRepository
import me.windy.demo.order.core.port.outgoing.StockAvailabilityChecker

/**
 * Application configuration factory.
 * Wires up application services with their dependencies (Dependency Injection).
 * Uses constructor injection for better testability and immutability.
 */
@Factory
class ApplicationConfig {
    
    /**
     * Creates the PlaceOrderService with all required dependencies.
     * Micronaut will automatically inject the implementations of the ports.
     */
    @Singleton
    fun placeOrderService(
        repository: OrderRepository,
        stockChecker: StockAvailabilityChecker,
        eventPublisher: DomainEventPublisher
    ): PlaceOrderService {
        return PlaceOrderService(repository, stockChecker, eventPublisher)
    }
}

