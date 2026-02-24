//package ydx.core
//
//import java.time.Instant
//
//class CallbackSchedulerTest {
//
//    private lateinit var scheduler: CallbackScheduler
//
//    fun setUp() {
//        // scheduler = new CallbackSchedulerImpl();
//    }
//
//    fun tearDown() {
//        scheduler.close();
//    }
//
//    fun testSimple() {
//        val result: CallbackResult = CallbackResult();
//
//        val runnable = Runnable {
//            synchronized(result) {
//                result.isDone = true;
//                result.notify()
//            }
//        }
//        scheduler.schedule(runnable, Instant.now().plusSeconds(2))
//
//        synchronized (result) {
//            result.wait();
//            assertTrue(result.isDone);
//        }
//    }
//}