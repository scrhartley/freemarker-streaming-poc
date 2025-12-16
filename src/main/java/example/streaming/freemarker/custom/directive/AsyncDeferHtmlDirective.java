package example.streaming.freemarker.custom.directive;

import static freemarker.template.Configuration.*;

import java.io.IOException;
import java.io.StringReader;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import example.streaming.freemarker.custom.TrackedModelFutures;
import freemarker.core.Environment;
import freemarker.core.Macro;
import freemarker.core.MarkupOutputFormat;
import freemarker.core.TemplateDateFormat;
import freemarker.core.TemplateMarkupOutputModel;
import freemarker.core.TemplateNumberFormat;
import freemarker.core.TemplateValueFormatException;
import freemarker.ext.servlet.FreemarkerServlet;
import freemarker.ext.servlet.HttpRequestHashModel;
import freemarker.template.Template;
import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModelEx2;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;
import freemarker.template.TemplateScalarModel;
import freemarker.template.TemplateSequenceModel;

public class AsyncDeferHtmlDirective implements TemplateDirectiveModel {

    private static final String SHARED_FALLBACK_MACRO = "sharedDeferFallback";
    private static final String DEPENDENCIES_PARAM = "dependencies";
    private static final String FALLBACK_PARAM = "fallback";
    private static final String FALLBACK_ID_PREFIX = "AD_fb:";
    private static final Pattern MACRO_NAME_ESCAPE_CHARS = Pattern.compile("[-.:#]");
    private static final String MACRO_NAME_REPLACEMENT = "\\\\$0";
    private static final Object ID_COUNTER_STATE_KEY = new Object();
    private static final Object PENDING_STATE_KEY = new Object();
    static final String START_DATA = "AD$";
    static final String END_DATA = "/AD$";

    @Override
    public void execute(
            Environment env, Map params, TemplateModel[] loopVars, TemplateDirectiveBody body)
            throws TemplateException, IOException {
        if (body == null) {
            throw new TemplateModelException("missing body");
        }
        if (loopVars.length != 0) {
            throw new TemplateModelException("This directive doesn't allow loop variables.");
        }

        if (!params.containsKey(DEPENDENCIES_PARAM)) {
            throw new TemplateModelException("Expected parameter " + DEPENDENCIES_PARAM + " not found");
        }
        if (params.size() > 2) {
            throw new TemplateModelException("Found " + params.size() + " parameters and expected <= 2");
        }

        Set<String> dependencies = extractDependencies(params.get(DEPENDENCIES_PARAM));
        checkDependencies(dependencies, env);

        if (params.size() == 1) {
            processMacro(SHARED_FALLBACK_MACRO, null, dependencies, env, body);
        }
        else {
            @SuppressWarnings({"rawtypes", "unchecked"})
            Map.Entry entry = ((Set<Map.Entry>)params.entrySet()).iterator().next();
            if (!FALLBACK_PARAM.equals(entry.getKey())) {
                throw new TemplateModelException("Expected param " + FALLBACK_PARAM + ", found: " + entry.getKey());
            }
            processWithFallback(entry.getValue(), dependencies, body, env);
        }
    }

    private static void processWithFallback(
            Object fallback, Set<String> dependencies, TemplateDirectiveBody body, Environment env)
            throws TemplateException, IOException {
        if (fallback instanceof Macro) {
            Macro macro = (Macro) fallback;
            String namespace = findMacroNamespace(macro, env);
            processMacro(macro.getName(), namespace, dependencies, env, body);
        }
        else if (fallback instanceof TemplateScalarModel) {
            String content = ((TemplateScalarModel)fallback).getAsString();
            processString(content, dependencies, env, body);
        }
        else if (fallback instanceof TemplateMarkupOutputModel<?>) {
            @SuppressWarnings("rawtypes")
            TemplateMarkupOutputModel model = (TemplateMarkupOutputModel) fallback;
            @SuppressWarnings("rawtypes")
            MarkupOutputFormat outputFormat = model.getOutputFormat();
            @SuppressWarnings("unchecked")
            String content = outputFormat.getMarkupString(model);
            processString(content, dependencies, env, body);
        }
        else if (fallback instanceof TemplateNumberModel) {
            try {
                TemplateNumberFormat format = env.getTemplateNumberFormat();
                String content = format.formatToPlainText(((TemplateNumberModel)fallback));
                processString(content, dependencies, env, body);
            } catch (TemplateValueFormatException e) {
                String format = env.getNumberFormat();
                String quoted = format==null ? "null" : '"' + format + '"';
                throw new TemplateModelException("Failed to format number with format " + quoted, e);
            }
        }
        else if (fallback instanceof TemplateDateModel) {
            try {
                TemplateDateModel model = (TemplateDateModel) fallback;
                TemplateDateFormat format = env.getTemplateDateFormat(
                        model.getDateType(), model.getAsDate().getClass());
                String content = format.formatToPlainText(model);
                processString(content, dependencies, env, body);
            } catch (TemplateValueFormatException e) {
                String format = env.getDateFormat();
                String quoted = format==null ? "null" : '"' + format + '"';
                throw new TemplateModelException("Failed to format date/time/datetime with format " + quoted, e);
            }
        }
        else {
            throw new TemplateModelException(
                    "Unexpected type for param " + FALLBACK_PARAM + ": " + fallback.getClass().getSimpleName());
        }
    }


