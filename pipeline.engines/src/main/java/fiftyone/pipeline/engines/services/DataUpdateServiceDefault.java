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

package fiftyone.pipeline.engines.services;

import fiftyone.common.wrappers.data.BinaryReader;
import fiftyone.common.wrappers.io.FileWrapper;
import fiftyone.common.wrappers.io.FileWrapperFactory;
import fiftyone.common.wrappers.io.FileWrapperFactoryDefault;
import fiftyone.common.wrappers.io.FileWrapperMemory;
import fiftyone.pipeline.engines.configuration.DataFileConfiguration;
import fiftyone.pipeline.engines.data.AspectData;
import fiftyone.pipeline.engines.data.AspectEngineDataFile;
import fiftyone.pipeline.engines.data.AspectEngineDataFileDefault;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.flowelements.OnPremiseAspectEngine;
import fiftyone.pipeline.engines.services.update.FutureFactory;
import fiftyone.pipeline.engines.services.update.FutureFactoryDefault;
import fiftyone.pipeline.util.Check;
import jakarta.xml.bind.DatatypeConverter;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import static fiftyone.pipeline.engines.services.DataUpdateService.AutoUpdateStatus.AUTO_UPDATE_REFRESH_FAILED;
import static fiftyone.pipeline.engines.services.DataUpdateService.AutoUpdateStatus.AUTO_UPDATE_SUCCESS;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.BooleanUtils.isFalse;

/**
 * Default singleton implementation of the {@link DataUpdateService}.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/features/data-updates.md">Specification</a>
 */
public class DataUpdateServiceDefault implements DataUpdateService {

    final List<AspectEngineDataFile> configurations = new ArrayList<>();
    private final Logger logger;
    private final boolean closeFutureFactory;
    private final List<OnUpdateComplete> onUpdateCompleteList = new ArrayList<>();
    private final Object configLock = new Object();
    /**
     * HTTP client used to download new data files.
     */
    private final HttpClient httpClient;
    /**
     * Random number generator used to vary the scheduled times.
     */
    private final Random random = new Random(new Date().getTime());
    /**
     * Data file factory used to access data files.
     */
    private final FileWrapperFactory fileWrapperFactory;
    private final FutureFactory futureFactory;

    /**
     * Constructor with default for everything
     */
    @SuppressWarnings("unused")
    public DataUpdateServiceDefault() {
        this(null, null, null, null);
    }

    /**
     * Construct a new instance of {@link DataUpdateService}.
     *
     * @param logger     the logger to use for logging
     * @param httpClient the HTTP client used to download new data files
     */
    public DataUpdateServiceDefault(
            Logger logger,
            HttpClient httpClient) {
        this(logger, httpClient, null, null);
    }

    /**
     * Construct a new instance of {@link DataUpdateService}.
     *
     * @param logger             the logger to use for logging
     * @param httpClient         the HTTP client used to download new data files
     * @param fileWrapperFactory the factory to create the file wrappers used to
     *                           access files
     * @param futureFactory      the future factory used to create update threads.
     *                           NOTE: This factory will be closed when the Data Update Service gets
     *                           closed so it is recommended to not share this factory, as
     *                           scheduled tasks from other objects might be shutdown unexpectedly.
     */
    public DataUpdateServiceDefault(
            Logger logger,
            HttpClient httpClient,
            FileWrapperFactory fileWrapperFactory,
            FutureFactory futureFactory) {
        if (Objects.nonNull(logger)) {
            this.logger = logger;
        } else {
            this.logger = LoggerFactory.getLogger(this.getClass());
        }
        if (fileWrapperFactory != null) {
            this.fileWrapperFactory = fileWrapperFactory;
        } else {
            this.fileWrapperFactory = new FileWrapperFactoryDefault();
        }
        if (httpClient != null) {
            this.httpClient = httpClient;
        } else {
            this.httpClient = new HttpClientDefault();
        }
        if (futureFactory != null) {
            closeFutureFactory = false;
            this.futureFactory = futureFactory;
        } else {
            closeFutureFactory = true;
            this.futureFactory = new FutureFactoryDefault();
        }
    }

