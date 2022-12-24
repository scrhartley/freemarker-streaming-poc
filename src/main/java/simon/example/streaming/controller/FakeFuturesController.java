package simon.example.streaming.controller;

//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import simon.example.streaming.service.BlockingSlowService;
//
//import java.util.List;
//import java.util.Objects;
//import java.util.concurrent.*;
//import java.util.concurrent.locks.Condition;
//import java.util.concurrent.locks.ReentrantLock;

/**
 * Demonstrates using an ExecutorService directly in a Controller.
 * Uses an ExecutorService implementation that can be used in place of a normal concurrent one.
 * This ExecutorService avoids the rough edges of using Callables directly
 * and allows a straightforward path of migrating to proper Futures at a later point in time.
 */
//@Controller
//@RequestMapping("/fake-futures")
public class FakeFuturesController {

//    @Autowired
//    private BlockingSlowService service;
//    @Autowired
//    private ExecutorService modelExecutorService;
//
//
//    @GetMapping
//    public String pageWithDependencies(Model model) {
//
//        Future<String> data1 = modelExecutorService.submit(service::getData1);
//
//        model.addAttribute("myData", data1);
//
//        model.addAttribute("myData2", modelExecutorService.submit(() -> {
//            String data = data1.get();
//            return service.getData2(data + " and sub-work is done");
//        }));
//
//        return "futures";
//    }
//
//
//
//    @Configuration
//    static class NonConcurrentMvcConfig {
//
//        @Bean
//        ExecutorService modelExecutorService() {
//            return new LazyDirectExecutorService();
//        }
//
//
//        // Future that runs the Callable/Runnable when get() is invoked,
//        // rather than always relying upon an ExecutorService
//        // to schedule and run the task asynchronously.
//        // Callable/Runnable is guaranteed to be run at most one time.
//        private static class LazyTask<V> extends FutureTask<V> {
//            public LazyTask(Callable<V> callable) {
//                super(callable);
//            }
//            public LazyTask(Runnable runnable, V result) {
//                super(runnable, result);
//            }
//
//            @Override
//            public V get() throws ExecutionException, InterruptedException {
//                run();
//                return super.get();
//            }
//            @Override
//            public V get(long timeout, TimeUnit unit)
//                    throws InterruptedException, ExecutionException, TimeoutException {
//                run();
//                return super.get(timeout, unit);
//            }
//        }
//
//        // A "fake" ExecutorService that instead relies upon the Futures to run themselves when get() is invoked.
//        // Using this ExecutorService allows deferring execution of a task without introducing concurrency.
//        private static class LazyDirectExecutorService extends AbstractExecutorService {
//
//            private volatile boolean shutdown;
//            private volatile boolean taskRunning;
//            private volatile boolean terminated;
//            private final ReentrantLock lock = new ReentrantLock();
//            private final Condition condition = lock.newCondition();
//
//
//            @Override
//            public Future<?> submit(Runnable task) {
//                Objects.requireNonNull(task);
//                return new LazyTask(task, null);
//            }
//            @Override
//            public <T> Future<T> submit(Runnable task, T result) {
//                Objects.requireNonNull(task);
//                return new LazyTask(task, result);
//            }
//            @Override
//            public <T> Future<T> submit(Callable<T> task) {
//                Objects.requireNonNull(task);
//                return new LazyTask(task);
//            }
//
//
//            @Override
//            public void execute(Runnable command) {
//                lock.lock();
//                try {
//                    if (shutdown) {
//                        throw new RejectedExecutionException();
//                    }
//                    taskRunning = true;
//                } finally {
//                    lock.unlock();
//                }
//
//                command.run();
//
//                lock.lock();
//                try {
//                    taskRunning = false;
//                    if (shutdown) {
//                        terminated = true;
//                        condition.signalAll();
//                    }
//                } finally {
//                    lock.unlock();
//                }
//            }
//
//
//            @Override
//            public void shutdown() {
//                shutdown = true;
//
//                lock.lock();
//                try {
//                    terminated = !taskRunning;
//                } finally {
//                    lock.unlock();
//                }
//            }
//
//            @Override
//            public List<Runnable> shutdownNow() {
//                shutdown();
//                return null;
//            }
//
//            @Override
//            public boolean isShutdown() {
//                return shutdown;
//            }
//
//            @Override
//            public boolean isTerminated() {
//                return terminated;
//            }
//
//            @Override
//            public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
//                lock.lock();
//                try {
//                    if (terminated) {
//                        return true;
//                    }
//                    condition.await(timeout, unit);
//                    return terminated;
//                } finally {
//                    lock.unlock();
//                }
//            }
//
//        }
//
//    }

}
