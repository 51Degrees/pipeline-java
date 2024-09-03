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

import java.nio.file.WatchKey;
import java.util.List;

/**
 * Interface representing the configuration parameters controlling the automatic
 * update checks for a specific data file. * 
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/features/data-updates.md">Specification</a>
 */
public interface DataFileConfiguration {

    /**
     * Get the identifier of the data file that this configuration
     * information applies to.
     * If the engine only supports a single data file then this
     * value will be ignored.
     * @return data file identifier
     */
    String getIdentifier();

    /**
     * Set the identifier of the data file that this configuration
     * information applies to.
     * If the engine only supports a single data file then this
     * value will be ignored.
     * @param identifier data file identifier
     */
    void setIdentifier(String identifier);

    /**
     * Get the complete file path to the location of the data file.
     * This value will be null if the file has been supplied from
     * a byte[] in memory.
     * @return data file path or null
     */
    String getDataFilePath();

    /**
     * Set the complete file path to the location of the data file.
     * @param dataFilePath data file path
     */
    void setDataFilePath(String dataFilePath);

    /**
     * Get whether a temporary copy of the data file should be used rather than
     * using the on at the location provided directly.
     * @return true if the engine should create a temporary data file
     */
    boolean getCreateTempDataCopy();

    /**
     * Set to true if the engine should create a temporary copy of
     * the data file rather than using the one at the location
     * provided directly.
     * This setting must be set to true if automatic updates are required.
     * @param createTempCopy true if the engine should create a temporary data
     *                       file
     */
    void setCreateTempCopy(boolean createTempCopy);

    /**
     * If set, this byte array contains an in-memory representation
     * of the data used by the engine.
     * This will be null unless this instance has specifically been
     * created from a byte array.
     * @return data file as a byte array or null
     */
    byte[] getData();

    /**
     * Set the data file as a byte array. If set, this byte array contains an
     * in-memory representation of the data used by the engine.
     * @param data data file as a byte array
     */
    void setData(byte[] data);

    /**
     * Get the URL to check when looking for updates to the data file.
     * @return URL to get updates from
     */
    String getDataUpdateUrl();

    /**
     * Set the URL to check when looking for updates to the data file.
     * @param url URL to get updates from
     */
    void setDataUpdateUrl(String url);

    /**
     * Flag that indicates if updates to the data file will be checked
     * for and applied to the engine automatically or not.
     * @return true if data file updates should be scheduled
     */
    boolean getAutomaticUpdatesEnabled();

    /**
     * Set the flag that indicates if updates to the data file will be checked
     * for and applied to the engine automatically or not.
     * @param enabled true if data file updates should be scheduled
     */
    void setAutomaticUpdatesEnabled(boolean enabled);

    /**
     * Get a list of license keys to use when checking for updates using the
     * {@link #getDataUpdateUrl()}. Note that the exact formatting of the query
     * string is controlled by the configured {@link #getUrlFormatter()}.
     * @return list of license keys
     */
    List<String> getDataUpdateLicenseKeys();

    /**
     * Set the list of license keys to use when checking for updates using the
     * {@link #getDataUpdateUrl()}. Note that the exact formatting of the query
     * string is controlled by the configured {@link #getUrlFormatter()}.
     * @param licenseKeys list of license keys
     */
    void setDataUpdateLicenseKeys(List<String> licenseKeys);

    /**
     * Get the {@link WatchKey} used to watch the file system for file changes.
     * This will be null if {@link #getFileSystemWatcherEnabled()} is false.
     * @return watch key or null
     */
    WatchKey getWatchKey();


    /**
     * Set the {@link WatchKey} used to watch the file system for file changes.
     * @param watchKey watch key for the data file
     */
    void setWatchKey(WatchKey watchKey);

    /**
     * If true, a {@link WatchKey} will be created to watch the file at
     * {@link #getDataFilePath()}. If the file is modified then the engine will
     * automatically be notified so that it can refresh it's internal data
     * structures.
     * @return true if file watcher is enabled
     */
    boolean getFileSystemWatcherEnabled();

