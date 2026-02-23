package com.hytale.fablescript.events;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Lightweight synchronous event bus for custom FableScript events.
 *
 * <p>Decouples subsystems (morality, quests, world) without requiring them to
 * hold direct references to one another. All listeners run on the calling thread.</p>
 *
 * <p>Usage:
 * <pre>
 *   // Register
 *   eventBus.subscribe(AlignmentChangeEvent.class, e -> applyVisualEffect(e));
 *
 *   // Publish
 *   eventBus.publish(new AlignmentChangeEvent(...));
 * </pre>
 * </p>
 */
public class FableEventBus {

    /** Map of event class → ordered list of consumers. */
    private final Map<Class<?>, List<Consumer<Object>>> listeners = new ConcurrentHashMap<>();

    /**
     * Registers a listener for a specific event type.
     *
     * @param <T>       the event type
     * @param eventType class token for the event
     * @param listener  consumer that handles the event
     */
    @SuppressWarnings("unchecked")
    public <T> void subscribe(Class<T> eventType, Consumer<T> listener) {
        listeners.computeIfAbsent(eventType, k -> new ArrayList<>())
                 .add(event -> listener.accept((T) event));
    }

    /**
     * Publishes an event to all registered listeners for that event's class.
     * Listeners are invoked in registration order.
     *
     * @param event the event instance to dispatch
     */
    public void publish(Object event) {
        List<Consumer<Object>> handlers = listeners.get(event.getClass());
        if (handlers != null) {
            for (Consumer<Object> handler : handlers) {
                handler.accept(event);
            }
        }
    }

    /**
     * Removes all listeners for a given event type.
     * Useful for testing and clean shutdown.
     *
     * @param eventType class token to clear
     */
    public void clearListeners(Class<?> eventType) {
        listeners.remove(eventType);
    }

    /** Removes all registered listeners. Called on plugin disable. */
    public void clearAll() {
        listeners.clear();
    }
}
