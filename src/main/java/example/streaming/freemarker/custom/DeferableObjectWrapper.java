package example.streaming.freemarker.custom;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import example.streaming.freemarker.custom.directive.Streaming;
import freemarker.core.Environment;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.Version;

public class DeferableObjectWrapper extends DefaultObjectWrapper {

    private final boolean autoFlush;

    public DeferableObjectWrapper(Version incompatibleImprovements) {
        this(incompatibleImprovements, true);
    }
    public DeferableObjectWrapper(Version incompatibleImprovements, boolean autoFlush) {
        super(incompatibleImprovements);
        this.autoFlush = autoFlush;
    }

    @Override
    protected TemplateModel handleUnknownType(Object obj) throws TemplateModelException {
        if (obj instanceof Future<?>) {
            // Similar to Callables, but potentially faster by leveraging concurrency.
            // Also, they don't require any extra caching to avoid duplicate work when there are dependencies.
            return handleFuture((Future<?>) obj);
        }
        return super.handleUnknownType(obj);
    }

    private TemplateModel handleFuture(Future<?> future) throws TemplateModelException {
        Environment env = Environment.getCurrentEnvironment();
        // Send the already finished content to the browser (streaming or chunked transfer-encoding).
        // Note: doesn't do anything in an attempt block.
        if (autoFlush && Streaming.isAutoStreamingAllowed(env)) {
            try {
                env.getOut().flush();
            } catch (IOException e) {
                throw new TemplateModelException("Failed flushing stream", e);
            }
        }
        try {
            return wrap(future.get()); // Blocking call
        } catch (ExecutionException e) {
            throw new TemplateModelException("Failure during Future's computation", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TemplateModelException("Interrupted waiting for Future", e);
        }
    }

}
