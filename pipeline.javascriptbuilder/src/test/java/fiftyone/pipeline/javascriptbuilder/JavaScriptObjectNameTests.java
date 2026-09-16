/* *********************************************************************
 * This Original Work is copyright of 51 Degrees Mobile Experts Limited.
 * Copyright 2026 51 Degrees Mobile Experts Limited, Davidson House,
 * Forbury Square, Reading, Berkshire, United Kingdom RG1 3EU.
 *
 * This Original Work is licensed under the European Union Public Licence
 * (EUPL) v.1.2 and is subject to its terms as set out below.
 *
 * If a copy of the EUPL was not distributed with this file, You can obtain
 * one at https://opensource.org/licenses/EUPL-1.2.
 *
 * The 'Compatible Licences' set out in the Appendix to the EUPL (as may be
 * amended by the European Commission) shall be deemed incompatible for
 * the purposes of the Work and the provisions of the compatibility
 * clause in Article 5 of the EUPL shall not apply.
 *
 * If using the Work as, or as part of, a network application, by
 * including the attribution notice(s) required under Article 5 of the EUPL
 * in the end user terms of the application under an appropriate heading,
 * such notice(s) shall fulfill the requirements of that article.
 * ********************************************************************* */

package fiftyone.pipeline.javascriptbuilder;

import fiftyone.common.testhelpers.TestLogger;
import fiftyone.common.testhelpers.TestLoggerFactory;
import fiftyone.pipeline.core.data.Evidence;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.TryGetResult;
import fiftyone.pipeline.core.exceptions.PipelineConfigurationException;
import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import fiftyone.pipeline.engines.data.AspectPropertyValueDefault;
import fiftyone.pipeline.javascriptbuilder.data.JavaScriptBuilderData;
import fiftyone.pipeline.javascriptbuilder.flowelements.JavaScriptBuilderElement;
import fiftyone.pipeline.javascriptbuilder.flowelements.JavaScriptBuilderElementBuilder;
import fiftyone.pipeline.jsonbuilder.data.JsonBuilderData;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static fiftyone.pipeline.javascriptbuilder.Constants.EVIDENCE_OBJECT_NAME;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Checks that the client side object can be given a different name, from the
 * builder and from the page request's evidence, and that a name which is not
 * a valid JavaScript identifier is never written into the script. The
 * rendered script is parsed and then run with the GraalVM JavaScript engine
 * against a minimal window, document and session storage, so unlike
 * {@link JavaScriptBuilderTests} no browser is needed.
 */
public class JavaScriptObjectNameTests {

    /**
     * The payload the script is rendered with, so a value can be read back
     * through the renamed object.
     */
    private static final String JSON =
        "{\"device\":{\"ismobile\":true},\"javascriptProperties\":[]}";

    /**
     * Stands in for the parts of a browser the script touches when it has no
     * endpoint to call. The script refers to the global object as window, so
     * a top level var is also a property of window.
     */
    private static final String BROWSER =
        "var window = globalThis;\n" +
        "var console = { log: function() {}, warn: function() {},\n" +
        "    error: function() {} };\n" +
        "var document = { cookie: '' };\n" +
        "var sessionStorage = (function() {\n" +
        "    var data = {};\n" +
        "    return {\n" +
        "        get length() { return Object.keys(data).length; },\n" +
        "        key: function(i) {\n" +
        "            var k = Object.keys(data)[i];\n" +
        "            return k === undefined ? null : k; },\n" +
        "        getItem: function(k) {\n" +
        "            return k in data ? data[k] : null; },\n" +
        "        setItem: function(k, v) { data[k] = String(v); },\n" +
        "        removeItem: function(k) { delete data[k]; }\n" +
        "    };\n" +
        "})();\n";

    private TestLoggerFactory loggerFactory;
    private Map<String, Object> evidence;
    private JavaScriptBuilderData result;

    @BeforeEach
    public void init() {
        loggerFactory = new TestLoggerFactory(null);
        evidence = new HashMap<>();
        result = null;
    }

