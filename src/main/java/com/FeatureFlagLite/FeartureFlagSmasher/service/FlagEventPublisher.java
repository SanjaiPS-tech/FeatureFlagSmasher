package com.featureflaglite.featureflagsmasher.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Service for broadcasting real-time Server-Sent Events (SSE) to connected clients.
 * Powers zero-latency live updates across the console and client demo applications.
 */
@Component
public class FlagEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(FlagEventPublisher.class);

    // Default 30 minutes connection duration
    private static final long SSE_TIMEOUT_MS = 30 * 60 * 1000L;

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    /**
     * Registers a new client SSE subscriber and sends an immediate handshake event.
     */
    public SseEmitter registerEmitter() {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        emitters.add(emitter);

        emitter.onCompletion(() -> {
            log.debug("SSE client completed connection");
            emitters.remove(emitter);
        });

        emitter.onTimeout(() -> {
            log.debug("SSE client timed out");
            emitter.complete();
            emitters.remove(emitter);
        });

        emitter.onError(e -> {
            log.debug("SSE client connection error: {}", e.getMessage());
            emitters.remove(emitter);
        });

        // Send instant welcome / handshake packet
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data(Map.of(
                            "status", "CONNECTED",
                            "protocol", "SSE-v1",
                            "timestamp", System.currentTimeMillis(),
                            "message", "Live real-time stream established with FeatureFlagLite"
                    )));
        } catch (IOException e) {
            emitters.remove(emitter);
        }

        log.info("Active real-time SSE subscribers: {}", emitters.size());
        return emitter;
    }

    /**
     * Broadcasts a real-time flag mutation event to all active subscribers.
     */
    public void broadcastFlagChange(String action, String flagName, String environment, Object details) {
        if (emitters.isEmpty()) {
            return;
        }

        Map<String, Object> payload = Map.of(
                "action", action,
                "flagName", flagName != null ? flagName : "*",
                "environment", environment != null ? environment : "*",
                "details", details != null ? details : Map.of(),
                "timestamp", System.currentTimeMillis()
        );

        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("flag-update")
                        .data(payload));
            } catch (Exception ex) {
                deadEmitters.add(emitter);
            }
        }

        if (!deadEmitters.isEmpty()) {
            emitters.removeAll(deadEmitters);
            log.debug("Purged {} disconnected SSE emitters. Active remaining: {}",
                    deadEmitters.size(), emitters.size());
        }
    }

    /**
     * Returns the count of currently connected SSE clients.
     */
    public int getActiveSubscriberCount() {
        return emitters.size();
    }
}