    private static void processString(
            String content, Set<String> dependencies, Environment env, TemplateDirectiveBody body) throws IOException {
        String fallbackId = getNextFallbackId(env);
        env.getOut().write(buildFallbackContent(content, fallbackId));
        addToPending(new PendingItem(dependencies, body, fallbackId), env);
    }

    private static void processMacro(
            String macroName, String namespace, Set<String> dependencies, Environment env, TemplateDirectiveBody body)
            throws TemplateException, IOException {
        String fallbackId = getNextFallbackId(env);
        Template currentTemplate = env.getCurrentDirectiveCallPlace().getTemplate();

        boolean squareBrackets = currentTemplate.getActualTagSyntax() == SQUARE_BRACKET_TAG_SYNTAX;
        macroName = buildFullMacroName(macroName, namespace);
        String macroInvoke = squareBrackets ? "[@" + macroName + " /]" : "<@" + macroName + " />";

        String fallback = buildFallbackContent(macroInvoke, fallbackId);
        env.include(new Template(
                null, null,
                new StringReader(fallback),
                currentTemplate.getConfiguration(),
                currentTemplate.getParserConfiguration(),
                currentTemplate.getEncoding()));

        addToPending(new PendingItem(dependencies, body, fallbackId), env);
    }

    private static String buildFullMacroName(String macroName, String namespace) {
        macroName = MACRO_NAME_ESCAPE_CHARS.matcher(macroName).replaceAll(MACRO_NAME_REPLACEMENT);
        if (namespace != null) {
            namespace = MACRO_NAME_ESCAPE_CHARS.matcher(namespace).replaceAll(MACRO_NAME_REPLACEMENT);
            macroName = namespace + '.' + macroName;
        }
        return macroName;
    }

    private static String buildFallbackContent(String fallback, String id) {
        return "<!--" + START_DATA + "-->" +
                "<template id=\"" + id + "\"></template>" + fallback +
                "<!--" + END_DATA + "-->";
    }

    private static Set<String> extractDependencies(Object param) throws TemplateModelException {
        if (param instanceof TemplateScalarModel) {
            String dep = ((TemplateScalarModel) param).getAsString();
            if (dep.contains(",")) {
                throw new TemplateModelException(
                        "Detected a string with a comma in " + DEPENDENCIES_PARAM + "; please use a sequence instead");
            }
            Set<String> dependencies = new HashSet<>(); // Needs to be mutable.
            dependencies.add(dep);
            return dependencies;
        }
        else if (param instanceof TemplateSequenceModel) {
            TemplateSequenceModel sequence = (TemplateSequenceModel) param;
            Set<String> dependencies = new HashSet<>();
            for (int i = 0; i < sequence.size(); i++) {
                TemplateModel dependency = sequence.get(i);
                if (dependency instanceof TemplateScalarModel) {
                    dependencies.add(((TemplateScalarModel) dependency).getAsString());
                } else {
                    throw new TemplateModelException(
                            "Unexpected type for " + DEPENDENCIES_PARAM + ": " + param.getClass().getSimpleName());
                }
            }
            if (dependencies.size() != sequence.size()) {
                throw new TemplateModelException("Detected duplicate dependency");
            }
            return dependencies;
        }
        else {
            throw new TemplateModelException(
                    "Unexpected type for param " + DEPENDENCIES_PARAM + ": " + param.getClass().getSimpleName());
        }
    }

    private static void checkDependencies(Set<String> dependencies, Environment env) throws TemplateModelException {
        Set<String> allowedAttributes = getFuturesState(env).getAttributeNames();

        for (String dependency : dependencies) {
            if (!allowedAttributes.contains(dependency)) {
                throw new TemplateModelException("Unknown or unsupported dependency: " + dependency);
            }
        }
    }

