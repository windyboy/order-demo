package me.windy.demo.order.core.fakes

import me.windy.demo.order.core.domain.event.DomainEvent
import me.windy.demo.order.core.port.outgoing.DomainEventPublisher

/**
 * Fake implementation for testing.
 * Tracks published events for verification.
 */
class FakeDomainEventPublisher(
    private val shouldFail: Boolean = false
) : DomainEventPublisher {
    
    val publishedEvents = mutableListOf<DomainEvent>()
    
    override fun publish(event: DomainEvent): Result<Unit> {
        return if (shouldFail) {
            Result.failure(RuntimeException("Event publishing failed"))
        } else {
            publishedEvents.add(event)
            Result.success(Unit)
        }
    }
}

