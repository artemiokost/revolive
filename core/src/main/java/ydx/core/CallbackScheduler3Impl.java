package ydx.core;

import java.time.Duration;
import java.time.Instant;
import java.util.PriorityQueue;

/**
 * Реализация на synchronized/wait/notify вместо ReentrantLock/Condition.
 *
 * Проще, меньше кода — один объект-монитор (lock) вместо Lock + Condition.
 * Минус: нет awaitNanos с возвратом оставшегося времени, используем wait(millis).
 */
public class CallbackScheduler3Impl implements CallbackScheduler {

    private record ScheduledTask(Runnable callback, Instant after) implements Comparable<ScheduledTask> {
        @Override
        public int compareTo(ScheduledTask other) {
            return this.after.compareTo(other.after);
        }
    }

    // Обычная PriorityQueue — доступ только под synchronized, потокобезопасность не нужна.
    private final PriorityQueue<ScheduledTask> queue = new PriorityQueue<>();

    // Объект-монитор для synchronized/wait/notify.
    private final Object lock = new Object();

    private volatile boolean stopped = false;
    private final Thread worker;

    public CallbackScheduler3Impl() {
        worker = new Thread(this::run, "callback3-scheduler-worker");
        worker.setDaemon(true);
        worker.start();
    }

    @Override
    public void schedule(Runnable callback, Instant after) {
        if (stopped) {
            throw new IllegalStateException("Планировщик остановлен");
        }

        synchronized (lock) {
            queue.add(new ScheduledTask(callback, after));
            lock.notify(); // Будим рабочий поток
        }
    }

    @Override
    public void close() {
        stopped = true;
        synchronized (lock) {
            lock.notify();
        }
    }

    /**
     * Достаёт из очереди задачу, готовую к выполнению. Если задач нет или время
     * ближайшей ещё не пришло — ждёт и возвращает null.
     */
    private ScheduledTask pollReady() throws InterruptedException {
        synchronized (lock) {
            if (queue.isEmpty()) {
                lock.wait();
                return null;
            }

            long millisToWait = Duration.between(Instant.now(), queue.peek().after()).toMillis();

            if (millisToWait > 0) {
                lock.wait(millisToWait);
                return null;
            }

            return queue.poll();
        }
    }

    private void run() {
        while (!stopped) {
            try {
                ScheduledTask task = pollReady();
                if (task != null) {
                    task.callback().run();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
