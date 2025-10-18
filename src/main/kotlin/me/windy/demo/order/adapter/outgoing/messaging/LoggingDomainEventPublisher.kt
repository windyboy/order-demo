package me.windy.demo.order.adapter.outgoing.messaging

import io.micronaut.context.annotation.Requires
import jakarta.inject.Singleton
import me.windy.demo.order.core.domain.event.DomainEvent
import me.windy.demo.order.core.domain.event.OrderPlacedEvent
import me.windy.demo.order.core.domain.event.OrderStatusChangedEvent
import me.windy.demo.order.core.port.outgoing.DomainEventPublisher
import org.slf4j.LoggerFactory

/**
 * Logging implementation of DomainEventPublisher.
 * Logs events instead of publishing to a real message broker.
 * In production, this would publish to Kafka, RabbitMQ, AWS SNS, etc.
 * Active by default unless a real messaging broker URL is configured.
 */
@Singleton
@Requires(missingProperty = "messaging.broker.url")
class LoggingDomainEventPublisher : DomainEventPublisher {
    private val log = LoggerFactory.getLogger(LoggingDomainEventPublisher::class.java)

    // Track published events for testing purposes
    private val publishedEvents = mutableListOf<DomainEvent>()

    override fun publish(event: DomainEvent): Result<Unit> {
        return runCatching {
            publishedEvents.add(event)

            when (event) {
                is OrderPlacedEvent -> {
                    log.info(
                        "📦 [OrderPlaced] orderId={}, total={}, items={}, eventId={}",
                        event.orderId.value,
                        event.totalAmount.amount,
                        event.itemCount,
                        event.eventId,
                    )
                }
                is OrderStatusChangedEvent -> {
                    log.info(
                        "🔄 [OrderStatusChanged] orderId={}, from={}, to={}, eventId={}",
                        event.orderId.value,
                        event.previousStatus,
                        event.newStatus,
                        event.eventId,
                    )
                }
                else -> {
                    log.info(
                        "📨 [{}] eventId={}, timestamp={}",
                        event.eventType,
                        event.eventId,
                        event.occurredAt,
                    )
                }
            }

            // In production, this would be:
            // kafkaProducer.send(ProducerRecord("domain-events", event.eventId, serializeEvent(event)))
            // or
            // rabbitTemplate.convertAndSend("domain.events", event.eventType, event)
        }
    }

    /**
     * Gets all published events. Useful for testing.
     */
    fun getPublishedEvents(): List<DomainEvent> = publishedEvents.toList()

    /**
     * Clears published events. Useful for testing.
     */
    fun clearEvents() {
        publishedEvents.clear()
        log.debug("All published events cleared")
    }
}
