package example.streaming.config;

import static java.util.Collections.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

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
        config.setSharedVariable("disallowAutoStreaming", Streaming.disallowAutoStreamingDirective());
        config.setSharedVariable("isAutoStreamingAllowed", Streaming.checkAutoStreamingAllowedModel());
        return config;
    }

    @Bean
    ExecutorService mvcExecutorService() {
        return new LazyDirectExecutorService();
    }


    @Bean // Only needed so it can call Future.cancel
    FreeMarkerViewResolver freeMarkerViewResolver(FreeMarkerProperties properties) {
        class CustomFreeMarkerView extends FreeMarkerView {
            @Override
            public void render(@Nullable Map<String,?> model, HttpServletRequest request,
                               HttpServletResponse response) throws Exception {
                List<Future<?>> futures = CANCEL_UNCOMPLETED_FUTURES ? getFutures(model) : emptyList();
                try {
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
