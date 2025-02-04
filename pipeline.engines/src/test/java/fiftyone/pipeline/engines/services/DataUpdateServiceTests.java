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

import ch.qos.logback.classic.Level;
import fiftyone.common.testhelpers.LogbackHelper;
import fiftyone.common.testhelpers.TestLogger;
import fiftyone.common.wrappers.io.FileWrapperDefault;
import fiftyone.common.wrappers.io.FileWrapperFactory;
import fiftyone.common.wrappers.io.FileWrapperFactoryDefault;
import fiftyone.pipeline.engines.Constants;
import fiftyone.pipeline.engines.configuration.DataFileConfiguration;
import fiftyone.pipeline.engines.configuration.DataFileConfigurationDefault;
import fiftyone.pipeline.engines.data.AspectData;
import fiftyone.pipeline.engines.data.AspectEngineDataFile;
import fiftyone.pipeline.engines.data.AspectEngineDataFileDefault;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.flowelements.OnPremiseAspectEngine;
import fiftyone.pipeline.engines.services.update.FutureFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.ArgumentMatcher;
import org.mockito.internal.matchers.LessOrEqual;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static fiftyone.pipeline.engines.services.DataUpdateService.AutoUpdateStatus.AUTO_UPDATE_HTTPS_ERR;
import static fiftyone.pipeline.engines.services.DataUpdateService.AutoUpdateStatus.AUTO_UPDATE_NOT_NEEDED;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeFalse;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
@RunWith(Parameterized.class)
public class DataUpdateServiceTests {

