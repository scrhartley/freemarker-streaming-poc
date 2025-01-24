package example.streaming.freemarker.custom.directive;

import static example.streaming.freemarker.custom.directive.DeferredHtmlDirective.*;
import static org.w3c.dom.Node.*;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.Map;

import example.streaming.FreeMarkerConfig;
import example.streaming.freemarker.custom.ExceptionAwareWriter;
import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

public class TriggerDeferredHtmlDirective implements TemplateDirectiveModel {

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
        trigger(env);
    }

    private static void trigger(Environment env) throws TemplateException, IOException {
        Writer out = env.getOut();
        LinkedHashMap<String, TemplateDirectiveBody> deferredMap = getAndClearPendingItems(env);
        while (deferredMap != null && !deferredMap.isEmpty()) {
            for (Map.Entry<String, TemplateDirectiveBody> deferred : deferredMap.entrySet()) {
                if (Streaming.isAutoStreamingAllowed(env)) {
                    out.flush(); // Rendering may block, so send buffered HTML to client first.
                }

                StringWriter writer = new StringWriter();
                deferred.getValue().render(new ExceptionAwareWriter(writer, out));

                StringBuilder builder = new StringBuilder();
                if (FreeMarkerConfig.MODERN_BROWSER_ONLY) {
                    builder.append("<template>").append(writer).append("</template>");
                    builder.append("<script>(() => {");
                    appendJavaScript(builder, deferred.getKey());
                    builder.append("})();</script>");
                } else {
                    builder.append("<template hidden>").append(writer).append("</template>");
                    builder.append("<script>(function() {");
                    appendJavaScript(builder, deferred.getKey());
                    builder.append("})();</script>");
                }

                out.write(builder.toString());
            }
            deferredMap = getAndClearPendingItems(env); // May be new ones due to nesting
        }
    }

    // Replace the fallback with the real content.
    // Expect fallback to look something like:
    // <!--FMD$--><template id="fbId"></template><div>1</div><div>2</div><!--/FMD$-->
    // (with the template always empty and the fallback's nodes following it)
    private static void appendJavaScript(StringBuilder builder, String fallbackId) {
        if (FreeMarkerConfig.MODERN_BROWSER_ONLY) {
            builder.append("const self = document.currentScript;");
            builder.append("const contentNode = self.previousSibling;"); // template
            builder.append("contentNode.remove();"); // Detach from DOM tree, but keep a reference.
        } else {
            builder.append("const self = document.currentScript || document.scripts[document.scripts.length-1];");
            builder.append("const contentNode = self.previousSibling;"); // template
            builder.append("contentNode.parentNode.removeChild(contentNode);"); // Detach from DOM, keeping a reference.
        }

        builder.append("const fb = document.getElementById('").append(fallbackId).append("');");
        builder.append("if (!fb) return;");
        builder.append("const fbParent = fb.parentNode;");
        // @formatter:off
        builder.append("let node = fb.previousSibling;"); // Start from opening comment
        builder.append("do {");
        builder.append(    "if (node.nodeType === ").append(COMMENT_NODE);
        builder.append(            " && node.data === '").append(END_DATA).append("') {");
        builder.append(        "break;");
        builder.append(    "}");
        builder.append(    "const nextNode = node.nextSibling;");
        builder.append(    "fbParent.removeChild(node);"); // Gradually clear fallback
        builder.append(    "node = nextNode;");
        builder.append("} while (node);");
        // @formatter:on

        if (FreeMarkerConfig.MODERN_BROWSER_ONLY) {
            builder.append("fbParent.replaceChild(contentNode.content, node);"); // Replace end comment
            builder.append("self.remove();");
        } else {
            builder.append("if (!contentNode.content) {");
            builder.append(    "contentNode.content = document.createDocumentFragment();");
            builder.append(    "Array.prototype.slice.call(contentNode.childNodes)");
            builder.append(        ".forEach(function(node) { contentNode.content.appendChild(node) });");
            builder.append("}");
            builder.append("fbParent.replaceChild(contentNode.content, node);"); // Replace end comment
            builder.append("self.parentNode.removeChild(self);");
        }
    }

}
