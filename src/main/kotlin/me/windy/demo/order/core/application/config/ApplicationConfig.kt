package me.windy.demo.order.core.application.config

import io.micronaut.context.annotation.Factory
import jakarta.inject.Singleton
import me.windy.demo.order.core.application.usecase.OrderPlacementHandler
import me.windy.demo.order.core.port.incoming.PlaceOrderUseCase
import me.windy.demo.order.core.port.outgoing.DomainEventPublisher
import me.windy.demo.order.core.port.outgoing.OrderRepository
import me.windy.demo.order.core.port.outgoing.StockAvailabilityChecker

/**
 * Application configuration factory.
 *
 * For the demo we keep the wiring explicit so workshop participants can see
 * exactly how the hexagon is assembled: the use case gets handed concrete
 * adapters implementing the outbound ports.
 */
@Factory
class ApplicationConfig {
    @Singleton
    fun placeOrderUseCase(
        repository: OrderRepository,
        stockChecker: StockAvailabilityChecker,
        eventPublisher: DomainEventPublisher,
    ): PlaceOrderUseCase {
        return OrderPlacementHandler(repository, stockChecker, eventPublisher)
    }
}
