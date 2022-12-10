package simon.example.streaming.freemarker.custom;

import freemarker.template.TemplateModelException;

/**
 * A special exception our enhanced TemplateExceptionHandlers can notice.
 * See notes in EnhancedTemplateExceptionHandlers for why you need enhanced ones.
 * @see EnhancedTemplateExceptionHandlers
 */
public class DeferredInvocationTemplateException extends TemplateModelException {
    public DeferredInvocationTemplateException(Throwable e) {
        super(e);
    }
}
