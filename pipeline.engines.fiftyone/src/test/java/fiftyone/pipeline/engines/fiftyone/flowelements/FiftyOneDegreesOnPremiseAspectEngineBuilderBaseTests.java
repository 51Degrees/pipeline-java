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

package fiftyone.pipeline.engines.fiftyone.flowelements;

import fiftyone.pipeline.engines.Constants;
import fiftyone.pipeline.engines.data.AspectData;
import fiftyone.pipeline.engines.data.AspectEngineDataFile;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.fiftyone.data.FiftyOneDataFile;
import fiftyone.pipeline.engines.services.DataUpdateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class FiftyOneDegreesOnPremiseAspectEngineBuilderBaseTests {
    final String licenseKey = "somelicensekey";
    final String downloadType = "sometype";

    @SuppressWarnings("unchecked")
    private FiftyOneAspectEngine<? extends AspectData, ? extends AspectPropertyMetaData> engine = mock(FiftyOneAspectEngine.class);

    private DataUpdateService updateService = mock(DataUpdateService.class);

    private class ImplementedBuilder extends
        FiftyOneOnPremiseAspectEngineBuilderBase<ImplementedBuilder, FiftyOneAspectEngine<? extends AspectData, ? extends AspectPropertyMetaData>> {

        @SuppressWarnings("unused")
        public ImplementedBuilder() {
            super();
        }

        @SuppressWarnings("unused")
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
        protected FiftyOneAspectEngine<? extends AspectData, ? extends AspectPropertyMetaData> newEngine(List<String> properties) {
            // Follow the logic used by 51Degrees engines.
            AspectEngineDataFile dataFile = dataFiles.get(0);
            dataFiles.remove(dataFile);
            engine.addDataFile(dataFile);
            dataFile.setEngine(engine);
            return engine;
        }

        @Override
        protected String getDefaultDataDownloadType() {
            return downloadType;
        }
    }

    @BeforeEach
    public void init() {
        doAnswer(new Answer<Object>() {
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
            LoggerFactory.getILoggerFactory(),
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
        LoggerFactory.getLogger(this.getClass()).info("OS name is {}",
                System.getProperty("os.name"));
        LoggerFactory.getLogger(this.getClass()).info("OS arch is {}",
                System.getProperty("os.arch"));
        ImplementedBuilder builder = new ImplementedBuilder(
            LoggerFactory.getILoggerFactory(),
            updateService);
        builder.setDataUpdateLicenseKey(licenseKey);
        builder.build("", false);
        assertEquals(
            "https://distributor.51degrees.com/api/v2/download?LicenseKeys="+licenseKey+"&Download=True&Type="+downloadType,
            engine.getDataFileMetaData().getFormattedUrl());
    }
}
