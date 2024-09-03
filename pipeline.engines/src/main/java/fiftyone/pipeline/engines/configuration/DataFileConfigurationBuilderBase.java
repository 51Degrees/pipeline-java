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

package fiftyone.pipeline.engines.configuration;

import fiftyone.pipeline.engines.data.DataUpdateUrlFormatter;
import fiftyone.pipeline.engines.services.DataUpdateService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Builder class that is used to create instances of
 * {@link DataFileConfiguration} objects.
 * @param <TBuilder> the type of the builder implementation
 * @param <TConfig> the type of configuration returned by the builder
 */
public abstract class DataFileConfigurationBuilderBase<
    TBuilder extends DataFileConfigurationBuilderBase<TBuilder, TConfig>,
    TConfig extends DataFileConfiguration> {

    private String identifier;
    private String dataUpdateUrlOverride = null;
    private Boolean autoUpdateEnabled = null;
    private Boolean dataFileSystemWatcherEnabled = null;
    private Integer updatePollingIntervalSeconds = null;
    private Integer updateMaxRandomisationSeconds = null;
    private DataUpdateUrlFormatter dataUpdateUrlFormatter = null;
    private Boolean dataUpdateVerifyMd5 = null;
    private Boolean dataUpdateDecompress = null;
    private Boolean dataUpdateVerifyModifiedSince = null;
    private Boolean updateOnStartup = null;
    private final List<String> licenseKeys = new ArrayList<>();

    /**
     * Get the license keys to use when updating the Engine's data file.
     * @return list of license keys
     */
    protected List<String> getDataUpdateLicenseKeys() {
        return licenseKeys;
    }

    public DataFileConfigurationBuilderBase() {
    }

    /**
     * Set the identifier of the data file that this configuration
     * information applies to.
     * If the engine only supports a single data file then this
     * value will be ignored.
     * @param identifier the identifier to use
     * @return this builder instance
     */
    @SuppressWarnings("unchecked")
    public TBuilder setDataFileIdentifier(String identifier) {
        this.identifier = identifier;
        return (TBuilder)this;
    }

    /**
     * Configure the engine to use the specified URL when looking for
     * an updated data file.
     * @param url the URL to check for a new data file.
     * @return this builder instance.
     */
    @SuppressWarnings("unchecked")
    public TBuilder setDataUpdateUrl(String url) {
        dataUpdateUrlOverride = url;
        return (TBuilder)this;
    }

    /**
     * Specify a DataUpdateUrlFormatter to be used by the DataUpdateService
     * when building the complete URL to query for updated data.
     * @param formatter  the formatter to use
     * @return  this builder instance
     */
    @SuppressWarnings("unchecked")
    public TBuilder setDataUpdateUrlFormatter( DataUpdateUrlFormatter formatter) {
        dataUpdateUrlFormatter = formatter;
        return (TBuilder)this;
    }

    /**
     * Set a value indicating if the {@link DataUpdateService}
     * should expect the response from the data update URL to contain a
     * 'content-md5' HTTP header that can be used to verify the integrity
     * of the content.
     * @param verify true if the content should be verified with the Md5 hash.
     *               False otherwise
     * @return this builder instance
     */
    @SuppressWarnings("unchecked")
    public TBuilder setDataUpdateVerifyMd5(boolean verify) {
        dataUpdateVerifyMd5 = verify;
        return (TBuilder)this;
    }

    /**
     * Set a value indicating if the {@link DataUpdateService} should expect
     * content from the configured data update URL to be compressed or not.
     * @param decompress  true if the content from the data update URL needs to
     *                    be decompressed. False otherwise
     * @return this builder instance
     */
    @SuppressWarnings("unchecked")
    public TBuilder setDataUpdateDecompress(boolean decompress) {
        dataUpdateDecompress = decompress;
        return (TBuilder)this;
    }

    /**
     * Enable or disable automatic updates for this engine.
     * @param enabled if true, the engine will update it's data file with no
     *                manual intervention. If false, the engine will never
     *                update it's data file unless the manual update method is
     *                called on {@link DataUpdateService}
     * @return this builder instance
     */
    @SuppressWarnings("unchecked")
    public TBuilder setAutoUpdate(boolean enabled) {
        autoUpdateEnabled = enabled;
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
     * @param enabled  the cache configuration to use
     * @return this builder instance
     */
    @SuppressWarnings("unchecked")
    public TBuilder setDataFileSystemWatcher(boolean enabled) {
        dataFileSystemWatcherEnabled = enabled;
        return (TBuilder)this;
    }

    /**
     * Set the time between checks for a new data file made by the
     * {@link DataUpdateService}.
     * Default = 30 minutes.
     *
     * Generally, the {@link DataUpdateService} will not check for a new data
     * file until the 'expected update time' that is stored in the current data
     * file.
     * This interval is the time to wait between checks after that time
     * if no update is initially found.
     * If automatic updates are disabled then this setting does nothing.
     * @param pollingIntervalSeconds the number of seconds between checks
     * @return this builder instance
     */
    @SuppressWarnings("unchecked")
    public TBuilder setUpdatePollingInterval(int pollingIntervalSeconds) {
        updatePollingIntervalSeconds = pollingIntervalSeconds;
        return (TBuilder)this;
    }

    /**
     * Set the time between checks for a new data file made by the
     * {@link DataUpdateService}.
     * Default = 30 minutes.
     *
     * Generally, the will not check for a new data file until the 'expected
     * update time' that is stored in the current data file.
     * This interval is the time to wait between checks after that time
     * if no update is initially found.
     * If automatic updates are disabled then this setting does nothing.
     * @param pollingIntervalMillis the time between checks
     * @return this builder instance
     */
    @SuppressWarnings("unchecked")
    public TBuilder setUpdatePollingInterval(long pollingIntervalMillis) {
        if (pollingIntervalMillis / 1000 > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Polling interval timespan too large.");
        }
        updatePollingIntervalSeconds = (int)pollingIntervalMillis / 1000;
        return (TBuilder)this;
    }

    /**
     * A random element can be added to the {@link DataUpdateService} polling
     * interval.
     * This option sets the maximum length of this random addition.
     * Default = 10 minutes.
     * @param maximumDeviationMillis the maximum time added to the data update
     *                               polling interval
     * @return this builder instance
     */
    @SuppressWarnings("unchecked")
    public TBuilder setUpdateRandomisationMax(long maximumDeviationMillis){
        if (maximumDeviationMillis / 1000 > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                "Update randomisation timespan too large.");
        }
        updateMaxRandomisationSeconds = (int)maximumDeviationMillis / 1000;
        return (TBuilder)this;
    }

    /**
     * Set if {@link DataUpdateService} sends the If-Modified-Since header
     * in the request for a new data file.
     * @param enabled whether to use the If-Modified-Since header
     * @return this builder instance
     */
    @SuppressWarnings("unchecked")
    public TBuilder setVerifyIfModifiedSince(boolean enabled) {
        dataUpdateVerifyModifiedSince = enabled;
        return (TBuilder)this;
    }

    /**
     * Set the license key to use when updating the Engine's data file.
     * @param key 51Degrees license key. This parameter can be set to
     *            null, but doing so will disable automatic updates for
     *            this file
     * @return this builder
     * */
    @SuppressWarnings("unchecked")
    public TBuilder setDataUpdateLicenseKey(String key) {
        if (key == null) {
            // Clear any configured license keys and disable
            // any features that would make use of the license key.
            licenseKeys.clear();
            autoUpdateEnabled = false;
            updateOnStartup = false;
        }
        else {
            licenseKeys.add(key);
        }
        return (TBuilder)this;
    }

    /**
     * Configure the data file to update on startup or not.
     * This action will block execution until the download is complete
     * and the engine has loaded the new file.
     * @param enabled if true then when this file is registered with the data
     *                update service, it will immediately try to download the
     *                latest copy of the file
     * @return this builder
     */
    @SuppressWarnings("unchecked")
    public TBuilder setUpdateOnStartup(boolean enabled) {
        updateOnStartup = enabled;
        return (TBuilder)this;
    }

    /**
     * Set the license keys to use when updating the Engine's data file.
     * @param keys 51Degrees license keys
     * @return this builder
     * */
    @SuppressWarnings("unchecked")
    public TBuilder setDataUpdateLicenseKeys(String[] keys) {
        Collections.addAll(licenseKeys, keys);
        return (TBuilder)this;
    }

    protected abstract TConfig createConfig();

    /**
     * Called to indicate that configuration of this file is complete
     * and the user can continue to configure the engine that the
     * data file will be used by.
     * @param filename path to the data file to build from
     * @param createTempCopy true if a temporary copy should be created to
     *                       stream from
     * @return the new {@link DataFileConfiguration} instance
     */
    public TConfig build(String filename, boolean createTempCopy) {
        TConfig config = createConfig();

        configureCommonOptions(config);
        config.setDataFilePath(filename);
        config.setCreateTempCopy(createTempCopy);

        return config;
    }

    /**
     * Called to indicate that configuration of this file is complete
     * and the user can continue to configure the engine that the
     * data file will be used by.
     * @param data byte array containing the data file contents in memory
     * @return the new {@link DataFileConfiguration} instance
     */
    public TConfig build(byte[] data) {
        TConfig config = createConfig();

        configureCommonOptions(config);
        config.setData(data);

        return config;
    }

    /**
     * Set any properties on the configuration object that are the
     * same regardless of the method of creation.
     * (i.e. file or byte array)
     * @param config the configuration object to update
     */
    private void configureCommonOptions(TConfig config) {
        config.setIdentifier(identifier);
        config.setDataUpdateLicenseKeys(licenseKeys);
        if (autoUpdateEnabled != null) {
            config.setAutomaticUpdatesEnabled(autoUpdateEnabled);
        }
        if (dataFileSystemWatcherEnabled != null) {
            config.setFileSystemWatcherEnabled(dataFileSystemWatcherEnabled);
        }
        if (updatePollingIntervalSeconds != null) {
            config.setPollingIntervalSeconds(updatePollingIntervalSeconds);
        }
        if (updateMaxRandomisationSeconds != null) {
            config.setMaxRandomisationSeconds(updateMaxRandomisationSeconds);
        }
        if (dataUpdateUrlOverride != null) {
            config.setDataUpdateUrl(dataUpdateUrlOverride);
        }
        if (dataUpdateUrlFormatter != null) {
            config.setUrlFormatter(dataUpdateUrlFormatter);
        }
        if (dataUpdateDecompress != null) {
            config.setDecompressContent(dataUpdateDecompress);
        }
        if (dataUpdateVerifyMd5 != null) {
            config.setVerifyMd5(dataUpdateVerifyMd5);
        }
        if (dataUpdateVerifyModifiedSince != null) {
            config.setVerifyModifiedSince(dataUpdateVerifyModifiedSince);
        }
        if (updateOnStartup != null) {
            config.setUpdateOnStartup(updateOnStartup);
        }
    }
}
