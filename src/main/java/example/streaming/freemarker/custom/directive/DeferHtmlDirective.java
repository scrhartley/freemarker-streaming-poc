package example.streaming.freemarker.custom.directive;

import static freemarker.template.Configuration.*;

import java.io.IOException;
import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import freemarker.core.Environment;
import freemarker.core.Environment.Namespace;
import freemarker.core.Macro;
import freemarker.core.MarkupOutputFormat;
import freemarker.core.TemplateDateFormat;
import freemarker.core.TemplateMarkupOutputModel;
import freemarker.core.TemplateNumberFormat;
import freemarker.core.TemplateValueFormatException;
import freemarker.template.Template;
import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModelEx2.KeyValuePair;
import freemarker.template.TemplateHashModelEx2.KeyValuePairIterator;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;
import freemarker.template.TemplateScalarModel;

public class DeferHtmlDirective implements TemplateDirectiveModel {

    private static final String SHARED_FALLBACK_MACRO = "sharedDeferFallback";
    private static final String FALLBACK_PARAM = "fallback";
    private static final String FALLBACK_ID_PREFIX = "FMD_fb:";
    private static final Pattern MACRO_NAME_ESCAPE_CHARS = Pattern.compile("[-.:#]");
    private static final String MACRO_NAME_REPLACEMENT = "\\\\$0";
    private static final Object ID_COUNTER_STATE_KEY = new Object();
    private static final Object PENDING_STATE_KEY = new Object();
    static final String START_DATA = "FMD$";
    static final String END_DATA = "/FMD$";

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
        if (params.size() > 1) {
            throw new TemplateModelException("This directive doesn't allow multiple parameters.");
        }

        if (params.isEmpty()) {
            processMacro(SHARED_FALLBACK_MACRO, null, env, body);
        }
        else {
            @SuppressWarnings({"rawtypes", "unchecked"})
            Map.Entry entry = ((Set<Map.Entry>)params.entrySet()).iterator().next();
            if (!FALLBACK_PARAM.equals(entry.getKey())) {
                throw new TemplateModelException("Expected param " + FALLBACK_PARAM + ", found: " + entry.getKey());
            }

            Object fallback = entry.getValue();
            if (fallback instanceof Macro) {
                Macro macro = (Macro) fallback;
                String namespace = findMacroNamespace(macro, env);
                processMacro(macro.getName(), namespace, env, body);
            }
            else if (fallback instanceof TemplateScalarModel) {
                String content = ((TemplateScalarModel)fallback).getAsString();
                processString(content, env, body);
            }
            else if (fallback instanceof TemplateMarkupOutputModel<?>) {
                @SuppressWarnings("rawtypes")
                TemplateMarkupOutputModel model = (TemplateMarkupOutputModel) fallback;
                @SuppressWarnings("rawtypes")
                MarkupOutputFormat outputFormat = model.getOutputFormat();
                @SuppressWarnings("unchecked")
                String content = outputFormat.getMarkupString(model);
                processString(content, env, body);
            }
            else if (fallback instanceof TemplateNumberModel) {
                try {
                    TemplateNumberFormat format = env.getTemplateNumberFormat();
                    String content = format.formatToPlainText(((TemplateNumberModel)fallback));
                    processString(content, env, body);
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
                    processString(content, env, body);
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
    }


    private static void processString(
            String content, Environment env, TemplateDirectiveBody body) throws IOException {
        String fallbackId = getNextFallbackId(env);
        env.getOut().write(buildFallbackContent(content, fallbackId));
        addToPending(body, fallbackId, env);
    }

    private static void processMacro(
            String macroName, String namespace, Environment env, TemplateDirectiveBody body)
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

        addToPending(body, fallbackId, env);
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

    private static String getNextFallbackId(Environment env) {
        Integer counter = (Integer) env.getCustomState(ID_COUNTER_STATE_KEY);
        counter = counter==null ? 1 : counter + 1;
        env.setCustomState(ID_COUNTER_STATE_KEY, counter);
        return FALLBACK_ID_PREFIX + counter;
    }

    private static String findMacroNamespace(Macro macro, Environment env) throws TemplateModelException {
        KeyValuePairIterator it = env.getCurrentNamespace().keyValuePairIterator();
        String macroName = macro.getName();
        while (it.hasNext()) {
            KeyValuePair pair = it.next();
            TemplateModel value = pair.getValue();
            if (value instanceof Namespace) {
                Namespace namespace = (Namespace) value;
                if (namespace.get(macroName) == macro) {
                    return ((TemplateScalarModel)pair.getKey()).getAsString();
                }
            } else if (value == macro) {
                break;
            }
        }
        return null;
    }

    private static void addToPending(TemplateDirectiveBody body, String fallbackId, Environment env) {
        @SuppressWarnings("unchecked")
        LinkedHashMap<String, TemplateDirectiveBody> deferreds =
                (LinkedHashMap<String, TemplateDirectiveBody>) env.getCustomState(PENDING_STATE_KEY);
        if (deferreds == null) {
            deferreds = new LinkedHashMap<>();
            env.setCustomState(PENDING_STATE_KEY, deferreds);
        }
        deferreds.put(fallbackId, body);
    }

    static LinkedHashMap<String, TemplateDirectiveBody> getAndClearPendingItems(Environment env) {
        @SuppressWarnings("unchecked")
        LinkedHashMap<String, TemplateDirectiveBody> result =
                (LinkedHashMap<String, TemplateDirectiveBody>) env.getCustomState(PENDING_STATE_KEY);
        if (result != null) {
            env.setCustomState(PENDING_STATE_KEY, null);
        }
        return result;
    }

}