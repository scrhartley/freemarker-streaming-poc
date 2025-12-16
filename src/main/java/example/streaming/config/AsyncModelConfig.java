package example.streaming.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.IntStream;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.annotation.ModelMethodProcessor;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import example.streaming.AsyncModel;
import example.streaming.freemarker.custom.TrackedModelFutures;
import example.streaming.util.future.LazyDirectExecutorService;

public class AsyncModelConfig {

    @Configuration
    public static class WebConfig implements WebMvcConfigurer {
        @Autowired
        ExecutorService blockingExecutorService;

        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
            argumentResolvers.add(new AsyncModelArgumentResolver(blockingExecutorService));
        }
    }


    private static class AsyncModelArgumentResolver implements HandlerMethodArgumentResolver {
        private final ExecutorService executorService;

        private AsyncModelArgumentResolver(ExecutorService executorService) {
            this.executorService = executorService;
        }

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return AsyncModel.class == parameter.getParameterType();
        }

        @Override
        public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
            Assert.notNull(mavContainer, "ModelAndViewContainer is required for model exposure");
            Object model = mavContainer.getModel();
            if (model == mavContainer.getDefaultModel()) { // Not redirect
                Assert.isInstanceOf(Model.class, model);
                model = new ExecutorAsyncModel((Model) model, executorService, webRequest);
            }
            return model;
        }
    }


    @Configuration @Lazy(false)
    public static class FixResolverOrderConfig implements InitializingBean {
        @Autowired
        RequestMappingHandlerAdapter handlerAdapter;

        @Override
        public void afterPropertiesSet() {
            List<HandlerMethodArgumentResolver> resolvers = handlerAdapter.getArgumentResolvers();

            OptionalInt syncIdx = IntStream.range(0, resolvers.size())
                    .filter(i-> resolvers.get(i) instanceof ModelMethodProcessor)
                    .findFirst();
            if (!syncIdx.isPresent()) {
                return;
            }

            OptionalInt asyncIdx = IntStream.range(syncIdx.getAsInt() + 1, resolvers.size())
                    .filter(i-> resolvers.get(i) instanceof AsyncModelArgumentResolver)
                    .findFirst();
            if (!asyncIdx.isPresent()) {
                return;
            }

            // If AsyncModel resolver is not before the Model one,
            // then it won't work, since the former interface extends the latter.
            List<HandlerMethodArgumentResolver> orderedResolvers = new ArrayList<>(resolvers);
            HandlerMethodArgumentResolver async = orderedResolvers.remove(asyncIdx.getAsInt());
            orderedResolvers.add(syncIdx.getAsInt(), async);
            handlerAdapter.setArgumentResolvers(orderedResolvers);
        }
    }


    private static class ExecutorAsyncModel extends WrappingModel implements AsyncModel {
        private final ExecutorService executorService;
        private final CompletionService<Object> completionService;
        private final BlockingQueue<Future<Object>> completionQueue;
        private final Map<String, Future<Object>> futureAttributes;

        private ExecutorAsyncModel(Model model, ExecutorService executorService, RequestAttributes request) {
            super(model);
            this.executorService = executorService;
            BlockingQueue<Future<Object>> completionQueue = new LinkedBlockingQueue<>();
            this.completionQueue = completionQueue;
            this.completionService = new ExecutorCompletionService<>(executorService, completionQueue);
            Map<String, Future<Object>> futureAttributes = new HashMap<>();
            this.futureAttributes = futureAttributes;

            request.setAttribute(
                    TrackedModelFutures.KEY,
                    new TrackedModelFutures(futureAttributes, completionQueue),
                    RequestAttributes.SCOPE_REQUEST);
        }

        @Override
        public <T> Future<T> addAttribute(String attributeName, Callable<T> callable) {
            validateAttribute(attributeName, null);
            Future<T> future = submit(attributeName, callable);
            super.addAttribute(attributeName, future);
            return future;
        }

        @Override
        public Model addAttribute(String name, @Nullable Object value) {
            validateAttribute(name, value);
            return super.addAttribute(name, value);
        }

        @Override
        public Model addAllAttributes(@Nullable Map<String, ?> attributes) {
            if (attributes != null) {
                for (Map.Entry<String, ?> entry : attributes.entrySet()) {
                    validateAttribute(entry.getKey(), entry.getValue());
                }
            }
            return super.addAllAttributes(attributes);
        }


        private void validateAttribute(String name, Object value) {
            Objects.requireNonNull(name);
            Object existingValue = getAttribute(name);
            if (existingValue instanceof Future && existingValue != value) {
                throw new IllegalStateException("Replacing a future is forbidden");
            }
        }

        @SuppressWarnings("unchecked")
        private <T> Future<T> submit(String attributeName, Callable<T> callable) {
            Future<T> specificfuture;
            Future<Object> generalFuture;
            if (executorService instanceof LazyDirectExecutorService) {
                // A CompletionService doesn't make sense for LazyDirectExecutorService
                // since either the work will be done on submit, or else
                // later we would hang when trying to take from it.
                specificfuture = executorService.submit(callable);
                generalFuture = (Future<Object>) specificfuture;
                completionQueue.add(generalFuture);
            } else {
                generalFuture = completionService.submit((Callable<Object>) callable);
                specificfuture = (Future<T>) generalFuture;
            }
            futureAttributes.put(attributeName, generalFuture);
            return specificfuture;
        }

        @Override
        public Map<String, Object> asMap() {
            return Collections.unmodifiableMap(super.asMap());
        }

    }

    private static class WrappingModel implements Model {
        private final Model source;
        WrappingModel(Model model) {
            this.source = model;
        }

        @Override
        public Model addAttribute(String name, @Nullable Object value) {
            return source.addAttribute(name, value);
        }
        @Override
        public Model addAttribute(Object value) {
            return source.addAttribute(value);
        }
        @Override
        public Model addAllAttributes(@Nullable Collection<?> values) {
            return source.addAllAttributes(values);
        }
        @Override
        public Model addAllAttributes(@Nullable Map<String, ?> attributes) {
            return source.addAllAttributes(attributes);
        }
        @Override
        public Model mergeAttributes(@Nullable Map<String, ?> attributes) {
            return source.mergeAttributes(attributes);
        }
        @Override
        public boolean containsAttribute(String name) {
            return source.containsAttribute(name);
        }
        @Override
        public Object getAttribute(String name) {
            return source.getAttribute(name);
        }
        @Override
        public Map<String, Object> asMap() {
            return source.asMap();
        }
    }

}
