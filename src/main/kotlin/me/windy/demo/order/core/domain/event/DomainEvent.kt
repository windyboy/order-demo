package me.windy.demo.order.core.domain.event

import java.time.Instant
import java.util.UUID

/**
 * Base interface for all domain events.
 * Domain events represent something that happened in the domain that domain experts care about.
 */
interface DomainEvent {
    /**
     * Unique identifier for this event instance.
     */
    val eventId: String
    
    /**
     * When this event occurred.
     */
    val occurredAt: Instant
    
    /**
     * Type of the event (for routing/filtering).
     */
    val eventType: String
}

/**
 * Base implementation providing common event metadata.
 */
abstract class BaseDomainEvent(
    override val eventId: String = UUID.randomUUID().toString(),
    override val occurredAt: Instant = Instant.now()
) : DomainEvent

