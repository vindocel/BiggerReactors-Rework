package biggerreactorsrework.core.threading;

import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@SuppressWarnings("unused")
public class WorkQueue {
    // Modified 2026-10-30: start workers on demand and terminate them explicitly on either side.
    private final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
    
    private final ArrayList<DequeueThread> dequeueThreads = new ArrayList<>();
    
    private volatile Throwable toRethrow;
    private boolean finished;
    
    public WorkQueue() {
    }
    
    private class DequeueThread implements Runnable {
        private final BlockingQueue<Runnable> queue;
        private final AtomicBoolean stop = new AtomicBoolean(false);
        private final Thread thread;
        private boolean started;
        
        public DequeueThread(BlockingQueue<Runnable> queue, String name) {
            this.queue = queue;
            thread = new Thread(this);
            if (name != null) {
                thread.setName(name);
            }
            thread.setDaemon(true); // just, because, shouldn't be necessary, but just because
        }

        void start() {
            if (!started) { started = true; thread.start(); }
        }
        
        public void run() {
            while (!stop.get()) {
                Runnable nextItem;
                try {
                    nextItem = queue.take();
                } catch (InterruptedException e) {
                    return;
                }
                try {
                    nextItem.run();
                } catch (Throwable e) {
                    WorkQueue.this.toRethrow = e;
                }
            }
            
        }
        
        public void finish() {
            stop.set(true);
            thread.interrupt();
        }
    }
    
    public WorkQueue addProcessingThread() {
        return addProcessingThreads(1, null);
    }
    
    public WorkQueue addProcessingThreads(int threads) {
        return addProcessingThreads(threads, null);
    }
    
    public synchronized WorkQueue addProcessingThreads(int threads, String name) {
        if (finished) throw new IllegalStateException("Work queue is closed");
        for (int i = 0; i < threads; i++) {
            dequeueThreads.add(new DequeueThread(queue, name == null ? null : name + i));
        }
        return this;
    }
    
    public void finish() {
        final ArrayList<DequeueThread> workers;
        synchronized (this) {
            finished = true;
            workers = new ArrayList<>(dequeueThreads);
            workers.forEach(DequeueThread::finish);
        }
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(1);
        for (var worker : workers) {
            if (worker.thread == Thread.currentThread()) continue;
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0) break;
            try { worker.thread.join(Math.max(1, java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(remaining))); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
        }
    }
    
    private static class WorkItem implements Runnable {
        final Event waitEvent = new Event();
        final Event readyEvent = new Event();
        final Runnable work;
        final AtomicLong unTriggeredWaitEvents = new AtomicLong(Long.MAX_VALUE);
        
        WorkItem(final Queue<Runnable> queue, final Runnable work, final Event[] waitEvents) {
            this.work = work;
            if (waitEvents.length == 0) {
                queue.add(this);
                return;
            }
            unTriggeredWaitEvents.set(waitEvents.length);
            for (Event event : waitEvents) {
                if (event == null) {
                    if (unTriggeredWaitEvents.decrementAndGet() == 0) {
                        readyEvent.trigger();
                    }
                    continue;
                }
                assert event != null;
                event.registerCallback(() -> {
                    if (unTriggeredWaitEvents.decrementAndGet() == 0) {
                        synchronized (readyEvent) {
                            readyEvent.trigger();
                        }
                    }
                });
            }
            readyEvent.registerCallback(() -> {
                queue.add(this);
            });
        }
        
        @Override
        public void run() {
            try {
                work.run();
            } finally {
                waitEvent.trigger();
            }
        }
    }
    
    public synchronized Event enqueue(Runnable runnable, Event... events) {
        prepareWorkers();
        WorkItem item = new WorkItem(queue, runnable, events);
        return item.waitEvent;
    }
    
    public synchronized void enqueueUntracked(Runnable runnable) {
        prepareWorkers();
        queue.add(runnable);
    }

    private void prepareWorkers() {
        if (finished) throw new IllegalStateException("Work queue is closed");
        if (toRethrow instanceof Error error) throw error;
        if (toRethrow instanceof RuntimeException exception) throw exception;
        if (toRethrow != null) throw new IllegalStateException("Worker failed", toRethrow);
        dequeueThreads.forEach(DequeueThread::start);
    }
    
    public boolean runOne() {
        if (!dequeueThreads.isEmpty()) {
            return false;
        }
        Runnable toRun = queue.poll();
        if (toRun != null) {
            toRun.run();
            return true;
        }
        return false;
    }
    
    public boolean runAll() {
        if (!dequeueThreads.isEmpty()) {
            return false;
        }
        Runnable toRun;
        boolean ranSomething = false;
        while ((toRun = queue.poll()) != null) {
            toRun.run();
            ranSomething = true;
        }
        return ranSomething;
    }
}
