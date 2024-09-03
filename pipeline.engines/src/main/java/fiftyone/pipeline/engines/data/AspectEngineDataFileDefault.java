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

package fiftyone.pipeline.engines.data;

import fiftyone.pipeline.engines.configuration.DataFileConfiguration;
import fiftyone.pipeline.engines.flowelements.OnPremiseAspectEngine;
import fiftyone.pipeline.engines.services.DataUpdateService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Date;
import java.util.concurrent.ScheduledFuture;

/**
 * Default implementation of the {@link AspectEngineDataFile} interface.
 */
public class AspectEngineDataFileDefault implements AspectEngineDataFile {
    private String identifier;
    private OnPremiseAspectEngine<? extends AspectData, ? extends AspectPropertyMetaData> engine;
    private DataFileConfiguration configuration;
    private DataUpdateService dataUpdateService = null;
    private String tempDataDirPath;

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public OnPremiseAspectEngine<? extends AspectData, ? extends AspectPropertyMetaData> getEngine() {
        return engine;
    }

    @Override
    public void setEngine(OnPremiseAspectEngine<? extends AspectData, ? extends AspectPropertyMetaData> engine) {
        this.engine = engine;
    }

    @Override
    public String getDataFilePath() {
        return configuration.getDataFilePath();
    }

    private String tempDataFilePath;

    @Override
    public String getTempDataFilePath() {
        if (tempDataFilePath == null &&
            getTempDataDirPath() != null &&
            getDataFilePath() != null) {
            // By default, use the temp path from the engine
            // combined with the name of the data file.
            tempDataFilePath = Paths.get(getTempDataDirPath(),
                new File(getDataFilePath()).getName()).toString();
        }
        return tempDataFilePath;
    }

    @Override
    public void setTempDataFilePath(String path) {
        this.tempDataFilePath = path;
    }

    @Override
    public String getTempDataDirPath() {
        if (tempDataDirPath == null &&
            engine != null &&
            engine.getTempDataDirPath() != null) {
            // By default, use the temp path from the engine.
            tempDataDirPath = engine.getTempDataDirPath();
        }
        return tempDataDirPath;
    }

    @Override
    public void setTempDataDirPath(String path) {
        this.tempDataDirPath = path;
    }

    @Override
    public boolean getAutomaticUpdatesEnabled() {
        return configuration.getAutomaticUpdatesEnabled();
    }

    private Date updateAvailableTime;

    @Override
    public Date getUpdateAvailableTime() {
        return updateAvailableTime;
    }

    @Override
    public void setUpdateAvailableTime(Date updateAvailableTime) {
        this.updateAvailableTime = updateAvailableTime;
    }

    private Date dataPublishedTime;

    @Override
    public Date getDataPublishedDateTime() {
        return dataPublishedTime == null ? new Date(0) : dataPublishedTime;
    }

    @Override
    public void setDataPublishedDateTime(Date dataPublishedTime) {
        this.dataPublishedTime = dataPublishedTime;
    }

    @Override
    public DataFileConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public void setConfiguration(DataFileConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public boolean getIsRegistered() {
        return this.dataUpdateService != null;
    }

    @Override
    public String getFormattedUrl() {
        if (configuration.getUrlFormatter() == null) {
            return configuration.getDataUpdateUrl();
        }
        else {
            return configuration.getUrlFormatter().getFormattedDataUpdateUrl(this);
        }
    }

    @Override
    public void setDataUpdateService(DataUpdateService dataUpdateService) {
        this.dataUpdateService = dataUpdateService;
    }

    private long lastUpdateFileCreateTime;

    public long getLastUpdateFileCreateTime() {
        return lastUpdateFileCreateTime;
    }

    public void setLastUpdateFileCreateTime(long createTime) {
        lastUpdateFileCreateTime = createTime;
    }

    private final Object syncLock = new Object();

    public Object getUpdateSyncLock() {
        return syncLock;
    }

    private ScheduledFuture<?> future;

    public ScheduledFuture<?> getFuture() {
        return future;
    }

    public void setFuture(ScheduledFuture<?> future) {
        this.future = future;
    }

    private ScheduledFuture<?> pollFuture;

    public ScheduledFuture<?> getPollFuture() {
        return pollFuture;
    }

    public void setPollFuture(ScheduledFuture<?> future) {
        this.pollFuture = future;
    }

    private boolean disposedValue = false;

    @Override
    public void close() throws IOException {
        if (!disposedValue)
        {
            if (dataUpdateService != null)
            {
                dataUpdateService.unregisterDataFile(this);
                dataUpdateService = null;
            }
            if (pollFuture != null)
            {
                pollFuture.cancel(true);
            }
            if (future != null)
            {
                future.cancel(true);
            }

            disposedValue = true;
        }
    }
}
