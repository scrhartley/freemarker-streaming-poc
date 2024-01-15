package example.streaming;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import example.streaming.freemarker.custom.FlushBoundaryDirective;

@Configuration
public class FreeMarkerConfig {

    @Bean
    freemarker.template.Configuration freeMarkerTemplateConfiguration(
            org.springframework.web.servlet.view.freemarker.FreeMarkerConfig configurer) {
        freemarker.template.Configuration config = configurer.getConfiguration();
        config.setSharedVariable("atom", new FlushBoundaryDirective()); // Useful macro
        return config;
    }

}
