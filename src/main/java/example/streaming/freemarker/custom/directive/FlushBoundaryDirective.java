package example.streaming.freemarker.custom.directive;

import example.streaming.freemarker.custom.ExceptionAwareWriter;
import freemarker.core.Environment;
import freemarker.template.*;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

// Equivalent to the following, except that exceptions will still work provided
// that the TemplateExceptionHandler used knows about ExceptionAwareWriter:
//    <#macro flusher>
//        <#flush>
//        <#local nestedContent><#nested></#local> <#-- Swallows flushes -->
//        ${nestedContent}
//    </#macro>
// This is useful if you're unhappy with auto-flushing that the nested body is doing.
public class FlushBoundaryDirective implements TemplateDirectiveModel {

    @Override
    public void execute(Environment env, Map params, TemplateModel[] loopVars, TemplateDirectiveBody body)
            throws TemplateException, IOException {
        if (!params.isEmpty()) {
            throw new TemplateModelException("This directive doesn't allow parameters.");
        }
        if (loopVars.length != 0) {
            throw new TemplateModelException("This directive doesn't allow loop variables.");
        }
        if (body == null) {
            throw new TemplateModelException("missing body");
        }

        Writer out = env.getOut();
        if (Streaming.isAutoStreamingAllowed(env)) {
            out.flush();
        }

        StringWriter tempWriter = new StringWriter();
        body.render(new ExceptionAwareWriter(tempWriter, out));
        out.write(tempWriter.toString());
    }

}