    @Parameterized.Parameter(0)
    public boolean autoUpdateEnabled;
    @Parameterized.Parameter(1)
    public boolean setEngineNull;
    @Parameterized.Parameters(name = "{index}: Test with autoUpdateEnabled={0}, setEngineNull={1} ")
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][]{
            {true, false},
            {false, false},
            {true, true},
            {false, true}};
        return Arrays.asList(data);
    }

    private FileWrapperFactory fileWrapperFactory;
    private FutureFactory futureFactory;
    private ScheduledExecutorService executorService;
    private TestLogger logger;

    private HttpClient httpClient;
    private HttpURLConnection httpClientConnection;

    private int ignoreWranings = 0;
    private int ignoreErrors = 0;

    private DataUpdateService dataUpdate;

    private ch.qos.logback.classic.Logger realLogger;

    @Before
    public void Init() throws IOException {
        // done this way to be able to set log level programmatically
        realLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(this.getClass());
        realLogger.setLevel(Level.INFO);
        logger = new TestLogger("test", realLogger);
        // Create mocks
        fileWrapperFactory = mock(FileWrapperFactory.class);
        when(fileWrapperFactory.build(anyString())).then(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                return new FileWrapperDefault((String) invocationOnMock.getArgument(0));
            }
        });
        futureFactory = mock(FutureFactory.class);
        executorService = Executors.newSingleThreadScheduledExecutor();
        when(futureFactory.scheduleRepeating(any(Runnable.class), anyLong()))
            .then(new Answer<Object>() {
                @Override
                public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                    return executorService.scheduleAtFixedRate(
                        (Runnable) invocationOnMock.getArgument(0),
                        (long) invocationOnMock.getArgument(1),
                        (long) invocationOnMock.getArgument(1),
                        TimeUnit.MILLISECONDS);
                }
            });
        when(futureFactory.schedule(any(Runnable.class), anyLong()))
            .then(new Answer<Object>() {
                @Override
                public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                    return executorService.schedule(
                        (Runnable) invocationOnMock.getArgument(0),
                        (long) invocationOnMock.getArgument(1),
                        TimeUnit.MILLISECONDS);
                }
            });
        // Create the HttpClient using the mock handler
        httpClientConnection = mock(HttpURLConnection.class);
        when(httpClientConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpClientConnection.getResponseMessage()).thenReturn("<empty />");
        httpClient = mock(HttpClient.class);
        when(httpClient.connect(any(URL.class))).thenReturn(httpClientConnection);

        // Create the data update service
        dataUpdate = new DataUpdateServiceDefault(
            logger,
            httpClient,
            fileWrapperFactory,
            futureFactory);
    }

    @After
    public void Cleanup() {
        executorService.shutdown();
        logger.assertMaxErrors(ignoreErrors);
        logger.assertMaxWarnings(ignoreWranings);
    }

    @Test(expected = IllegalArgumentException.class)
    public void DataUpdateService_Register_Null() {
        dataUpdate.registerDataFile(null);
    }

    @Test
    public void DataUpdateService_Register_AutoUpdateDefaults() {
        // Arrange
        OnPremiseAspectEngine<? extends AspectData, ? extends AspectPropertyMetaData> engine = mock(OnPremiseAspectEngine.class);
        DataFileConfiguration config = new DataFileConfigurationDefault();
        config.setAutomaticUpdatesEnabled(true);

        AspectEngineDataFile file = new AspectEngineDataFileDefault();
        file.setEngine(engine);
        file.setConfiguration(config);

        // Act
        dataUpdate.registerDataFile(file);

        // Assert
        // Make sure the time interval is as expected
        verify(futureFactory, times(1)).schedule(
            any(Runnable.class),
            longThat(new ArgumentMatcher<Long>() {
                @Override
                public boolean matches(Long aLong) {
                    return aLong >= Constants.DATA_UPDATE_POLLING_DEFAULT * 1000 &&
                        aLong <= (Constants.DATA_UPDATE_POLLING_DEFAULT +
                            Constants.DATA_UPDATE_RANDOMISATION_DEFAULT) * 1000;
                }
            }));
    }

    @Test
    public void DataUpdateService_Register_AutoUpdateExpectedTime() {
        // Arrange
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_YEAR, 1);
        long testTime = calendar.getTimeInMillis();
        OnPremiseAspectEngine<? extends AspectData, ? extends AspectPropertyMetaData> engine = mock(OnPremiseAspectEngine.class);
        DataFileConfigurationDefault config = new DataFileConfigurationDefault();
        config.setAutomaticUpdatesEnabled(true);

        AspectEngineDataFileDefault file = new AspectEngineDataFileDefault();
        file.setUpdateAvailableTime(new Date(System.currentTimeMillis() + testTime));
        file.setEngine(engine);
        file.setConfiguration(config);

        // Act
        dataUpdate.registerDataFile(file);
        // Assert
        // Make sure the time interval is as expected
        final long minExpectedMilliSeconds = testTime - 2000;
        final long maxExpectedMiiliSeconds = testTime + (config.getMaxRandomisationSeconds() * 1000) + 2000;
        verify(futureFactory).schedule(
            any(Runnable.class),
            longThat(new ArgumentMatcher<Long>() {
                @Override
                public boolean matches(Long aLong) {
                    return aLong >= minExpectedMilliSeconds &&
                        aLong <= maxExpectedMiiliSeconds;
                }
            })
        );
    }

    @Test
    public void DataUpdateService_Register_AutoUpdateConfiguredInterval() {
        // Arrange
        OnPremiseAspectEngine<? extends AspectData, ? extends AspectPropertyMetaData> engine = mock(OnPremiseAspectEngine.class);
        DataFileConfigurationDefault config = new DataFileConfigurationDefault();
        config.setAutomaticUpdatesEnabled(true);
        config.setPollingIntervalSeconds(0);

        AspectEngineDataFileDefault file = new AspectEngineDataFileDefault();
        file.setEngine(engine);
        file.setConfiguration(config);

        // Act
        dataUpdate.registerDataFile(file);

        // Assert
        // Make sure the time interval is as expected
        verify(futureFactory).schedule(
            any(Runnable.class),
            longThat(new LessOrEqual<>(
                (long) Constants.DATA_UPDATE_RANDOMISATION_DEFAULT * 1000)));
    }

    @Test
    public void DataUpdateService_Register_AutoUpdateNoRandomisation() {
        // Arrange
        OnPremiseAspectEngine<? extends AspectData, ? extends AspectPropertyMetaData> engine = mock(OnPremiseAspectEngine.class);
        DataFileConfigurationDefault config = new DataFileConfigurationDefault();
        config.setAutomaticUpdatesEnabled(true);
        config.setMaxRandomisationSeconds(0);

        AspectEngineDataFileDefault file = new AspectEngineDataFileDefault();
        file.setEngine(engine);
        file.setConfiguration(config);

        // Act
        dataUpdate.registerDataFile(file);

        // Assert
        // Make sure the time interval is as expected
        verify(futureFactory).schedule(
            any(Runnable.class),
            eq((long) (Constants.DATA_UPDATE_POLLING_DEFAULT * 1000)));
    }

    @Test
    public void DataUpdateService_Register_AutoUpdateSameEngineTwice() {
        // Arrange
        // Configure the timer factory to return a timer that does
        // nothing. This is needed so that the timer can be stored against
        // the configuration, otherwise the second call to register will
        // not know that the engine is already registered.
        when(futureFactory.schedule(
            any(Runnable.class),
            anyLong()))
            .thenReturn(mock(ScheduledFuture.class));

        OnPremiseAspectEngine<? extends AspectData, ? extends AspectPropertyMetaData> engine = mock(OnPremiseAspectEngine.class);
        DataFileConfigurationDefault config = new DataFileConfigurationDefault();
        config.setAutomaticUpdatesEnabled(true);

        AspectEngineDataFileDefault file = new AspectEngineDataFileDefault();
        file.setEngine(engine);
        file.setConfiguration(config);

        // Act
        dataUpdate.registerDataFile(file);
        dataUpdate.registerDataFile(file);

        // Assert
        // Check that the timer factory was only called once.
        verify(futureFactory, times(1))
            .schedule(
                any(Runnable.class),
                anyLong());
    }

    @Test
    public void DataUpdateService_Register_FileSystemWatcher() throws IOException {
        // Arrange
        OnPremiseAspectEngine<? extends AspectData, ? extends AspectPropertyMetaData> engine = mock(OnPremiseAspectEngine.class);
        DataFileConfigurationDefault config = new DataFileConfigurationDefault();
        config.setAutomaticUpdatesEnabled(autoUpdateEnabled);
        config.setFileSystemWatcherEnabled(true);

        AspectEngineDataFileDefault file = new AspectEngineDataFileDefault();
        file.setEngine(engine);
        file.setConfiguration(config);

        String tempData = File.createTempFile("test", ".tmp").getAbsolutePath();

        config.setDataFilePath(tempData.toString());
        try {
            // Act
            dataUpdate.registerDataFile(file);

            // Assert
            assertNotNull(config.getWatchKey());
        } finally {
            // Make sure we tidy up the temp file.
            File tempFile = new File(tempData);
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    @Test
    public void DataUpdateService_UpdateFromWatcher() throws IOException, InterruptedException {
        assumeFalse("File watchers are not well implemented in OS X, " +
                        "so don't run this unit test as it is unlikely to pass.",
                System.getProperty("os.name").contains("Mac OS X"));

        // Arrange
        OnPremiseAspectEngine<? extends AspectData, ? extends AspectPropertyMetaData> engine = mock(OnPremiseAspectEngine.class);
        DataFileConfigurationDefault config = new DataFileConfigurationDefault();
        config.setAutomaticUpdatesEnabled(true);
        config.setFileSystemWatcherEnabled(true);
        config.setPollingIntervalSeconds(1);

        AspectEngineDataFileDefault file = new AspectEngineDataFileDefault();
        file.setEngine(engine);
        file.setConfiguration(config);

        String tempData = File.createTempFile("test", ".tmp").getAbsolutePath();
        config.setDataFilePath(tempData.toString());
        try {
            when(fileWrapperFactory.getLastModified(anyString()))
                .then(new Answer<Object>() {
                    @Override
                    public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                        return new File((String) invocationOnMock.getArgument(0)).lastModified();
                    }
                });
            when(fileWrapperFactory.build(anyString())).then(new Answer<Object>() {
                @Override
                public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                    return new FileWrapperDefault((String) invocationOnMock.getArgument(0));
                }
            });

            // Configure a flag to be set when processing
            // is complete.
            final Semaphore completeFlag = new Semaphore(1);
            completeFlag.acquire();
            dataUpdate.onUpdateComplete(new OnUpdateComplete() {
                @Override
                public void call(Object sender, DataUpdateService.DataUpdateCompleteArgs args) {
                    completeFlag.release();
                }
            });

            // Act
            dataUpdate.registerDataFile(file);
            String temp2 = File.createTempFile("test", ".tmp").getAbsolutePath();
            Files.write(Paths.get(temp2), "Testing".getBytes());
            Files.copy(Paths.get(temp2), Paths.get(tempData), StandardCopyOption.REPLACE_EXISTING);
            // Wait until processing is complete.
            boolean completed = completeFlag.tryAcquire(4, TimeUnit.SECONDS);

            // Assert
            assertTrue("The OnUpdateComplete event was never fired",
                completed);
            verify(engine, times(1)).refreshData((String)any());
        } finally {
            // Make sure we tidy up the temp file.
            File tempFile = new File(tempData);
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    @Test
    public void DataUpdateService_UpdateFromFile_FileNotUpdated() throws InterruptedException {
        // Arrange
        configureTimerImmediateCallback();
        OnPremiseAspectEngine<? extends AspectData, ? extends AspectPropertyMetaData> engine = mock(OnPremiseAspectEngine.class);
        DataFileConfigurationDefault config = new DataFileConfigurationDefault();
        config.setAutomaticUpdatesEnabled(true);
        config.setFileSystemWatcherEnabled(false);

        AspectEngineDataFileDefault file = new AspectEngineDataFileDefault();
        file.setEngine(engine);
        file.setConfiguration(config);
        when(engine.getDataFileMetaData()).thenReturn(file);

        configureFileNoUpdate(engine);

        // Configure a ManualResetEvent to be set when processing
        // is complete.
        final Semaphore completeFlag = new Semaphore(1);
        completeFlag.acquire();
        dataUpdate.onUpdateComplete(new OnUpdateComplete() {
            @Override
            public void call(Object sender, DataUpdateService.DataUpdateCompleteArgs args) {
                completeFlag.release();
            }
        });

        // Act
        dataUpdate.registerDataFile(file);
        // Wait until processing is complete.
        boolean completed = completeFlag.tryAcquire(1, TimeUnit.SECONDS);

        // Assert
        assertTrue("The OnUpdateComplete event was never fired",
            completed);
        // Make sure that refresh is not called on the engine.
        verify(engine, never()).refreshData(anyString());
        // Verify that timer factory was only called once to
        // set up the initial timer.
        verify(futureFactory, atLeast(1))
            .schedule(
                any(Runnable.class),
                anyLong());
    }

    @Test
    public void DataUpdateService_UpdateFromFile_FileUpdated() throws InterruptedException {
        // Arrange
        // Configure the timer to execute immediately.
        // When subsequent timers are created, they will not execute.
        configureTimerImmediateCallbackOnce();
        OnPremiseAspectEngine<? extends AspectData, ? extends AspectPropertyMetaData> engine = mock(OnPremiseAspectEngine.class);
        DataFileConfigurationDefault config = new DataFileConfigurationDefault();
        config.setAutomaticUpdatesEnabled(true);
        config.setFileSystemWatcherEnabled(false);

        AspectEngineDataFileDefault file = new AspectEngineDataFileDefault();
        file.setEngine(engine);
        file.setConfiguration(config);
        when(engine.getDataFileMetaData()).thenReturn(file);

        configureFileUpdate(engine);

        // Configure a ManualResetEvent to be set when processing
        // is complete.
        final Semaphore completeFlag = new Semaphore(1);
        completeFlag.acquire();
        dataUpdate.onUpdateComplete(new OnUpdateComplete() {
            @Override
            public void call(Object sender, DataUpdateService.DataUpdateCompleteArgs args) {
                completeFlag.release();
            }
        });

        // Act
        dataUpdate.registerDataFile(file);
        // Wait until processing is complete.
        boolean completed = completeFlag.tryAcquire(2, TimeUnit.SECONDS);

        // Assert
        assertTrue("The OnUpdateComplete event was never fired",
            completed);
        // Make sure that refresh is not called on the engine.
        verify(engine, times(1)).refreshData((String)any());
        // Verify that timer factory was called twice. Once for the
        // initial timer and again after the update was complete.
        verify(futureFactory, times(2)).schedule(
            any(Runnable.class),
            anyLong());
    }

    @Test
    public void DataUpdateService_UpdateFromUrl_NoUpdate() throws InterruptedException, IOException {
        // Arrange
        configureTimerImmediateCallbackOnce();
        configureHttpNoUpdateAvailable();
        // Getting no data from the URL will cause an error to be logged
        // so we need to ignore this
        ignoreWranings = 1;

        OnPremiseAspectEngine<? extends AspectData, ? extends AspectPropertyMetaData> engine = mock(OnPremiseAspectEngine.class);
        DataFileConfigurationDefault config = new DataFileConfigurationDefault();
        config.setAutomaticUpdatesEnabled(true);

        AspectEngineDataFileDefault file = new AspectEngineDataFileDefault();
        file.setEngine(engine);
        file.setConfiguration(config);

        String tempPath = System.getProperty("java.io.tmpdir");
        when(engine.getTempDataDirPath()).thenReturn(tempPath);
        config.setDataFilePath(Paths.get(tempPath, "test.dat").toString());
        config.setDataUpdateUrl("http://www.test.com");

        // Configure a ManualResetEvent to be set when processing
        // is complete.
        final Semaphore completeFlag = new Semaphore(1);
        completeFlag.acquire();
        dataUpdate.onUpdateComplete(new OnUpdateComplete() {
            @Override
            public void call(Object sender, DataUpdateService.DataUpdateCompleteArgs args) {
                completeFlag.release();
            }
        });

        // Act
        dataUpdate.registerDataFile(file);
        // Wait until processing is complete.
        boolean completed = completeFlag.tryAcquire(1, TimeUnit.SECONDS);

        // Assert
        assertTrue("The OnUpdateComplete event was never fired",
            completed);
        verify(httpClient, atLeast(1)).connect(any(URL.class));
        // Make sure engine was not refreshed
        verify(engine, never()).refreshData(anyString());
    }

    @Test
    public void DataUpdateService_UpdateFromUrl_UpdateAvailable() throws IOException, InterruptedException {
        // Arrange
        // Configure the timer to execute immediately.
        // When subsequent timers are created, they will not execute.
        configureTimerDelayCallback(1, 1000);

        realLogger.setLevel(Level.INFO);
        // Configure the mock HTTP handler to return the test data file
        when(httpClientConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpClientConnection.getResponseMessage()).thenReturn(null);
        when(httpClientConnection.getHeaderField("Content-MD5")).thenReturn("08527dcbdd437e7fa6c084423d06dba6");
        when(httpClientConnection.getInputStream()).thenReturn(
            getClass().getClassLoader().getResourceAsStream("file.gz"));

        // Configure a ManualResetEvent to be set when processing
        // is complete.
        final Semaphore completeFlag = new Semaphore(1);
        completeFlag.acquire();
        dataUpdate.onUpdateComplete(new OnUpdateComplete() {
            @Override
            public void call(Object sender, DataUpdateService.DataUpdateCompleteArgs args) {
                completeFlag.release();
            }
        });

        OnPremiseAspectEngine<? extends AspectData, ? extends AspectPropertyMetaData> engine = mock(OnPremiseAspectEngine.class);
        DataFileConfigurationDefault config = new DataFileConfigurationDefault();
        config.setAutomaticUpdatesEnabled(true);
        config.setVerifyMd5(true);
        config.setDecompressContent(true);

        AspectEngineDataFileDefault file = new AspectEngineDataFileDefault();
        file.setEngine(engine);
        file.setConfiguration(config);

        String tempPath = System.getProperty("java.io.tmpdir");
        String dataFile = File.createTempFile("test", ".tmp").getAbsolutePath();
        assertTrue("data file does not exist", Paths.get(dataFile).toFile().exists());

        try {
            // Configure the engine to return the relevant paths.
            when(engine.getTempDataDirPath()).thenReturn(tempPath);
            config.setDataFilePath(dataFile);
            config.setDataUpdateUrl("http://www.test.com");

            // Act
            dataUpdate.registerDataFile(file);
            // Wait until processing is complete.
            boolean completed = completeFlag.tryAcquire(3, TimeUnit.SECONDS);

            // Assert
            assertTrue("The OnUpdateComplete event was never fired", completed);
            verify(httpClient, times(1)).connect(
                any(URL.class));
            // Make sure engine was refreshed
            verify(engine, times(1)).refreshData((String)any());
            // The timer factory should have been called twice, once for
            // the initial registration and again after the update was
            // applied to the engine.
            verify(futureFactory, times(2)).schedule(
                any(Runnable.class),
                anyLong());
        } finally {
            File tempFile = new File(dataFile);
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    @Test
    public void DataUpdateService_UpdateFromUrl_MD5Invalid() throws IOException, InterruptedException {
        // Arrange
        // Configure the timer to execute immediately.
        // When subsequent timers are created, they will not execute.
        configureTimerImmediateCallbackOnce();

        // Configure the mock HTTP handler to return the test data file
        when(httpClientConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpClientConnection.getResponseMessage()).thenReturn(null);
        when(httpClientConnection.getHeaderField("Content-MD5")).thenReturn("08527dcbdd437e7fa6c084423d06dbd0");
        when(httpClientConnection.getInputStream()).thenReturn(
            DataUpdateServiceTests.class.getClassLoader().getResourceAsStream("file.gz"));

        // Configure a ManualResetEvent to be set when processing
        // is complete.
        final Semaphore completeFlag = new Semaphore(1);
        completeFlag.acquire();
        dataUpdate.onUpdateComplete(new OnUpdateComplete() {
            @Override
            public void call(Object sender, DataUpdateService.DataUpdateCompleteArgs args) {
                completeFlag.release();
            }
        });

        // The invalid MD5 will cause a warning to be logged so we want
        // to ignore this
        ignoreWranings = 1;

        OnPremiseAspectEngine<? extends AspectData, ? extends AspectPropertyMetaData> engine = mock(OnPremiseAspectEngine.class);
        DataFileConfigurationDefault config = new DataFileConfigurationDefault();
        config.setAutomaticUpdatesEnabled(true);
        config.setVerifyMd5(true);
        config.setFileSystemWatcherEnabled(false);

        AspectEngineDataFileDefault file = new AspectEngineDataFileDefault();
        file.setEngine(engine);
        file.setConfiguration(config);

        String tempPath = System.getProperty("java.io.tmpdir");
        String dataFile = File.createTempFile("test", ".tmp").getAbsolutePath();

        try {
            // Configure the engine to return the relevant paths.
            when(engine.getTempDataDirPath()).thenReturn(tempPath);
            config.setDataFilePath(dataFile);
            config.setDataUpdateUrl("http://www.test.com");

            // Act
            dataUpdate.registerDataFile(file);
            // Wait until processing is complete.
            boolean completed = completeFlag.tryAcquire(1, TimeUnit.SECONDS);

            // Assert
            assertTrue("The OnUpdateComplete event was never fired",
                completed);
            verify(httpClient, times(1)).connect(
                any(URL.class));
            // Make sure engine was not refreshed
            verify(engine, never()).refreshData(anyString());
            // The timer factory should only be called once when the engine
            // is registered. As the update fails, the timer will be reset
            // rather than creating a new one with new data.
            verify(futureFactory, atLeast(1)).schedule(
                any(Runnable.class),
                anyLong());
        } finally {
            File tempFile = new File(dataFile);
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }


    /**
     * Configure the engine to have a temp path but no temp file.
     * Therefore file system check will not occur but URL check will.
     * Configure HTTP handler to return an exception and ensure it
     * is handled correctly.
     *
     * This test writes to and reads from the file system temp path.
     * This is required in this case in order to fully test the
     * interaction between the various temp files that are required.
     */
    @Test
    public void DataUpdateService_UpdateFromUrl_HttpException() throws IOException, InterruptedException {
        // Arrange
        // For this test we want to use the real FileWrapper to allow
        // the test to perform file system read/write operations.
        configureRealFileSystem();
        // Configure the timer to execute immediately.
        // When subsequent timers are created, they will not execute.
        configureTimerImmediateCallbackOnce();

        // Configure the mock HTTP handler to throw an exception
        final String errorText = "There was an error!";
        when(httpClient.connect(any(URL.class))).then(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                throw new Exception(errorText);
            }
        });

        // Configure a ManualResetEvent to be set when processing
        // is complete.
        final Semaphore completeFlag = new Semaphore(1);
        completeFlag.acquire();
        final DataUpdateService.DataUpdateCompleteArgs[] completeEventArgs = {null};
        dataUpdate.onUpdateComplete(new OnUpdateComplete() {
            @Override
            public void call(Object sender, DataUpdateService.DataUpdateCompleteArgs args) {
                completeEventArgs[0] = args;
                completeFlag.release();
            }
        });

        OnPremiseAspectEngine<? extends AspectData, ? extends AspectPropertyMetaData> engine = mock(OnPremiseAspectEngine.class);
        String tempPath = System.getProperty("java.io.tmpdir");
        String dataFile = File.createTempFile("test", ".tmp").getAbsolutePath();
        try {
            // Configure the engine to return the relevant paths.
            when(engine.getTempDataDirPath()).thenReturn(tempPath);
            DataFileConfiguration config = new DataFileConfigurationDefault();
            config.setAutomaticUpdatesEnabled(true);
            config.setDataUpdateUrl("http://www.test.com");
            config.setDataFilePath(dataFile);
            config.setVerifyMd5(true);
            config.setDecompressContent(true);
            config.setFileSystemWatcherEnabled(false);
            config.setVerifyModifiedSince(false);
            config.setUpdateOnStartup(false);
            AspectEngineDataFile file = new AspectEngineDataFileDefault();
            file.setEngine(engine);
            file.setConfiguration(config);
            when(engine.getDataFileMetaData(anyString())).thenReturn(file);

            realLogger.info("This test deliberately causes errors");
            // change logback set up so that any errors are logged as intentional from here
            LogbackHelper.intentionalErrorConfig();

            dataUpdate.registerDataFile(file);
            // Wait until processing is complete.
            boolean completed = completeFlag.tryAcquire(1, TimeUnit.SECONDS);

            // reset the logback config to log errors in red as usual
            LogbackHelper.defaultConfig();

            // Assert
            assertTrue("The 'checkForUpdateComplete' " +
                "event was never fired",
                completed);
            verify(httpClient, times(1))
                .connect(any(URL.class));
            // Make sure engine was not refreshed
            verify(engine, never()).refreshData(config.getIdentifier());
            // The timer factory should have been called once for
            // the initial registration.
            // After the update fails, the same timer will be
            // reconfigured to try again later.
            verify(futureFactory, atLeast(1))
                .schedule(any(Runnable.class), anyLong());
            // We expect one error to be logged so make sure it's
            // ignored in cleanup and verify its presence.
            ignoreErrors = 1;
            assertEquals(1, logger.errorsLogged.size());
            assertEquals(AUTO_UPDATE_HTTPS_ERR, completeEventArgs[0].getStatus());
        }
        finally {
            File tempFile = new File(dataFile);
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }
    @Test
    public void DataUpdateService_UpdateAllEnabled_NoUpdates() throws IOException, InterruptedException {
        // Arrange
        configureTimerImmediateCallbackOnce();
        configureHttpNoUpdateAvailable();
        // Getting no data from the URL will cause an error to be logged
        // so we need to ignore this
        ignoreWranings = 1;

        OnPremiseAspectEngine<? extends AspectData, ? extends AspectPropertyMetaData> engine = mock(OnPremiseAspectEngine.class);
        DataFileConfigurationDefault config = new DataFileConfigurationDefault();
        config.setAutomaticUpdatesEnabled(true);
        config.setFileSystemWatcherEnabled(true);

        AspectEngineDataFileDefault file = new AspectEngineDataFileDefault();
        file.setEngine(engine);
        file.setConfiguration(config);

        String tempPath = System.getProperty("java.io.tmpdir");
        String dataFile = File.createTempFile("test", ".tmp").getAbsolutePath();
        try {
            // Configure the engine to return the relevant paths.
            when(engine.getTempDataDirPath()).thenReturn(tempPath);
            config.setDataFilePath(dataFile);
            config.setDataUpdateUrl("http://www.test.com");

            // Configure a ManualResetEvent to be set when processing
            // is complete.
            final Semaphore completedFlag = new Semaphore(1);
            completedFlag.acquire();
            dataUpdate.onUpdateComplete(new OnUpdateComplete() {
                @Override
                public void call(Object sender, DataUpdateService.DataUpdateCompleteArgs args) {
                    completedFlag.release();
                }
            });

            // Act
            dataUpdate.registerDataFile(file);
            // Wait until processing is complete.
            boolean completed = completedFlag.tryAcquire(1, TimeUnit.SECONDS);

            // Assert
            assertTrue("The OnUpdatedComplete event was never fired",
                completed);
            verify(httpClient, times(1)).connect(
                any(URL.class));
            // Make sure engine was not refreshed
            verify(engine, never()).refreshData(anyString());
            // Verify that timer factory was only called once to
            // set up the initial timer.
            verify(futureFactory, atLeast(1)).schedule(
                any(Runnable.class),
                anyLong());
        } finally {
            File tempFile = new File(dataFile);
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    @Test
    public void DataUpdateService_CheckForUpdate_FileNotUpdated() {
        // Arrange
        OnPremiseAspectEngine<? extends AspectData, ? extends AspectPropertyMetaData> engine = mock(OnPremiseAspectEngine.class);
        DataFileConfigurationDefault config = new DataFileConfigurationDefault();

        AspectEngineDataFileDefault file = new AspectEngineDataFileDefault();
        file.setEngine(engine);
        file.setConfiguration(config);
        when(engine.getDataFileMetaData()).thenReturn(file);

        configureFileNoUpdate(engine);

        // Act
        dataUpdate.checkForUpdate(engine);

        // Assert
        // Make sure that refresh is not called on the engine.
        verify(engine, never()).refreshData(anyString());
        // Verify that timer factory has not been called to
        // set up a new timer.
        verify(futureFactory, never()).schedule(
            any(Runnable.class),
            anyLong());
    }

    @Test
    public void DataUpdateService_CheckForUpdate_FileUpdated() {
        // Arrange
        OnPremiseAspectEngine<? extends AspectData, ? extends AspectPropertyMetaData> engine = mock(OnPremiseAspectEngine.class);
        DataFileConfigurationDefault config = new DataFileConfigurationDefault();
        config.setAutomaticUpdatesEnabled(true);
        config.setFileSystemWatcherEnabled(false);

        AspectEngineDataFileDefault file = new AspectEngineDataFileDefault();
        file.setEngine(engine);
        file.setConfiguration(config);
        when(engine.getDataFileMetaData()).thenReturn(file);

        configureFileUpdate(engine);

        // Act
        dataUpdate.checkForUpdate(engine);

        // Assert
        // Make sure that refresh is called on the engine.
        verify(engine, times(1)).refreshData((String)any());
        // Verify that timer factory has not been called to
        // set up a new timer.
        verify(futureFactory, never()).schedule(
            any(Runnable.class),
            anyLong());
    }

    @Test
    public void DataUpdateService_CheckForUpdate_UrlNoUpdate() throws IOException {
        // Getting no data from the URL will cause an error to be logged
        // so we need to ignore this
        ignoreWranings = 1;

        // Arrange
        configureHttpNoUpdateAvailable();
        OnPremiseAspectEngine<? extends AspectData, ? extends AspectPropertyMetaData> engine = mock(OnPremiseAspectEngine.class);
        DataFileConfigurationDefault config = new DataFileConfigurationDefault();
        config.setAutomaticUpdatesEnabled(true);
        config.setDataUpdateUrl("http://www.test.com");

        AspectEngineDataFileDefault file = new AspectEngineDataFileDefault();
        file.setEngine(engine);
        file.setConfiguration(config);

        String tempPath = System.getProperty("java.io.tmpdir");
        when(engine.getTempDataDirPath()).thenReturn(tempPath);
        when(engine.getDataFileMetaData()).thenReturn(file);

        // Act
        dataUpdate.checkForUpdate(engine);

        // Assert
        // Make sure that refresh is not called on the engine.
        verify(engine, never()).refreshData(anyString());
    }



    /**
     * Configure the engine to update on startup and have no existing
     * data file on disk.
     * The update service should download the latest file and save it.
     *
     * This test writes to and reads from the file system temp path.
     * This is required in this case in order to fully test the
     * interaction between the various temp files that are required.
     */
    @Test
    public void DataUpdateService_Register_UpdateOnStartup_NoFile() throws IOException, InterruptedException {
        // Arrange
        // For this test we want to use the real FileWrapper to allow
        // the test to perform file system read/write operations.
        configureRealFileSystem();
        // Configure the timer to execute as normal
        configureTimerAccurateCallback();

        // Configure the mock HTTP handler to return the test data file
        when(httpClientConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpClientConnection.getResponseMessage()).thenReturn(null);
        when(httpClientConnection.getHeaderField("Content-MD5")).thenReturn("08527dcbdd437e7fa6c084423d06dba6");
        when(httpClientConnection.getInputStream()).thenReturn(
            DataUpdateServiceTests.class.getClassLoader().getResourceAsStream("file.gz"));

        // Configure a flag to be set when processing
        // is complete.
        final Semaphore completeFlag = new Semaphore(1);
        completeFlag.acquire();
        dataUpdate.onUpdateComplete(new OnUpdateComplete() {
            @Override
            public void call(Object sender, DataUpdateService.DataUpdateCompleteArgs args) {
                completeFlag.release();
            }
        });

        OnPremiseAspectEngine<? extends AspectData, ? extends AspectPropertyMetaData> engine = mock(OnPremiseAspectEngine.class);
        String tempDir = System.getProperty("java.io.tmpdir");
        Path dataFile = Paths.get(tempDir, getClass().getName() + ".tmp");
        // We want to make sure there is no existing data file.
        // The update service should create it.
        Files.deleteIfExists(dataFile);
        try
        {
            // Configure the engine to return the relevant paths.
            when(engine.getTempDataDirPath()).thenReturn(tempDir);
            DataFileConfiguration config = new DataFileConfigurationDefault();
            config.setAutomaticUpdatesEnabled(autoUpdateEnabled);
            config.setDataUpdateUrl("http://test.com");
            config.setDataFilePath(dataFile.toString());
            config.setVerifyMd5(true);
            config.setDecompressContent(true);
            config.setFileSystemWatcherEnabled(false);
            config.setVerifyModifiedSince(false);
            config.setUpdateOnStartup(true);
            AspectEngineDataFile file = new AspectEngineDataFileDefault();
            // Don't set the engine as it will not have been created yet.
             file.setEngine(setEngineNull ? null : engine);
             when(engine.getDataFileMetaData()).thenReturn(file);
             when(engine.getDataFileMetaData(anyString())).thenReturn(file);
            file.setConfiguration(config);
            file.setTempDataDirPath(tempDir);

            // Check that files do not exist before the test starts
            assertFalse(
                "Data file already exists before test starts",
                Files.exists(Paths.get(file.getDataFilePath())));
            assertFalse(
                "Temp data file already exists before test starts",
                file.getTempDataFilePath() != null &&
                Files.exists(Paths.get(file.getTempDataFilePath())));

            // Act
            dataUpdate.registerDataFile(file);
            // Wait until processing is complete.
            boolean completed = completeFlag.tryAcquire(2, TimeUnit.SECONDS);

            // Assert
            assertTrue(
                "The 'CheckForUpdateComplete' event was never fired",
                completed);
            verify(httpClient, times(1)).connect(any(URL.class));
            // The timer factory should have been called once.
            if (autoUpdateEnabled) {
                verify(futureFactory, times(1))
                    .schedule(any(Runnable.class), anyLong());
            }
            // Check that files exist at both the original and temporary
            // locations.
            assertTrue(
                "Data file does not exist after test",
                Files.exists(Paths.get(file.getDataFilePath())));
            assertTrue("Temp data file does not exist after test",
                Files.exists(Paths.get(file.getTempDataFilePath())));
        }
        finally {
            Files.deleteIfExists(dataFile);
        }
    }

    /**
     * Configure the engine to update on startup and operate
     * entirely in memory
     * The update service should download the latest file and
     * use it to refresh the engine.
     */
    @Test
    public void DataUpdateService_Register_UpdateOnStartup_InMemory() throws InterruptedException, IOException {
        // Arrange
        // Configure the test to have no file system. This will
        // verify that everything takes place in memory.
        configureNoFileSystem();
        // Configure the timer to execute as normal
        configureTimerAccurateCallback();

        // Configure the mock HTTP handler to return the test data file
        when(httpClientConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpClientConnection.getResponseMessage()).thenReturn(null);
        when(httpClientConnection.getHeaderField("Content-MD5")).thenReturn("08527dcbdd437e7fa6c084423d06dba6");
        when(httpClientConnection.getInputStream()).thenReturn(
            DataUpdateServiceTests.class.getClassLoader().getResourceAsStream("file.gz"));

        // Configure a ManualResetEvent to be set when processing
        // is complete.
        final Semaphore completeFlag = new Semaphore(1);
        completeFlag.acquire();
        dataUpdate.onUpdateComplete(new OnUpdateComplete() {
            @Override
            public void call(Object sender, DataUpdateService.DataUpdateCompleteArgs args) {
                completeFlag.release();
            }
        });

        OnPremiseAspectEngine<? extends AspectData, ? extends AspectPropertyMetaData> engine = mock(OnPremiseAspectEngine.class);
        String tempPath = System.getProperty("java.io.tmpdir");

        // Configure the engine to return the relevant paths.
        when(engine.getTempDataDirPath()).thenReturn(tempPath);
        DataFileConfiguration config = new DataFileConfigurationDefault();
        config.setAutomaticUpdatesEnabled(true);
        config.setDataUpdateUrl("http://test.com");
        config.setVerifyMd5(true);
        config.setDecompressContent(true);
        config.setFileSystemWatcherEnabled(false);
        config.setVerifyModifiedSince(false);
        config.setUpdateOnStartup(true);
        config.setData(null);
        AspectEngineDataFile file = new AspectEngineDataFileDefault();
        file.setConfiguration(config);
        file.setEngine(setEngineNull ? null : engine);

        when(engine.getDataFileMetaData()).thenReturn(file);

        // Act
        dataUpdate.registerDataFile(file);

        // Wait until processing is complete.
        boolean completed = completeFlag.tryAcquire(1, TimeUnit.SECONDS);

        // Assert
        assertTrue(
            "The 'CheckForUpdateComplete' event was never fired",
            completed);
        verify(httpClient, times(1)).connect(any(URL.class));
        if (setEngineNull) {
            assertNotEquals(
                DataUpdateServiceTests.class.getClassLoader().getResource("file.gz").getContent(),
                file.getConfiguration().getData());
        }
        else {
            // Make sure engine was refreshed
            verify(engine, times(1)).refreshData(
                (String) any(),
                argThat(
                    new ArgumentMatcher<byte[]>() {
                        @Override
                        public boolean matches(byte[] argument) {
                            return argument != null;
                        }
                    }));
        }
        // The timer factory should have been called once.
        verify(futureFactory, times(1)).schedule(any(Runnable.class), anyLong());
    }



    /**
     * Configure the engine to update on startup.
     * The update service will find that an update is not required.
     * In this scenario, confirm that the timer is configured so that
     * the service will check again in the future.
     */
    @Test
    public void DataUpdateService_Register_UpdateOnStartup_NotNeeded() throws IOException, InterruptedException {
        // Arrange
        // For this test we want to use the real FileWrapper to allow
        // the test to perform file system read/write operations.
        configureRealFileSystem();
        // Configure the timer to execute as normal
        configureTimerAccurateCallback();
        // Configure the HTTP client to return a 'NOT_MODIFIED' status
        configureHttpNoUpdateAvailable();

        // Configure a ManualResetEvent to be set when processing
        // is complete.
        final Semaphore completeFlag = new Semaphore(1);
        completeFlag.acquire();
        final DataUpdateService.DataUpdateCompleteArgs[] completeEventArgs = {null};
        dataUpdate.onUpdateComplete(new OnUpdateComplete() {
            @Override
            public void call(Object sender, DataUpdateService.DataUpdateCompleteArgs args) {
                completeEventArgs[0] = args;
                completeFlag.release();
            }
        });

        OnPremiseAspectEngine<? extends AspectData, ? extends AspectPropertyMetaData> engine = mock(OnPremiseAspectEngine.class);
        String tempDir = System.getProperty("java.io.tmpdir");
        Path dataFile = Paths.get(tempDir, getClass().getName() + ".tmp");

        try (FileWriter writer = new FileWriter(dataFile.toFile().getAbsolutePath())) {
            writer.write("TEST");
        }

        try {
            // Configure the engine to return the relevant paths.
            when(engine.getTempDataDirPath()).thenReturn(tempDir);
            DataFileConfiguration config = new DataFileConfigurationDefault();
            config.setAutomaticUpdatesEnabled(autoUpdateEnabled);
            config.setDataUpdateUrl("http://www.test.com");
            config.setDataFilePath(dataFile.toFile().getAbsolutePath());
            config.setVerifyMd5(true);
            config.setDecompressContent(true);
            config.setFileSystemWatcherEnabled(false);
            config.setVerifyModifiedSince(false);
            config.setUpdateOnStartup(true);
            AspectEngineDataFile file = new AspectEngineDataFileDefault();
            file.setEngine(setEngineNull ? null : engine);
            file.setConfiguration(config);
            file.setTempDataDirPath(tempDir);

            when(engine.getDataFileMetaData(anyString())).thenReturn(file);

            // Act
            dataUpdate.registerDataFile(file);
            // Wait until processing is complete.
            boolean completed = completeFlag.tryAcquire(1000, TimeUnit.SECONDS);

            // Assert
            assertTrue("The 'CheckForUpdateComplete' " +
                "event was never fired",
                completed);
            verify(httpClient, times(1)).connect(any(URL.class));

            if (autoUpdateEnabled) {
                // If auto update is enabled then the timer factory
                // should have been called once to set up the next
                // update check.
                verify(futureFactory, times(1))
                    .schedule(any(Runnable.class), anyLong());
            }
            assertEquals(AUTO_UPDATE_NOT_NEEDED, completeEventArgs[0].getStatus());
        }
        finally {
            Files.deleteIfExists(dataFile);
        }
    }

    /**
     * Check that unregistering a data file works without exception.
     */
    @Test
    public void DataUpdateService_Unregister() {
        OnPremiseAspectEngine<? extends AspectData, ? extends AspectPropertyMetaData> engine = mock(OnPremiseAspectEngine.class);

        DataFileConfiguration config = new DataFileConfigurationDefault();
        config.setAutomaticUpdatesEnabled(true);
        config.setPollingIntervalSeconds(Integer.MAX_VALUE);
        AspectEngineDataFile file = new AspectEngineDataFileDefault();
        file.setEngine(engine);
        file.setConfiguration(config);

        // Act
        dataUpdate.registerDataFile(file);

        // Assert
        dataUpdate.unregisterDataFile(file);
    }

    /**
     * Check that a failure when updating does not prevent the next
     * update check from occurring.
     */
    @Test
    public void DataUpdateService_Register_TimerSetAfter429() throws InterruptedException, IOException {
        // Arrange
        OnPremiseAspectEngine<? extends AspectData, ? extends AspectPropertyMetaData> engine = mock(OnPremiseAspectEngine.class);
        DataFileConfiguration config = new DataFileConfigurationDefault();
        config.setAutomaticUpdatesEnabled(true);
        config.setPollingIntervalSeconds(10);
        config.setMaxRandomisationSeconds(0);
        config.setDataUpdateUrl("https://test.com");
        config.setFileSystemWatcherEnabled(false);
        
        AspectEngineDataFileDefault file = new AspectEngineDataFileDefault();
        file.setEngine(engine);
        file.setConfiguration(config);
        // Set defaults as no engine is configured.
        file.setIdentifier("default");
        file.setTempDataDirPath("C:/test");
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        file.setUpdateAvailableTime(calendar.getTime());

        when(engine.getDataFileMetaData()).thenReturn(file);
        configureFileNoUpdate(engine);
        configureHttpTooManyRequests();
        configureTimerImmediateCallback(1, terminalFuture);
        // Configure a ManualResetEvent to be set when processing
        // is complete.
        final Semaphore completeFlag = new Semaphore(1);
        completeFlag.acquire();
        final DataUpdateService.DataUpdateCompleteArgs[] completeEventArgs = {null};
        dataUpdate.onUpdateComplete(new OnUpdateComplete() {
            @Override
            public void call(Object sender, DataUpdateService.DataUpdateCompleteArgs args) {
                completeEventArgs[0] = args;
                completeFlag.release();
            }
        });

        // don't highlight errors in Red
        LogbackHelper.intentionalErrorConfig();
        // Act
        dataUpdate.registerDataFile(file);

        // Wait until processing is complete.
        boolean completed = completeFlag.tryAcquire(1, TimeUnit.SECONDS);

        // reset error highlighting
        LogbackHelper.intentionalErrorConfig();

        // Assert
        assertTrue("The 'checkForUpdateComplete' " +
            "event was never fired",
            completed);
        // Ignore the error that is logged due to the 429
        ignoreErrors = 1;
        // Check that the timer has been set to go off again.
        assertNotNull(file.getFuture());
        // Check that the timer has been set to expire in 10 seconds.
        assertEquals(10000, lastDelay);
    }

    private long lastDelay = -1;

    private void configureNoFileSystem() {
        // Configure the file wrapper` to be 'strict'. This means that
        // and calls to methods that have not been set up will throw
        // an exception.
        // We use this behavior to verify there is no interaction
        // with the file system.
        fileWrapperFactory = mock(
            FileWrapperFactory.class,
            new Answer<Object>() {
                @Override
                public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                    throw new IllegalArgumentException(
                        "'" + invocationOnMock.getMethod().getName() +
                            "' should not have been called during this test");
                }
            });
        dataUpdate = new DataUpdateServiceDefault(
            logger,
            httpClient,
            fileWrapperFactory,
            futureFactory);
    }

    private void configureRealFileSystem() {
        // For this test we want to use the real FileWrapper to allow
        // the test to perform file system read/write operations.
        fileWrapperFactory = new FileWrapperFactoryDefault();
        dataUpdate = new DataUpdateServiceDefault(
            logger,
            httpClient,
            fileWrapperFactory,
            futureFactory);
    }

    private void configureTimerAccurateCallback() {
        // Configure the timer factory to return a timer that will
        // execute the callback immediately
        when(futureFactory.schedule(any(Runnable.class), anyLong()))
            .thenAnswer(new Answer<ScheduledFuture<?>>() {
                @Override
                public ScheduledFuture<?> answer(InvocationOnMock invocationOnMock) throws Throwable {
                    lastDelay = invocationOnMock.getArgument(1);
                    return executorService.schedule(
                        (Runnable)invocationOnMock.getArgument(0),
                        (long)invocationOnMock.getArgument(1),
                        TimeUnit.MILLISECONDS);
                }
            });
    }

    private void configureTimerImmediateCallback() {
        // Configure the timer factory to return a timer that will
        // execute the callback immediately
        when(futureFactory.schedule(
                any(Runnable.class),
                anyLong())).then(new Answer<ScheduledFuture<?>>() {
            @Override
            public ScheduledFuture<?> answer(InvocationOnMock invocationOnMock) throws Throwable {
                lastDelay = invocationOnMock.getArgument(1);
                return executorService.schedule(
                        (Runnable) invocationOnMock.getArgument(0),
                        0,
                        TimeUnit.SECONDS);
            }
        });
    }

    private static final ScheduledFuture<?> terminalFuture = mock(ScheduledFuture.class);

    private void configureTimerImmediateCallback(final int n, final ScheduledFuture<?> termination) {
        // Configure the timer factory to return a timer that will
        // execute the callback immediately
        final AtomicInteger counter = new AtomicInteger(0);
        when(futureFactory.schedule(
            any(Runnable.class),
            anyLong())).then(new Answer<ScheduledFuture<?>>() {
            @Override
            public ScheduledFuture<?> answer(InvocationOnMock invocationOnMock) throws Throwable {
                if (counter.get() < n) {
                    counter.incrementAndGet();
                    lastDelay = invocationOnMock.getArgument(1);
                    return executorService.schedule(
                        (Runnable) invocationOnMock.getArgument(0),
                        0,
                        TimeUnit.MILLISECONDS);
                } else {
                    return executorService.schedule((Runnable) invocationOnMock.getArgument(0),
                            lastDelay, TimeUnit.MILLISECONDS);
                }
            }
        });
    }

    private void configureTimerDelayCallback(final int n, final int delay) {
        // Configure the timer factory to return a timer that will
        // execute the callback immediately
        final AtomicInteger counter = new AtomicInteger(0);
        when(futureFactory.schedule(
                any(Runnable.class),
                anyLong())).then(new Answer<ScheduledFuture<?>>() {
            @Override
            public ScheduledFuture<?> answer(InvocationOnMock invocationOnMock) throws Throwable {
                if (counter.get() < n) {
                    counter.incrementAndGet();
                    lastDelay = invocationOnMock.getArgument(1);
                    return executorService.schedule(
                            (Runnable) invocationOnMock.getArgument(0),
                            delay,
                            TimeUnit.MILLISECONDS);
                } else {
                    return executorService.schedule((Runnable) invocationOnMock.getArgument(0),
                            lastDelay, TimeUnit.MILLISECONDS);
                }
            }
        });
    }

    private void configureTimerImmediateCallbackOnce() {
        configureTimerImmediateCallback(1, null);
    }

    private void configureFileNoUpdate(OnPremiseAspectEngine<? extends AspectData, ? extends AspectPropertyMetaData> engine) {
        String dataFile = "C:/test/tempFile.dat";
        String tempFile = "C:/test/dataFile.dat";
        //when(engine.getTempDataDirPath()).thenReturn("C:/test");

        AspectEngineDataFileDefault aspectDataFile =
            (AspectEngineDataFileDefault)engine.getDataFileMetaData();
        aspectDataFile.setTempDataFilePath(tempFile);
        aspectDataFile.getConfiguration().setDataFilePath(dataFile);

        Calendar calendar = Calendar.getInstance();
        calendar.set(2018, 5, 10, 12, 0, 0);

        // Configure file wrapper to return specified creation dates
        // for data files.
        when(fileWrapperFactory.getLastModified(eq(dataFile))).thenReturn(calendar.getTimeInMillis());
        when(fileWrapperFactory.getLastModified(eq(tempFile))).thenReturn(calendar.getTimeInMillis());
    }

    private void configureFileUpdate(OnPremiseAspectEngine<? extends AspectData, ? extends AspectPropertyMetaData> engine) {
        String dataFile = "C:/test/tempFile.dat";
        String tempFile = "C:/test/dataFile.dat";
        when(engine.getTempDataDirPath()).thenReturn("C:/test");
        AspectEngineDataFileDefault aspectDataFile =
            (AspectEngineDataFileDefault)engine.getDataFileMetaData();
        aspectDataFile.setTempDataFilePath(tempFile);
        aspectDataFile.getConfiguration().setDataFilePath(dataFile);

        Calendar calendar = Calendar.getInstance();
        calendar.set(2018, 5, 11, 12, 0, 0);
        Calendar tempCalendar = Calendar.getInstance();
        tempCalendar.set(2018, 5, 10, 12, 0, 0);

        // Configure file wrapper to return specified creation dates
        // for data files.
        when(fileWrapperFactory.getLastModified(eq(dataFile))).thenReturn(calendar.getTimeInMillis());
        when(fileWrapperFactory.getLastModified(eq(tempFile))).thenReturn(tempCalendar.getTimeInMillis());
    }

    private void configureHttpNoUpdateAvailable() throws IOException {
        // Configure the mock HTTP handler to return an 'NotModified'
        // status code.
        when(httpClientConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_NOT_MODIFIED);
        when(httpClientConnection.getResponseMessage()).thenReturn("<empty />");
    }

    private void configureHttpTooManyRequests() throws IOException {
        // Configure the mock HTTP handler to return a 429
        // 'TooManyRequests' status code.
        when(httpClientConnection.getResponseCode()).thenReturn(429);
        when(httpClientConnection.getResponseMessage()).thenReturn("<empty />");
    }
}
