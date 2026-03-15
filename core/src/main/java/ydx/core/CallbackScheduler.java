package ydx.core;

import java.time.Instant;

/**
 * Планировщик отложенных коллбэков.
 * Позволяет запланировать выполнение Runnable после указанного момента времени.
 */
public interface CallbackScheduler extends AutoCloseable {

    /**
     * Планирует выполнение коллбэка после указанного момента времени.
     *
     * @param callback задача для выполнения
     * @param after    момент времени, после которого задача должна быть выполнена
     */
    void schedule(Runnable callback, Instant after);
}
