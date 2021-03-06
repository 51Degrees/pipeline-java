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

import fiftyone.pipeline.engines.Constants;
import fiftyone.pipeline.engines.data.DataUpdateUrlFormatter;

import java.nio.file.WatchKey;
import java.util.List;

/**
 * This class contains the automatic update configuration parameters that can be
 * supplied to an engine for a particular data file that the engine uses.
 */
public class DataFileConfigurationDefault implements DataFileConfiguration {
    private String identifier = "Default";
    private String dataFilePath;
    private boolean createTempDataCopy;
    private byte[] data;
    private String dataUpdateUrl;
    private boolean autoUpdatesEnabled = true;
    private List<String> licenseKeys;
    private WatchKey watchKey;
    private boolean watcherEnabled = true;
    private int pollingIntervalSeconds = Constants.DATA_UPDATE_POLLING_DEFAULT;
    private int maxRandomization = Constants.DATA_UPDATE_RANDOMISATION_DEFAULT;
    private DataUpdateUrlFormatter urlFormatter = null;
    private boolean decompress = true;
    private boolean verifyMd5 = true;
    private boolean verifyIfModifiedSince = true;
    private boolean updateOnStartup = false;

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public String getDataFilePath() {
        return dataFilePath;
    }

    @Override
    public void setDataFilePath(String dataFilePath) {
        this.dataFilePath = dataFilePath;
    }

    @Override
    public boolean getCreateTempDataCopy() {
        return createTempDataCopy;
    }

    @Override
    public void setCreateTempCopy(boolean createTempCopy) {
        this.createTempDataCopy = createTempCopy;
    }

    @Override
    public byte[] getData() {
        return data;
    }

    @Override
    public void setData(byte[] data) {
        this.data = data;
    }

    @Override
    public String getDataUpdateUrl() {
        return dataUpdateUrl;
    }

    @Override
    public void setDataUpdateUrl(String url) {
        this.dataUpdateUrl = url;
    }

    @Override
    public boolean getAutomaticUpdatesEnabled() {
        return autoUpdatesEnabled;
    }

    @Override
    public void setAutomaticUpdatesEnabled(boolean enabled) {
        this.autoUpdatesEnabled = enabled;
    }

    @Override
    public List<String> getDataUpdateLicenseKeys() {
        return licenseKeys;
    }

    @Override
    public void setDataUpdateLicenseKeys(List<String> licenseKeys) {
        this.licenseKeys = licenseKeys;
    }

    @Override
    public WatchKey getWatchKey() {
        return watchKey;
    }

    @Override
    public void setWatchKey(WatchKey watchKey) {
        this.watchKey = watchKey;
    }

    @Override
    public boolean getFileSystemWatcherEnabled() {
        return watcherEnabled;
    }

    @Override
    public void setFileSystemWatcherEnabled(boolean enabled) {
        this.watcherEnabled = enabled;
    }

    @Override
    public int getPollingIntervalSeconds() {
        return pollingIntervalSeconds;
    }

    @Override
    public void setPollingIntervalSeconds(int seconds) {
        this.pollingIntervalSeconds = seconds;
    }

    @Override
    public int getMaxRandomisationSeconds() {
        return maxRandomization;
    }

    @Override
    public void setMaxRandomisationSeconds(int seconds) {
        this.maxRandomization = seconds;
    }

    @Override
    public DataUpdateUrlFormatter getUrlFormatter() {
        return urlFormatter;
    }

    @Override
    public void setUrlFormatter(DataUpdateUrlFormatter formatter) {
        this.urlFormatter = formatter;
    }

    @Override
    public boolean getDecompressContent() {
        return decompress;
    }

    @Override
    public void setDecompressContent(boolean decompress) {
        this.decompress = decompress;
    }

    @Override
    public boolean getVerifyMd5() {
        return verifyMd5;
    }

    @Override
    public void setVerifyMd5(boolean verify) {
        this.verifyMd5 = verify;
    }

    @Override
    public boolean getVerifyModifiedSince() {
        return verifyIfModifiedSince;
    }

    @Override
    public void setVerifyModifiedSince(boolean verify) {
        this.verifyIfModifiedSince = verify;
    }

    @Override
    public boolean getUpdateOnStartup() {
        return this.updateOnStartup;
    }

    @Override
    public void setUpdateOnStartup(boolean enabled) {
        this.updateOnStartup = enabled;
    }
}
