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

import fiftyone.pipeline.annotations.BuildArg;
import fiftyone.pipeline.core.exceptions.PipelineConfigurationException;
import fiftyone.pipeline.engines.configuration.DataFileConfiguration;
import fiftyone.pipeline.engines.configuration.DataFileConfigurationBuilder;
import fiftyone.pipeline.engines.data.DataUpdateUrlFormatter;
import fiftyone.pipeline.engines.services.DataUpdateService;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class that exposes the common options that all on-premise
 * engine builders using a single data file should make use of.
 * @param <TBuilder> the specific builder type to use as the return type from
 *                  the fluent builder methods
 * @param <TEngine> the type of the engine that this builder will build
 */
@SuppressWarnings("rawtypes")
public abstract class SingleFileAspectEngineBuilderBase<
    TBuilder extends OnPremiseAspectEngineBuilderBase<TBuilder, TEngine>,
    TEngine extends OnPremiseAspectEngine>
    extends OnPremiseAspectEngineBuilderBase<TBuilder, TEngine>{

    private final DataFileConfigurationBuilder dataFileBuilder =
        new DataFileConfigurationBuilder();

    /**
     * Default constructor which uses the {@link ILoggerFactory} implementation
     * returned by {@link LoggerFactory#getILoggerFactory()}.
     */
    public SingleFileAspectEngineBuilderBase() {
        super();
    }

    /**
     * Construct a new instance using the {@link ILoggerFactory} supplied.
     * @param loggerFactory the logger factory to use
     */
    public SingleFileAspectEngineBuilderBase(ILoggerFactory loggerFactory) {
        super(loggerFactory);
    }

    /**
     * Construct a new instance using the {@link ILoggerFactory} and
     * {@link DataUpdateService} supplied.
     * @param loggerFactory the logger factory to use
     * @param dataUpdateService the {@link DataUpdateService} to use when
     *                          automatic updates happen on the data file
     */
    public SingleFileAspectEngineBuilderBase(
        ILoggerFactory loggerFactory,
        DataUpdateService dataUpdateService) {
        super(loggerFactory, dataUpdateService);
    }
    /**
     * Build an engine using the current options and the specified
     * data file.
     * Also registers the data file with the data update service.
     * @param dataFile complete path to the data file to use when creating the
     *                 engine
     * @param createTempDataCopy if true, the engine will create a copy of the
     *                           data file in a temporary location rather than
     *                           using the file provided directly. If not
     *                           loading all data into memory, this is required
     *                           for automatic data updates to occur
     * @return new {@link AspectEngine} instance
     * @throws Exception if the engine could not be created
     */
    public TEngine build(
        @BuildArg("dataFile")String dataFile,
        @BuildArg("createTempDataCopy") boolean createTempDataCopy) throws Exception {
        DataFileConfiguration config = dataFileBuilder.build(
            dataFile,
            createTempDataCopy);
        addDataFile(config);
        return build();
    }

    /**
     * Build an engine using the current options and the specified
     * byte array.
     * Also registers the data file with the data update service.
     * @param data a byte[] containing an in-memory representation of a data
     *             file
     * @return new {@link AspectEngine} instance
     * @throws Exception if the engine could not be created
     */
    public TEngine build(byte[] data) throws Exception {
        DataFileConfiguration config = dataFileBuilder.build(data);
        addDataFile(config);
        return build();
    }

    /**
     * Build an engine using the configured options.
     * Also registers the data file with the data update service.
     * @return new {@link AspectEngine} instance
     * @throws Exception if the engine could not be created
     */
    protected TEngine build() throws Exception {
        if (dataFileConfigs.size() != 1) {
            throw new PipelineConfigurationException("This builder " +
                "requires one and only one data file to be configured " +
                "but " + dataFileConfigs.size() + " data file " +
                "configurations that have been supplied.");
        }
        return buildEngine();
    }

    /**
     * Configure the engine to use the specified URL when looking for
     * an updated data file.
     * @param url the URL to check for a new data file
     * @return this builder
     */
    @SuppressWarnings("unchecked")
    public TBuilder setDataUpdateUrl(String url) {
        dataFileBuilder.setDataUpdateUrl(url);
        return (TBuilder)this;
    }

    /**
     * Specify a {@link DataUpdateUrlFormatter} to be used by the
     * {@link DataUpdateService} when building the complete URL to query for
     * updated data.
     * @param formatter the formatter to use
     * @return this builder
     */
    @SuppressWarnings("unchecked")
    public TBuilder setDataUpdateUrlFormatter(DataUpdateUrlFormatter formatter) {
        dataFileBuilder.setDataUpdateUrlFormatter(formatter);
        return (TBuilder)this;
    }

    /**
     * Set a value indicating if the {@link DataUpdateService} should expect the
     * response from the data update URL to contain a 'content-md5' HTTP header
     * that can be used to verify the integrity of the content.
     * @param verify true if the content should be verified with the Md5 hash.
     * False otherwise
     * @return this builder
     */
    @SuppressWarnings("unchecked")
    public TBuilder setDataUpdateVerifyMd5(boolean verify) {
        dataFileBuilder.setDataUpdateVerifyMd5(verify);
        return (TBuilder)this;
    }

    /**
     * Set a value indicating if the {@link DataUpdateService} should expect
     * content from the configured data update URL to be compressed or not.
     * @param decompress true if the content from the data update URL needs to
     * be decompressed. False otherwise
     * @return this builder
     */
    @SuppressWarnings("unchecked")
    public TBuilder setDataUpdateDecompress(boolean decompress) {
        dataFileBuilder.setDataUpdateDecompress(decompress);
        return (TBuilder)this;
    }

    /**
     * Enable or disable automatic updates for this engine.
     * @param enabled  if true, the engine will update it's data file with no
     *                 manual intervention. If false, the engine will never
     *                 update it's data file unless the manual update method is
     *                 called on {@link DataUpdateService}
     * @return this builder
     */
    @SuppressWarnings("unchecked")
    public TBuilder setAutoUpdate(boolean enabled) {
        dataFileBuilder.setAutoUpdate(enabled);
        return (TBuilder)this;
    }

    /**
     * The {@link DataUpdateService} has the ability to watch a data file on
     * disk and automatically refresh the engine as soon as the file is updated.
     * This setting enables/disables that feature.
     *
     * The {@link #setAutoUpdate(boolean)} feature must also be enabled in order
     * for the file system watcher to work.
     * If the engine is built from a byte[] then this setting does nothing.
     * @param enabled the cache configuration to use
     * @return this builder
     */
    @SuppressWarnings("unchecked")
    public TBuilder setDataFileSystemWatcher(boolean enabled) {
        dataFileBuilder.setDataFileSystemWatcher(enabled);
        return (TBuilder)this;
    }

    /**
     * Set the time between checks for a new data file made by the
     * {@link DataUpdateService}.
     * Default = 30 minutes.
     *
     * Generally, the {@link DataUpdateService} will not check for a new data
     * file until the 'expected update time' that is stored in the current data
     * file. This interval is the time to wait between checks after that time
     * if no update is initially found.
     * If automatic updates are disabled then this setting does nothing.
     * @param pollingIntervalSeconds the number of seconds between checks
     * @return this builder
     */
    @SuppressWarnings("unchecked")
    public TBuilder setUpdatePollingInterval(int pollingIntervalSeconds) {
        dataFileBuilder.setUpdatePollingInterval(pollingIntervalSeconds);
        return (TBuilder)this;
    }

    /**
     * Set the time between checks for a new data file made by the
     * {@link DataUpdateService}.
     * Default = 30 minutes.
     *
     * Generally, the {@link DataUpdateService} will not check for a new data
     * file until the 'expected update time' that is stored in the current data
     * file. This interval is the time to wait between checks after that time
     * if no update is initially found.
     * If automatic updates are disabled then this setting does nothing.
     * @param pollingIntervalMillis the time between checks
     * @return this builder
     */
    @SuppressWarnings("unchecked")
    public TBuilder setUpdatePollingInterval(long pollingIntervalMillis) {
        dataFileBuilder.setUpdatePollingInterval(pollingIntervalMillis);
        return (TBuilder)this;
    }

    /**
     * A random element can be added to the {@link DataUpdateService} polling
     * interval. This option sets the maximum length of this random addition.
     * Default = 10 minutes.
     * @param maximumDeviationMillis the maximum time added to the data update
     *                               polling interval
     * @return this builder
     */
    @SuppressWarnings("unchecked")
    public TBuilder setUpdateRandomisationMax(long maximumDeviationMillis) {
        dataFileBuilder.setUpdateRandomisationMax(maximumDeviationMillis);
        return (TBuilder)this;
    }

    /**
     * Set if {@link DataUpdateService} sends the If-Modified-Since header
     * in the request for a new data file.
     * @param enabled whether to use the If-Modified-Since header
     * @return this builder
     */
    @SuppressWarnings("unchecked")
    public TBuilder setVerifyIfModifiedSince(boolean enabled) {
        dataFileBuilder.setVerifyIfModifiedSince(enabled);
        return (TBuilder)this;
    }

    /**
     * Set the license key to use when updating the Engine's data file.
     * @param key the license key to use when checking for updates to the data
     *            file. A license key can be obtained from the
     *            <a href="https://51degrees.com/pricing">51Degrees website</a>.
     *            If you have no license key then this parameter can be set to
     *            null, but doing so will disable automatic updates.
     * @return this builder
     */
    @SuppressWarnings("unchecked")
    public TBuilder setDataUpdateLicenseKey(String key) {
        dataFileBuilder.setDataUpdateLicenseKey(key);
        return (TBuilder)this;
    }

    /**
     * Set the license keys to use when updating the Engine's data file.
     * @param keys 51Degrees license keys
     * @return this builder
     */
    @SuppressWarnings("unchecked")
    public TBuilder setDataUpdateLicenseKeys(String[] keys) {
        dataFileBuilder.setDataUpdateLicenseKeys(keys);
        return (TBuilder)this;
    }

    /**
     * Configure the data file to update on startup or not.
     * @param enabled if true then when this file is registered with the data
     *                update service, it will immediately try to download the
     *                latest copy of the file.
     *                This action will block execution until the download is
     *                complete and the engine has loaded the new file.
     * @return this builder
     */
    @SuppressWarnings("unchecked")
    public TBuilder setDataUpdateOnStartup(boolean enabled) {
        dataFileBuilder.setUpdateOnStartup(enabled);
        return (TBuilder)this;
    }
}
