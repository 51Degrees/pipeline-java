package fiftyone.pipeline.engines.fiftyone.flowelements;

import fiftyone.pipeline.engines.Constants;
import fiftyone.pipeline.engines.data.AspectEngineDataFile;
import fiftyone.pipeline.engines.fiftyone.data.FiftyOneDataFile;
import fiftyone.pipeline.engines.flowelements.OnPremiseAspectEngineBase;
import fiftyone.pipeline.engines.services.DataUpdateService;
import fiftyone.pipeline.engines.testhelpers.flowelements.EmptyEngine;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.ILoggerFactory;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class FiftyOneDegreesOnPremiseAspectEngineBuilderBaseTests {
    final String licenseKey = "somelicensekey";
    final String downloadType = "sometype";

    private FiftyOneAspectEngine engine = mock(FiftyOneAspectEngine.class);

    private DataUpdateService updateService = mock(DataUpdateService.class);

    private class ImplementedBuilder extends
        FiftyOneOnPremiseAspectEngineBuilderBase<ImplementedBuilder, FiftyOneAspectEngine> {

        public ImplementedBuilder() {
            super();
        }

        public ImplementedBuilder(ILoggerFactory loggerFactory) {
            super(loggerFactory);
        }

        public ImplementedBuilder(ILoggerFactory loggerFactory, DataUpdateService dataUpdateService) {
            super(loggerFactory, dataUpdateService);
        }

        @Override
        public ImplementedBuilder setConcurrency(int concurrency) {
            return this;
        }

        @Override
        public ImplementedBuilder setPerformanceProfile(Constants.PerformanceProfiles profile) {
            return this;
        }

        @Override
        protected FiftyOneAspectEngine newEngine(List<String> properties) {
            // Follow the logic used by 51Degrees engines.
            AspectEngineDataFile dataFile = dataFiles.get(0);
            dataFiles.remove(dataFile);
            engine.addDataFile(dataFile);
            dataFile.setEngine(engine);
            return engine;
        }
    }

    @Before
    public void init() {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                // Follow the logic used by 51Degrees engines.
                FiftyOneDataFile dataFile = invocationOnMock.getArgument(0);
                dataFile.setDataUpdateDownloadType(downloadType);
                when(engine.getDataFileMetaData()).thenReturn(dataFile);
                return null;
            }
        }).when(engine).addDataFile(any(AspectEngineDataFile.class));
    }

    /**
     * Check that the builder constructs a data file for the engine which
     * has the engine set. This is done by the engine's constructor.
     * @throws Exception
     */
    @Test
    public void DataFile_SetEngine() throws Exception {
        ImplementedBuilder builder = new ImplementedBuilder(
            mock(ILoggerFactory.class),
            updateService);
        builder.build("", false);

        assertNotNull(engine.getDataFileMetaData());
        assertEquals(engine, engine.getDataFileMetaData().getEngine());
    }

    /**
     * Check that the builder constructs a data file for the engine which
     * builds the correct URL for updates. This should be a 51Degrees specific
     * URL.
     * @throws Exception
     */
    @Test
    public void DataFile_UrlFormatter() throws Exception {
        final FiftyOneDataFile[] dataFile = new FiftyOneDataFile[1];

        ImplementedBuilder builder = new ImplementedBuilder(
            mock(ILoggerFactory.class),
            updateService);
        builder.setDataUpdateLicenseKey(licenseKey);
        builder.build("", false);
        assertEquals(
            "https://distributor.51degrees.com/api/v2/download?LicenseKeys="+licenseKey+"&Download=True&Type="+downloadType,
            engine.getDataFileMetaData().getFormattedUrl());
    }

}
