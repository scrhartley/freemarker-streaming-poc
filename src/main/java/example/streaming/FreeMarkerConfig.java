package example.streaming;

import java.util.concurrent.ExecutorService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import example.streaming.freemarker.custom.directive.DeferredHtmlDirective;
import example.streaming.freemarker.custom.directive.FlushBoundaryDirective;
import example.streaming.freemarker.custom.directive.Streaming;
import example.streaming.freemarker.custom.directive.TriggerDeferredHtmlDirective;
import example.streaming.util.future.LazyDirectExecutorService;

@Configuration
public class FreeMarkerConfig {

    @Bean @Lazy(false)
    freemarker.template.Configuration freeMarkerTemplateConfiguration(
            org.springframework.web.servlet.view.freemarker.FreeMarkerConfig configurer) {
        freemarker.template.Configuration config = configurer.getConfiguration();
        config.setSharedVariable("atom", new FlushBoundaryDirective());
        config.setSharedVariable("deferred", new DeferredHtmlDirective());
        config.setSharedVariable("triggerDeferred", new TriggerDeferredHtmlDirective());
        config.setSharedVariable("disallowAutoStreaming", Streaming.disallowAutoStreamingDirective());
        config.setSharedVariable("isAutoStreamingAllowed", Streaming.checkAutoStreamingAllowedModel());
        return config;
    }

    @Bean
    ExecutorService mvcExecutorService() {
        return new LazyDirectExecutorService();
    }

}