    private static TrackedModelFutures getFuturesState(Environment env) throws TemplateModelException {
        TemplateModel tm = env.getDataModelOrSharedVariable(FreemarkerServlet.KEY_REQUEST);
        if (tm instanceof HttpRequestHashModel) {
            HttpServletRequest request = ((HttpRequestHashModel) tm).getRequest();
            Object value = request.getAttribute(TrackedModelFutures.KEY);
            if (value == null) {
                throw new TemplateModelException("Could not find future tracking information in request");
            }
            return (TrackedModelFutures) value;
        } else {
            throw new TemplateModelException("Could not extract request from environment");
        }
    }

    private static String getNextFallbackId(Environment env) {
        Integer counter = (Integer) env.getCustomState(ID_COUNTER_STATE_KEY);
        counter = counter==null ? 1 : counter + 1;
        env.setCustomState(ID_COUNTER_STATE_KEY, counter);
        return FALLBACK_ID_PREFIX + counter;
    }

    private static String findMacroNamespace(Macro macro, Environment env) throws TemplateModelException {
        TemplateHashModelEx2.KeyValuePairIterator it = env.getCurrentNamespace().keyValuePairIterator();
        String macroName = macro.getName();
        while (it.hasNext()) {
            TemplateHashModelEx2.KeyValuePair pair = it.next();
            TemplateModel value = pair.getValue();
            if (value instanceof Environment.Namespace) {
                Environment.Namespace namespace = (Environment.Namespace) value;
                if (namespace.get(macroName) == macro) {
                    return ((TemplateScalarModel)pair.getKey()).getAsString();
                }
            } else if (value == macro) {
                break;
            }
        }
        return null;
    }

    private static void addToPending(PendingItem pending, Environment env) {
        @SuppressWarnings("unchecked")
        Set<PendingItem> deferreds = (Set<PendingItem>)
                env.getCustomState(PENDING_STATE_KEY);
        if (deferreds == null) {
            deferreds = new HashSet<>();
            env.setCustomState(PENDING_STATE_KEY, deferreds);
        }
        deferreds.add(pending);
    }

    private static class PendingItem {
        final Set<String> dependencies;
        final TemplateDirectiveBody body;
        final String id;

        public PendingItem(Set<String> dependencies, TemplateDirectiveBody body, String id) {
            this.dependencies = dependencies;
            this.body = body;
            this.id = id;
        }
    }

    static Iterator<Map.Entry<String, TemplateDirectiveBody>>
            getAndConsumePending(Environment env) throws TemplateModelException {
        @SuppressWarnings("unchecked")
        Set<PendingItem> pendingItems = (Set<PendingItem>) env.getCustomState(PENDING_STATE_KEY);
        if (pendingItems == null || pendingItems.isEmpty()) {
            return Collections.emptyIterator();
        }

        return new Iterator<>() {

            final Iterator<Collection<String>> queue = getFuturesState(env).getCompletionIterable().iterator();
            final Set<String> allResolved = new HashSet<>();
            int expectedPendingCount = pendingItems.size();

            @Override
            public boolean hasNext() {
                return !pendingItems.isEmpty() && (queue.hasNext() || findReady().isPresent());
            }

            @Override
            public Map.Entry<String, TemplateDirectiveBody> next() {
                Map.Entry<String, TemplateDirectiveBody> ready;

                // We only return one item at a time but one queue
                // dependency may cause multiple pending to be ready.
                if ((ready = nextReady()) != null) {
                    return ready;
                }

                // The caller of this method may generate a new pending item which
                // depends upon something previously returned from the queue.
                // Update any new pending with those previous dependencies and potentially return one.
                if (expectedPendingCount != pendingItems.size()) {
                    expectedPendingCount = pendingItems.size();
                    for (PendingItem item : pendingItems) {
                        item.dependencies.removeAll(allResolved);
                    }
                    if ((ready = nextReady()) != null) {
                        return ready;
                    }
                }

                while (queue.hasNext()) {
                    Collection<String> resolved = queue.next();
                    allResolved.addAll(resolved);
                    for (PendingItem item : pendingItems) {
                        item.dependencies.removeAll(resolved);
                    }
                    if ((ready = nextReady()) != null) {
                        return ready;
                    }
                }

                throw new IllegalStateException("Problem processing pending items queue");
            }

            private Optional<PendingItem> findReady() {
                return pendingItems.stream()
                        .filter(item -> item.dependencies.isEmpty())
                        .findAny();
            }

            private Map.Entry<String, TemplateDirectiveBody> nextReady() {
                return findReady()
                        .map(item -> {
                            pendingItems.remove(item);
                            expectedPendingCount--;
                            return new AbstractMap.SimpleImmutableEntry<>(item.id, item.body);
                        })
                        .orElse(null);
            }

        };
    }
}
