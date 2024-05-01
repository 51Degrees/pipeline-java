package fiftyone.pipeline.javascriptbuilder;

import fiftyone.common.testhelpers.TestLoggerFactory;
import fiftyone.pipeline.core.data.*;
import fiftyone.pipeline.core.flowelements.FlowElementBase;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.fiftyone.flowelements.SequenceElement;
import fiftyone.pipeline.engines.fiftyone.flowelements.SequenceElementBuilder;
import fiftyone.pipeline.javascriptbuilder.flowelements.JavaScriptBuilderElement;
import fiftyone.pipeline.javascriptbuilder.flowelements.JavaScriptBuilderElementBuilder;
import fiftyone.pipeline.jsonbuilder.flowelements.JsonBuilderElement;
import fiftyone.pipeline.jsonbuilder.flowelements.JsonBuilderElementBuilder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CookieTests {
    private TestLoggerFactory loggerFactory;
    public CookieTests() {
        ILoggerFactory internalLogger = mock(ILoggerFactory.class);
        when(internalLogger.getLogger(anyString())).thenReturn(mock(Logger.class));
        loggerFactory = new TestLoggerFactory(internalLogger);
    }

    class CookieData extends ElementDataBase {

        public CookieData(Logger logger, FlowData flowData) {
            super(logger, flowData);
        }
    }

    class CookieElement extends FlowElementBase<CookieData, ElementPropertyMetaDataDefault> {

        public CookieElement(Logger logger) {
            super(logger, null);
        }

        @Override
        protected void processInternal(FlowData data) throws Exception {
            CookieData result = new CookieData(this.logger, data);
            result.put("javascript", "document.cookie =  \"some cookie value\"");
            data.getOrAdd(getElementDataKey(), d -> result);
        }

        @Override
        public String getElementDataKey() {
            return "cookie";
        }

        @Override
        public EvidenceKeyFilter getEvidenceKeyFilter() {
            return new EvidenceKeyFilterWhitelist(Arrays.asList());
        }

        @Override
        public List<ElementPropertyMetaDataDefault> getProperties() {
            return Arrays.asList(
                new ElementPropertyMetaDataDefault(
                    "javascript",
                    this,
                    "category",
                    String.class,
                    true));
        }

        @Override
        protected void managedResourcesCleanup() {

        }

        @Override
        protected void unmanagedResourcesCleanup() {

        }
    }

    /**
     * Test various configurations for enabling cookies to verify
     * that cookies are/aren't written for each configuration.
     *
     * The source JavaScript contains code to set a cookie. The JSBuilder
     * element should replace this if the config says that cookies are not
     * enabled.
     * @param enableInConfig True if cookies are enabled in the element configuration.
     * @param enableInEvidence True if cookies are enabled in the evidence.
     * @param expectCookie True if the test should expect cookies to be enabled for this configuration.
     */
    @ParameterizedTest
    @CsvSource(
        {"false, false, false",
        "true, false, false",
        "false, true, true",
        "true, true, true"})
    public void TestJavaScriptCookies(
        boolean enableInConfig,
        boolean enableInEvidence,
        boolean expectCookie) throws Exception {
        // Arrange
        CookieElement cookieElement = new CookieElement(loggerFactory.getLogger("cookie"));
        SequenceElement sequenceElement = new SequenceElementBuilder(loggerFactory)
            .build();
        JsonBuilderElement jsonElement = new JsonBuilderElementBuilder(loggerFactory)
            .build();
        JavaScriptBuilderElement jsElement = new JavaScriptBuilderElementBuilder(loggerFactory)
            .setEnableCookies(enableInConfig)
            .build();
        Pipeline pipeline = new PipelineBuilder(loggerFactory)
            .addFlowElement(cookieElement)
            .addFlowElement(sequenceElement)
            .addFlowElement(jsonElement)
            .addFlowElement(jsElement)
            .build();

        // Act
        String javaScript = null;
        try (FlowData flowData = pipeline.createFlowData())
        {
            flowData.addEvidence(
                Constants.EVIDENCE_ENABLE_COOKIES,
                Boolean.toString(enableInEvidence));
            flowData.process();
            javaScript = flowData.getFromElement(jsElement).getJavaScript();
        }

        // Assert
        Pattern cookieRegex = Pattern.compile("document\\.cookie");
        Matcher matcher = cookieRegex.matcher(javaScript);
        long count = 0;
        while (matcher.find()) {
            count++;
        }
        if (expectCookie)
        {
            assertEquals(
                2,
                count,
                "The original script to set cookies should not have been replaced.");
        }
        else
        {
            assertEquals(
                1,
                count,
                "The original script to set cookies should have been replaced.");
        }
    }
}
