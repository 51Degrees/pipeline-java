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

import fiftyone.common.testhelpers.TestLoggerFactory;
import fiftyone.pipeline.core.data.ElementPropertyMetaData;
import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import fiftyone.pipeline.core.data.EvidenceKeyFilterWhitelist;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.FlowElementBase;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.engines.fiftyone.flowelements.SequenceElementBuilder;
import fiftyone.pipeline.javascriptbuilder.flowelements.JavaScriptBuilderElement;
import fiftyone.pipeline.javascriptbuilder.flowelements.JavaScriptBuilderElementBuilder;
import fiftyone.pipeline.jsonbuilder.flowelements.JsonBuilderDataInternal;
import fiftyone.pipeline.jsonbuilder.flowelements.JsonBuilderElementBuilder;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static fiftyone.pipeline.engines.fiftyone.flowelements.Constants.EVIDENCE_SEQUENCE;
import static fiftyone.pipeline.engines.fiftyone.flowelements.Constants.EVIDENCE_SESSIONID;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Checks the session id and the sequence the builder writes into the
 * script. The session id is written inside double quotes and the sequence
 * as a number, so a value that could break the script is replaced, with an
 * empty session id or a sequence of 1. Each script is parsed with the
 * GraalVM JavaScript engine, without running it, so no browser is needed.
 * <p>
 * The JSON builder refuses to run without a numeric sequence in the
 * evidence, which in practice means without the Sequence Element. So that
 * the builder's own handling can be tested, the tests "without the Sequence
 * Element" use a stand in for the JSON builder that supplies the payload and
 * leaves the evidence as it was given.
 */
public class SessionIdAndSequenceTests {

    private static final Pattern SESSION_ID_LINE =
        Pattern.compile("var sessionId = \"(.*)\";");

    private static final Pattern SEQUENCE_LINE =
        Pattern.compile("var sequence = (.*);");

    private final TestLoggerFactory loggerFactory =
        new TestLoggerFactory(null);

    /**
     * Supplies an empty JSON payload in place of the JSON builder, without
     * reading the evidence.
     */
    private static class JsonStandIn extends
        FlowElementBase<JsonBuilderDataInternal, ElementPropertyMetaData> {

        JsonStandIn(Logger logger) {
            super(logger, null);
        }

        @Override
        protected void processInternal(FlowData data) {
            JsonBuilderDataInternal json =
                new JsonBuilderDataInternal(logger, data);
            json.put("json", "{\"javascriptProperties\":[]}");
            data.getOrAdd(getElementDataKey(), f -> json);
        }

        @Override
        public String getElementDataKey() {
            return "json-builder";
        }

        @Override
        public EvidenceKeyFilter getEvidenceKeyFilter() {
            return new EvidenceKeyFilterWhitelist(
                Collections.<String>emptyList());
        }

        @Override
        public List<ElementPropertyMetaData> getProperties() {
            return Collections.emptyList();
        }

        @Override
        protected void managedResourcesCleanup() {
        }

        @Override
        protected void unmanagedResourcesCleanup() {
        }
    }

    @Test
    public void SessionId_Valid_Rendered() throws Exception {
        String script = render(false, "abc-123", null);
        assertEquals("abc-123", sessionId(script));
        assertEquals("1", sequence(script));
    }

    @Test
    public void SessionId_LongestValid_Rendered() throws Exception {
        String id = repeat('a', 64);
        String script = render(false, id, null);
        assertEquals(id, sessionId(script));
    }

    static Stream<String> invalidSessionIds() {
        return Stream.of(
            "a\"b",
            "a\\b",
            "</script>",
            repeat('a', 65),
            "caf\u00e9",
            "");
    }

    @ParameterizedTest
    @MethodSource("invalidSessionIds")
    public void SessionId_Invalid_RenderedEmpty(String id) throws Exception {
        String script = render(false, id, null);
        assertEquals("", sessionId(script));
    }

    /**
     * The Sequence Element keeps a session id that is already in the
     * evidence, so an invalid one reaches the builder that way too.
     */
    @ParameterizedTest
    @MethodSource("invalidSessionIds")
    public void SessionId_InvalidWithSequenceElement_RenderedEmpty(String id)
        throws Exception {
        String script = render(true, id, 1);
        assertEquals("", sessionId(script));
        assertEquals("2", sequence(script));
    }

    @Test
    public void SessionId_FromSequenceElement_Rendered() throws Exception {
        String script = render(true, null, null);
        assertTrue(sessionId(script).matches("[A-Za-z0-9-]{1,64}"),
            sessionId(script));
        assertEquals("1", sequence(script));
    }