    @Override
    public void onUpdateComplete(OnUpdateComplete onUpdateComplete) {
        onUpdateCompleteList.add(onUpdateComplete);
    }

    @Override
    public AutoUpdateStatus checkForUpdate(OnPremiseAspectEngine<? extends AspectData, ?
            extends AspectPropertyMetaData> engine) {
        return checkForUpdate(engine.getDataFileMetaData(), true);
    }

    @Override
    public AutoUpdateStatus checkForUpdate(OnPremiseAspectEngine<? extends AspectData, ?
            extends AspectPropertyMetaData> engine, String identifier) {
        return checkForUpdate(engine.getDataFileMetaData(identifier), true);
    }

    /**
     * Private method that performs the following actions:
     * 1. Checks for an update to the data file on disk.
     * 2. Checks for an update using the update URL.
     * 3. Refresh engine with new data if available.
     * 4. Schedule the next update check if needed.
     *
     * @param state        the data file meta data
     * @param manualUpdate true if the checkForUpdate was manually called, false
     *                     if it was called as the result of an automated update
     * @return {@link AutoUpdateStatus#AUTO_UPDATE_SUCCESS} if the data file was
     * successfully updated
     */
    private AutoUpdateStatus checkForUpdate(Object state, boolean manualUpdate) {

        AutoUpdateStatus result = AutoUpdateStatus.AUTO_UPDATE_IN_PROGRESS;
        final AspectEngineDataFileDefault dataFile =
                state instanceof AspectEngineDataFileDefault ?
                        (AspectEngineDataFileDefault) state :
                        null;
        boolean newDataAvailable = false;
        if (dataFile != null) {
            // Only check the file system if the file system watcher
            // is not enabled and the engine is using a temporary file.
            logger.debug("CheckForUpdate {}", dataFile.getDataFilePath());
            if (dataFile.getConfiguration().getFileSystemWatcherEnabled() == false &&
                    dataFile.getDataFilePath() != null &&
                    dataFile.getDataFilePath().isEmpty() == false &&
                    dataFile.getTempDataFilePath() != null &&
                    dataFile.getTempDataFilePath().isEmpty() == false) {
                long fileModified = fileWrapperFactory.getLastModified(
                        dataFile.getDataFilePath());
                long tempFileModified = fileWrapperFactory.getLastModified(
                        dataFile.getTempDataFilePath());

                // If the data file is newer than the temp file currently
                // being used by the engine then we need to tell the engine
                // to refresh itself.
                if (fileModified > tempFileModified) {
                    logger.debug("Specified file is newer than existing temp file");
                    newDataAvailable = true;
                }
            }
            if (newDataAvailable == false &&
                    dataFile.getConfiguration().getDataUpdateUrl() != null &&
                    dataFile.getConfiguration().getDataUpdateUrl().isEmpty() == false) {
                result = checkForUpdateFromUrl(dataFile);
                newDataAvailable =
                        result == AutoUpdateStatus.AUTO_UPDATE_IN_PROGRESS ||
                                result == AutoUpdateStatus.AUTO_UPDATE_SUCCESS;
            }
            if (newDataAvailable &&
                    result == AutoUpdateStatus.AUTO_UPDATE_IN_PROGRESS) {
                // Data update was available but engine has not
                // yet been refreshed.
                logger.debug("Calling updatedFileAvailable");
                result = updatedFileAvailable(dataFile);
            } else {
                // No update available.
                // If this was a manual call to update then do nothing.
                // If it was triggered by the timer expiring then modify
                // the timer to check again after the configured interval.
                // This will repeat until the update is acquired.
                if (manualUpdate == false) {
                    logger.debug("Cancelling and resetting update timer");
                    if (dataFile.getFuture() != null) {
                        dataFile.getFuture().cancel(true);
                        dataFile.setFuture(futureFactory.schedule(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        checkForUpdate(dataFile, false);
                                    }
                                },
                                getInterval(dataFile.getConfiguration())));
                    }
                }
            }
            if (manualUpdate == false) {
                // Re-register the engine with the data update service
                // so it knows when next set of data should be available.
                registerDataFile(dataFile);
            }

        }
        for (OnUpdateComplete onUpdateComplete : onUpdateCompleteList) {
            onUpdateComplete.call(state, new DataUpdateCompleteArgs(
                    result,
                    dataFile));
        }
        return result;
    }

    /**
     * Called when a data update is available and the file at
     * engine.DataFilePath contains this new data.
     * 1. Refresh the engine.
     * 2. Dispose of the existing update timer if there is one.
     * 3. Re-register the engine with the update service.
     *
     * @param dataFile the data file to update
     * @return {@link AutoUpdateStatus#AUTO_UPDATE_SUCCESS} if the data file was
     * successfully updated
     */
    private AutoUpdateStatus updatedFileAvailable(AspectEngineDataFile dataFile) {
        AspectEngineDataFileDefault aspectDataFile = (AspectEngineDataFileDefault) dataFile;

        if (aspectDataFile.getFuture() != null) {
            // Dispose of the old timer object
            aspectDataFile.getFuture().cancel(true);
            aspectDataFile.setFuture(null);
        }

        if (dataFile.getEngine() == null) {
            // Engine not yet set so no need to refresh it.
            // We can consider the update a success.
            logger.debug("No engine to update so we are done");
            return AutoUpdateStatus.AUTO_UPDATE_SUCCESS;
        }

        // Try to update the file multiple times to ensure the file is not locked.
        int tries = 0;
        while (tries < 10) {
            try {
                dataFile.getEngine().refreshData(dataFile.getIdentifier());
                return AUTO_UPDATE_SUCCESS;
            } catch (Exception ex) {
                logger.warn("File Update: Error applying a data update to engine '{}'",
                        dataFile.getEngine().getClass().getSimpleName(), ex);
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ignored) {
                }
            }
            tries++;
        }
        return AUTO_UPDATE_REFRESH_FAILED;
    }

    /**
     * Event handler that is called when the data file is updated. The
     * {@link WatchKey} will raise multiple events in many cases, for example,
     * if a file is copied over an existing file then 3 'changed' events will be
     * raised. This handler deals with the extra events by using synchronisation
     * with a double-check lock to ensure that the update will only be done once.
     *
     * @param sender the {@link AspectEngineDataFile}
     */
    private void dataFileUpdated(Object sender) {
        AutoUpdateStatus status = AutoUpdateStatus.AUTO_UPDATE_IN_PROGRESS;
        // Get the associated update configuration

        AspectEngineDataFile dataFile = null;
        for (AspectEngineDataFile current : configurations) {
            if (current.equals(sender)) {
                dataFile = current;
                break;
            }
        }

        if (dataFile != null) {
            AspectEngineDataFileDefault aspectDataFile = (AspectEngineDataFileDefault) dataFile;
            // Get the creation time of the new data file
            long modifiedTime = fileWrapperFactory.getLastModified(dataFile.getDataFilePath());
            // Use a lock with a double check on file creation time to make
            // sure we only run the update once even if multiple events fire
            // for a single file.
            if (aspectDataFile.getLastUpdateFileCreateTime() < modifiedTime) {
                synchronized (aspectDataFile.getUpdateSyncLock()) {
                    if (aspectDataFile.getLastUpdateFileCreateTime() < modifiedTime) {
                        aspectDataFile.setLastUpdateFileCreateTime(modifiedTime);

                        // Make sure we can actually open the file for reading
                        // before notifying the engine, otherwise the copy
                        // may still be in progress.
                        boolean fileLockable = false;
                        while (fileLockable == false) {
                            try (BinaryReader reader = fileWrapperFactory.build(
                                    aspectDataFile.getDataFilePath()).getReader()) {
                                fileLockable = true;
                                // Complete the update
                                status = updatedFileAvailable(dataFile);
                            } catch (IOException e) {
                                logger.warn("Exception while reading the data file at '{}'",
                                        aspectDataFile.getDataFilePath(), e);
                            }
                        }
                    }
                }
            }
        }
        for (OnUpdateComplete onUpdateComplete : onUpdateCompleteList) {
            onUpdateComplete.call(sender, new DataUpdateCompleteArgs(status, dataFile));
        }
    }

    @Override
    public AutoUpdateStatus updateFromMemory(AspectEngineDataFile dataFile, byte[] data) {
        AutoUpdateStatus result;
        if (dataFile.getDataFilePath() != null &&
                dataFile.getDataFilePath().isEmpty() == false) {
            // The engine has an associated data file so update it first.
            try {
                fileWrapperFactory.build(dataFile.getDataFilePath()).getWriter()
                        .writeBytes(data);
            } catch (Exception ex) {
                logger.error("An error occurred when writing to " +
                                "'" + dataFile.getDataFilePath() +
                                "'. The engine will be updated to use the new data " +
                                "but the file on disk will still contain old data.",
                        ex);
            }
        }

        try {
            // Refresh the engine using the new data.
            dataFile.getEngine().refreshData(dataFile.getIdentifier(), data);
            result = AUTO_UPDATE_SUCCESS;
        } catch (Exception ex) {
            logger.error("An error occurred when applying a " +
                            "data update to engine '" + dataFile.getEngine().getClass().getSimpleName() +
                            "'.",
                    ex);
            result = AutoUpdateStatus.AUTO_UPDATE_REFRESH_FAILED;
        }

        return result;
    }

    @Override
    public void registerDataFile(final AspectEngineDataFile dataFile) {
        logger.debug("Entering registerDataFile");
        if (dataFile == null) {
            throw new IllegalArgumentException("updateConfig");
        }
        boolean alreadyRegistered = dataFile.getIsRegistered();
        dataFile.setDataUpdateService(this);

        // If the data file is configured to refresh the data
        // file on startup then download an update immediately.
        // We also want to do this synchronously so that execution
        // will block until the engine is ready.
        if (dataFile.getConfiguration().getUpdateOnStartup() && isFalse(alreadyRegistered)) {
            logger.debug("Checking for Update on Startup {}", dataFile.getDataFilePath());
            AutoUpdateStatus status = checkForUpdate(dataFile, false);
            if (ObjectUtils.notEqual(status, AUTO_UPDATE_SUCCESS) &&
                    Check.notFileExists(dataFile.getDataFilePath())){
                throw new IllegalStateException("Update on start up failed and there is no " +
                        "existing data file. Status was " + status);
            }
        } else {
            final AspectEngineDataFileDefault aspectDataFile =
                    (AspectEngineDataFileDefault) dataFile;

            // Only create an automatic update timer if auto updates are
            // enabled for this engine and there is not already an associated
            // timer.
            if (aspectDataFile.getAutomaticUpdatesEnabled() &&
                    aspectDataFile.getFuture() == null) {
                long delay = getInterval(aspectDataFile.getConfiguration());
                if (aspectDataFile.getUpdateAvailableTime() != null) {
                    if (aspectDataFile.getUpdateAvailableTime().getTime()
                            > System.currentTimeMillis()) {
                        delay = applyIntervalRandomisation(
                                aspectDataFile.getUpdateAvailableTime().getTime()
                                        - System.currentTimeMillis(),
                                aspectDataFile.getConfiguration());
                    }
                }
                // Create a timer that will go off when the engine expects
                // updated data to be available.
                logger.debug("Scheduling update check with delay {}", delay);
                try {
                    ScheduledFuture<?> future = futureFactory.schedule(
                            new Runnable() {
                                @Override
                                public void run() {
                                    logger.debug("running checkForUpdate {}",
                                            aspectDataFile.getDataFilePath());
                                    checkForUpdate(aspectDataFile, false);
                                    logger.debug("Returning from checkForUpdate");
                                }
                            },
                            delay);
                    logger.debug("Update check scheduled for {} millis",
                            future.getDelay(TimeUnit.MILLISECONDS));
                    aspectDataFile.setFuture(future);
                } catch (Throwable t) {
                    logger.error("Exception when scheduling update check", t);
                }
            }

            // If file system watcher is enabled then set it up.
            if (aspectDataFile.getConfiguration().getFileSystemWatcherEnabled() &&
                    aspectDataFile.getConfiguration().getWatchKey() == null &&
                    aspectDataFile.getDataFilePath() != null &&
                    aspectDataFile.getDataFilePath().isEmpty() == false) {
                logger.debug("Initialising watch service");
                final Path aspectDataFilePath =
                        Paths.get(aspectDataFile.getDataFilePath()).toAbsolutePath();

                try {
                    if (Check.notFileExists(aspectDataFilePath.getParent().toString())) {
                        logger.error("Parent directory {} for {} does not exist",
                                aspectDataFilePath.getParent(), aspectDataFile.getDataFilePath());
                        throw new IllegalStateException("Cannot find directory to watch");
                    }
                    logger.debug("Getting a watcher using {}", aspectDataFilePath);
                    WatchService watcher = aspectDataFilePath
                            .getFileSystem()
                            .newWatchService();
                    logger.debug("Setting a watcher for {}", aspectDataFilePath.getParent());
                    aspectDataFile.getConfiguration().setWatchKey(aspectDataFilePath.getParent().register(
                            watcher,
                            StandardWatchEventKinds.ENTRY_MODIFY));
                } catch (Exception e) {
                    logger.error("File watcher for '{}' could not be initialised. " + e.getMessage(),
                            aspectDataFile.getDataFilePath(), e);
                }
                try {
                    long pollingInterval =
                            aspectDataFile.getConfiguration().getPollingIntervalSeconds() * 1000L;
                    logger.debug("Setting poll future {} millis", pollingInterval);
                    aspectDataFile.setPollFuture(futureFactory.scheduleRepeating(
                            new Runnable() {
                                @Override
                                public void run() {
                                    logger.debug("Watcher is running");
                                    for (WatchEvent<?> event :
                                            aspectDataFile.getConfiguration().getWatchKey().pollEvents()) {
                                        if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                                            if (((Path) event.context()).endsWith(aspectDataFilePath.getFileName())) {
                                                dataFileUpdated(aspectDataFile);
                                            }
                                        }
                                    }
                                    // make sure to reset the watch key or we are not called again
                                    aspectDataFile.getConfiguration().getWatchKey().reset();
                                }
                            },
                            pollingInterval));
                    logger.debug("Poll check scheduled for {} mins",
                            aspectDataFile.getPollFuture().getDelay(TimeUnit.MINUTES));
                } catch (Exception e) {
                    logger.error("Could not schedule watcher for {}.",
                            aspectDataFile.getDataFilePath(), e);
                }
            }

            synchronized (configLock) {
                // Add the configuration to the list of configurations.
                if (configurations.contains(aspectDataFile) == false) {
                    configurations.add(aspectDataFile);
                }
            }
        }
        logger.debug("Returning from registerDataFile");
    }

    @Override
    public void unregisterDataFile(AspectEngineDataFile dataFile) {
        synchronized (configLock) {
            for (Iterator<AspectEngineDataFile> iterator = configurations.iterator();
                 iterator.hasNext(); ) {
                AspectEngineDataFile config = iterator.next();
                if (config == dataFile) {
                    iterator.remove();
                }
                AspectEngineDataFileDefault aspectDataFile =
                        (AspectEngineDataFileDefault) dataFile;
                if (aspectDataFile.getConfiguration().getWatchKey() != null) {
                    if (aspectDataFile.getPollFuture() != null) {
                        aspectDataFile.getPollFuture().cancel(true);
                        aspectDataFile.setPollFuture(null);
                    }
                    aspectDataFile.getConfiguration().getWatchKey().cancel();
                    aspectDataFile.getConfiguration().setWatchKey(null);
                }
                if (aspectDataFile.getFuture() != null) {
                    aspectDataFile.getFuture().cancel(true);
                    aspectDataFile.setFuture(null);
                }
            }
        }
    }

    /**
     * Get the random delay in milliseconds to add on to the scheduled time
     * for a specific Engine, no greater than the Engine's max randomisation.
     *
     * @param config to get the delay for
     * @return a random delay in milliseconds.
     */
    private long getInterval(DataFileConfiguration config) {
        int seconds = 0;
        if (config.getPollingIntervalSeconds() > 0) {
            seconds = config.getPollingIntervalSeconds();
        }
        return applyIntervalRandomisation(seconds, config) * 1000;

    }

    /**
     * Add a random amount of time to the specified interval
     *
     * @param interval the interval to add a random amount of time to
     * @param config   the {@link DataFileConfiguration} object that specifies the
     *                 maximum number of seconds to add
     * @return the new interval
     */
    private long applyIntervalRandomisation(long interval, DataFileConfiguration config) {
        long seconds = 0;
        if (config.getMaxRandomisationSeconds() > 0) {
            seconds = random.nextInt(config.getMaxRandomisationSeconds());
        }
        return interval + seconds;
    }

    /**
     * Get the most recent data file available from the configured update URL.
     * If the data currently used by the engine is the newest available then
     * nothing will be downloaded.
     *
     * @param dataFile the data file to use
     * @param tempFile the temp file to write the data to
     * @return the {@link AutoUpdateStatus} value indicating the result
     */
    private AutoUpdateStatus downloadFile(
            AspectEngineDataFile dataFile,
            FileWrapper tempFile) {

        String url = dataFile.getFormattedUrl();
        logger.debug("downloadFile from {}", url);

        try {
            HttpURLConnection connection = httpClient.connect(new URL(url.trim()));
            if (connection == null) {
                logger.error("No response from data update service at '{}' for engine '{}'",
                        dataFile.getFormattedUrl(), Objects.nonNull(dataFile.getEngine()) ?
                                dataFile.getEngine().getClass().getSimpleName() : "No Engine");
                return AutoUpdateStatus.AUTO_UPDATE_HTTPS_ERR;
            }
            try {
                // Set last-modified header to ensure that a file will only
                // be downloaded if it is newer than the data we already have.
                if (dataFile.getConfiguration().getVerifyModifiedSince() == true) {
                    long ifModifiedSince = dataFile
                            .getDataPublishedDateTime()
                            .getTime();
                    connection.setIfModifiedSince(ifModifiedSince);
                }

                connection.setInstanceFollowRedirects(true);

                switch (connection.getResponseCode()) {
                    case (HttpURLConnection.HTTP_OK): {
                        logger.debug("HTTP Status is OK");
                        // wrap input stream in a buffered stream
                        InputStream is = new BufferedInputStream(connection.getInputStream());
                        // if checking md5 wrap in a DigestStream
                        MessageDigest md = null;
                        if (dataFile.getConfiguration().getVerifyMd5()) {
                            try {
                                md = MessageDigest.getInstance("MD5");
                                if (Objects.isNull(md)) {
                                    return AutoUpdateStatus.AUTO_UPDATE_ERR_MD5_VALIDATION_FAILED;
                                }
                            } catch (NoSuchAlgorithmException e) {
                                // this should be impossible
                                logger.error("MD5 Algorithm not found");
                                return AutoUpdateStatus.AUTO_UPDATE_ERR_MD5_VALIDATION_FAILED;
                            }
                            is = new DigestInputStream(is, md);
                        }
                        // decompress, if necessary
                        if (dataFile.getConfiguration().getDecompressContent()) {
                            is = new GZIPInputStream(is);
                        }
                        // copy resulting stream to destination
                        if (Objects.nonNull(tempFile.getPath())) {
                            logger.debug("Copying download stream");
                            // destination is a file
                            java.nio.file.Files.copy(is, Paths.get(tempFile.getPath()),
                                    StandardCopyOption.REPLACE_EXISTING);
                            logger.debug("Copied downloaded stream");
                        } else {
                            // looks like we are writing to memory, or somewhere that has no path
                            byte[] buffer = new byte[1024];
                            int len = is.read(buffer);
                            while (len != -1) {
                                tempFile.getWriter().writeBytes(buffer, len);
                                len = is.read(buffer);
                            }
                        }
                        // check md5
                        if (dataFile.getConfiguration().getVerifyMd5()) {
                            String md5Header = connection.getHeaderField("Content-MD5");
                            // for the compiler
                            assert md != null;
                            String md5Hex = DatatypeConverter.printHexBinary(md.digest());
                            if (md5Header.equalsIgnoreCase(md5Hex) == false) {
                                return AutoUpdateStatus.AUTO_UPDATE_ERR_MD5_VALIDATION_FAILED;
                            }
                        }
                        return AutoUpdateStatus.AUTO_UPDATE_IN_PROGRESS;
                    }

                    // Note: needed because TooManyRequests is not available
                    // in some versions of the HttpStatusCode enum.
                    case (429):
                        logger.error("Too many requests to '{}' for {}",
                                dataFile.getFormattedUrl(), getIdForLogging(dataFile));
                        return AutoUpdateStatus.AUTO_UPDATE_ERR_429_TOO_MANY_ATTEMPTS;
                    case HttpURLConnection.HTTP_NOT_MODIFIED:
                        logger.debug("No data update available from '{}' for {}",
                                dataFile.getFormattedUrl(), getIdForLogging(dataFile));
                        return AutoUpdateStatus.AUTO_UPDATE_NOT_NEEDED;
                    case HttpURLConnection.HTTP_FORBIDDEN:
                        logger.error("Access denied to data update service at '{}' for {}",
                                dataFile.getFormattedUrl(), getIdForLogging(dataFile));
                        return AutoUpdateStatus.AUTO_UPDATE_ERR_403_FORBIDDEN;
                    default:
                        logger.error("HTTP status code '{}' from data update service at '{}' for {}",
                                connection.getResponseCode(), dataFile.getFormattedUrl(),
                                getIdForLogging(dataFile));
                        return AutoUpdateStatus.AUTO_UPDATE_HTTPS_ERR;
                }
            } finally {
                connection.disconnect();
            }
        } catch (Exception e) {
            logger.error("Error while processing data file download", e);
            return AutoUpdateStatus.AUTO_UPDATE_HTTPS_ERR;
        }
    }

    @Override
    public void close() {
        if (closeFutureFactory) {
            try {
                futureFactory.close();
            } catch (IOException e) {
                logger.error("Error closing future factory.", e);
            }
        }
    }

    private String getIdForLogging(AspectEngineDataFile dataFile) {
        return dataFile.getEngine() == null ?
                "data file '" + dataFile.getIdentifier() + "'" :
                "engine '" + dataFile.getEngine().getClass().getSimpleName() + "'";
    }

    private AutoUpdateStatus checkForUpdateFromUrl(AspectEngineDataFile dataFile) {
        if (dataFile.getDataFilePath() == null || dataFile.getDataFilePath().isEmpty()) {
            // There is no data file path specified so perform the
            // update entirely in memory.
            try {
                FileWrapper uncompressedData = new FileWrapperMemory();

                AutoUpdateStatus memoryDownloadResult = downloadFile(dataFile, uncompressedData);
                if (memoryDownloadResult != AutoUpdateStatus.AUTO_UPDATE_IN_PROGRESS) {
                    return memoryDownloadResult;
                }
                try (BinaryReader reader = uncompressedData.getReader()) {
                    if (dataFile.getEngine() != null) {
                        // Tell the engine to refresh itself with
                        // the new data.
                        dataFile.getEngine().refreshData(dataFile.getIdentifier(),
                                reader.toByteArray());
                    } else {
                        // No associated engine at the moment so just set
                        // the value of the data array.
                        dataFile.getConfiguration().setData(reader.toByteArray());
                    }
                }
                return memoryDownloadResult;
            } catch (Exception ex) {
                logger.error("An error occurred when applying a data update to engine '{}'" ,
                                Objects.nonNull(dataFile.getEngine())?
                                        dataFile.getEngine().getClass().getName(): "no engine",ex);
                return AutoUpdateStatus.AUTO_UPDATE_REFRESH_FAILED;
            }
        }


        Path uncompressedTempFile = Paths.get(
                dataFile.getTempDataDirPath(),
                dataFile.getIdentifier() + "-" + randomUUID() + ".tmp");
        FileWrapper uncompressedData = fileWrapperFactory.build(uncompressedTempFile.toString());
        try {
            // Check if there is an update and download it if there is
            AutoUpdateStatus result = downloadFile(dataFile, uncompressedData);

            if (result == AutoUpdateStatus.AUTO_UPDATE_IN_PROGRESS) {
                WatchKey watchKey = dataFile.getConfiguration().getWatchKey();
                // If this engine has a data file watcher then we need to
                // disable it while the update is occurring.
                if (dataFile.getConfiguration().getWatchKey() != null) {
                    watchKey.cancel();
                    dataFile.getConfiguration().setWatchKey(null);
                }

                try {
                    // Move the uncompressed file to the engine's data file location
                    logger.debug("Moving {} to {}",
                            uncompressedTempFile.toFile().getAbsolutePath(),
                            dataFile.getDataFilePath());
                    Files.copy(Paths.get(uncompressedTempFile.toFile().getAbsolutePath()),
                            Paths.get(dataFile.getDataFilePath()),
                            StandardCopyOption.REPLACE_EXISTING);
                    // check the file is readable
                    File f = Paths.get(dataFile.getDataFilePath()).toFile();
                    try (DataInputStream d =
                            new DataInputStream(new BufferedInputStream(new FileInputStream(f)))){
                        logger.debug("Reading file {}", d.read());
                    }
                } catch (Exception ex) {
                    logger.error("An error occurred when moving a data file to replace " +
                                    "the existing one at {} for engine '{}'.",
                            dataFile.getDataFilePath(),
                            Objects.nonNull(dataFile.getEngine()) ?
                                    dataFile.getEngine().getClass().getSimpleName() : "No engine defined",
                            ex);
                    result = AutoUpdateStatus.AUTO_UPDATE_NEW_FILE_CANT_RENAME;
                }
                // Make sure to enable the file watcher again if needed.
                if (watchKey != null) {
                    final Path filePath =
                            Paths.get(dataFile.getDataFilePath()).toAbsolutePath();
                    WatchService watcher = filePath.getFileSystem().newWatchService();
                    watchKey = filePath.getParent().register(watcher,
                            StandardWatchEventKinds.ENTRY_MODIFY);
                    dataFile.getConfiguration().setWatchKey(watchKey);
                }
            }
            return result;
        } catch (Throwable e) {
            logger.error("Exception thrown", e);
            return AutoUpdateStatus.AUTO_UPDATE_REFRESH_FAILED;
        } finally {
            // Make sure the temp files are cleaned up
            try {
                logger.debug("Deleting temp file {}", uncompressedTempFile);
                if (Files.exists(uncompressedTempFile)) {
                    Files.delete(uncompressedTempFile);
                }
            } catch (IOException e) {
                logger.warn("Could not delete temporary download file file");
            }
        }
    }
}
