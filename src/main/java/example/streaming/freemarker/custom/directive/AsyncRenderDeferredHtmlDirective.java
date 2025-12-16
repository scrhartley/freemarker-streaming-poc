package example.streaming.freemarker.custom.directive;

import static example.streaming.freemarker.custom.directive.AsyncDeferHtmlDirective.*;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.Map;

import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateException;

public class AsyncRenderDeferredHtmlDirective extends RenderDeferredHtmlDirective {

    @Override
    protected void execute(Environment env) throws TemplateException, IOException {
        Writer out = env.getOut();

        Iterator<Map.Entry<String, TemplateDirectiveBody>> it = getAndConsumePending(env);
        while (it.hasNext()) {
            do {
                if (Streaming.isAutoStreamingAllowed(env)) {
                    out.flush(); // flush before blocking during iterator item access
                }
                render(it.next(), out);
            } while (it.hasNext());

            // Probably not necessary, but ensure new ones added due to nesting aren't missed.
            it = getAndConsumePending(env);
        }
    }

    @Override
    protected String getEndDataMarker() {
        return AsyncDeferHtmlDirective.END_DATA;
    }

}
