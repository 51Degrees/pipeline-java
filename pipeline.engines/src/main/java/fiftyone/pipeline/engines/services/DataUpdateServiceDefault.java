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

package fiftyone.pipeline.engines.services;

import fiftyone.common.wrappers.data.BinaryReader;
import fiftyone.common.wrappers.data.BinaryWriter;
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
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.zip.GZIPInputStream;

import static fiftyone.pipeline.engines.services.DataUpdateService.AutoUpdateStatus.AUTO_UPDATE_SUCCESS;
import static java.util.UUID.randomUUID;

/**
 * Default singleton implementation of the {@link DataUpdateService}.
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
    private HttpClient httpClient = new HttpClientDefault();
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
     * Construct a new instance of {@link DataUpdateService}.
     * @param logger the logger to use for logging
     * @param httpClient the HTTP client used to download new data files
     */
    public DataUpdateServiceDefault(
        Logger logger,
        HttpClient httpClient) {
        this(logger, httpClient, null, null);
    }

    /**
     * Construct a new instance of {@link DataUpdateService}.
     * @param logger the logger to use for logging
     * @param httpClient the HTTP client used to download new data files
     * @param fileWrapperFactory the factory to create the file wrappers used to
     *                           access files
     * @param futureFactory the future factory used to create update threads
     */
    public DataUpdateServiceDefault(
        Logger logger,
        HttpClient httpClient,
        FileWrapperFactory fileWrapperFactory,
        FutureFactory futureFactory) {
        this.logger = logger;
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
    public AutoUpdateStatus checkForUpdate(OnPremiseAspectEngine<? extends AspectData, ? extends AspectPropertyMetaData> engine) {
        return checkForUpdate(engine.getDataFileMetaData(), true);
    }

    @Override
    public AutoUpdateStatus checkForUpdate(OnPremiseAspectEngine<? extends AspectData, ? extends AspectPropertyMetaData> engine, String identifier) {
        return checkForUpdate(engine.getDataFileMetaData(identifier), true);
    }

    /**
     * Private method that performs the following actions:
     * 1. Checks for an update to the data file on disk.
     * 2. Checks for an update using the update URL.
     * 3. Refresh engine with new data if available.
     * 4. Schedule the next update check if needed.
     * @param state the data file meta data
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
                // being used by the engine the we need to tell the engine
                // to refresh itself.
                if (fileModified > tempFileModified) {
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
                result = updatedFileAvailable(dataFile);
            } else {
                // No update available.
                // If this was a manual call to update then do nothing.
                // If it was triggered by the timer expiring then modify
                // the timer to check again after the configured interval.
                // This will repeat until the update is acquired.
                if (manualUpdate == false) {
                    if (dataFile.getFuture() != null) {
                        dataFile.getFuture().cancel(true);
                    }
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
     * @param dataFile the data file to update
     * @return {@link AutoUpdateStatus#AUTO_UPDATE_SUCCESS} if the data file was
     * successfully updated
     */
    private AutoUpdateStatus updatedFileAvailable(
        AspectEngineDataFile dataFile) {
        AutoUpdateStatus result = AutoUpdateStatus.AUTO_UPDATE_IN_PROGRESS;
        AspectEngineDataFileDefault aspectDataFile =
            (AspectEngineDataFileDefault)dataFile;

        int tries = 0;

        if (dataFile.getEngine() != null) {
            // Try to update the file multiple times to ensure the file is not
            // locked.
            while (result != AutoUpdateStatus.AUTO_UPDATE_SUCCESS && tries < 10) {
                try {
                    dataFile.getEngine().refreshData(dataFile.getIdentifier());
                    result = AUTO_UPDATE_SUCCESS;
                } catch (Exception ex) {
                    logger.error("An error occurred when applying a data update to " +
                                    "engine '" +
                                    dataFile.getEngine().getClass().getSimpleName() + "'.",
                            ex);
                    result = AutoUpdateStatus.AUTO_UPDATE_REFRESH_FAILED;
                    try {
                        Thread.sleep(200);
                    }
                    catch (InterruptedException e) {
                        // Do nothing
                    }
                }
                tries++;
                if (aspectDataFile.getFuture() != null) {
                    // Dispose of the old timer object
                    aspectDataFile.getFuture().cancel(true);
                    aspectDataFile.setFuture(null);
                }
            }
        }
        else {
            // Engine not yet set so no need to refresh it.
            // We can consider the update a success.
            result = AutoUpdateStatus.AUTO_UPDATE_SUCCESS;
        }

        return result;
    }

    /**
     * Event handler that is called when the data file is updated. The
     * {@link WatchKey} will raise multiple events in many cases, for example,
     * if a file is copied over an existing file then 3 'changed' events will be
     * raised. This handler deals with the extra events by using synchronisation
     * with a double-check lock to ensure that the update will only be done once.
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
            AspectEngineDataFileDefault aspectDataFile =
                (AspectEngineDataFileDefault)dataFile;
            // Get the creation time of the new data file
            long modifiedTime = fileWrapperFactory.getLastModified(
                dataFile.getDataFilePath());
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
                            try (BinaryReader reader = fileWrapperFactory.build(aspectDataFile.getDataFilePath()).getReader()) {
                                fileLockable = true;
                            } catch (IOException e) {
                                logger.warn("Exception while reading the data" +
                                    "file at '" +
                                    aspectDataFile.getDataFilePath() + "'",
                                    e);
                            }

                            // Complete the update
                            status = updatedFileAvailable(dataFile);
                        }
                    }
                }
            }
        }
        for (OnUpdateComplete onUpdateComplete : onUpdateCompleteList) {
            onUpdateComplete.call(sender, new DataUpdateCompleteArgs(
                status,
                dataFile));
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

        if (dataFile == null) {
            throw new IllegalArgumentException("updateConfig");
        }
        boolean alreadyRegistered = dataFile.getIsRegistered();
        dataFile.setDataUpdateService(this);

        // If the data file is configured to refresh the data
        // file on startup then download an update immediately.
        // We also want to do this synchronously so that execution
        // will block until the engine is ready.
        if (dataFile.getConfiguration().getUpdateOnStartup() &&
            alreadyRegistered == false) {
            checkForUpdate(dataFile, false);
        }
        else {
            final AspectEngineDataFileDefault aspectDataFile =
                (AspectEngineDataFileDefault)dataFile;

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
                ScheduledFuture<?> future = futureFactory.schedule(
                    new Runnable() {
                        @Override
                        public void run() {
                            checkForUpdate(aspectDataFile, false);
                        }
                    },
                    delay);
                aspectDataFile.setFuture(future);
            }

            // If file system watcher is enabled then set it up.
            if (aspectDataFile.getConfiguration().getFileSystemWatcherEnabled() &&
                aspectDataFile.getConfiguration().getWatchKey() == null &&
                aspectDataFile.getDataFilePath() != null &&
                aspectDataFile.getDataFilePath().isEmpty() == false) {
                try {
                    final Path filePath = Paths.get(aspectDataFile.getDataFilePath());
                    WatchService watcher = filePath
                        .getFileSystem()
                        .newWatchService();
                    aspectDataFile.getConfiguration().setWatchKey(filePath.getParent().register(
                        watcher,
                        StandardWatchEventKinds.ENTRY_MODIFY));
                    aspectDataFile.setPollFuture(futureFactory.scheduleRepeating(
                        new Runnable() {
                            @Override
                            public void run() {
                                for (WatchEvent<?> event : aspectDataFile.getConfiguration().getWatchKey().pollEvents()) {
                                    if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                                        if (((Path) event.context()).endsWith(filePath.getFileName())) {
                                            dataFileUpdated(aspectDataFile);
                                        }
                                    }
                                }
                            }
                        },
                        aspectDataFile.getConfiguration().getPollingIntervalSeconds() * 1000));
                } catch (Exception e) {
                    logger.error("File watcher for '" +
                        aspectDataFile.getEngine().getClass().getSimpleName() +
                        "' could not be initialised.", e);
                }
            }

            synchronized (configLock) {
                // Add the configuration to the list of configurations.
                if (configurations.contains(aspectDataFile) == false) {
                    configurations.add(aspectDataFile);
                }
            }
        }
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
                    (AspectEngineDataFileDefault)dataFile;
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
     * @param interval the interval to add a random amount of time to
     * @param config the {@link DataFileConfiguration} object that specifies the
     *               maximum number of seconds to add
     * @return the new interval
     */
    private long applyIntervalRandomisation(long interval, DataFileConfiguration config) {
        int seconds = 0;
        if (config.getMaxRandomisationSeconds() > 0) {
            seconds = random.nextInt(config.getMaxRandomisationSeconds());
        }
        return interval + seconds;
    }

    /**
     * Get the most recent data file available from the configured update URL.
     * If the data currently used by the engine is the newest available then
     * nothing will be downloaded.
     * @param dataFile the data file to use
     * @param tempFile the temp file to write the data to
     * @return the {@link AutoUpdateStatus} value indicating the result
     */
    @SuppressWarnings("unused")
    private DownloadResult downloadFile(
        AspectEngineDataFile dataFile,
        FileWrapper tempFile) {
        DownloadResult result = new DownloadResult();

        String url = dataFile.getFormattedUrl();

        long ifModifiedSince = -1;
        // Set last-modified header to ensure that a file will only
        // be downloaded if it is newer than the data we already have.
        if (dataFile.getConfiguration().getVerifyModifiedSince() == true) {
            ifModifiedSince = dataFile
                .getDataPublishedDateTime()
                .getTime();
        }

        HttpURLConnection connection;
        try {
            connection = httpClient.connect(new URL(url.trim()));
            if (ifModifiedSince >= 0) {
                connection.setIfModifiedSince(ifModifiedSince);
            }
            connection.setInstanceFollowRedirects(true);

            if (connection == null) {
                result.status = AutoUpdateStatus.AUTO_UPDATE_HTTPS_ERR;
                logger.error("No response from data update service at " +
                    dataFile.getFormattedUrl() + "' for engine '" +
                    dataFile.getEngine().getClass().getSimpleName() + "'.");
            }
        } catch (Exception e) {
            result.status = AutoUpdateStatus.AUTO_UPDATE_HTTPS_ERR;
            logger.error("Error accessing data update service at '" +
                    dataFile.getFormattedUrl() + "' for engine '" +
                    dataFile.getEngine().getClass().getSimpleName() + "'.",
                e);
            connection = null;
        }


        if (result.status == AutoUpdateStatus.AUTO_UPDATE_IN_PROGRESS) {
            try {
                if (connection != null &&
                    connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    // If the response is successful then save the content to a
                    // temporary file
                    try (
                        BinaryWriter os = tempFile.getWriter();
                        InputStream is = connection.getInputStream()) {
                        byte[] buffer = new byte[1000];
                            int read = is.read(buffer);
                            while (read > 0) {
                                os.writeBytes(buffer, read);
                                read = is.read(buffer);
                            }
                    }
                    if (dataFile.getConfiguration().getVerifyMd5()) {
                        result.md5Header = connection.getHeaderField("Content-MD5");
                    }
                } else if (connection != null) {
                    switch (connection.getResponseCode()) {
                        // Note: needed because TooManyRequests is not available
                        // in some versions of the HttpStatusCode enum.
                        case (429):
                            result.status = AutoUpdateStatus.
                                AUTO_UPDATE_ERR_429_TOO_MANY_ATTEMPTS;
                            logger.error("Too many requests to '" +
                                dataFile.getFormattedUrl() + "' for " +
                                getIdForLogging(dataFile));
                            break;
                        case HttpURLConnection.HTTP_NOT_MODIFIED:
                            result.status = AutoUpdateStatus.AUTO_UPDATE_NOT_NEEDED;
                            logger.debug("No data update available from '" +
                                dataFile.getFormattedUrl() + "' for " +
                                getIdForLogging(dataFile));
                            break;
                        case HttpURLConnection.HTTP_FORBIDDEN:
                            result.status = AutoUpdateStatus.AUTO_UPDATE_ERR_403_FORBIDDEN;
                            logger.error("Access denied to data update service at '" +
                                dataFile.getFormattedUrl() + "' for " +
                                getIdForLogging(dataFile));
                            break;
                        default:
                            result.status = AutoUpdateStatus.AUTO_UPDATE_HTTPS_ERR;
                            logger.error("HTTP status code '" + connection.getResponseCode() +
                                "' from data update service at '" +
                                dataFile.getFormattedUrl() + "' for " +
                                getIdForLogging(dataFile));
                            break;
                    }
                }
            } catch (IOException e) {
                logger.error("Error while processing data file download", e);
            }
        }
        if (connection != null) {
            connection.disconnect();
        }
        return result;
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

    /**
     * Helper to avoid the possibility of MD5 not being known
     */
    private MessageDigest getMd5Instance() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            // wrap caught exception as uncaught
            throw new IllegalStateException(e);
        }
    }

    /**
     * Get the hexadecimal string from a byte array.
     *
     * @param bytes hex digits as a byte array
     * @return hex string
     */
    private String getHexString(byte[] bytes) {
        StringBuilder buffer = new StringBuilder();
        for (byte aByte : bytes) {
            buffer.append(Character.forDigit(
                (aByte >> 4) & 0xF, 16));
            buffer.append(Character.forDigit((aByte & 0xF), 16));
        }

        return buffer.toString();
    }

    /**
     * Get the MD5 string from the data contained in the {@link BinaryReader}.
     * @param reader containing data to hash
     * @return MD5 string
     */
    private String getMd5(BinaryReader reader) {
        MessageDigest md5 = getMd5Instance();
        long bufferLength = 1024;
        reader.setPosition(0);
        long remaining = reader.getSize();
        while (remaining > 0) {
            md5.update(reader.readBytes((int) Math.min(bufferLength, remaining)));
            remaining = reader.getSize() - reader.getPosition();
        }
        return getHexString(md5.digest());
    }

    /**
     * Verify that the hash returned by the server matches the MD5 hash of the
     * data downloaded.
     * @param dataFile data file to check
     * @param serverHash hash from the server which should match the data
     * @param reader containing the data to hash
     * @return {@link AutoUpdateStatus#AUTO_UPDATE_IN_PROGRESS} if the has of
     * downloaded data matches the server hash
     */
    private AutoUpdateStatus verifyMd5(
        AspectEngineDataFile dataFile,
        String serverHash,
        BinaryReader reader) {
        AutoUpdateStatus status = AutoUpdateStatus.AUTO_UPDATE_IN_PROGRESS;
        String downloadHash = getMd5(reader);
        if (serverHash == null ||
            serverHash.equals(downloadHash) == false) {
            status = AutoUpdateStatus.AUTO_UPDATE_ERR_MD5_VALIDATION_FAILED;
            logger.warn(
                "Integrity check failed. MD5 hash in HTTP response " +
                    "'" + serverHash + "' for '" +
                    dataFile.getEngine().getClass().getSimpleName() +
                    "' data update does not match calculated hash for the " +
                    "downloaded file '" + downloadHash + "'.");
        }
        return status;
    }

    /**
     * Decompress a GZipped file to another location.
     * @param source compressed file
     * @param destination file that the uncompressed data will be written to
     * @return {@link AutoUpdateStatus#AUTO_UPDATE_IN_PROGRESS} if the has of
     * downloaded data matches the server hash
     */
    private AutoUpdateStatus decompress(
        FileWrapper source,
        FileWrapper destination) {
        AutoUpdateStatus status = AutoUpdateStatus.AUTO_UPDATE_IN_PROGRESS;
        byte[] buffer = new byte[1024];
        try (BinaryWriter writer = destination.getWriter();
            BinaryReader reader = source.getReader()) {
            try (GZIPInputStream input = new GZIPInputStream(
                new ByteArrayInputStream(reader.toByteArray()))) {
                int len;
                while ((len = input.read(buffer)) > 0) {
                    writer.writeBytes(buffer, len);
                }
            }
        } catch (IOException e) {
            status = AutoUpdateStatus.AUTO_UPDATE_NEW_FILE_CANT_RENAME;
        }
        return status;
    }

    private AutoUpdateStatus checkForUpdateFromUrl(AspectEngineDataFile dataFile) {
        AutoUpdateStatus result;

        if (dataFile.getDataFilePath() == null ||
            dataFile.getDataFilePath().isEmpty()) {
            // There is no data file path specified so perform the
            // update entirely in memory.
            try  {
                FileWrapper compressedData = new FileWrapperMemory();
                FileWrapper uncompressedData = new FileWrapperMemory();
                result = checkForUpdateFromUrl(dataFile,
                    compressedData,
                    uncompressedData);

                if (dataFile.getEngine() != null) {
                    // Tell the engine to refresh itself with
                    // the new data.
                    try (BinaryReader reader = uncompressedData.getReader()) {
                        dataFile.getEngine().refreshData(
                            dataFile.getIdentifier(),
                            reader.toByteArray());
                    }
                }
                else {
                    // No associated engine at the moment so just set
                    // the value of the data array.
                    try (BinaryReader reader = uncompressedData.getReader()) {
                        dataFile.getConfiguration().setData(reader.toByteArray());
                    }
                }
            }
            catch (Exception ex) {
                result = AutoUpdateStatus.AUTO_UPDATE_REFRESH_FAILED;
                logger.error("An error occurred when applying a " +
                        "data update to engine '" +
                        dataFile.getEngine().getClass().getName() + "',",
                    ex);
            }
        }
        else {
            Path compressedTempFile = Paths.get(
                dataFile.getTempDataDirPath(),
                    dataFile.getIdentifier() + "-" + randomUUID() + ".tmp");
            Path uncompressedTempFile = Paths.get(
                dataFile.getTempDataDirPath(),
                    dataFile.getIdentifier() + "-" + randomUUID() + ".tmp");
            FileWrapper uncompressedData = fileWrapperFactory.build(uncompressedTempFile.toString());
            FileWrapper compressedData = fileWrapperFactory.build(compressedTempFile.toString());
            try {
                // Check if there is an update and download it if there is
                    result = checkForUpdateFromUrl(
                        dataFile,
                        compressedData,
                        uncompressedData);

                if (result == AutoUpdateStatus.AUTO_UPDATE_IN_PROGRESS) {
                    WatchKey watchKey = dataFile.getConfiguration().getWatchKey();
                    // If this engine has a data file watcher then we need to
                    // disable it while the update is occurring.
                    if (dataFile.getConfiguration().getWatchKey() != null) {
                        dataFile.getConfiguration().setWatchKey(null);
                    }

                    // Copy the uncompressed file to the engine's
                    // data file location
                    try (BinaryReader reader = uncompressedData.getReader();
                         BinaryWriter writer = fileWrapperFactory.build(dataFile.getDataFilePath())
                             .getWriter()) {
                        writer.writeBytes(reader.toByteArray());
                    } catch (Exception ex) {
                        logger.error("An error occurred when copying a " +
                                "data file to replace the existing one at " +
                                dataFile.getDataFilePath() +
                                " for engine '" +
                                dataFile.getEngine().getClass().getSimpleName() +
                                "'.",
                            ex);
                        result = AutoUpdateStatus.AUTO_UPDATE_NEW_FILE_CANT_RENAME;
                    } finally {
                        // Make sure to enable the file watcher again
                        // if needed.
                        if (watchKey != null) {
                            watchKey.pollEvents();
                            dataFile.getConfiguration().setWatchKey(watchKey);
                        }
                    }
                }
            } catch (Throwable e) {
                result = AutoUpdateStatus.AUTO_UPDATE_HTTPS_ERR;
            } finally {
                // Make sure the temp files are cleaned up
                if (fileWrapperFactory.exists(compressedTempFile.toString())) {
                    fileWrapperFactory.delete(compressedTempFile.toString());
                }
                if (fileWrapperFactory.exists(uncompressedTempFile.toString())) {
                    fileWrapperFactory.delete(uncompressedTempFile.toString());
                }
            }
        }
        return result;
    }

    private AutoUpdateStatus checkForUpdateFromUrl(
        AspectEngineDataFile dataFile,
        FileWrapper compressedData,
        FileWrapper uncompressedData) {
        AutoUpdateStatus status;

        DownloadResult downloadResult = downloadFile(
            dataFile,
            compressedData);
        status = downloadResult.status;
        // Check data integrity
        if (status == AutoUpdateStatus.AUTO_UPDATE_IN_PROGRESS &&
            dataFile.getConfiguration().getVerifyMd5()) {
            try (BinaryReader compressedReader = compressedData.getReader()) {
                status = verifyMd5(dataFile, downloadResult.md5Header, compressedReader);
            } catch (IOException e) {
                status = AutoUpdateStatus.AUTO_UPDATE_NEW_FILE_CANT_RENAME;
            }
        }
        // decompress the file
        if (status == AutoUpdateStatus.AUTO_UPDATE_IN_PROGRESS) {
            if (dataFile.getConfiguration().getDecompressContent()) {
                    status = decompress(compressedData, uncompressedData);
            } else {
                // If decompression is not needed then just update
                // the name of the uncompressed file to point to
                // the existing one.
                try (
                    BinaryReader compressedReader = compressedData.getReader();
                    BinaryWriter uncompressedWriter = uncompressedData.getWriter()) {
                    uncompressedWriter.writeBytes(compressedReader.toByteArray());
                } catch (IOException e) {
                    status = AutoUpdateStatus.AUTO_UPDATE_NEW_FILE_CANT_RENAME;
                }
            }
        }
        return status;
    }

    private class DownloadResult {
        String md5Header = null;
        AutoUpdateStatus status = AutoUpdateStatus.AUTO_UPDATE_IN_PROGRESS;
    }
}
