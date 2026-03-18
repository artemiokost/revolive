package ydx.core;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class CallbackScheduler2Impl implements AutoCloseable {

    private record ScheduledTask(Runnable callback, Instant after) implements Comparable<ScheduledTask> {
        @Override
        public int compareTo(ScheduledTask other) {
            return this.after.compareTo(other.after);
        }
    }

    private final PriorityBlockingQueue<ScheduledTask> queue = new PriorityBlockingQueue<>();

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition newTaskAdded = lock.newCondition();

    private volatile boolean stopped = false;

    private final Thread worker;

    public CallbackScheduler2Impl() {
        this.worker = new Thread(this::run, "worker.thread");
        this.worker.setDaemon(true);
        this.worker.start();
    }

    @Override
    public void close() throws Exception {
        this.stopped = true;

        lock.lock();
        try {
            newTaskAdded.signal();
        } finally {
            lock.unlock();
        }

        try {
            worker.join(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

//    @Override
//    public void close() {
//        stopped = true;
//        worker.interrupt();
//    }


    public void schedule(ScheduledTask task) {
        if (stopped) {
            throw new IllegalStateException("Планировщик остановлен");
        }

        queue.add(task);
        lock.lock();
        try {
            newTaskAdded.signal();
        } finally {
            lock.unlock();
        }
    }

    private ScheduledTask pollReady() throws InterruptedException {
        lock.lock();
        try {
            ScheduledTask next = queue.peek();
            if (next == null) {
                newTaskAdded.await();
                return null;
            }

            long nanosToWait = Duration.between(Instant.now(), next.after).toNanos();

            if (nanosToWait > 0) {
                newTaskAdded.awaitNanos(nanosToWait);
                return null;
            }

            return queue.poll();
        } finally {
            lock.unlock();
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
