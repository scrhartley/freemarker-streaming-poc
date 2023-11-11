package example.streaming;

import org.springframework.boot.autoconfigure.freemarker.FreeMarkerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactory;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;
import example.streaming.freemarker.spring.InterleavedExecutionHtmlFreemarkerConfigurer;

import java.util.Properties;

@Configuration
public class FreeMarkerConfig {

    private static final boolean PRODUCTION = false; // Prod behaviour or best for dev/debug?

    @Bean
    FreeMarkerConfigurer getFreeMarkerConfigurer(FreeMarkerProperties properties) {
        FreeMarkerConfigurer configurer = new InterleavedExecutionHtmlFreemarkerConfigurer(PRODUCTION);
        applyProperties(configurer, properties);
        return configurer;
    }


    // Copied from Spring's AbstractFreeMarkerConfiguration.
    private static void applyProperties(FreeMarkerConfigurationFactory factory, FreeMarkerProperties properties) {
        factory.setTemplateLoaderPaths(properties.getTemplateLoaderPath());
        factory.setPreferFileSystemAccess(properties.isPreferFileSystemAccess());
        factory.setDefaultEncoding(properties.getCharsetName());
        Properties settings = new Properties();
        settings.put("recognize_standard_file_extensions", "true");
        settings.putAll(properties.getSettings());
        factory.setFreemarkerSettings(settings);
    }

}
