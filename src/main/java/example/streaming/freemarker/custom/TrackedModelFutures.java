package example.streaming.freemarker.custom;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TrackedModelFutures {

    public static final String KEY = "mvc.model.future.tracking";
    private static final int DEFAULT_TIMEOUT_SECONDS = 10 * 60;

    private final Map<String, Future<Object>> futureAttributes;
    private final BlockingQueue<Future<Object>> completionQueue;
    private final int timeoutSeconds;
    private Set<String> futureAttributeNames;
    private Iterable<Collection<String>> completionIterator;
    private final Set<CompletableFuture<?>> trackedCompletableFutures = new HashSet<>();

    public TrackedModelFutures(
            Map<String, Future<Object>> futureAttributes,
            BlockingQueue<Future<Object>> completionQueue) {
        this(futureAttributes, completionQueue, DEFAULT_TIMEOUT_SECONDS);
    }
    public TrackedModelFutures(
            Map<String, Future<Object>> futureAttributes,
            BlockingQueue<Future<Object>> completionQueue,
            int timeoutSeconds) {
        this.futureAttributes = futureAttributes;
        this.completionQueue = completionQueue;
        this.timeoutSeconds = timeoutSeconds;
    }

    public TrackedModelFutures() {
        this(DEFAULT_TIMEOUT_SECONDS);
    }
    public TrackedModelFutures(int timeoutSeconds) {
        this(new HashMap<>(), new LinkedBlockingQueue<>(), timeoutSeconds);
    }


    public Set<String> getAttributeNames() {
        ensureReadOnly();
        return futureAttributeNames;
    }

    public Iterable<Collection<String>> getCompletionIterable() {
        ensureReadOnly();
        return completionIterator;
    }

    // Support futures not created by AsyncModel.
    public void addCompletableFutures(Stream<Map.Entry<String, CompletableFuture<Object>>> entryStream) {
        if (isReadOnly()) {
            throw new IllegalStateException("Tried to add CompletableFuture while in read-only mode");
        }

        entryStream.forEach(entry -> {
            String name = entry.getKey();
            CompletableFuture<Object> cf = entry.getValue();

            if (futureAttributes.containsKey(name)) {
                if (futureAttributes.get(name) == cf) {
                    return; // "continue"/skip as already tracking
                } else {
                    throw new IllegalArgumentException("Other entry already exists");
                }
            }

            futureAttributes.put(name, cf);
            if (trackedCompletableFutures.add(cf)) {
                cf.whenComplete((v, t) -> completionQueue.add(cf));
            } // Else we've already seen it with a different name
        });
    }

    public boolean isReadOnly() {
        return futureAttributeNames != null;
    }

    private void ensureReadOnly() {
        if (isReadOnly()) {
            return;
        }

        futureAttributeNames = futureAttributes.keySet();
        completionIterator = new Iterable<>() {
            final List<Collection<String>> allCompleted = new ArrayList<>();
            final Map<Future<Object>, List<String>> attributeLookup = futureAttributes.entrySet().stream()
                    .collect(Collectors.groupingBy(
                            Map.Entry::getValue,
                            Collectors.mapping(Map.Entry::getKey, Collectors.toList())));;

            @Override
            public Iterator<Collection<String>> iterator() {
                return new Iterator<>() {
                    final Iterator<Collection<String>> doneIt = allCompleted.isEmpty()
                            ? Collections.emptyIterator() : new ArrayList<>(allCompleted).iterator(); // Snapshot
                    int pending = attributeLookup.size() - allCompleted.size();

                    @Override
                    public boolean hasNext() {
                        return doneIt.hasNext() || pending > 0;
                    }

                    @Override
                    public Collection<String> next() {
                        if (doneIt.hasNext()) {
                            return doneIt.next();
                        } else if (pending == 0) {
                            throw new NoSuchElementException();
                        } else {
                            try {
                                return nextFromQueue();
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                throw new RuntimeException(e);
                            }
                        }
                    }

                    private Collection<String> nextFromQueue() throws InterruptedException {
                        Future<?> future = completionQueue.poll(timeoutSeconds, TimeUnit.SECONDS);
                        if (future == null) {
                            throw new RuntimeException(new TimeoutException());
                        }
                        pending--;
                        Collection<String> attribute = attributeLookup.get(future);
                        Objects.requireNonNull(attribute, "Something has gone wrong");
                        allCompleted.add(attribute);
                        return attribute;
                    }
                };
            }
        };
    }

}
