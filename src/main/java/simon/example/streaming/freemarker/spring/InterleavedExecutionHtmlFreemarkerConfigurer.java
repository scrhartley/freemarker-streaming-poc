package simon.example.streaming.freemarker.spring;

import freemarker.core.Environment;
import freemarker.template.*;
import simon.example.streaming.freemarker.custom.DeferredInvocationTemplateException;
import simon.example.streaming.freemarker.custom.FlushBoundaryDirective;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static simon.example.streaming.freemarker.custom.EnhancedTemplateExceptionHandlers.JS_ENHANCED_HTML_DEBUG_HANDLER;
import static simon.example.streaming.freemarker.custom.EnhancedTemplateExceptionHandlers.newMetaRefreshEnhancedRethrowHandler;

public class InterleavedExecutionHtmlFreemarkerConfigurer extends CustomFreeMarkerConfigurer {

    private static final String MY_ERROR_PAGE = "/error/500.html";

    private final boolean autoFlush;

    public InterleavedExecutionHtmlFreemarkerConfigurer(boolean productionMode) {
        this(productionMode, true);
    }
    public InterleavedExecutionHtmlFreemarkerConfigurer(boolean productionMode, boolean autoFlush) {
        super(productionMode);
        this.autoFlush = autoFlush;
    }

    @Override
    protected void postProcessConfiguration(Configuration config) {
        config.setTemplateExceptionHandler(
                productionMode ? newMetaRefreshEnhancedRethrowHandler(MY_ERROR_PAGE) : JS_ENHANCED_HTML_DEBUG_HANDLER);
        config.setSharedVariable("atom", new FlushBoundaryDirective()); // Useful macro
    }

    @Override
    protected DefaultObjectWrapper newObjectWrapper(Version version) {
        return new DefaultObjectWrapper(version) {

            @Override
            protected TemplateModel handleUnknownType(Object obj) throws TemplateModelException {
                if (obj instanceof Callable<?>) {
                    // Interleaved execution.
                    // If there are dependencies between callables, this may require the callables
                    // created in the template to implement caching in order to avoid duplicated work.
                    // Callables also have the nice property of declaring that they throw Exception,
                    // hence the Exception can get to the template directly and our handling mechanisms.
                    return handleCallable((Callable<?>) obj);
                }
                if (obj instanceof Future<?>) {
                    // Similar to Callables, but potentially faster by leveraging concurrency.
                    // Also, they don't require any extra caching to avoid duplicate work when there are dependencies.
                    return handleFuture((Future<?>) obj);
                }
                return super.handleUnknownType(obj);
            }


            private TemplateModel handleCallable(Callable<?> callable) throws TemplateModelException {
                Environment env = Environment.getCurrentEnvironment();
                try {
                    // Send the already finished content to the browser (streaming or chunked transfer-encoding).
                    // Note: doesn't do anything in an attempt block.
                    if (shouldAutoFlush(env)) {
                        env.getOut().flush();
                    }
                    return wrap(callable.call()); // Blocking call
                } catch (Exception e) {
                    // A special exception our enhanced TemplateExceptionHandlers can notice.
                    // See notes in EnhancedTemplateExceptionHandlers for why you need enhanced ones.
                    throw new DeferredInvocationTemplateException(e);
                }
            }

            private TemplateModel handleFuture(Future<?> future) throws TemplateModelException {
                Environment env = Environment.getCurrentEnvironment();
                try {
                    // Send the already finished content to the browser (streaming or chunked transfer-encoding).
                    // Note: doesn't do anything in an attempt block.
                    if (shouldAutoFlush(env)) {
                        env.getOut().flush();
                    }
                    return wrap(future.get()); // Blocking call
                } catch (ExecutionException e) {
                    throw new DeferredInvocationTemplateException(e.getCause());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new DeferredInvocationTemplateException(e);
                } catch (IOException e) {
                    throw new DeferredInvocationTemplateException(e);
                }
            }

            private boolean shouldAutoFlush(Environment env) throws TemplateModelException {
                try {
                    // This is a hack for demo purposes to easily disable auto-flushing from the template.
                    TemplateModel override = env.getGlobalVariable("AUTO_FLUSH");
                    if (override instanceof TemplateBooleanModel) { // Implicit null check
                        return ((TemplateBooleanModel) override).getAsBoolean();
                    } else {
                        return autoFlush;
                    }
                } catch (TemplateModelException e) {
                    throw new DeferredInvocationTemplateException(e);
                }
            }

        };
    }
}
