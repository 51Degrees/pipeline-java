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

package fiftyone.pipeline.engines.configuration;

import fiftyone.pipeline.engines.data.DataUpdateUrlFormatter;

import java.nio.file.WatchKey;
import java.util.List;

public interface DataFileConfiguration {


    /**
     * The identifier of the data file that this configuration
     * information applies to.
     * If the engine only supports a single data file then this
     * value will be ignored.
     * @return
     */
    String getIdentifier();

    void setIdentifier(String identifier);

    /**
     * The complete file path to the location of the data file.
     * This value will be null if the file has been supplied from
     * a byte[] in memory.
     * @return
     */
    String getDataFilePath();

    void setDataFilePath(String dataFilePath);

    /**
     * Set to true if the engine should create a temporary copy of
     * the data file rather than using the one at the location
     * provided directly.
     * This setting must be set to true if automatic updates are required.
     * @return
     */
    boolean getCreateTempDataCopy();

    void setCreateTempCopy(boolean createTempCopy);

    /**
     * If set, this byte array contains an in-memory representation
     * of the data used by the engine.
     * This will be null unless this instance has specifically been
     * created from a byte array.
     * @return
     */
    byte[] getData();

    void setData(byte[] data);

    /**
     * The URL to check when looking for updates to the data file.
     * @return
     */
    String getDataUpdateUrl();

    void setDataUpdateUrl(String url);

    /**
     * Flag that indicates if updates to the data file will be checked
     * for and applied to the engine automatically or not.
     * @return
     */
    boolean getAutomaticUpdatesEnabled();

    void setAutomaticUpdatesEnabled(boolean enabled);

    List<String> getDataUpdateLicenseKeys();

    void setDataUpdateLicenseKeys(List<String> licenseKeys);

    WatchKey getWatchKey();

    void setWatchKey(WatchKey watchKey);

    boolean getFileSystemWatcherEnabled();

    void setFileSystemWatcherEnabled(boolean enabled);

    int getPollingIntervalSeconds();

    void setPollingIntervalSeconds(int seconds);

    int getMaxRandomisationSeconds();

    void setMaxRandomisationSeconds(int seconds);

    /**
     * The formatter to use when getting the data update URL with
     * query string parameters set.
     */
    DataUpdateUrlFormatter getUrlFormatter();

    void setUrlFormatter(DataUpdateUrlFormatter formatter);

    /**
     * Must return true if the data downloaded from the DataUpdateUrl
     * is compressed and false otherwise.
     * @return
     */
    boolean getDecompressContent();

    void setDecompressContent(boolean decompress);

    /**
     * Must return true if the response from the DataUpdateUrl
     * is expected to include a 'Content-Md5' HTTP header that
     * contains an Md5 hash that can be used to check the
     * integrity of the downloaded content.
     * @return
     */
    boolean getVerifyMd5();

    void setVerifyMd5(boolean verify);

    /**
     *  Must return true if the request to the DataUpdateUrl supports
     * the 'If-Modified-Since' header and false if it does not.
     * @return
     */
    boolean getVerifyModifiedSince();

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

    void setUpdateOnStartup(boolean enabled);
}
