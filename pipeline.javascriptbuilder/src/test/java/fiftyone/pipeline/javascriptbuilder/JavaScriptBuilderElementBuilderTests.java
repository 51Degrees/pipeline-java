package fiftyone.pipeline.javascriptbuilder;

import fiftyone.common.testhelpers.TestLoggerFactory;
import fiftyone.pipeline.core.exceptions.PipelineConfigurationException;
import fiftyone.pipeline.javascriptbuilder.flowelements.JavaScriptBuilderElementBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JavaScriptBuilderElementBuilderTests {

    private TestLoggerFactory loggerFactory;

    @BeforeEach
    void init() {
        loggerFactory = new TestLoggerFactory(LoggerFactory.getILoggerFactory());
    }

    /**
     * Test that an invalid object name provided in configuration throws an
     * exception.
     * @param objName
     */
    @ParameterizedTest()
    @ValueSource(strings = {"22j2n2", "%2j2n2", "+asaaa", "\\asd23"})
    public void JavaScriptBuilderElement_Builder_SetObjectName_InvalidName(String objName) {
        assertThrows(
            PipelineConfigurationException.class,
            () -> {
                new JavaScriptBuilderElementBuilder(loggerFactory)
                        .setObjectName(objName)
                        .build();
            });
    }

    /**
     * Test that no exceptions are thrown if a valid object name is provided in
     * the configuration.
     * @param objName
     */
    @ParameterizedTest
    @ValueSource(strings = {"fod", "fifty1Degrees", "data", "data2"})
    public void JavaScriptBuilderElement_Builder_SetObjectName_ValidName(
        String objName) {
            new JavaScriptBuilderElementBuilder(loggerFactory)
                .setObjectName(objName)
                .build();
    }

    /**
     * Test that a warning is logged if an invalid protocol is provided in the
     * configuration.
     * @param protocol
     */
    @ParameterizedTest
    @ValueSource(strings = {"htp", "htps", "ftp", "tcp"})
    public void JavaScriptBuilderElement_Builder_SetDefaultProtocol_InvalidProtocol(
        String protocol) {
        assertThrows(
            PipelineConfigurationException.class,
            () -> {
                    new JavaScriptBuilderElementBuilder(loggerFactory)
                        .setProtocol(protocol)
                        .build();
            },
            "Expected exception was not thrown");
    }

    /**
     * Test that no warnings are logged if a valid protocol is provided in the
     * configuration.
     * @param protocol
     */
    @ParameterizedTest
    @ValueSource(strings = {"http", "https"})
    public void JavaScriptBuilderElement_Builder_SetDefaultProtocol_ValidProtocol(
        String protocol) {
            new JavaScriptBuilderElementBuilder(loggerFactory)
                .setProtocol(protocol)
                .build();

        loggerFactory.loggers.forEach(testLogger ->
            assertTrue(testLogger.warningsLogged.size() == 0));
    }
}
