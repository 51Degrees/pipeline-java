/* *********************************************************************
 * This Original Work is copyright of 51 Degrees Mobile Experts Limited.
 * Copyright 2023 51 Degrees Mobile Experts Limited, Davidson House,
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

package fiftyone.pipeline.core.flowelements;

import fiftyone.common.testhelpers.TestLogger;
import fiftyone.common.testhelpers.TestLoggerFactory;
import fiftyone.pipeline.core.configuration.ElementOptions;
import fiftyone.pipeline.core.configuration.PipelineOptions;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.exceptions.PipelineConfigurationException;
import fiftyone.pipeline.core.services.PipelineService;
import fiftyone.pipeline.core.testclasses.flowelements.*;
import fiftyone.pipeline.core.testclasses.data.ListSplitterElementData;
import fiftyone.pipeline.core.testclasses.data.TestElementData;
import fiftyone.pipeline.core.testclasses.services.TestService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

@SuppressWarnings("rawtypes")
public class PipelineBuilderTests {
    private PipelineBuilder builder;

    private FlowElement element = mock(FlowElement.class);

    private TestLoggerFactory loggerFactory;

    private int maxErrors = 0;
    private int maxWarnings = 0;

    @Before
    public void initialise() throws ClassNotFoundException {
        ILoggerFactory internalLogger = mock(ILoggerFactory.class);
        when(internalLogger.getLogger(anyString())).thenReturn(mock(Logger.class));
        loggerFactory = new TestLoggerFactory(internalLogger);
        builder = new PipelineBuilder(loggerFactory);
    }

    @After
    public void cleanup() {
        for (TestLogger logger : loggerFactory.loggers) {
            logger.assertMaxErrors(maxErrors);
            logger.assertMaxWarnings(maxWarnings);
        }
    }

    @Test(expected = IllegalStateException.class)
    public void PipelineBuilder_AddFlowElement_Disposed() {
        when(element.isClosed()).thenReturn(true);
        builder.addFlowElement(element);
    }

    @Test(expected = IllegalStateException.class)
    public void PipelineBuilder_AddFlowElementsParallel_Disposed() {
        when(element.isClosed()).thenReturn(true);
        FlowElement element2 = mock(FlowElement.class);
        builder.addFlowElementsParallel(new FlowElement[]{element2, element});
    }

    @Test(expected = IllegalArgumentException.class)
    public void PipelineBuilder_BuildFromConfiguration_Null() throws Exception {
        builder.buildFromConfiguration(null);
    }

    @Test
    public void PipelineBuilder_BuildFromConfiguration_SingleMandatoryParameter() throws Exception {
        // Create the configuration object.
        PipelineOptions opts = new PipelineOptions();
        ElementOptions elOpts = new ElementOptions();
        elOpts.builderName = "MultiplyByElementBuilder";
        elOpts.buildParameters.put("multiple", "8");
        opts.elements.add(elOpts);

        VerifyMultiplyByElementPipeline(opts);
    }

    @Test
    public void PipelineBuilder_BuildFromConfiguration_ClassNameByConvention() throws Exception {
        // Create the configuration object.
        PipelineOptions opts = new PipelineOptions();
        ElementOptions elOpts = new ElementOptions();
        elOpts.builderName = "MultiplyByElement";
        elOpts.buildParameters.put("multiple", "8");
        opts.elements.add(elOpts);

        VerifyMultiplyByElementPipeline(opts);
    }

    @Test
    public void PipelineBuilder_BuildFromConfiguration_ClassNameAlternate() throws Exception {
        // Create the configuration object.
        PipelineOptions opts = new PipelineOptions();
        ElementOptions elOpts = new ElementOptions();
        elOpts.builderName = "Multiply";
        elOpts.buildParameters.put("multiple", "8");
        opts.elements.add(elOpts);

        VerifyMultiplyByElementPipeline(opts);
    }

    @Test(expected = PipelineConfigurationException.class)
    public void PipelineBuilder_BuildFromConfiguration_MandatoryParameterNotSet() throws Exception {
        // Create the configuration object.
        PipelineOptions opts = new PipelineOptions();
        ElementOptions elOpts = new ElementOptions();
        elOpts.builderName = "MultiplyByElementBuilder";
        opts.elements.add(elOpts);

        maxErrors = 1;

        // Pass the configuration to the builder to create the pipeline.
        builder.buildFromConfiguration(opts);
    }

    @Test(expected = PipelineConfigurationException.class)
    public void PipelineBuilder_BuildFromConfiguration_MandatoryParameterWrongType() throws Exception {
        // Create the configuration object.
        PipelineOptions opts = new PipelineOptions();
        ElementOptions elOpts = new ElementOptions();
        elOpts.builderName = "MultiplyByElementBuilder";
        elOpts.buildParameters.put("multiple", "WrongType");
        opts.elements.add(elOpts);

        maxErrors = 1;

        // Pass the configuration to the builder to create the pipeline.
        builder.buildFromConfiguration(opts);
    }

    @Test(expected = PipelineConfigurationException.class)
    public void PipelineBuilder_BuildFromConfiguration_NoBuilder() throws Exception {
        // Create the configuration object.
        PipelineOptions opts = new PipelineOptions();
        ElementOptions elOpts = new ElementOptions();
        elOpts.builderName = "NoBuilder";
        opts.elements.add(elOpts);

        maxErrors = 1;

        // Pass the configuration to the builder to create the pipeline.
        builder.buildFromConfiguration(opts);
    }

    @Test(expected = PipelineConfigurationException.class)
    public void PipelineBuilder_BuildFromConfiguration_WrongBuilder() throws Exception {
        // Create the configuration object.
        PipelineOptions opts = new PipelineOptions();
        ElementOptions elOpts = new ElementOptions();
        elOpts.builderName = "PipelineBuilder";
        opts.elements.add(elOpts);

        maxErrors = 1;

        // Pass the configuration to the builder to create the pipeline.
        builder.buildFromConfiguration(opts);
    }

    @Test
    public void PipelineBuilder_BuildFromConfiguration_OptionalParameter() throws Exception {
        // Create the configuration object.
        PipelineOptions opts = new PipelineOptions();
        ElementOptions elOpts = new ElementOptions();
        elOpts.builderName = "ListSplitterElement";
        elOpts.buildParameters.put("SetDelimiter", "|");
        opts.elements.add(elOpts);

        VerifyListSplitterElementPipeline(opts, SplitOption.Pipe);
    }

    @Test
    public void PipelineBuilder_BuildFromConfiguration_MethodNameConvention() throws Exception {
        // Create the configuration object.
        PipelineOptions opts = new PipelineOptions();
        ElementOptions elOpts = new ElementOptions();
        elOpts.builderName = "ListSplitterElement";
        elOpts.buildParameters.put("Delimiter", "|");
        opts.elements.add(elOpts);

        VerifyListSplitterElementPipeline(opts, SplitOption.Pipe);
    }

    @Test
    public void PipelineBuilder_BuildFromConfiguration_MethodNameAlternate() throws Exception {
        // Create the configuration object.
        PipelineOptions opts = new PipelineOptions();
        ElementOptions elOpts = new ElementOptions();
        elOpts.builderName = "ListSplitterElement";
        elOpts.buildParameters.put("delim", "|");
        opts.elements.add(elOpts);

        VerifyListSplitterElementPipeline(opts, SplitOption.Pipe);
    }

    @Test
    public void PipelineBuilder_BuildFromConfiguration_OptionalDefault() throws Exception {
        // Create the configuration object.
        PipelineOptions opts = new PipelineOptions();
        ElementOptions elOpts = new ElementOptions();
        elOpts.builderName = "ListSplitterElement";
        opts.elements.add(elOpts);

        VerifyListSplitterElementPipeline(opts, SplitOption.Comma);
    }

    @Test(expected = PipelineConfigurationException.class)
    public void PipelineBuilder_BuildFromConfiguration_OptionalMethodMissing() throws Exception {
        // Create the configuration object.
        PipelineOptions opts = new PipelineOptions();
        ElementOptions elOpts = new ElementOptions();
        elOpts.builderName = "ListSplitterElement";
        elOpts.buildParameters.put("NoMethod", "|");
        opts.elements.add(elOpts);

        maxErrors = 1;

        VerifyListSplitterElementPipeline(opts, SplitOption.Comma);
    }

    @Test
    public void PipelineBuilder_BuildFromConfiguration_OptionalMethodInteger() throws Exception {
        // Create the configuration object.
        PipelineOptions opts = new PipelineOptions();
        ElementOptions elOpts = new ElementOptions();
        elOpts.builderName = "ListSplitterElement";
        elOpts.buildParameters.put("MaxLength", "3");
        opts.elements.add(elOpts);

        VerifyListSplitterElementPipeline(opts, SplitOption.CommaMaxLengthThree);
    }

    @Test(expected = PipelineConfigurationException.class)
    public void PipelineBuilder_BuildFromConfiguration_OptionalMethodWrongType() throws Exception {
        // Create the configuration object.
        PipelineOptions opts = new PipelineOptions();
        ElementOptions elOpts = new ElementOptions();
        elOpts.builderName = "ListSplitterElement";
        elOpts.buildParameters.put("MaxLength", "WrongType");
        opts.elements.add(elOpts);

        maxErrors = 1;

        VerifyListSplitterElementPipeline(opts, SplitOption.Comma);
    }

    @Test
    public void PipelineBuilder_BuildFromConfiguration_ServiceConstructor() throws Exception {
        // Create the configuration object.
        PipelineOptions opts = new PipelineOptions();
        ElementOptions elOpts = new ElementOptions();
        elOpts.builderName = "ElementRequiringService";
        opts.elements.add(elOpts);

        TestService service = new TestService();

        builder.addService(service);
        try(Pipeline pipeline = builder.buildFromConfiguration(opts)) {
        	assertEquals(
            service,
            pipeline.getElement(ElementRequiringService.class).getService());
        	
        	// Make sure service is added and managed by pipeline
        	List<PipelineService> services = pipeline.getServices();
        	assertEquals(1, services.size());
        	assertTrue(services.contains(service));
        }
    }

    @Test(expected = PipelineConfigurationException.class)
    public void PipelineBuilder_BuildFromConfiguration_MissingService() throws Exception {
        // Create the configuration object.
        PipelineOptions opts = new PipelineOptions();
        ElementOptions elOpts = new ElementOptions();
        elOpts.builderName = "ElementRequiringService";
        opts.elements.add(elOpts);

        maxErrors = 1;
        builder.buildFromConfiguration(opts);
    }

    public class WrongService implements PipelineService {}

    @Test(expected = PipelineConfigurationException.class)
    public void PipelineBuilder_BuildFromConfiguration_WrongService() throws Exception {
        // Create the configuration object.
        PipelineOptions opts = new PipelineOptions();
        ElementOptions elOpts = new ElementOptions();
        elOpts.builderName = "ElementRequiringService";
        opts.elements.add(elOpts);

        WrongService service = new WrongService();
        builder.addService(service);

        maxErrors = 1;

        builder.buildFromConfiguration(opts);
    }

    @Test
    public void PipelineBuilder_BuildFromConfiguration_ParallelElements() throws Exception {
        // Create the configuration object.
        PipelineOptions options = new PipelineOptions();
        ElementOptions elOpts = new ElementOptions();
        ElementOptions listOpts = new ElementOptions();
        listOpts.builderName = "ListSplitterElement";
        ElementOptions multiplyOpts = new ElementOptions();
        multiplyOpts.builderName = "MultiplyByElement";
        multiplyOpts.buildParameters.put("Multiple", "3");
        elOpts.subElements.add(listOpts);
        elOpts.subElements.add(multiplyOpts);
        options.elements.add(elOpts);

        // Pass the configuration to the builder to create the pipeline.
        Pipeline pipeline = builder.buildFromConfiguration(options);
        // Get the elements
        ListSplitterElement splitterElement = pipeline.getElement(ListSplitterElement.class);
        MultiplyByElement multiplyByElement = pipeline.getElement(MultiplyByElement.class);

        // Create, populate and process flow data.
        try (FlowData flowData = pipeline.createFlowData()) {
            flowData
                .addEvidence(splitterElement.evidenceKeys.get(0), "1,2,abc")
                .addEvidence(multiplyByElement.evidenceKeys.get(0), 25)
                .process();
    
            // Get the results and verify them.
            ListSplitterElementData splitterData = flowData.getFromElement(splitterElement);
            TestElementData multiplyByData = flowData.getFromElement(multiplyByElement);
    
            assertEquals("1", splitterData.getResult().get(0));
            assertEquals("2", splitterData.getResult().get(1));
            assertEquals("abc", splitterData.getResult().get(2));
            assertEquals(75, multiplyByData.getResult());
        }
    }

    private void VerifyListSplitterElementPipeline(
        PipelineOptions options,
        SplitOption splitOn) throws Exception {
        // Pass the configuration to the builder to create the pipeline.
        Pipeline pipeline = builder.buildFromConfiguration(options);

        ListSplitterElement element = pipeline.getElement(ListSplitterElement.class);
        // Check we've got the expected number of evidence keys.
        assertEquals(1, element.evidenceKeys.size());

        // Create, populate and process flow data.
        try (FlowData flowData = pipeline.createFlowData()) {
            flowData.addEvidence(element.evidenceKeys.get(0), "123,456|789,0")
                .process();
    
            // Get the result and verify it.
            ListSplitterElementData elementData = flowData.getFromElement(element);
            switch (splitOn) {
                case Comma:
                    assertEquals("123", elementData.getResult().get(0));
                    assertEquals("456|789", elementData.getResult().get(1));
                    assertEquals("0", elementData.getResult().get(2));
                    break;
                case Pipe:
                    assertEquals("123,456", elementData.getResult().get(0));
                    assertEquals("789,0", elementData.getResult().get(1));
                    break;
                case CommaMaxLengthThree:
                    assertEquals("123", elementData.getResult().get(0));
                    assertEquals("456", elementData.getResult().get(1));
                    assertEquals("|78", elementData.getResult().get(2));
                    assertEquals("9", elementData.getResult().get(3));
                    assertEquals("0", elementData.getResult().get(4));
                    break;
                default:
                    break;
            }
        }
    }

    private void VerifyMultiplyByElementPipeline(PipelineOptions options) throws Exception {
        // Pass the configuration to the builder to create the pipeline.
        Pipeline pipeline = builder.buildFromConfiguration(options);
        MultiplyByElement element = pipeline.getElement(MultiplyByElement.class);

        // Check we've got the expected number of evidence keys.
        assertEquals(1, element.evidenceKeys.size());

        // Create, populate and process flow data.
        try (FlowData flowData = pipeline.createFlowData()) {
            flowData.addEvidence(element.evidenceKeys.get(0), 5)
                .process();
    
            // Get the result and verify it.
            TestElementData elementData = flowData.getFromElement(element);
            assertEquals(40, elementData.getResult());
        }
    }

    private enum SplitOption {Comma, Pipe, CommaMaxLengthThree}
}
