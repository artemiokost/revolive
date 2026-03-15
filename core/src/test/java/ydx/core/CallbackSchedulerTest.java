package ydx.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class CallbackSchedulerTest {

    private CallbackScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new CallbackSchedulerImpl();
    }

    @AfterEach
    void tearDown() throws Exception {
        scheduler.close();
    }

    @Test
    void callbackExecutesAfterSpecifiedTime() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        Instant scheduledAt = Instant.now();
        Instant[] executedAt = new Instant[1];

        scheduler.schedule(() -> {
            executedAt[0] = Instant.now();
            latch.countDown();
        }, scheduledAt.plusMillis(500));

        assertTrue(latch.await(3, TimeUnit.SECONDS), "Коллбэк не был вызван в течение таймаута");
        assertNotNull(executedAt[0]);
        assertTrue(executedAt[0].isAfter(scheduledAt.plusMillis(400)),
                "Коллбэк выполнился слишком рано");
    }

    @Test
    void multipleCallbacksExecuteInCorrectOrder() throws InterruptedException {
        int count = 3;
        CountDownLatch latch = new CountDownLatch(count);
        List<Integer> executionOrder = Collections.synchronizedList(new ArrayList<>());

        Instant now = Instant.now();

        // Планируем в обратном порядке, чтобы убедиться, что очередь сортирует правильно.
        scheduler.schedule(() -> { executionOrder.add(3); latch.countDown(); }, now.plusMillis(1500));
        scheduler.schedule(() -> { executionOrder.add(1); latch.countDown(); }, now.plusMillis(500));
        scheduler.schedule(() -> { executionOrder.add(2); latch.countDown(); }, now.plusMillis(1000));

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Не все коллбэки были вызваны");
        assertEquals(List.of(1, 2, 3), executionOrder, "Коллбэки выполнились в неправильном порядке");
    }

    @Test
    void closeStopsSchedulerGracefully() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        scheduler.schedule(latch::countDown, Instant.now().plusMillis(100));
        assertTrue(latch.await(3, TimeUnit.SECONDS), "Коллбэк не был вызван до close()");

        scheduler.close();

        // После close() планировщик должен выбросить исключение.
        assertThrows(IllegalStateException.class,
                () -> scheduler.schedule(() -> {}, Instant.now()));
    }
}
