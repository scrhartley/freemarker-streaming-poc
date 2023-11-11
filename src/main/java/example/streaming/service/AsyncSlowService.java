package example.streaming.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;

@Service
public class AsyncSlowService {

    @Async
    public CompletableFuture<String> getData1() {
        try {
            Thread.sleep(4_000);
            return completedFuture("Work done");
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return failedFuture(e);
        }
    }

    @Async
    public CompletableFuture<String> getIntermediateData() {
        return completedFuture("Intermediate work done");
    }

    @Async
    public CompletableFuture<String> getData2(String param) {
        try {
            Thread.sleep(3_000);
            return completedFuture(param);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return failedFuture(e);
        }
    }

}
