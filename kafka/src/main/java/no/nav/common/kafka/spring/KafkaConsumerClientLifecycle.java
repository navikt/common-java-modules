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
 *
 * <p>STOPPED -> STARTING -> RUNNING -> STOPPING -> STOPPED.
 * A failed start rolls back through STOPPING. Failed shutdown stays STOPPING and
 * retains only clients whose cleanup must be retried.
 */
public final class KafkaConsumerClientLifecycle implements SmartLifecycle, DisposableBean {

    private final List<KafkaConsumerClient> clients;
    private final List<KafkaConsumerClient> clientsAwaitingStop = new ArrayList<>();
    private final int phase;

    private enum State {
        STOPPED, STARTING, RUNNING, STOPPING
    }

    private volatile State state = State.STOPPED;

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
        if (state == State.RUNNING) {
            return;
        }
        if (state != State.STOPPED) {
            throw new IllegalStateException("Previous consumer shutdown is incomplete; call stop() before restarting");
        }

        state = State.STARTING;
        try {
            for (KafkaConsumerClient client : clients) {
                // Include a failing start: it may have allocated resources before throwing.
                clientsAwaitingStop.add(client);
                client.start();
            }
            state = State.RUNNING;
        } catch (RuntimeException | Error failure) {
            rollbackStartup(failure);
            throw failure;
        }
    }

    @Override
    public synchronized void stop() {
        if (state == State.STOPPED) {
            return;
        }
        state = State.STOPPING;
        stopClientsInReverseOrder();
        state = State.STOPPED;
    }

    private void rollbackStartup(Throwable startupFailure) {
        try {
            stop();
        } catch (RuntimeException | Error cleanupFailure) {
            if (cleanupFailure != startupFailure) {
                startupFailure.addSuppressed(cleanupFailure);
            }
        }
    }

    private void stopClientsInReverseOrder() {
        Throwable failure = null;
        for (int i = clientsAwaitingStop.size() - 1; i >= 0; i--) {
            try {
                clientsAwaitingStop.get(i).stop();
                clientsAwaitingStop.remove(i);
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
        return state == State.RUNNING;
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
