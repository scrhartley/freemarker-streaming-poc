package example.streaming.util.future;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

// A "fake" ExecutorService that instead relies upon the Futures to run themselves when get() is invoked.
// Using this ExecutorService allows deferring execution of a task without introducing concurrency.
public class LazyDirectExecutorService extends AbstractExecutorService {

    private boolean shutdown;
    private int runningTasks;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();


    @Override
    public <T> Future<T> submit(Callable<T> task) {
        Objects.requireNonNull(task);
        return new LazyTask<>(task);
    }
    @Override
    public Future<?> submit(Runnable task) {
        Objects.requireNonNull(task);
        return new LazyTask<>(task, null);
    }
    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        Objects.requireNonNull(task);
        return new LazyTask<>(task, result);
    }


    @Override
    public void execute(Runnable command) {
        lock.lock();
        try {
            if (shutdown) {
                throw new RejectedExecutionException();
            }
            runningTasks++;
        } finally {
            lock.unlock();
        }

        try {
            command.run();
        } finally {
            lock.lock();
            try {
                int numRunning = --runningTasks;
                if (numRunning == 0) {
                    condition.signalAll();
                }
            } finally {
                lock.unlock();
            }
        }
    }


    @Override
    public void shutdown() {
        lock.lock();
        try {
            shutdown = true;
            if (runningTasks == 0) {
                condition.signalAll();
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<Runnable> shutdownNow() {
        shutdown();
        return null;
    }

    @Override
    public boolean isShutdown() {
        lock.lock();
        try {
            return shutdown;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean isTerminated() {
        lock.lock();
        try {
            return shutdown && runningTasks == 0;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        lock.lock();
        try {
            while (true) {
                if (shutdown && runningTasks == 0) {
                    return true;
                } else if (nanos <= 0L) {
                    return false;
                } else {
                    nanos = condition.awaitNanos(nanos);
                }
            }
        } finally {
            lock.unlock();
        }
    }

}
