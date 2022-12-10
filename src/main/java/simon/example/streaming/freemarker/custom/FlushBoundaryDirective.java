package simon.example.streaming.freemarker.custom;

import freemarker.core.Environment;
import freemarker.template.*;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

// Equivalent to the following, except that exceptions will still work
// if the TemplateExceptionHandler used knows about ExceptionAwareWriter:
//    <#macro flusher>
//        <#flush>
//        <#local nestedContent><#nested></#local> <#-- Swallows flushes -->
//        ${nestedContent}
//    </#macro>
// This is useful if you're unhappy with the auto-flushing that the nested body is doing.
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

        if (body != null) {
            Writer out = env.getOut();
            out.flush();

            StringWriter tempWriter = new StringWriter();
            body.render(new ExceptionAwareWriter(tempWriter, out));
            out.write(tempWriter.toString());
        } else {
            throw new TemplateModelException("missing body");
        }
    }

}