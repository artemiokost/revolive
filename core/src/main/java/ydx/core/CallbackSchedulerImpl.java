package ydx.core;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Реализация планировщика отложенных коллбэков.
 *
 * Основная идея: рабочий поток спит до момента выполнения ближайшей задачи,
 * а не опрашивает очередь каждую секунду (busy-wait / polling).
 *
 * Ключевые решения:
 * - PriorityBlockingQueue — задачи автоматически упорядочены по времени выполнения,
 *   так что ближайшая задача всегда на вершине очереди.
 * - ReentrantLock + Condition — позволяют рабочему потоку спать ровно столько,
 *   сколько нужно (awaitNanos), и просыпаться при добавлении новой задачи.
 * - volatile boolean stopped — флаг остановки, видимый всем потокам без синхронизации.
 */
public class CallbackSchedulerImpl implements CallbackScheduler {

    /**
     * Обёртка над задачей: хранит коллбэк и время, после которого его нужно выполнить.
     * Comparable нужен для PriorityBlockingQueue — задачи сортируются по времени.
     */
    private record ScheduledTask(Runnable callback, Instant after) implements Comparable<ScheduledTask> {
        @Override
        public int compareTo(ScheduledTask other) {
            return this.after.compareTo(other.after);
        }
    }

    // Очередь с приоритетом: ближайшая по времени задача всегда первая.
    private final PriorityBlockingQueue<ScheduledTask> queue = new PriorityBlockingQueue<>();

    // Лок и условие для пробуждения рабочего потока при добавлении новой задачи.
    // Без этого поток бы спал до старого дедлайна, не зная о новой, более ранней задаче.
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition newTaskAdded = lock.newCondition();

    // Флаг остановки. volatile гарантирует, что запись в одном потоке (close)
    // будет видна в другом (рабочий поток) без дополнительной синхронизации.
    private volatile boolean stopped = false;

    // Рабочий поток, который выполняет задачи.
    private final Thread worker;

    public CallbackSchedulerImpl() {
        // Поток-демон: не будет мешать завершению JVM, если забыли вызвать close().
        worker = new Thread(this::run, "callback-scheduler-worker");
        worker.setDaemon(true);
        worker.start();
    }

    @Override
    public void schedule(Runnable callback, Instant after) {
        if (stopped) {
            throw new IllegalStateException("Планировщик остановлен");
        }

        queue.add(new ScheduledTask(callback, after));

        // Сигнализируем рабочему потоку, что появилась новая задача.
        // Это критически важно: если новая задача должна выполниться раньше текущей
        // ближайшей, рабочий поток должен проснуться и пересчитать время ожидания.
        lock.lock();
        try {
            newTaskAdded.signal();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() {
        stopped = true;

        // Будим рабочий поток, чтобы он увидел флаг stopped и завершился.
        lock.lock();
        try {
            newTaskAdded.signal();
        } finally {
            lock.unlock();
        }

        try {
            worker.join(5000); // Ждём завершения не более 5 секунд.
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Главный цикл рабочего потока.
     *
     * Алгоритм:
     * 1. Если очередь пуста — ждём сигнала о новой задаче (await).
     * 2. Если очередь не пуста — смотрим ближайшую задачу (peek, не извлекаем!).
     * 3. Если время пришло — извлекаем (poll) и выполняем.
     * 4. Если время не пришло — ждём ровно столько, сколько осталось (awaitNanos).
     *    При этом нас может разбудить signal() из schedule(), если пришла более ранняя задача.
     */
    private void run() {
        while (!stopped) {
            lock.lock();
            try {
                ScheduledTask next = queue.peek();

                if (next == null) {
                    // Очередь пуста — ждём новую задачу.
                    newTaskAdded.await();
                    continue;
                }

                long nanosToWait = Duration.between(Instant.now(), next.after()).toNanos();

                if (nanosToWait > 0) {
                    // Время ещё не пришло — спим ровно столько, сколько нужно.
                    // awaitNanos может вернуться раньше из-за signal() — это нормально,
                    // мы просто пересчитаем на следующей итерации.
                    newTaskAdded.awaitNanos(nanosToWait);
                } else {
                    // Время пришло — извлекаем и выполняем задачу.
                    ScheduledTask task = queue.poll();
                    if (task != null) {
                        // Выполняем вне лока, чтобы не блокировать schedule().
                        lock.unlock();
                        try {
                            task.callback().run();
                        } catch (Exception e) {
                            // Ловим исключения, чтобы рабочий поток не упал.
                            e.printStackTrace();
                        } finally {
                            lock.lock();
                        }
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } finally {
                lock.unlock();
            }
        }
    }
}
