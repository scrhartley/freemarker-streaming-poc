package example.streaming.freemarker.custom.directive;

import java.util.List;
import java.util.Map;

import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

public class Streaming {

    public static boolean isAutoStreamingAllowed(Environment env) {
        boolean disabled = NoAutoStreamingDirective.isAutoStreamingDisabled(env);
        return !disabled;
    }

    public static TemplateDirectiveModel disallowAutoStreamingDirective() {
        return NoAutoStreamingDirective.SINGLETON;
    }

    public static TemplateMethodModelEx checkAutoStreamingAllowedModel() {
        return CheckAutoStreamingAllowedMethodModel.SINGLETON;
    }


    private enum NoAutoStreamingDirective implements TemplateDirectiveModel {
        SINGLETON;

        @Override
        public void execute(Environment env, Map params, TemplateModel[] loopVars, TemplateDirectiveBody body)
                throws TemplateException {
            if (!params.isEmpty()) {
                throw new TemplateModelException("This directive doesn't allow parameters.");
            }
            if (loopVars.length != 0) {
                throw new TemplateModelException("This directive doesn't allow loop variables.");
            }
            if (body != null) {
                throw new TemplateModelException("This directive doesn't allow a body");
            }

            env.setCustomState(NoAutoStreamingDirective.class, Boolean.TRUE);
        }

        static boolean isAutoStreamingDisabled(Environment env) {
            return env.getCustomState(NoAutoStreamingDirective.class) == Boolean.TRUE;
        }
    }

    private enum CheckAutoStreamingAllowedMethodModel implements TemplateMethodModelEx {
        SINGLETON;

        @Override
        public Object exec(List arguments) throws TemplateModelException {
            if (!arguments.isEmpty()) {
                throw new TemplateModelException("Arguments not allowed");
            }
            Environment env = Environment.getCurrentEnvironment();
            return isAutoStreamingAllowed(env);
        }
    }

}
