package simon.example.streaming.freemarker.spring;

import freemarker.template.*;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

abstract class CustomFreeMarkerConfigurer extends FreeMarkerConfigurer {

    private static final Version LATEST_VERSION = Configuration.VERSION_2_3_31;

    protected final boolean productionMode;

    public CustomFreeMarkerConfigurer(boolean productionMode) {
        this.productionMode = productionMode;
    }

    @Override
    protected Configuration newConfiguration() {
        Configuration config = new Configuration(LATEST_VERSION);

        // Recommended settings by FreeMarker for new projects
        config.setDefaultEncoding("UTF-8");
        config.setLogTemplateExceptions(false);
        config.setWrapUncheckedExceptions(true);
        config.setFallbackOnNullLoopVariable(false);

        config.setTemplateExceptionHandler(productionMode ?
                TemplateExceptionHandler.RETHROW_HANDLER : TemplateExceptionHandler.HTML_DEBUG_HANDLER);

        config.setObjectWrapper(createObjectWrapper(config.getIncompatibleImprovements()));

        return config;
    }

    private ObjectWrapper createObjectWrapper(Version version) {
        DefaultObjectWrapper ow = newObjectWrapper(version);

        // Recommended settings by FreeMarker for new projects
        ow.setForceLegacyNonListCollections(false);
        ow.setDefaultDateType(TemplateDateModel.DATETIME);

        ow.writeProtect();
        return ow;
    }

    abstract protected DefaultObjectWrapper newObjectWrapper(Version version);

//    protected DefaultObjectWrapper newObjectWrapper(Version version) {
//        return new DefaultObjectWrapper(version);
//    }
}
