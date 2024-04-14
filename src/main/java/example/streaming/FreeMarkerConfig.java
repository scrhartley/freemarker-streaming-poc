package example.streaming;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import example.streaming.freemarker.custom.directive.DeferredHtmlDirective;
import example.streaming.freemarker.custom.directive.FlushBoundaryDirective;
import example.streaming.freemarker.custom.directive.TriggerDeferredHtmlDirective;
import freemarker.template.TemplateBooleanModel;

@Configuration
public class FreeMarkerConfig {

    public static final boolean MODERN_BROWSER_ONLY = false; // false for IE11 support

    @Bean @Lazy(false)
    freemarker.template.Configuration freeMarkerTemplateConfiguration(
            org.springframework.web.servlet.view.freemarker.FreeMarkerConfig configurer) {
        freemarker.template.Configuration config = configurer.getConfiguration();
        config.setSharedVariable("_$_MODERN_BROWSER___SUSPEND_MACRO_$_",
                MODERN_BROWSER_ONLY ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE);
        config.setSharedVariable("atom", new FlushBoundaryDirective());
        config.setSharedVariable("deferred", new DeferredHtmlDirective());
        config.setSharedVariable("triggerDeferred", new TriggerDeferredHtmlDirective());
        return config;
    }

}
