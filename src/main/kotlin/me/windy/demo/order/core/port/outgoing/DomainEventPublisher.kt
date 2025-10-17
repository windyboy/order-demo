package me.windy.demo.order.core.port.outgoing

import me.windy.demo.order.core.domain.event.DomainEvent

/**
 * Port for publishing domain events.
 * Allows the domain to communicate changes to the outside world
 * without depending on specific messaging infrastructure.
 */
interface DomainEventPublisher {
    /**
     * Publishes a single domain event.
     * @param event The domain event to publish
     * @return Result indicating success or failure
     */
    fun publish(event: DomainEvent): Result<Unit>
    
    /**
     * Publishes multiple domain events.
     * @param events The domain events to publish
     * @return Result indicating success or failure
     */
    fun publishAll(events: List<DomainEvent>): Result<Unit> {
        return runCatching {
            events.forEach { event ->
                publish(event).getOrThrow()
            }
        }
    }
}

