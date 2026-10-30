package biggerreactorsrework.core.threading;

// Modified 2026-10-30: regression coverage for actual worker shutdown and dependency completion.

import org.junit.jupiter.api.Test;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class WorkQueueTest {
    @Test void closingWakesAndJoinsAnIdleWorker() throws Exception {
        var queue = new WorkQueue().addProcessingThread();
        var worker = new AtomicReference<Thread>();
        var ran = new CountDownLatch(1);
        try {
            queue.enqueue(() -> { worker.set(Thread.currentThread()); ran.countDown(); });
            assertTrue(ran.await(2, TimeUnit.SECONDS));
            queue.finish();
            assertFalse(worker.get().isAlive());
            assertThrows(IllegalStateException.class, () -> queue.enqueue(() -> { }));
        } finally { queue.finish(); }
    }

    @Test void dependenciesRunExactlyOnceAndNullMeansAlreadyComplete() throws Exception {
        var queue = new WorkQueue().addProcessingThread();
        var dependency = new Event();
        var completed = new CountDownLatch(1);
        try {
            var done = queue.enqueue(completed::countDown, null, dependency);
            assertFalse(done.ready());
            dependency.trigger();
            dependency.trigger();
            assertTrue(completed.await(2, TimeUnit.SECONDS));
            assertTrue(done.join(1000));
        } finally { queue.finish(); }
    }

    @Test void fatalWorkerErrorsAreReportedWithoutClientClasses() throws Exception {
        var queue = new WorkQueue().addProcessingThread();
        var failed = new CountDownLatch(1);
        try {
            queue.enqueueUntracked(() -> { try { throw new AssertionError("worker failure"); } finally { failed.countDown(); } });
            assertTrue(failed.await(2, TimeUnit.SECONDS));
            // This marker is completed only after the worker has stored its previous error.
            var drained = new CountDownLatch(1);
            try { queue.enqueueUntracked(drained::countDown); }
            catch (AssertionError expected) { return; }
            assertTrue(drained.await(2, TimeUnit.SECONDS));
            assertThrows(AssertionError.class, () -> queue.enqueue(() -> { }));
        } finally { queue.finish(); }
    }
}
