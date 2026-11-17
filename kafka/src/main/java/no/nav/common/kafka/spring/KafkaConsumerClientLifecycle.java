package no.nav.common.kafka.spring;

import no.nav.common.kafka.consumer.KafkaConsumerClient;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.SmartLifecycle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Explicit, exclusive lifecycle owner for a group of consumers. Register as a bean;
 * do not also start or stop the supplied clients elsewhere.
 *
 * <p>Starts during context refresh, in list order, and stops in reverse order.
 * The default phase starts after lower-phase infrastructure and stops before it.
 * Use explicit bean dependencies for infrastructure in the same phase.
 */
public final class KafkaConsumerClientLifecycle implements SmartLifecycle, DisposableBean {

    private final List<KafkaConsumerClient> clients;
    private final List<KafkaConsumerClient> startedClients = new ArrayList<>();
    private final int phase;
    private volatile boolean running;

    public KafkaConsumerClientLifecycle(List<KafkaConsumerClient> clients) {
        this(clients, SmartLifecycle.DEFAULT_PHASE);
    }

    public KafkaConsumerClientLifecycle(List<KafkaConsumerClient> clients, int phase) {
        this.clients = List.copyOf(clients);
        Set<KafkaConsumerClient> uniqueClients = Collections.newSetFromMap(new IdentityHashMap<>());
        for (KafkaConsumerClient client : this.clients) {
            if (!uniqueClients.add(client)) {
                throw new IllegalArgumentException("A consumer must only occur once in a lifecycle group");
            }
        }
        this.phase = phase;
    }

    @Override
    public synchronized void start() {
        if (running) {
            return;
        }
        if (!startedClients.isEmpty()) {
            throw new IllegalStateException("Previous consumer shutdown is incomplete; call stop() before restarting");
        }

        try {
            for (KafkaConsumerClient client : clients) {
                // Include a failing start: it may have allocated resources before throwing.
                startedClients.add(client);
                client.start();
            }
            running = true;
        } catch (RuntimeException | Error failure) {
            try {
                stop();
            } catch (RuntimeException | Error cleanupFailure) {
                if (cleanupFailure != failure) {
                    failure.addSuppressed(cleanupFailure);
                }
            }
            throw failure;
        }
    }

    @Override
    public synchronized void stop() {
        running = false;
        Throwable failure = null;
        for (int i = startedClients.size() - 1; i >= 0; i--) {
            try {
                startedClients.get(i).stop();
                startedClients.remove(i);
            } catch (RuntimeException | Error stopFailure) {
                if (failure == null) {
                    failure = stopFailure;
                } else if (failure != stopFailure) {
                    failure.addSuppressed(stopFailure);
                }
            }
        }
        if (failure instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        if (failure instanceof Error error) {
            throw error;
        }
    }

    /**
     * The callback acknowledges completed shutdown, not merely a stop request.
     * If shutdown fails, propagate the error without signalling completion.
     */
    @Override
    public synchronized void stop(Runnable callback) {
        Objects.requireNonNull(callback, "callback");
        stop();
        callback.run();
    }

    /**
     * True once all start calls have returned, including clients paused by a toggle.
     * Does not imply broker connectivity, partition assignment, or healthy processing.
     */
    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return phase;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    /**
     * Also covers refresh cancellation, where Spring may destroy singletons without
     * invoking the lifecycle processor's normal shutdown sequence.
     */
    @Override
    public void destroy() {
        stop();
    }
}
