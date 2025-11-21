package example.streaming.freemarker.custom.directive;

import static example.streaming.freemarker.custom.directive.DeferHtmlDirective.*;
import static org.w3c.dom.Node.*;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.Map;

import example.streaming.freemarker.custom.ExceptionAwareWriter;
import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

public class RenderDeferredHtmlDirective implements TemplateDirectiveModel {

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
            throw new TemplateModelException("This directive doesn't allow a body");
        }
        execute(env);
    }

    protected void execute(Environment env) throws TemplateException, IOException {
        Writer out = env.getOut();
        LinkedHashMap<String, TemplateDirectiveBody> deferredMap = getAndClearPendingItems(env);
        while (deferredMap != null && !deferredMap.isEmpty()) {
            for (Map.Entry<String, TemplateDirectiveBody> deferred : deferredMap.entrySet()) {
                if (Streaming.isAutoStreamingAllowed(env)) {
                    out.flush(); // Rendering may block, so send buffered HTML to client first.
                }
                render(deferred, out);
            }
            deferredMap = getAndClearPendingItems(env); // May be new ones due to nesting
        }
    }

    protected void render(
            Map.Entry<String, TemplateDirectiveBody> deferred, Writer out) throws TemplateException, IOException {
        StringWriter writer = new StringWriter();
        deferred.getValue().render(new ExceptionAwareWriter(writer, out));

        StringBuilder builder = new StringBuilder();
        builder.append("<template>").append(writer).append("</template>");
        builder.append("<script>(() => {");
        appendJavaScript(builder, deferred.getKey());
        builder.append("})();</script>");

        out.write(builder.toString());
    }

    // Replace the fallback with the real content.
    // Expect fallback to look something like:
    // <!--FMD$--><template id="fbId"></template><div>1</div><div>2</div><!--/FMD$-->
    // (with the template always empty and the fallback's nodes following it)
    private void appendJavaScript(StringBuilder builder, String fallbackId) {
        builder.append("const self = document.currentScript;");
        builder.append("const contentNode = self.previousSibling;"); // template
        builder.append("contentNode.remove();"); // Detach from DOM tree, but keep a reference.

        builder.append("const fb = document.getElementById('").append(fallbackId).append("');");
        builder.append("if (!fb) return;");
        builder.append("const fbParent = fb.parentNode;");
        // @formatter:off
        builder.append("let node = fb.previousSibling;"); // Start from opening comment
        builder.append("do {");
        builder.append(    "if (node.nodeType === ").append(COMMENT_NODE);
        builder.append(            " && node.data === '").append(getEndDataMarker()).append("') {");
        builder.append(        "break;");
        builder.append(    "}");
        builder.append(    "const nextNode = node.nextSibling;");
        builder.append(    "fbParent.removeChild(node);"); // Gradually clear fallback
        builder.append(    "node = nextNode;");
        builder.append("} while (node);");
        // @formatter:on

        builder.append("fbParent.replaceChild(contentNode.content, node);"); // Replace end comment
        builder.append("self.remove();");
    }

    protected String getEndDataMarker() {
        return DeferHtmlDirective.END_DATA;
    }

}