    @Test
    public void NoSequenceElement_NoEvidence_DefaultsRendered()
        throws Exception {
        String script = render(false, null, null);
        assertEquals("", sessionId(script));
        assertEquals("1", sequence(script));
    }

    /**
     * A sequence from a web request is text. With no Sequence Element it
     * reaches the builder as it is.
     */
    @ParameterizedTest
    @ValueSource(strings = {"abc", "-1", "0", "99999999999", "", "2147483648"})
    public void Sequence_InvalidText_RenderedOne(String value)
        throws Exception {
        String script = render(false, "abc-123", value);
        assertEquals("1", sequence(script));
    }

    static Stream<Object> invalidSequenceNumbers() {
        return Stream.of(-1, 0, Integer.MIN_VALUE, 99999999999L);
    }

    @ParameterizedTest
    @MethodSource("invalidSequenceNumbers")
    public void Sequence_InvalidNumber_RenderedOne(Object value)
        throws Exception {
        String script = render(false, "abc-123", value);
        assertEquals("1", sequence(script));
    }

    static Stream<Object[]> validSequences() {
        return Stream.of(
            new Object[] { 5, "5" },
            new Object[] { "5", "5" },
            new Object[] { "2147483647", "2147483647" },
            new Object[] { Integer.MAX_VALUE, "2147483647" });
    }

    @ParameterizedTest
    @MethodSource("validSequences")
    public void Sequence_Valid_Rendered(Object value, String expected)
        throws Exception {
        String script = render(false, "abc-123", value);
        assertEquals(expected, sequence(script));
    }

    /**
     * The Sequence Element adds one to a numeric sequence before the builder
     * sees it, so -1 reaches the builder as 0 and the largest integer wraps
     * round to the smallest. Both are rendered as 1. A sequence given as
     * text never reaches the builder when the Sequence Element is present,
     * because that element only accepts a number.
     */
    @ParameterizedTest
    @ValueSource(ints = {-1, Integer.MAX_VALUE})
    public void Sequence_InvalidWithSequenceElement_RenderedOne(int value)
        throws Exception {
        String script = render(true, "abc-123", value);
        assertEquals("abc-123", sessionId(script));
        assertEquals("1", sequence(script));
    }

    /**
     * Builds a pipeline with the Sequence Element and the JSON builder, or
     * with the stand in for them, processes it
     * with the evidence given, checks the script parses and returns it.
     * A null value means the evidence is not added.
     */
    private String render(
        boolean sequenceElement,
        String sessionId,
        Object sequence) throws Exception {
        JavaScriptBuilderElement jsElement =
            new JavaScriptBuilderElementBuilder(loggerFactory).build();
        PipelineBuilder builder = new PipelineBuilder(loggerFactory);
        if (sequenceElement) {
            builder.addFlowElement(
                new SequenceElementBuilder(loggerFactory).build());
            builder.addFlowElement(
                new JsonBuilderElementBuilder(loggerFactory).build());
        } else {
            builder.addFlowElement(
                new JsonStandIn(loggerFactory.getLogger("json")));
        }
        builder.addFlowElement(jsElement);
        String script;
        try (Pipeline pipeline = builder.build();
            FlowData flowData = pipeline.createFlowData()) {
            if (sessionId != null) {
                flowData.addEvidence(EVIDENCE_SESSIONID, sessionId);
            }
            if (sequence != null) {
                flowData.addEvidence(EVIDENCE_SEQUENCE, sequence);
            }
            flowData.process();
            script = flowData.getFromElement(jsElement).getJavaScript();
        }
        assertParses(script);
        return script;
    }

    private static void assertParses(String script) {
        try (Context context = Context.newBuilder("js")
            .option("engine.WarnInterpreterOnly", "false")
            .build()) {
            // Parsing does not run the script.
            context.parse(Source.create("js", script));
        } catch (PolyglotException e) {
            fail("the script does not parse: " + e.getMessage());
        }
    }

    private static String sessionId(String script) {
        return single(SESSION_ID_LINE, script);
    }

    private static String sequence(String script) {
        return single(SEQUENCE_LINE, script);
    }

    private static String single(Pattern pattern, String script) {
        Matcher matcher = pattern.matcher(script);
        assertTrue(matcher.find(), "no match for " + pattern);
        String value = matcher.group(1);
        assertFalse(matcher.find(), "more than one match for " + pattern);
        return value;
    }

    private static String repeat(char c, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(c);
        }
        return sb.toString();
    }
}
