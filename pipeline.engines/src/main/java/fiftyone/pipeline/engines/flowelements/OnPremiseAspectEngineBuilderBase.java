/* *********************************************************************
 * This Original Work is copyright of 51 Degrees Mobile Experts Limited.
 * Copyright 2025 51 Degrees Mobile Experts Limited, Davidson House,
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

package fiftyone.pipeline.engines.flowelements;

import fiftyone.pipeline.annotations.DefaultValue;
import fiftyone.pipeline.engines.Constants;
import fiftyone.pipeline.engines.configuration.DataFileConfiguration;
import fiftyone.pipeline.engines.data.AspectEngineDataFile;
import fiftyone.pipeline.engines.data.AspectEngineDataFileDefault;
import fiftyone.pipeline.engines.services.DataUpdateService;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.apache.commons.lang3.BooleanUtils.isFalse;

/**
 * Abstract base class that exposes the common options that all 51Degrees
 * on-premise engine builders should make use of.
 * @param <TBuilder> the specific builder type to use as the return type from
 *                  the fluent builder methods
 * @param <TEngine> the type of the engine that this builder will build
 */
@SuppressWarnings("rawtypes")
public abstract class OnPremiseAspectEngineBuilderBase<
    TBuilder extends OnPremiseAspectEngineBuilderBase<TBuilder, TEngine>,
    TEngine extends OnPremiseAspectEngine>
    extends AspectEngineBuilderBase<TBuilder, TEngine> {

    private final DataUpdateService dataUpdateService;

    protected final List<DataFileConfiguration> dataFileConfigs = new ArrayList<>();
    protected final List<AspectEngineDataFile> dataFiles = new ArrayList<>();
    protected String tempDir = Paths.get(
            System.getProperty("java.io.tmpdir"),"fiftyone.tempfiles").toString();

    protected Logger logger = LoggerFactory.getLogger(OnPremiseAspectEngineBuilderBase.class);

    /**
     * Default constructor which uses the {@link ILoggerFactory} implementation
     * returned by {@link LoggerFactory#getILoggerFactory()}.
     */
    public OnPremiseAspectEngineBuilderBase() {
        this(null);
    }

    /**
     * Construct a new instance using the {@link ILoggerFactory} supplied.
     * @param loggerFactory the logger factory to use
     */
    public OnPremiseAspectEngineBuilderBase(ILoggerFactory loggerFactory) {
        this(loggerFactory, null);
    }

    /**
     * Construct a new instance using the {@link ILoggerFactory} and
     * {@link DataUpdateService} supplied.
     * @param loggerFactory the logger factory to use
     * @param dataUpdateService the {@link DataUpdateService} to use when
     *                          automatic updates happen on the data file
     */
    public OnPremiseAspectEngineBuilderBase(
        ILoggerFactory loggerFactory,
        DataUpdateService dataUpdateService) {
        super(loggerFactory);
        this.dataUpdateService = dataUpdateService;

        // create an empty temp directory as a sub-directory of the system temp directory if it
        // does not exist already
        createAndVerifyTempDir(Paths.get(tempDir));
    }

    public void createAndVerifyTempDir(Path pathToCreate) {
        try {
            File directory = pathToCreate.toFile();
            if (isFalse(directory.exists())) {
                directory = Files.createDirectories(pathToCreate).toFile();
                boolean success;

                try {
                    Set<PosixFilePermission> permissions = EnumSet.of(
                            PosixFilePermission.OWNER_WRITE,
                            PosixFilePermission.OWNER_READ,
                            PosixFilePermission.OWNER_EXECUTE,
                            PosixFilePermission.GROUP_READ,
                            PosixFilePermission.GROUP_EXECUTE
                    );
                    Files.setPosixFilePermissions(pathToCreate, permissions);
                    success = directory.canRead() && directory.canWrite();
                } catch (UnsupportedOperationException e) {
                    success = directory.setReadable(true, true)
                            && directory.setWritable(true, true);
                }

                logger.debug("Temp dir is {} can write {}", tempDir, directory.canWrite());
                logger.debug("File permission setting reported {}.", success);

            } else {
                if (isFalse(directory.isDirectory())) {
                    throw new IllegalStateException(
                            "Temporary directory path exists and is not a directory: " + pathToCreate);
                }
            }

            tempDir = directory.getAbsolutePath();

            // ensure read/write access - throws an exception if not
            FileSystem fs = Paths.get(directory.getPath()).getFileSystem();
            fs.provider().checkAccess(pathToCreate, AccessMode.WRITE, AccessMode.READ);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot access Temp Directory '" + pathToCreate +
                    "' with correct permissions", e);
        }
    }

    /**
     * Add a data file for this engine to use.
     * @param configuration  the data file configuration to add to this engine
     * @return this engine builder instance
     */
    @SuppressWarnings("unchecked")
    public TBuilder addDataFile(DataFileConfiguration configuration) {
        dataFileConfigs.add(configuration);
        return (TBuilder)this;
    }

    /**
     * Set the temporary path to use when the engine needs to create
     * temporary files. (e.g. when downloading data updates)
     * <p>
     * By default, this is set to System.getProperty(java.io.tmpdir)+"/fiftyone.tempfiles"
     * @param dirPath the full path to the temporary directory
     * @return this engine builder instance
     */
    @DefaultValue("System.getProperty(java.io.tmpdir)+\"/fiftyone.tempfiles\"")
    @SuppressWarnings("unchecked")
    public TBuilder setTempDirPath(String dirPath) {
        tempDir = dirPath;
        return (TBuilder)this;
    }

    /**
     * Set the performance profile that the engine should use.
     * @param profile the performance profile for the engine to use
     * @return this engine builder instance
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
                            "This can be corrected by passing an DataUpdateService " +
                            "instance to the engine builder constructor. " +
                            "If building from configuration, this can be corrected " +
                            "by adding an DataUpdateService instance to the " +
                            "the pipeline builder via addService() method.");
                }
                dataUpdateService.registerDataFile(dataFile);
            }
            dataFiles.add(dataFile);
        }
    }

    @SuppressWarnings("unchecked")
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

    /**
     * Create a new empty data file instance to be populated with the details of
     * the data file to be used.
     * @return new {@link AspectEngineDataFile} instance
     */
    protected AspectEngineDataFile newAspectEngineDataFile() {
        return new AspectEngineDataFileDefault();
    }
}