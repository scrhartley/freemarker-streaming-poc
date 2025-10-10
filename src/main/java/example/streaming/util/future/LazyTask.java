package example.streaming.util.future;

import java.util.concurrent.*;

// Non-concurrent Future that runs the Callable/Runnable when get() is invoked,
// rather than relying upon an ExecutorService to schedule and run the task asynchronously.
// Callable/Runnable is guaranteed to be run at most one time.
public class LazyTask<V> extends FutureTask<V> {

    public LazyTask(Callable<V> callable) {
        super(callable);
    }

    public LazyTask(Runnable runnable, V result) {
        super(runnable, result);
    }

    @Override
    public V get() throws ExecutionException, InterruptedException {
        run();
        return super.get();
    }

    @Override
    public V get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        run();
        return super.get(timeout, unit);
    }
}