    @Test
    public void ObjectName_FromBuilder_UsedThroughout() {
        JavaScriptBuilderElement element =
            new JavaScriptBuilderElementBuilder(loggerFactory)
                .setObjectName("myFod")
                .build();

        String script = render(element);

        assertWorkingObject(script, "myFod");
        assertEquals(0, warnings().size());
    }

    @Test
    public void ObjectName_FromEvidence_UsedThroughout() {
        JavaScriptBuilderElement element =
            new JavaScriptBuilderElementBuilder(loggerFactory).build();
        evidence.put(EVIDENCE_OBJECT_NAME, "myFod");

        String script = render(element);

        assertWorkingObject(script, "myFod");
        assertEquals(0, warnings().size());
    }

    /**
     * Each invalid name, with text that would only be in the script if the
     * requested name had been written into it. Mustache escapes quotes, so
     * the escaped form is listed as well as the raw one.
     */
    static Stream<Arguments> invalidEvidenceNames() {
        return Stream.of(
            Arguments.of("a;b//", new String[] { "a;b", "var a;" }),
            Arguments.of("9bad", new String[] { "9bad" }),
            Arguments.of("x\"y",
                new String[] { "x\"y", "x&quot;y", "var x&", "var x\"" }),
            Arguments.of("", new String[] { "var  =" }),
            Arguments.of("class", new String[] { "var class" }),
            Arguments.of("fiftyoneDegreesManager",
                new String[] { "var fiftyoneDegreesManager" }));
    }

    @ParameterizedTest
    @MethodSource("invalidEvidenceNames")
    public void ObjectName_InvalidFromEvidence_DefaultUsed(
        String name,
        String[] absent) {
        JavaScriptBuilderElement element =
            new JavaScriptBuilderElementBuilder(loggerFactory).build();
        evidence.put(EVIDENCE_OBJECT_NAME, name);

        String script = render(element);

        assertWorkingObject(script, Constants.DEFAULT_OBJECT_NAME);
        String rest = withoutParameters(script);
        for (String text : absent) {
            assertFalse(rest.contains(text),
                "the script should not contain '" + text + "'");
        }
        assertEquals(1, count(script, "new fiftyoneDegreesManager()"));
        List<String> warnings = warnings();
        assertEquals(1, warnings.size(), String.join("\n", warnings));
        assertTrue(warnings.get(0).contains(
            "not a valid JavaScript identifier"), warnings.get(0));
    }

