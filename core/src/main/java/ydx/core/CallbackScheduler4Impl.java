package ydx.core;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Реализация на ScheduledExecutorService.
 * Вся логика очередей, потоков и таймингов — внутри стандартной библиотеки.
 */
public class CallbackScheduler4Impl implements CallbackScheduler {

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "callback4-scheduler-worker");
        t.setDaemon(true);
        return t;
    });

    @Override
    public void schedule(Runnable callback, Instant after) {
        if (executor.isShutdown()) {
            throw new IllegalStateException("Планировщик остановлен");
        }

        long delayMillis = Duration.between(Instant.now(), after).toMillis();
        executor.schedule(callback, Math.max(0, delayMillis), TimeUnit.MILLISECONDS);
    }

    @Override
    public void close() {
        executor.shutdown();
    }
}
