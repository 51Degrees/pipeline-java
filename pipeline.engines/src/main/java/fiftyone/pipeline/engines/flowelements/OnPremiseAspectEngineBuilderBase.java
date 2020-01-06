/* *********************************************************************
 * This Original Work is copyright of 51 Degrees Mobile Experts Limited.
 * Copyright 2019 51 Degrees Mobile Experts Limited, 5 Charlotte Close,
 * Caversham, Reading, Berkshire, United Kingdom RG4 7BY.
 *
 * This Original Work is licensed under the European Union Public Licence (EUPL) 
 * v.1.2 and is subject to its terms as set out below.
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

package fiftyone.pipeline.engines.flowelements;

import fiftyone.pipeline.engines.Constants;
import fiftyone.pipeline.engines.configuration.DataFileConfiguration;
import fiftyone.pipeline.engines.data.AspectEngineDataFile;
import fiftyone.pipeline.engines.data.AspectEngineDataFileDefault;
import fiftyone.pipeline.engines.services.DataUpdateService;
import org.slf4j.ILoggerFactory;

import java.util.ArrayList;
import java.util.List;

public abstract class OnPremiseAspectEngineBuilderBase<
    TBuilder extends OnPremiseAspectEngineBuilderBase<TBuilder, TEngine>,
    TEngine extends OnPremiseAspectEngine>
    extends AspectEngineBuilderBase<TBuilder, TEngine> {

    private DataUpdateService dataUpdateService;

    protected List<DataFileConfiguration> dataFileConfigs = new ArrayList<>();
    protected List<AspectEngineDataFile> dataFiles = new ArrayList<>();
    protected String tempDir = System.getProperty("java.io.tmpdir");

    public OnPremiseAspectEngineBuilderBase() {
        this(null);
    }

    public OnPremiseAspectEngineBuilderBase(ILoggerFactory loggerFactory) {
        this(loggerFactory, null);
    }

    public OnPremiseAspectEngineBuilderBase(ILoggerFactory loggerFactory, DataUpdateService dataUpdateService) {
        super(loggerFactory);
        this.dataUpdateService = dataUpdateService;
    }

    /**
     * Add a data file for this engine to use.
     * @param configuration  The data file configuration to add to this engine.
     * @return This engine builder instance.
     */
    public TBuilder addDataFile(DataFileConfiguration configuration) {
        dataFileConfigs.add(configuration);
        return (TBuilder)this;
    }

    /**
     * Set the temporary path to use when the engine needs to create
     * temporary files. (e.g. when downloading data updates)
     * @param dirPath The full path to the temporary directory
     * @return This engine builder instance.
     */
    public TBuilder setTempDirPath(String dirPath) {
        tempDir = dirPath;
        return (TBuilder)this;
    }

    /**
     * Set the performance profile that the engine should use.
     */
    public abstract TBuilder setPerformanceProfile(Constants.PerformanceProfiles profile);

    @Override
    protected void preCreateEngine() {
        // Register any configured files with the data update service.
        // Any files that have the 'update on startup' flag set
        // will be updated now.
        // Create the auto-update configuration and register it.
        for (DataFileConfiguration dataFileConfig : dataFileConfigs) {
            AspectEngineDataFile dataFile = newAspectEngineDataFile();
            dataFile.setIdentifier(dataFileConfig.getIdentifier());
            dataFile.setConfiguration(dataFileConfig);
            dataFile.setTempDataDirPath(tempDir);

            if (dataFileConfig.getAutomaticUpdatesEnabled()) {
                if (dataUpdateService == null) {
                    throw new RuntimeException(
                        "Auto update enabled by data update service does not exist. " +
                            "This can be corrected by passing an IDataUpdateService " +
                            "instance to the engine builder constructor. " +
                            "If building from configuration, an IServiceProvider " +
                            "instance able to resolve an IDataUpdateService " +
                            "must be passed to the pipeline builder.");
                }
                dataUpdateService.registerDataFile(dataFile);
            }
            dataFiles.add(dataFile);
        }
    }

    /**
     * Private method that performs any generalised configuration on
     * the engine.
     * @param engine The engine to configure.
     */
    @Override
    protected void configureEngine(TEngine engine) throws Exception {
        super.configureEngine(engine);
        // Create the auto-update configuration and register it.
        for (AspectEngineDataFile dataFile : dataFiles) {
            dataFile.setEngine(engine);
            if (engine != null) {
                engine.addDataFile(dataFile);
            }
        }
    }

    protected AspectEngineDataFile newAspectEngineDataFile() {
        return new AspectEngineDataFileDefault();
    }
}