    @Test
    public void ObjectName_InvalidFromEvidence_ConfiguredNameUsed() {
        JavaScriptBuilderElement element =
            new JavaScriptBuilderElementBuilder(loggerFactory)
                .setObjectName("myFod")
                .build();
        evidence.put(EVIDENCE_OBJECT_NAME, "9bad");

        String script = render(element);

        assertWorkingObject(script, "myFod");
        assertFalse(withoutParameters(script).contains("9bad"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "a;b", "9bad", "x\"y", "", "var", "fiftyoneDegreesManager"})
    public void ObjectName_InvalidFromBuilder_Refused(String name) {
        assertThrows(
            PipelineConfigurationException.class,
            () -> new JavaScriptBuilderElementBuilder(loggerFactory)
                .setObjectName(name)
                .build());
    }

    /**
     * The element's constructor is public, so a name that does not come
     * through the builder is checked as well. An empty name still means the
     * default, as it always has.
     */
    @ParameterizedTest
    @ValueSource(strings = {
        "a;b", "9bad", "x\"y", "var", "fiftyoneDegreesManager"})
    public void ObjectName_InvalidFromConstructor_Refused(String name) {
        assertThrows(
            PipelineConfigurationException.class,
            () -> new JavaScriptBuilderElement(
                loggerFactory.getLogger("test"),
                null,
                "",
                name,
                false,
                "",
                ""));
    }

    private void assertWorkingObject(String script, String name) {
        assertTrue(Pattern.compile(
                "var " + Pattern.quote(name) +
                " = new fiftyoneDegreesManager\\(\\);")
                .matcher(script).find(),
            "the script should declare var " + name);
        if (name.equals("fod") == false) {
            assertFalse(Pattern.compile("var fod\\b").matcher(script).find(),
                "the script should not declare var fod");
        }
        assertTrue(script.contains("var sessionKey = \"" + name + "\";"),
            "the storage key should be " + name);
        assertTrue(script.contains("window[\"" + name + "Evidence\"]"),
            "the evidence lookup should use " + name);

        try (Context context = Context.newBuilder("js")
            .option("engine.WarnInterpreterOnly", "false")
            .build()) {
            Source source = Source.create("js", script);
            try {
                // Parsing does not run the script.
                context.parse(source);
            } catch (PolyglotException e) {
                fail("the script does not parse: " + e.getMessage());
            }

            context.eval("js", BROWSER);
            context.eval(source);

            Value window = context.eval("js", "window");
            assertTrue(window.hasMember(name),
                "window." + name + " should exist");
            Value obj = window.getMember(name);
            assertFalse(obj.isNull(), "window." + name + " should be set");
            assertTrue(obj.getMember("complete").canExecute());
            assertTrue(obj.getMember("onChange").canExecute());
            assertTrue(obj.getMember("refresh").canExecute());
            assertTrue(obj.getMember("device")
                .getMember("ismobile").asBoolean());
        }
    }

    private List<String> warnings() {
        List<String> warnings = new ArrayList<>();
        for (TestLogger logger : loggerFactory.loggers) {
            warnings.addAll(logger.warningsLogged);
        }
        return warnings;
    }

    /**
     * The script with the line that assigns its request parameters taken
     * out. The script sends the page's query evidence, the requested object
     * name included, back with its own request. The builder url encodes
     * each value and the line holds them as JSON strings, so the requested
     * name there is data and not code, and it is left out of the checks
     * that the name was not written into the code. The line is found as the
     * existing tests find it, by the word and a brace, because the template
     * has named the declaration differently over time.
     */
    private static String withoutParameters(String script) {
        StringBuilder rest = new StringBuilder();
        for (String line : script.split("\\r?\\n")) {
            if (line.toLowerCase().contains("parameters =") &&
                line.contains("{")) {
                continue;
            }
            rest.append(line).append('\n');
        }
        return rest.toString();
    }

    private static int count(String text, String find) {
        Matcher matcher = Pattern.compile(Pattern.quote(find)).matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    /**
     * Processes the element against a mocked flow data holding the payload
     * and the evidence, and returns the script. No host or endpoint is set,
     * so the script makes no requests when it runs.
     */
    @SuppressWarnings("unchecked")
    private String render(JavaScriptBuilderElement element) {
        FlowData flowData = mock(FlowData.class);

        JsonBuilderData json = mock(JsonBuilderData.class);
        when(json.getJson()).thenReturn(JSON);
        when(flowData.get(JsonBuilderData.class)).thenReturn(json);

        Evidence evidenceObj = mock(Evidence.class);
        when(evidenceObj.asKeyMap()).thenReturn(evidence);
        when(flowData.getEvidence()).thenReturn(evidenceObj);
        when(flowData.tryGetEvidence(anyString(), any(Class.class)))
            .thenAnswer(invocation -> {
                TryGetResult<Object> getResult = new TryGetResult<>();
                String key = invocation.getArgument(0);
                if (evidence.containsKey(key)) {
                    getResult.setValue(evidence.get(key));
                }
                return getResult;
            });
        when(flowData.getAs(anyString(), eq(AspectPropertyValue.class)))
            .thenReturn(new AspectPropertyValueDefault<>("None"));
        doAnswer(invocation -> {
            FlowElement.DataFactory<JavaScriptBuilderData> factory =
                invocation.getArgument(1);
            result = factory.create(flowData);
            return result;
        }).when(flowData).getOrAdd(
            anyString(),
            any(FlowElement.DataFactory.class));

        try {
            element.process(flowData);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return result.getJavaScript();
    }
}
