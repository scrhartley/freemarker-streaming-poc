package example.streaming.freemarker.custom;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import freemarker.core.Environment;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.TemplateBooleanModel;
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

}
