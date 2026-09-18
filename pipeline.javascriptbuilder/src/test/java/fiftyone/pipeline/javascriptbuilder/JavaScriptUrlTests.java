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
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.fiftyone.flowelements.SequenceElementBuilder;
import fiftyone.pipeline.javascriptbuilder.flowelements.JavaScriptBuilderElement;
import fiftyone.pipeline.javascriptbuilder.flowelements.JavaScriptBuilderElementBuilder;
import fiftyone.pipeline.jsonbuilder.flowelements.JsonBuilderElementBuilder;
import org.junit.jupiter.api.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Checks the callback URL the rendered script is given. These tests render
 * the script only and need no browser.
 */
public class JavaScriptUrlTests {
    private final TestLoggerFactory loggerFactory;

    public JavaScriptUrlTests() {
        ILoggerFactory internalLogger = mock(ILoggerFactory.class);
        when(internalLogger.getLogger(anyString()))
            .thenReturn(mock(Logger.class));
        loggerFactory = new TestLoggerFactory(internalLogger);
    }

    private String render(String contextRoot) throws Exception {
        JavaScriptBuilderElementBuilder builder =
            new JavaScriptBuilderElementBuilder(loggerFactory)
                .setEndpoint("/json")
                .setHost("example.com")
                .setProtocol("https");
        if (contextRoot != null) {
            builder.setContextRoot(contextRoot);
        }
        JavaScriptBuilderElement jsElement = builder.build();
        Pipeline pipeline = new PipelineBuilder(loggerFactory)
            .addFlowElement(new SequenceElementBuilder(loggerFactory).build())
            .addFlowElement(new JsonBuilderElementBuilder(loggerFactory).build())
            .addFlowElement(jsElement)
            .build();
        try (FlowData flowData = pipeline.createFlowData()) {
            flowData.addEvidence("query.user-agent", "iPhone");
            flowData.addEvidence("query.id.usage", "non-marketing");
            flowData.process();
            return flowData.getFromElement(jsElement).getJavaScript();
        }
    }

    /**
     * Every URL the script requests, taken from the fetch and the
     * XMLHttpRequest calls in the rendered script.
     */
    private static List<String> requestUrls(String javaScript) {
        List<String> urls = new ArrayList<>();
        Matcher matcher = Pattern
            .compile("(?:fetch\\(|createCORSRequest\\('POST', )'([^']*)'")
            .matcher(javaScript);
        while (matcher.find()) {
            urls.add(matcher.group(1));
        }
        return urls;
    }

    /**
     * The script is given protocol, host and endpoint only, as the .NET
     * builder renders it. Query evidence reaches the script through the
     * parameters object and nowhere else, so every builder renders the same
     * script for the same request.
     */
    @Test
    public void Url_CarriesNoQueryEvidence() throws Exception {
        String javaScript = render(null);

        List<String> urls = requestUrls(javaScript);
        assertFalse(urls.isEmpty(), "No request URL found in the script.");
        for (String url : urls) {
            assertEquals("https://example.com/json", url,
                "The script's URL must carry no query string.");
        }
        // The line assigning the parameters is found without naming the
        // declaration, as JavaScriptBuilderTests does, because the template
        // has named it both "parameters" and "renderedParameters".
        String parameters = null;
        for (String line : javaScript.split("\\r?\\n")) {
            if (line.toLowerCase().contains("parameters =")
                    && line.contains("{")) {
                parameters = line;
                break;
            }
        }
        assertNotNull(parameters, "No parameters object in the script.");
        assertTrue(parameters.contains("\"user-agent\":\"iPhone\""),
            "The parameters object lost the query evidence: " + parameters);
        assertTrue(parameters.contains("\"id.usage\":\"non-marketing\""),
            "The parameters object lost the query evidence: " + parameters);
    }

    /**
     * A context root still sits between the host and the endpoint, and the
     * URL still carries no query string.
     */
    @Test
    public void Url_KeepsContextRoot() throws Exception {
        List<String> urls = requestUrls(render("/app/"));
        assertFalse(urls.isEmpty(), "No request URL found in the script.");
        for (String url : urls) {
            assertEquals("https://example.com/app/json", url);
        }
    }
}