    /**
     * If true, a {@link WatchKey} will be created to watch the file at
     * {@link #getDataFilePath()}. If the file is modified then the engine will
     * automatically be notified so that it can refresh it's internal data
     * structures.
     * @param enabled true if file watcher should be
     */
    void setFileSystemWatcherEnabled(boolean enabled);

    /**
     * Get the interval between checks for updates for this data file using the
     * specified {@link #getDataUpdateUrl()}.
     * @return polling interval in seconds
     */
    int getPollingIntervalSeconds();

    /**
     * Set the interval between checks for updates for this data file using the
     * specified {@link #getDataUpdateUrl()}.
     * @param seconds polling interval in seconds
     */
    void setPollingIntervalSeconds(int seconds);

    /**
     * Get the maximum time in seconds that the polling interval may be
     * randomized by. I.e. each polling interval will be the configured
     * interval + or - a random amount between zero and this value.
     * This settings is intended to be used to allow multiple instances of a
     * system stagger their update requests to reduce chance of conflict errors
     * or slow update downloads.
     * @return maximum randomisation in seconds
     */
    int getMaxRandomisationSeconds();

    /**
     * Set the maximum time in seconds that the polling interval may be
     * randomized by. I.e. each polling interval will be the configured
     * interval + or - a random amount between zero and this value.
     * This settings is intended to be used to allow multiple instances of a
     * system stagger their update requests to reduce chance of conflict errors
     * or slow update downloads.
     * @param seconds maximum randomisation in seconds
     */
    void setMaxRandomisationSeconds(int seconds);

    /**
     * Get the formatter to use when getting the data update URL with query
     * string parameters set.
     * @return URL formatter
     */
    DataUpdateUrlFormatter getUrlFormatter();

    /**
     * Set the formatter to use when getting the data update URL with query
     * string parameters set.
     * @param formatter URL formatter
     */
    void setUrlFormatter(DataUpdateUrlFormatter formatter);

    /**
     * Must return true if the data downloaded from the
     * {@link #getDataUpdateUrl()} is compressed and false otherwise.
     * @return true if downloaded data should be decompressed
     */
    boolean getDecompressContent();

    /**
     * Set whether the data downloaded from the {@link #getDataUpdateUrl()} is
     * compressed.
     * @param decompress true if downloaded data should be decompressed
     */
    void setDecompressContent(boolean decompress);

    /**
     * Must return true if the response from the {@link #getDataUpdateUrl()}
     * is expected to include a 'Content-Md5' HTTP header that
     * contains an MD5 hash that can be used to check the
     * integrity of the downloaded content.
     * @return true if data MD5 should be verified
     */
    boolean getVerifyMd5();

    /**
     * Set whether  the response from the {@link #getDataUpdateUrl()}
     * is expected to include a 'Content-Md5' HTTP header that
     * contains an MD5 hash that can be used to check the
     * integrity of the downloaded content.
     * @param verify true if data MD5 should be verified
     */
    void setVerifyMd5(boolean verify);

    /**
     * Must return true if the request to the {@link #getDataUpdateUrl()}
     * supports the 'If-Modified-Since' header and false if it does not.
     * @return true if the 'If-Modified-Since' header should be checked
     */
    boolean getVerifyModifiedSince();

    /**
     * Set whether the request to the {@link #getDataUpdateUrl()}
     * supports the 'If-Modified-Since' header and false if it does not.
     * @param verify true if the 'If-Modified-Since' header should be checked
     */
    void setVerifyModifiedSince(boolean verify);

    /**
     * If true then when this file is registered with the data
     * update service, it will immediately try to download the latest
     * copy of the file.
     * This action will block execution until the download is complete
     * and the engine has loaded the new file.
     * @return true if the data file should be updated during startup
     */
    boolean getUpdateOnStartup();

    /**
     * Set if when this file is registered with the data update service, it will
     * immediately try to download the latest copy of the file.
     * This action will block execution until the download is complete
     * and the engine has loaded the new file.
     * @param enabled true if the data file should be updated during startup
     */
    void setUpdateOnStartup(boolean enabled);
}
