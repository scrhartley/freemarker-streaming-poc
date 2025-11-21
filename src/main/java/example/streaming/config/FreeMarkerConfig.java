package example.streaming.config;

import static java.util.Collections.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.boot.autoconfigure.freemarker.FreeMarkerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.view.AbstractUrlBasedView;
import org.springframework.web.servlet.view.freemarker.FreeMarkerView;
import org.springframework.web.servlet.view.freemarker.FreeMarkerViewResolver;

import example.streaming.freemarker.custom.TrackedModelFutures;
import example.streaming.freemarker.custom.directive.AsyncDeferHtmlDirective;
import example.streaming.freemarker.custom.directive.AsyncRenderDeferredHtmlDirective;
import example.streaming.freemarker.custom.directive.DeferHtmlDirective;
import example.streaming.freemarker.custom.directive.FlushBoundaryDirective;
import example.streaming.freemarker.custom.directive.RenderDeferredHtmlDirective;
import example.streaming.freemarker.custom.directive.Streaming;
import example.streaming.util.future.LazyDirectExecutorService;

@Configuration
public class FreeMarkerConfig {

    private static boolean CANCEL_UNCOMPLETED_FUTURES = true; // Intended to avoid leaving stuck threads.

    @Bean @Lazy(false)
    freemarker.template.Configuration freeMarkerTemplateConfiguration(
            org.springframework.web.servlet.view.freemarker.FreeMarkerConfig configurer) {
        freemarker.template.Configuration config = configurer.getConfiguration();
        config.setSharedVariable("atom", new FlushBoundaryDirective());
        config.setSharedVariable("defer", new DeferHtmlDirective());
        config.setSharedVariable("renderDeferred", new RenderDeferredHtmlDirective());
        config.setSharedVariable("asyncDefer", new AsyncDeferHtmlDirective());
        config.setSharedVariable("renderAsyncDeferred", new AsyncRenderDeferredHtmlDirective());
        config.setSharedVariable("disallowAutoStreaming", Streaming.disallowAutoStreamingDirective());
        config.setSharedVariable("isAutoStreamingAllowed", Streaming.checkAutoStreamingAllowedModel());
        return config;
    }

    @Bean
    ExecutorService mvcExecutorService() {
        return new LazyDirectExecutorService();
    }


    @Bean // Originally added so it can call Future.cancel
    FreeMarkerViewResolver freeMarkerViewResolver(FreeMarkerProperties properties) {
        class CustomFreeMarkerView extends FreeMarkerView {
            @Override
            public void render(@Nullable Map<String,?> model, HttpServletRequest request,
                               HttpServletResponse response) throws Exception {
                List<Future<?>> futures = CANCEL_UNCOMPLETED_FUTURES ? getFutures(model) : emptyList();
                try {
                    trackCompletableFutures(model, request);
                    super.render(model, request, response);
                } finally {
                    for (Future<?> future : futures) {
                        future.cancel(true);
                    }
                }
            }

            private List<Future<?>> getFutures(@Nullable Map<String, ?> model) {
                return (model == null || model.isEmpty())
                        ? emptyList()
                        : model.values().stream()
                                .filter(Future.class::isInstance)
                                .<Future<?>>map(Future.class::cast)
                                .collect(Collectors.toList());
            }

            private void trackCompletableFutures(@Nullable Map<String,?> model, HttpServletRequest request) {
                TrackedModelFutures tracking = (TrackedModelFutures) request.getAttribute(TrackedModelFutures.KEY);
                if (tracking == null) {
                    tracking = new TrackedModelFutures();
                    request.setAttribute(TrackedModelFutures.KEY, tracking);
                }

                if (model == null || model.isEmpty()) {
                    return;
                }
                @SuppressWarnings("unchecked")
                Stream<Map.Entry<String, CompletableFuture<Object>>> cfs = model.entrySet().stream()
                        .filter(entry -> entry.getValue() instanceof CompletableFuture)
                        .map(Map.Entry.class::cast);
                tracking.addCompletableFutures(cfs);
            }
        }

        class CustomFreeMarkerViewResolver extends FreeMarkerViewResolver {
            @Override
            protected Class<?> requiredViewClass() {
                return CustomFreeMarkerView.class;
            }
            @Override
            protected AbstractUrlBasedView instantiateView() {
                return getViewClass() == CustomFreeMarkerView.class
                        ? new CustomFreeMarkerView() : super.instantiateView();
            }
        }

        FreeMarkerViewResolver resolver = new CustomFreeMarkerViewResolver();
        properties.applyToMvcViewResolver(resolver);
        return resolver;
    }

}
