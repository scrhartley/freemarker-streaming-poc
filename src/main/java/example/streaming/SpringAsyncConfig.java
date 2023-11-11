package example.streaming;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.*;

@Configuration
@EnableAsync
public class SpringAsyncConfig {

    // For @Async methods. Replaces default SimpleAsyncTaskExecutor.
    @Bean
    public TaskExecutor springAsyncTaskExecutor() {
        return new TaskExecutorAdapter(concurrentExecutorService());
    }


    private ExecutorService concurrentExecutorService() {
        // Please don't use this in production since the thread pool size in unbounded.
        return Executors.newCachedThreadPool();

        // Perhaps consider adding a maximum thread pool size
        // and fallback to using running synchronously when
        // the maximum number of active threads is reached:
        // return new ThreadPoolExecutor(0, maxThreads,
        //         60L, TimeUnit.SECONDS, new SynchronousQueue<>(),
        //         new ThreadPoolExecutor.CallerRunsPolicy());
    }

}
