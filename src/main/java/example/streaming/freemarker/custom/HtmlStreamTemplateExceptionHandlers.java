package example.streaming.freemarker.custom;

import freemarker.core.Environment;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

/**
 * With normal MVC, all the data is prepared beforehand and if there's an error,
 * then the web framework can then show some error content using a completely different view.
 * With a deferred approach, we've potentially already sent some content and the web framework
 * isn't helping us anymore. Implement error handling ourselves with some HTML or JS tricks.
 */
public class HtmlStreamTemplateExceptionHandlers {

    // For production use
    // Note: Error path should prevent caching to avoid being cached as the content of the requested page.
    public static class MetaRefreshRethrowHandler implements TemplateExceptionHandler {
        private final String errorPath;

        public MetaRefreshRethrowHandler(String errorPath) {
            this.errorPath = errorPath;
        }

        @Override
        public void handleTemplateException(
                TemplateException te, Environment env, Writer out) throws TemplateException {
            if (out instanceof ExceptionAwareWriter) {
                out = ((ExceptionAwareWriter) out).getExceptionWriter();
            }

            if (!env.isInAttemptBlock()) {
                PrintWriter pw = (out instanceof PrintWriter) ? (PrintWriter) out : new PrintWriter(out);
                pw.write(String.format(
                        "<meta http-equiv=\"refresh\" content=\"0; url=%s\">", errorPath));
                // Close the stream so that browser thinks it should act upon the redirect,
                // rather than just log the incomplete stream error in the console.
                pw.close();
            }
            TemplateExceptionHandler.RETHROW_HANDLER.handleTemplateException(te, env, out);
        }
    }


    // For development use
    public static final TemplateExceptionHandler JS_ENHANCED_HTML_DEBUG_HANDLER = new TemplateExceptionHandler() {
        private final TemplateExceptionHandler BASE_HANDLER = TemplateExceptionHandler.HTML_DEBUG_HANDLER;
        @Override
        public void handleTemplateException(
                TemplateException te, Environment env, Writer out) throws TemplateException {
            if (out instanceof ExceptionAwareWriter) {
                out = ((ExceptionAwareWriter) out).getExceptionWriter();
            }

            if (!env.isInAttemptBlock()) {
                // Pass TemplateExceptionHandler a different writer so that we don't care if it closes it or not,
                // and we can still write some JavaScript ourselves afterwards.
                // Unfortunately, document.body.innerHTML='' before the TemplateExceptionHandler doesn't work.
                StringWriter outCapture = new StringWriter();
                try {
                    BASE_HANDLER.handleTemplateException(te, env, outCapture);
                } catch (TemplateException baseTemplateException) { // Always thrown
                    throw te;
                } finally {
                    PrintWriter pw = (out instanceof PrintWriter) ? (PrintWriter) out : new PrintWriter(out);
                    pw.write(outCapture.toString());
                    // Clear page so there's only the error message.
                    pw.write(
                            "<script>(self => setTimeout(() =>\n" +
                                    "document.body.innerHTML = self.previousElementSibling.outerHTML\n" +
                                "))(document.currentScript);</script>");
                    //  Close the stream so that browser thinks it should render the debug HTML,
                    //  rather than just log the incomplete stream error in the console.
                    pw.close();
                }
            } else {
                BASE_HANDLER.handleTemplateException(te, env, out);
            }
            throw te; // Should never be reached
        }
    };

}
